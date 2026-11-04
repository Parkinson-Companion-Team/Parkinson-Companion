from django.test import TestCase
import cv2
import numpy as np
from ml_engine.write.preprocess import InvalidImageError, clean_and_threshold_image
from tempfile import TemporaryDirectory
from pathlib import Path


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
