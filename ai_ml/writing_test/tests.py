from django.test import TestCase
from django.contrib.auth.models import User
from django.test import override_settings
from django.urls import reverse
from rest_framework.authtoken.models import Token
from rest_framework.test import APIClient
from django.core.files.uploadedfile import SimpleUploadedFile
from unittest import skipUnless
import cv2
import numpy as np
from ml_engine.write.preprocess import InvalidImageError, clean_and_threshold_image
from tempfile import TemporaryDirectory
from pathlib import Path
from datetime import timedelta
from django.utils import timezone
from .models import DailyWritingTest, Patient
from .trends import build_history, euclidean, ewma, mean_embedding
import os


class PreprocessTests(TestCase):
    def _spiral(self):
        t = np.linspace(0, 6 * np.pi, 800)
        r = 15 * t
        points = np.stack([400 + r * np.cos(t), 400 + r * np.sin(t)], axis=1).astype(np.int32)
        image = np.full((800, 800), 255, np.uint8)
        cv2.polylines(image, [points], False, 0, 6)
        return image

    def _process(self, image, suffix=".png"):
        with TemporaryDirectory() as directory:
            path = Path(directory) / f"image{suffix}"
            cv2.imwrite(str(path), image)
            return np.asarray(clean_and_threshold_image(path))

    def test_black_spiral_on_white(self):
        output = self._process(self._spiral())
        self.assertEqual(output.shape, (224, 224))
        self.assertTrue(0.01 < (output > 0).mean() < 0.4)

    def test_inverted_drawing_agrees(self):
        expected = self._process(self._spiral())
        actual = self._process(255 - self._spiral())
        self.assertGreater((expected == actual).mean(), 0.97)

    def test_transparent_rgba_agrees(self):
        source = self._spiral()
        rgba = np.zeros((800, 800, 4), np.uint8)
        rgba[:, :, :3] = 0
        rgba[source < 128, :3] = 0
        rgba[source < 128, 3] = 255
        expected = self._process(source)
        actual = self._process(rgba)
        self.assertGreater((expected == actual).mean(), 0.97)

    def test_garbage_png_raises(self):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "x.png"
            path.write_bytes(b"not an image")
            with self.assertRaises(InvalidImageError):
                clean_and_threshold_image(path)


class TrendsTests(TestCase):
    def test_helpers_and_history(self):
        self.assertEqual(ewma([100, 0], 0.5), [100, 50.0])
        self.assertEqual(ewma([None, 10]), [None, 10])
        self.assertEqual(mean_embedding([[0, 0], [2, 2]]), [1.0, 1.0])
        self.assertEqual(euclidean([0, 0], [3, 4]), 5.0)
        now = timezone.now()
        rows = [
            {"id": i, "created_at": now + timedelta(days=i), "stability_score": 80, "distance": 0.1, "embedding": [x, 0]}
            for i, x in enumerate([0, 0, 0, 1, 2])
        ]
        result = build_history(rows)
        self.assertTrue(result["baseline_ready"])
        self.assertEqual([p["personal_distance"] for p in result["points"]], [0.0, 0.0, 0.0, 1.0, 2.0])

    def test_incomplete_baseline(self):
        now = timezone.now()
        rows = [{"id": i, "created_at": now, "stability_score": 80, "distance": 0.1, "embedding": [0, 0]} for i in range(2)]
        result = build_history(rows)
        self.assertFalse(result["baseline_ready"])
        self.assertTrue(all(point["personal_distance"] is None for point in result["points"]))


class EndpointTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user("writer", password="password")
        self.client = APIClient()

    def test_requires_authentication(self):
        response = self.client.post(reverse("analyze_writing_test"), {"test_type": "spiral"})
        self.assertEqual(response.status_code, 401)

    def test_invalid_uploads(self):
        token = Token.objects.create(user=self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")
        self.assertEqual(self.client.post(reverse("analyze_writing_test"), {"test_type": "banana"}).status_code, 400)
        self.assertEqual(self.client.post(reverse("analyze_writing_test"), {"test_type": "spiral"}).status_code, 400)
        response = self.client.post(
            reverse("analyze_writing_test"),
            {"test_type": "spiral", "uploaded_file": SimpleUploadedFile("x.jpg", b"not an image")},
        )
        self.assertEqual(response.status_code, 400)
        response = self.client.post(
            reverse("analyze_writing_test"),
            {"test_type": "spiral", "uploaded_file": SimpleUploadedFile("x.jpg", b"x" * (11 * 1024 * 1024))},
        )
        self.assertEqual(response.status_code, 413)


class HistoryEndpointTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user("history", password="password")
        self.other = User.objects.create_user("other", password="password")
        self.token = Token.objects.create(user=self.user)
        self.client = APIClient()
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {self.token.key}")

    def test_filters_to_user_and_days(self):
        patient = Patient.objects.create(user=self.user)
        other_patient = Patient.objects.create(user=self.other)
        for owner in (patient, other_patient):
            DailyWritingTest.objects.create(
                patient=owner, test_type="spiral", test_image="x.png",
                stability_score=80, distance=0.2, embedding=[0] * 128, model_version="test",
            )
        response = self.client.get(reverse("writing_history"), {"test_type": "spiral"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data["points"]), 1)
        DailyWritingTest.objects.filter(patient=patient).update(created_at=timezone.now() - timedelta(days=3))
        self.assertLess(
            len(self.client.get(reverse("writing_history"), {"test_type": "spiral", "days": 1}).data["points"]),
            len(response.data["points"]),
        )


class AccountEndpointTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    def test_guest_token_works_for_history(self):
        response = self.client.post("/guest/", {}, format="json")
        self.assertEqual(response.status_code, 201)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {response.data['token']}")
        history = self.client.get(reverse("writing_history"), {"test_type": "spiral"})
        self.assertEqual(history.status_code, 200)

    def test_register_creates_user_and_patient(self):
        response = self.client.post(
            "/register/", {"email": "Person@Example.com", "password": "secret1"}, format="json"
        )
        self.assertEqual(response.status_code, 201)
        user = User.objects.get(username="person@example.com")
        self.assertTrue(Patient.objects.filter(user=user).exists())
        self.assertTrue(user.has_usable_password())

    def test_register_converts_guest_user(self):
        guest_response = self.client.post("/guest/", {}, format="json")
        guest_token = guest_response.data["token"]
        guest_user = User.objects.get(username__startswith="guest_")
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {guest_token}")
        response = self.client.post(
            "/register/", {"email": "converted@example.com", "password": "secret1"}, format="json"
        )
        self.assertEqual(response.status_code, 201)
        guest_user.refresh_from_db()
        self.assertEqual(guest_user.id, User.objects.get(username="converted@example.com").id)
        self.assertTrue(Patient.objects.filter(user=guest_user).exists())

    def test_register_duplicate_email_returns_conflict(self):
        self.client.post("/register/", {"email": "duplicate@example.com", "password": "secret1"}, format="json")
        response = self.client.post(
            "/register/", {"email": "DUPLICATE@example.com", "password": "secret2"}, format="json"
        )
        self.assertEqual(response.status_code, 409)
