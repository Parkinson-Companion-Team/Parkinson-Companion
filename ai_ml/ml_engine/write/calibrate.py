import os
import json
import argparse
import statistics
import torch

from ml_engine.write.dataset import ParkinsonSiameseDataset, split_patients
from ml_engine.write.architecture import SiameseParkinsonNetwork
from ml_engine.write.evaluate import embed_all
from ml_engine.write.train import DATA_DIR, WEIGHTS_DIR

TARGET_HEALTHY = 90.0   # score a typical held-out healthy drawing should get
TARGET_PD = 35.0        # score a typical held-out PD drawing should get


def make_score_fn(d_healthy, d_pd):
    """Two-point exponential curve: d_healthy -> 90, d_pd -> 35, clamped to 0..100."""
    ratio = TARGET_PD / TARGET_HEALTHY
    def score(distance):
        exponent = (distance - d_healthy) / (d_pd - d_healthy)
        return max(0.0, min(100.0, TARGET_HEALTHY * (ratio ** exponent)))
    return score


def calibrate(kind):
    tr, va = split_patients(DATA_DIR, kind)
    net = SiameseParkinsonNetwork()
    net.load_state_dict(torch.load(os.path.join(WEIGHTS_DIR, f"siamese_{kind}.pth"), map_location="cpu"))
    net.eval()

    E_tr, y_tr = embed_all(net, ParkinsonSiameseDataset(DATA_DIR, kind, patient_ids=tr))
    E_va, y_va = embed_all(net, ParkinsonSiameseDataset(DATA_DIR, kind, patient_ids=va))

    anchor = E_tr[y_tr == 0].mean(dim=0)            # healthy centroid from TRAIN patients only
    d_va = torch.norm(E_va - anchor, dim=1)         # held-out distances to that anchor
    d_healthy = statistics.median(d_va[y_va == 0].tolist())
    d_pd = statistics.median(d_va[y_va == 1].tolist())

    score = make_score_fn(d_healthy, d_pd)
    print(f"{kind}: held-out median distance  healthy {d_healthy:.3f} | PD {d_pd:.3f}")
    print(f"score at anchor (d=0): {score(0.0):.1f} | healthy median: {score(d_healthy):.1f} | PD median: {score(d_pd):.1f}")
    s_h = [score(d) for d in d_va[y_va == 0].tolist()]
    s_p = [score(d) for d in d_va[y_va == 1].tolist()]
    print(f"mean score  healthy {sum(s_h)/len(s_h):.1f} | PD {sum(s_p)/len(s_p):.1f}")

    out = os.path.join(WEIGHTS_DIR, f"calibration_{kind}.json")
    with open(out, "w") as f:
        json.dump({"anchor": anchor.tolist(), "d_healthy": d_healthy, "d_pd": d_pd,
                   "target_healthy": TARGET_HEALTHY, "target_pd": TARGET_PD}, f)
    print("saved:", out)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--type', required=True)
    calibrate(parser.parse_args().type)