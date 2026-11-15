import os

import torch
from sklearn.metrics import roc_auc_score

from .buildPairs import load_split
from .model import SiameseVoiceNet
from .preprocess import preprocess_audio


VOICE_DIR = os.path.dirname(os.path.abspath(__file__))
WEIGHTS_PATH = os.path.join(VOICE_DIR, "weights", "voice_model.pth")


def load_model(weights_path: str = WEIGHTS_PATH) -> SiameseVoiceNet:
    model = SiameseVoiceNet(embedding_dim=128)
    model.load_state_dict(torch.load(weights_path, map_location="cpu"))
    model.eval()
    return model


def embed_files(model, files: list) -> torch.Tensor:
    embeddings = []
    with torch.no_grad():
        for path in files:
            mel = preprocess_audio(path).unsqueeze(0)
            embeddings.append(model.embed(mel).squeeze(0))
    if not embeddings:
        raise ValueError("No WAV files were available for evaluation.")
    return torch.stack(embeddings)


def evaluate(weights_path: str = WEIGHTS_PATH) -> dict:
    split = load_split()
    model = load_model(weights_path)
    train_healthy = embed_files(model, split["healthy_train"])
    heldout_healthy = embed_files(model, split["healthy_val"])
    heldout_pd = embed_files(model, split["pd_val"])
    anchor = train_healthy.mean(dim=0)

    healthy_distances = torch.norm(heldout_healthy - anchor, dim=1)
    pd_distances = torch.norm(heldout_pd - anchor, dim=1)
    distances = torch.cat((healthy_distances, pd_distances))
    labels = [0] * len(healthy_distances) + [1] * len(pd_distances)
    auc = roc_auc_score(labels, distances.numpy())

    print(f"Held-out files used: {len(healthy_distances) + len(pd_distances)}")
    print(f"Mean healthy-anchor distance: {healthy_distances.mean().item():.4f}")
    print(f"Mean PD-anchor distance: {pd_distances.mean().item():.4f}")
    print(f"ROC AUC: {auc:.4f}")
    return {
        "anchor": anchor,
        "healthy_distances": healthy_distances,
        "pd_distances": pd_distances,
        "auc": auc,
    }


if __name__ == "__main__":
    evaluate()
