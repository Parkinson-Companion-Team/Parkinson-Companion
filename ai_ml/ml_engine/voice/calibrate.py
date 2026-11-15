import json
import os
import statistics

from .evaluate import WEIGHTS_PATH, evaluate


VOICE_DIR = os.path.dirname(os.path.abspath(__file__))
CALIBRATION_PATH = os.path.join(VOICE_DIR, "weights", "voice_calibration.json")
TARGET_HEALTHY = 90.0
TARGET_PD = 35.0


def calibrate(weights_path: str = WEIGHTS_PATH) -> bool:
    metrics = evaluate(weights_path)
    d_healthy = statistics.median(metrics["healthy_distances"].tolist())
    d_pd = statistics.median(metrics["pd_distances"].tolist())
    print(f"Median healthy-anchor distance: {d_healthy:.4f}")
    print(f"Median PD-anchor distance: {d_pd:.4f}")

    if d_pd - d_healthy < 0.1:
        print("WARNING: calibration refused because d_pd - d_healthy is less than 0.1.")
        return False
    if metrics["auc"] < 0.65:
        print("WARNING: calibration refused because ROC AUC is below 0.65.")
        return False

    with open(CALIBRATION_PATH, "w", encoding="utf-8") as file:
        json.dump(
            {
                "anchor": metrics["anchor"].tolist(),
                "d_healthy": d_healthy,
                "d_pd": d_pd,
                "target_healthy": TARGET_HEALTHY,
                "target_pd": TARGET_PD,
            },
            file,
            indent=2,
        )
    print(f"Saved calibration: {CALIBRATION_PATH}")
    return True


if __name__ == "__main__":
    calibrate()
