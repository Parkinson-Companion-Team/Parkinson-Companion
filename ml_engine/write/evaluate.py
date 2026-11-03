import os
import argparse
import torch
from sklearn.metrics import roc_auc_score

from ml_engine.write.dataset import ParkinsonSiameseDataset, split_patients
from ml_engine.write.architecture import SiameseParkinsonNetwork
from ml_engine.write.train import DATA_DIR, WEIGHTS_DIR


def embed_all(net, ds):
    """TAKES: the network and a dataset. RETURNS: (embeddings [N,128], is_pd [N] as 0/1)."""
    net.eval()
    with torch.no_grad():
        E = torch.stack([net.twin_cnn(ds.images[i].unsqueeze(0)).squeeze(0) for i in range(len(ds))])
    is_pd = torch.tensor([1 if d['CLASS_TYPE'] == 2 else 0 for d in ds.data])
    return E, is_pd


def evaluate(kind):
    tr, va = split_patients(DATA_DIR, kind)
    net = SiameseParkinsonNetwork()
    net.load_state_dict(torch.load(os.path.join(WEIGHTS_DIR, f"siamese_{kind}.pth"), map_location="cpu"))

    E_tr, y_tr = embed_all(net, ParkinsonSiameseDataset(DATA_DIR, kind, patient_ids=tr))
    E_va, y_va = embed_all(net, ParkinsonSiameseDataset(DATA_DIR, kind, patient_ids=va))

    anchor = E_tr[y_tr == 0].mean(dim=0)   # healthy centroid, built from TRAIN patients only

    for name, E, y in (("TRAIN", E_tr, y_tr), ("VAL", E_va, y_va)):
        d = torch.norm(E - anchor, dim=1)  # distance of every image to the anchor
        auc = roc_auc_score(y.numpy(), d.numpy())
        print(f"{kind} {name}: healthy dist {d[y == 0].mean():.3f} | PD dist {d[y == 1].mean():.3f} | AUC {auc:.3f}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--type', required=True)
    evaluate(parser.parse_args().type)