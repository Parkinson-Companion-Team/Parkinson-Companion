import tempfile
from pathlib import Path

import cv2
import numpy as np
from django.test import SimpleTestCase

from ml_engine.write.predictor import distance_to_stability
from ml_engine.write.preprocess import clean_and_threshold_image


class WritingPreprocessTests(SimpleTestCase):
    def test_preprocess_preserves_canvas_as_square(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "drawing.png"
            image = np.zeros((80, 240), dtype=np.uint8)
            cv2.line(image, (10, 40), (230, 40), 255, 3)
            cv2.imwrite(str(path), image)

            processed = clean_and_threshold_image(path)

            self.assertEqual(processed.size, (224, 224))
            self.assertEqual(processed.mode, "L")


class WritingCalibrationTests(SimpleTestCase):
    def test_distance_to_stability_is_clamped_and_rounded(self):
        calibration = {
            "d_healthy": 0.5,
            "d_pd": 1.0,
            "target_healthy": 90.0,
            "target_pd": 35.0,
        }

        self.assertEqual(distance_to_stability(0.5, calibration), 90.0)
        self.assertEqual(distance_to_stability(1.0, calibration), 35.0)
        self.assertEqual(distance_to_stability(-10, calibration), 100.0)

    def test_invalid_calibration_is_rejected(self):
        with self.assertRaises(ValueError):
            distance_to_stability(
                0.5,
                {
                    "d_healthy": 1.0,
                    "d_pd": 1.0,
                    "target_healthy": 90.0,
                    "target_pd": 35.0,
                },
            )
