import argparse
import os
import random

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import DataLoader, Dataset

from .buildPairs import build_train_val_pairs
from .model import SiameseVoiceNet
from .preprocess import preprocess_audio


VOICE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.normpath(os.path.join(VOICE_DIR, "..", "data"))
WEIGHTS_DIR = os.path.join(VOICE_DIR, "weights")
DEFAULT_WEIGHTS_PATH = os.path.join(WEIGHTS_DIR, "voice_model.pth")


class ContrastiveLoss(nn.Module):
    def __init__(self, margin: float = 1.0):
        super().__init__()
        self.margin = margin

    def forward(self, emb1, emb2, label):
        distance = torch.norm(emb1 - emb2, p=2, dim=1)
        similar_loss = label * distance.pow(2)
        different_loss = (1 - label) * torch.clamp(self.margin - distance, min=0).pow(2)
        return (similar_loss + different_loss).mean()


class VoicePairDataset(Dataset):
    def __init__(self, pairs: list, cache: dict):
        self.pairs = pairs
        self.cache = cache

    def __len__(self):
        return len(self.pairs)

    def __getitem__(self, idx):
        path1, path2, label = self.pairs[idx]
        if path1 not in self.cache:
            self.cache[path1] = preprocess_audio(path1)
        if path2 not in self.cache:
            self.cache[path2] = preprocess_audio(path2)
        return self.cache[path1], self.cache[path2], torch.tensor(label, dtype=torch.float32)


def collate_fn(batch):
    mels1, mels2, labels = zip(*batch)
    max_len = max(mel.shape[-1] for mel in mels1 + mels2)

    def pad(mel):
        pad_amount = max_len - mel.shape[-1]
        return F.pad(mel, (0, pad_amount), value=mel.amin().item())

    return (
        torch.stack([pad(mel) for mel in mels1]),
        torch.stack([pad(mel) for mel in mels2]),
        torch.stack(labels),
    )


def run_epoch(model, loader, criterion, optimizer=None):
    is_training = optimizer is not None
    model.train() if is_training else model.eval()
    total_loss = 0.0
    similar_distances = []
    different_distances = []
    context = torch.enable_grad() if is_training else torch.no_grad()

    with context:
        for mel1, mel2, label in loader:
            if is_training:
                optimizer.zero_grad()
            emb1, emb2 = model(mel1, mel2)
            loss = criterion(emb1, emb2, label)
            if is_training:
                loss.backward()
                optimizer.step()

            distances = F.pairwise_distance(emb1, emb2)
            similar_distances.extend(distances[label == 1].tolist())
            different_distances.extend(distances[label == 0].tolist())
            total_loss += loss.item()

    mean = lambda values: sum(values) / len(values) if values else float("nan")
    return total_loss / len(loader), mean(similar_distances), mean(different_distances)


def train(
    train_pairs: list,
    val_pairs: list,
    epochs: int = 40,
    batch_size: int = 8,
    lr: float = 1e-4,
    weights_path: str = DEFAULT_WEIGHTS_PATH,
):
    cache = {}
    train_loader = DataLoader(
        VoicePairDataset(train_pairs, cache), batch_size=batch_size, shuffle=True, collate_fn=collate_fn
    )
    val_loader = DataLoader(
        VoicePairDataset(val_pairs, cache), batch_size=batch_size, shuffle=False, collate_fn=collate_fn
    )
    model = SiameseVoiceNet(embedding_dim=128)
    criterion = ContrastiveLoss(margin=1.0)
    optimizer = torch.optim.Adam(model.parameters(), lr=lr)
    best_val_loss = float("inf")
    epochs_without_improvement = 0

    for epoch in range(epochs):
        train_loss, _, _ = run_epoch(model, train_loader, criterion, optimizer)
        val_loss, similar_distance, different_distance = run_epoch(
            model, val_loader, criterion
        )
        print(
            f"Epoch {epoch + 1}/{epochs} - train loss {train_loss:.4f} - "
            f"val loss {val_loss:.4f} - similar distance {similar_distance:.4f} - "
            f"different distance {different_distance:.4f}"
        )

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            epochs_without_improvement = 0
            os.makedirs(os.path.dirname(weights_path), exist_ok=True)
            torch.save(model.state_dict(), weights_path)
            print(f"  saved best checkpoint: {weights_path}")
        else:
            epochs_without_improvement += 1
            if epochs_without_improvement >= 8:
                print("Early stopping after 8 epochs without validation improvement.")
                break


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--epochs", type=int, default=40)
    parser.add_argument("--train-pairs", type=int, default=800)
    parser.add_argument("--val-pairs", type=int, default=200)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--lr", type=float, default=1e-4)
    parser.add_argument("--weights-path", default=DEFAULT_WEIGHTS_PATH)
    args = parser.parse_args()

    random.seed(42)
    torch.manual_seed(42)
    train_pairs, val_pairs = build_train_val_pairs(
        healthy_dir=os.path.join(DATA_DIR, "raw", "healthy_voice"),
        pd_dir=os.path.join(DATA_DIR, "raw", "parkinsons_voice"),
        train_pairs_count=args.train_pairs,
        test_pairs_count=args.val_pairs,
    )
    train(
        train_pairs,
        val_pairs,
        epochs=args.epochs,
        batch_size=args.batch_size,
        lr=args.lr,
        weights_path=args.weights_path,
    )
