import json
import os
import random
import re


VOICE_DIR = os.path.dirname(os.path.abspath(__file__))
WEIGHTS_DIR = os.path.join(VOICE_DIR, "weights")
SPLIT_PATH = os.path.join(WEIGHTS_DIR, "split.json")
SUBJECT_PATTERN = re.compile(r"(healthy_\d+|parkinsons_\d+)")


def list_wav_files(directory: str) -> list:
    return sorted(
        os.path.join(directory, name)
        for name in os.listdir(directory)
        if name.lower().endswith(".wav")
    )


def subject_id(path: str) -> str:
    match = SUBJECT_PATTERN.search(os.path.basename(path))
    if not match:
        raise ValueError(f"Could not find a subject id in WAV filename: {path}")
    return match.group(1)


def split_by_subject(files: list, val_ratio: float = 0.2, seed: int = 42) -> tuple:
    subjects = {}
    for path in files:
        subjects.setdefault(subject_id(path), []).append(path)
    subject_names = sorted(subjects)
    random.Random(seed).shuffle(subject_names)
    split_idx = int(len(subject_names) * (1 - val_ratio))
    train_subjects = subject_names[:split_idx]
    val_subjects = subject_names[split_idx:]
    train_files = [path for name in train_subjects for path in subjects[name]]
    val_files = [path for name in val_subjects for path in subjects[name]]
    return sorted(train_files), sorted(val_files)


def generate_pairs(healthy_files: list, pd_files: list, num_pairs: int) -> list:
    if len(healthy_files) < 2 or not pd_files:
        raise ValueError("Both classes need enough WAV files to generate pairs.")
    pairs = []
    for _ in range(num_pairs // 2):
        healthy_a, healthy_b = random.sample(healthy_files, 2)
        pairs.append((healthy_a, healthy_b, 1))
        pairs.append((random.choice(healthy_files), random.choice(pd_files), 0))
    if num_pairs % 2:
        pairs.append((random.choice(healthy_files), random.choice(pd_files), 0))
    random.shuffle(pairs)
    return pairs


def _relative_paths(paths: list) -> list:
    return [os.path.relpath(path, VOICE_DIR) for path in paths]


def _absolute_paths(paths: list) -> list:
    return [os.path.normpath(os.path.join(VOICE_DIR, path)) for path in paths]


def save_split(healthy_train, healthy_val, pd_train, pd_val) -> None:
    os.makedirs(WEIGHTS_DIR, exist_ok=True)
    with open(SPLIT_PATH, "w", encoding="utf-8") as file:
        json.dump(
            {
                "seed": 42,
                "healthy_train": _relative_paths(healthy_train),
                "healthy_val": _relative_paths(healthy_val),
                "pd_train": _relative_paths(pd_train),
                "pd_val": _relative_paths(pd_val),
            },
            file,
            indent=2,
        )


def load_split() -> dict:
    if not os.path.exists(SPLIT_PATH):
        raise FileNotFoundError(
            f"Missing {SPLIT_PATH}. Run buildPairs.py or train.py first to create the held-out split."
        )
    with open(SPLIT_PATH, encoding="utf-8") as file:
        split = json.load(file)
    return {key: _absolute_paths(value) for key, value in split.items() if key != "seed"}


def build_train_val_pairs(
    healthy_dir: str,
    pd_dir: str,
    train_pairs_count: int = 800,
    test_pairs_count: int = 200,
    test_ratio: float = 0.2,
) -> tuple:
    healthy_files = list_wav_files(healthy_dir)
    pd_files = list_wav_files(pd_dir)
    healthy_train, healthy_val = split_by_subject(healthy_files, test_ratio, seed=42)
    pd_train, pd_val = split_by_subject(pd_files, test_ratio, seed=42)
    save_split(healthy_train, healthy_val, pd_train, pd_val)

    print(f"Healthy: {len(healthy_train)} train files, {len(healthy_val)} validation files")
    print(f"PD: {len(pd_train)} train files, {len(pd_val)} validation files")
    random.seed(42)
    train_pairs = generate_pairs(healthy_train, pd_train, train_pairs_count)
    val_pairs = generate_pairs(healthy_val, pd_val, test_pairs_count)
    return train_pairs, val_pairs


if __name__ == "__main__":
    train_pairs, val_pairs = build_train_val_pairs(
        healthy_dir=os.path.join(VOICE_DIR, "..", "data", "voice", "healthy_voice"),
        pd_dir=os.path.join(VOICE_DIR, "..", "data", "voice", "parkinsons_voice"),
    )
    print(f"Train pairs: {len(train_pairs)}, Validation pairs: {len(val_pairs)}")
