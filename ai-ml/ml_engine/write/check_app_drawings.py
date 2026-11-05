import argparse
import json
import os
from pathlib import Path

import numpy as np

from ml_engine.write.predictor import WEIGHTS_DIR, analyze_drawing
from ml_engine.write.preprocess import clean_and_threshold_image


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--type", required=True, choices=("spiral", "meander"))
    parser.add_argument("--folder", required=True)
    parser.add_argument("--save-debug", action="store_true")
    args = parser.parse_args()

    calibration_path = os.path.join(WEIGHTS_DIR, f"calibration_{args.type}.json")
    with open(calibration_path) as handle:
        calibration = json.load(handle)
    rows = []
    for path in sorted(Path(args.folder).iterdir()):
        if path.name.startswith("._") or path.suffix.lower() not in {".png", ".jpg", ".jpeg"}:
            continue
        result = analyze_drawing(str(path), args.type)
        rows.append(result)
        print(f"{path.name}\t{result['distance']}\t{result['stability_score']}")
        if args.save_debug:
            debug_dir = Path("debug_app")
            debug_dir.mkdir(exist_ok=True)
            clean_and_threshold_image(path).save(debug_dir / f"{path.stem}.png")

    scores = [row["stability_score"] for row in rows]
    distances = [row["distance"] for row in rows]
    print("summary")
    print(f"n\t{len(rows)}")
    if not rows:
        return
    median_distance = float(np.median(distances))
    position = (median_distance - calibration["d_healthy"]) / (
        calibration["d_pd"] - calibration["d_healthy"]
    ) * 100
    print(f"mean_score\t{float(np.mean(scores))}")
    print(f"median_score\t{float(np.median(scores))}")
    print(f"min_score\t{float(np.min(scores))}")
    print(f"median_distance_position_percent\t{position}")


if __name__ == "__main__":
    main()
