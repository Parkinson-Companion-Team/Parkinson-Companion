import argparse
import json
import os
import random

import numpy as np
import torch
from sklearn.metrics import roc_auc_score
from torch.utils.data import DataLoader

from ml_engine.write.architecture import SiameseParkinsonNetwork
from ml_engine.write.dataset import ParkinsonSiameseDataset, split_patients
from ml_engine.write.evaluate import embed_all
from ml_engine.write.train import DATA_DIR, ContrastiveLoss, validate


def _folds(groups, count, seed):
    values = list(groups)
    random.Random(seed).shuffle(values)
    return [set(chunk) for chunk in np.array_split(values, count)]


def _auc(net, train_ds, val_ds):
    train_embeddings, train_labels = embed_all(net, train_ds)
    val_embeddings, val_labels = embed_all(net, val_ds)
    anchor = train_embeddings[train_labels == 0].mean(dim=0)
    distances = torch.norm(val_embeddings - anchor, dim=1).numpy()
    return float(roc_auc_score(val_labels.numpy(), distances))


def run(test_type, folds=5, epochs=40, seed=42, norm="batch"):
    all_ds = ParkinsonSiameseDataset(DATA_DIR, test_type, patient_ids=None)
    healthy = {row["ID_PATIENT"] for row in all_ds.data if row["CLASS_TYPE"] == 1}
    parkinson = {row["ID_PATIENT"] for row in all_ds.data if row["CLASS_TYPE"] == 2}
    healthy_folds = _folds(healthy, folds, seed)
    parkinson_folds = _folds(parkinson, folds, seed + 1)
    reports = []
    for fold in range(folds):
        val_ids = healthy_folds[fold] | parkinson_folds[fold]
        train_ids = (healthy | parkinson) - val_ids
        train_ds = ParkinsonSiameseDataset(DATA_DIR, test_type, patient_ids=train_ids)
        val_ds = ParkinsonSiameseDataset(DATA_DIR, test_type, patient_ids=val_ids, fixed_pairs=True)
        train_loader = DataLoader(train_ds, batch_size=16, shuffle=True, num_workers=0)
        model = SiameseParkinsonNetwork(norm=norm)
        criterion = ContrastiveLoss(margin=1.0)
        optimizer = torch.optim.Adam(model.parameters(), lr=0.0005)
        checked = []
        for epoch in range(1, epochs + 1):
            model.train()
            for image_a, image_b, label in train_loader:
                optimizer.zero_grad()
                output_a, output_b = model(image_a, image_b)
                criterion(output_a, output_b, label).backward()
                optimizer.step()
            if epoch % 5 == 0 or epoch == epochs:
                checked.append(_auc(model, train_ds, val_ds))
        report = {
            "fold": fold + 1,
            "train_groups": len(train_ids),
            "validation_groups": len(val_ids),
            "train_images": len(train_ds),
            "validation_images": len(val_ds),
            "final_epoch_auc": checked[-1],
            "best_checked_auc_optimistic": max(checked),
        }
        reports.append(report)
        print(report)
    values = [report["final_epoch_auc"] for report in reports]
    print(f"mean +/- std: {np.mean(values):.3f} +/- {np.std(values):.3f}")
    output_dir = os.path.join(os.path.dirname(__file__), "reports")
    os.makedirs(output_dir, exist_ok=True)
    with open(os.path.join(output_dir, f"crossval_{test_type}.json"), "w") as handle:
        json.dump({"type": test_type, "folds": reports}, handle, indent=2)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--type", required=True, choices=("spiral", "meander"))
    parser.add_argument("--folds", type=int, default=5)
    parser.add_argument("--epochs", type=int, default=40)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--norm", choices=("batch", "group"), default="batch")
    args = parser.parse_args()
    torch.manual_seed(args.seed)
    run(args.type, args.folds, args.epochs, args.seed, args.norm)
