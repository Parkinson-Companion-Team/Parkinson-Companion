from django.test import TestCase

# Create your tests here.
from django.contrib.auth.models import User
from django.db import IntegrityError
from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from writing_test.models import Patient

from ml_engine.voice.anchor import get_recent_mean_embedding
from .models import PatientVoiceBaseline, VoiceTestResult


class VoiceBaselineTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user("voice", password="password")
        self.patient = Patient.objects.create(user=self.user)

    def test_baselines_are_scoped_by_task(self):
        PatientVoiceBaseline.objects.create(patient=self.patient, task_type="vowel", embedding=[0], model_version="test")
        PatientVoiceBaseline.objects.create(patient=self.patient, task_type="ddk", embedding=[0], model_version="test")
        with self.assertRaises(IntegrityError):
            PatientVoiceBaseline.objects.create(patient=self.patient, task_type="vowel", embedding=[0], model_version="test")

    def test_recent_mean_filters_task(self):
        VoiceTestResult.objects.create(patient=self.patient, task_type="vowel", stability_score=100, embedding=[1, 1], model_version="test")
        VoiceTestResult.objects.create(patient=self.patient, task_type="ddk", stability_score=100, embedding=[9, 9], model_version="test")
        self.assertEqual(get_recent_mean_embedding(self.patient, "vowel").tolist(), [1.0, 1.0])
        self.assertIsNone(get_recent_mean_embedding(self.patient, "reading"))


class VoiceEndpointTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user("voice-api", password="password")
        self.client = APIClient()

    def test_validation(self):
        url = "/voice-test/upload/"
        self.assertEqual(self.client.post(url).status_code, 401)
        token = Token.objects.create(user=self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")
        self.assertEqual(self.client.post(url, {"task_type": "vowel"}).status_code, 400)
        self.assertEqual(self.client.post(url, {"task_type": "invalid"}).status_code, 400)
