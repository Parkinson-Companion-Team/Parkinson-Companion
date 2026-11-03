"""
=============================================================================
LOGIC FLOWCHART: ParkinsonSiameseDataset  (upgraded)
=============================================================================
[ INIT PHASE (Preparing the Dataset) ]
Input: Directory Paths (img_dir, subfolder), patient_ids, fixed_pairs, cache
     |
     v
Scan 'healthy...' (1) & 'patient...' (2) subfolders   (e.g. healthySpiral, patientSpiral)
     |--> Skip hidden/junk folders (__MACOSX) and junk files (._xxx.jpg)
     |--> Read filenames, extract ID_PATIENT  (sp4-P19.jpg -> "P19") & CLASS_TYPE
     |--> If patient_ids is given, keep ONLY those patients (train/val split)
     |--> Store FULL_PATH and metadata in self.data list
     |--> Preprocess each image ONCE (Otsu threshold -> 224x224 -> tensor)
     |    and keep the tensors in RAM (cache), so training never re-reads the disk
     |--> Separate indices into healthy_indices & parkinson_indices
     |--> Extract unique patients into healthy_patients & parkinson_patients
     |--> Precompute, for every image, its valid same-class partners
     |    (same class, DIFFERENT patient)  -> no endless loops possible
     v
Dataset Ready (Length = Total Images kept)

[ GET ITEM PHASE (Fetching Pairs during Training) ]
Input: idx (Integer)
     |
1. Fetch Anchor Image metadata (Image A) at idx
     |
2. Flip a coin (is_same_class? True/False)
     |    (training:   coin and partner are random -> new pairs every epoch)
     |    (validation: fixed_pairs=True -> same pairs every epoch, fair comparison)
     |--> [True: POSITIVE PAIR]
     |      |--> Pick random partner from the precomputed same-class list
     |      |--> (list already guarantees a different ID_PATIENT)
     |      |--> Label = 0.0 (Meaning "Distance is 0 / Similar")
     |
     |--> [False: NEGATIVE PAIR]
     |      |--> Pick random image from DIFFERENT class pool
     |      |--> Label = 1.0 (Meaning "Distance is 1 / Dissimilar")
     |
3. Take both images from the RAM cache (or load from disk if cache=False)
     |
Output: img_a (Tensor), img_b (Tensor), label (Tensor)

[ SPLIT HELPER: split_patients() ]
Splits PATIENTS (not images) into train and validation, separately for healthy
and Parkinson's, so all 4 images of one person always stay on the same side.
=============================================================================
"""

import os
import random
import torch
from torch.utils.data import Dataset
import hashlib
import torchvision.transforms as transforms
from ml_engine.write.preprocess import clean_and_threshold_image
def build_patient_groups(folders):
    """
    Some drawings are byte-identical copies filed under different patient ids
    (e.g. P1, P4 and P25 share the same images). Those ids are really one recording,
    so we link them into one group; splitting and pairing by group means a copy
    can never leak between train and validation.
    TAKES: folders (dict: class -> folder path).
    RETURNS: dict patient_id -> group id (the smallest id in the group).
    """
    parent = {}  # union-find: each id points towards its group's main id

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    first_owner = {}  # file hash -> first patient id that owned this exact image
    for folder in folders.values():
        for f in sorted(os.listdir(folder)):
            if not f.lower().endswith(('.jpg', '.png')) or f.startswith('._') or '-' not in f:
                continue
            pid = f.split('-')[-1].split('.')[0]
            parent.setdefault(pid, pid)
            with open(os.path.join(folder, f), 'rb') as fh:
                h = hashlib.md5(fh.read()).hexdigest()
            if h in first_owner:
                ra, rb = find(pid), find(first_owner[h])
                if ra != rb:
                    parent[ra] = rb
            else:
                first_owner[h] = pid
    return {p: find(p) for p in parent}

class ParkinsonSiameseDataset(Dataset):
    def __init__(self, img_dir, subfolder="", transform=None,
                 patient_ids=None, fixed_pairs=False, seed=42, cache=True):
        # os.path.join():
        # TAKES: Two or more strings representing folder/file names.
        # RETURNS: A single concatenated string with correct OS-specific slashes (e.g., 'data/raw\spiral').
        self.base_dir = os.path.join(img_dir, subfolder)
        self.patient_ids = patient_ids      # None = use everyone, or a set like {"H1", "P4"}
        self.fixed_pairs = fixed_pairs      # True for validation: same pairs every epoch
        self.seed = seed                    # makes the fixed validation pairs repeatable
        self.cache = cache                  # True = preprocess every image once and keep it in RAM

        # The image is already grayscale + 224x224 after clean_and_threshold_image(),
        # so we only convert to a tensor and scale pixel values to the range -1..1.
        # (Moved above the scan, because caching needs the transform while scanning.)
        self.transform = transform if transform else transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.5], std=[0.5])
        ])

        # os.path.exists():
        # TAKES: A string representing a file or directory path.
        # RETURNS: A boolean (True if the path exists on the hard drive, False otherwise).
        if not os.path.exists(self.base_dir):
            raise FileNotFoundError(f"Dataset folder not found: {self.base_dir}")

        # Define the paths to the sub-subfolders you created
        # Folders are found by their name prefix, so "healthySpiral" and "patientSpiral" both work.
        self.folders_to_scan = {}
        for name in sorted(os.listdir(self.base_dir)):
            full = os.path.join(self.base_dir, name)
            if not os.path.isdir(full) or name.startswith(('_', '.')):
                continue  # skips __MACOSX and hidden folders
            if name.lower().startswith('healthy'):
                self.folders_to_scan[1] = full
            elif name.lower().startswith('patient'):
                self.folders_to_scan[2] = full

        if 1 not in self.folders_to_scan or 2 not in self.folders_to_scan:
            raise FileNotFoundError(
                f"Need a 'healthy...' and a 'patient...' folder inside {self.base_dir}, "
                f"found: {sorted(self.folders_to_scan.values())}"
            )
                # ids that share identical images become ONE patient group
        self.group_of = build_patient_groups(self.folders_to_scan)

        self.data = []
        self.healthy_indices = []
        self.parkinson_indices = []
        self.images = []  # cached tensors, same order as self.data

        # Scan both directories
        idx = 0

        # dict.items():
        # TAKES: Nothing (called on a dictionary).
        # RETURNS: A view object that yields (key, value) tuple pairs for iteration.
        for class_type, folder_path in self.folders_to_scan.items():

            # os.listdir():
            # TAKES: A string representing a directory path.
            # RETURNS: A list of strings containing the names of the files in that directory.

            # str.endswith():
            # TAKES: A string or a tuple of strings (like ('.jpg', '.png')).
            # RETURNS: A boolean (True if the string ends with any of the specified suffixes).
            # sorted() makes the order the same on every run; "._" files are Mac junk and are skipped.
            all_files = sorted(f for f in os.listdir(folder_path)
                               if f.lower().endswith(('.jpg', '.png')) and not f.startswith('._'))

            for filename in all_files:

                # str.split():
                # TAKES: A separator string (like '-').
                # RETURNS: A list of strings broken apart at the separator.
                parts = filename.split('-') # split right and left word of "-" and return in a list

                # len():
                # TAKES: A sequence or collection (like a list, string, or dictionary).
                # RETURNS: An integer representing the number of items in that sequence.
                if len(parts) < 2:#[spiral, H1.png]
                    continue

                patient_part = parts[-1].split('.')[0] # e.g., "H1" or "P4"
                patient_part = self.group_of.get(patient_part, patient_part)  # use the group id
                # Train/validation split: skip patients that belong to the other split
                if self.patient_ids is not None and patient_part not in self.patient_ids:
                    continue

                # Store the FULL path relative to the root so OpenCV can open it later
                full_image_path = os.path.join(folder_path, filename)

                # list.append():
                # TAKES: An object to add to the list.
                # RETURNS: None (it modifies the original list in place).
                self.data.append({
                    'FULL_PATH': full_image_path,
                    'CLASS_TYPE': class_type, # store key value 1 or 2
                    'ID_PATIENT': patient_part
                })

                # Preprocess once and keep the tensor, so training never touches the disk again
                if self.cache:
                    self.images.append(self.transform(clean_and_threshold_image(full_image_path)))

                if class_type == 1:
                    self.healthy_indices.append(idx)
                else:
                    self.parkinson_indices.append(idx)
                idx += 1

        # Each class needs at least one image, otherwise no pairs can be built
        if not self.healthy_indices or not self.parkinson_indices:
            raise ValueError(
                f"Both classes need images. Healthy: {len(self.healthy_indices)}, "
                f"Parkinson's: {len(self.parkinson_indices)}. Check patient_ids / folders."
            )

        # NOTE: Added the precomputation of unique patients from our previous discussion
        # so the partner lists below work correctly!

        # set():
        # TAKES: An iterable (like a list or a generator expression).
        # RETURNS: A new set object containing only the UNIQUE elements from the iterable.
        self.healthy_patients = set(self.data[i]['ID_PATIENT'] for i in self.healthy_indices)
        self.parkinson_patients = set(self.data[i]['ID_PATIENT'] for i in self.parkinson_indices)

        # Precompute the valid same-class partners of every image: same class, DIFFERENT patient.
        # This prevents the model from just memorizing individual handwriting quirks,
        # and it is built once here so __getitem__ never needs a risky while-loop.
        self.positive_candidates = {}
        for pool in (self.healthy_indices, self.parkinson_indices):
            for i in pool:
                me = self.data[i]['ID_PATIENT']
                others = [j for j in pool if self.data[j]['ID_PATIENT'] != me]
                fallback = [j for j in pool if j != i]
                self.positive_candidates[i] = others or fallback or [i]

        # print():
        # TAKES: One or more objects (usually strings).
        # RETURNS: None (it outputs the text to the console).
        print(f"Successfully loaded {len(self.data)} images from nested structure.")
        print(f"-> Healthy Subfolder count: {len(self.healthy_indices)} images, {len(self.healthy_patients)} patients")
        print(f"-> Patient Subfolder count: {len(self.parkinson_indices)} images, {len(self.parkinson_patients)} patients")

    def __len__(self):
        return len(self.data)

    def _get_image(self, i):
        """
        Returns the ready-to-use tensor of image number i.
        TAKES: i (int) - position of the image in self.data.
        RETURNS: A tensor of shape [1, 224, 224], from RAM if cached, otherwise loaded from disk.
        """
        if self.cache:
            return self.images[i]
        return self.transform(clean_and_threshold_image(self.data[i]['FULL_PATH']))

    def __getitem__(self, idx: int):
        """
        Fetches an image pair (Anchor and a randomly chosen Positive/Negative)
        and their distance label for Siamese Network training.
        """
        # Validation: a private random generator per index -> identical pairs every epoch.
        # Training: the global `random` module -> new random pairs every epoch.
        rng = random.Random(self.seed * 100_003 + idx) if self.fixed_pairs else random

        # 1. Get the Anchor image (Image A) metadata
        anchor_info = self.data[idx]
        anchor_class = anchor_info['CLASS_TYPE']

        # random.choice():
        # TAKES: A non-empty sequence (like a list or tuple).
        # RETURNS: A randomly selected single element from that sequence.
        # (rng.choice works the same way, it just uses our own generator when fixed_pairs=True)
        # 2. Randomly decide whether to pair with the same class or a different class
        is_same_class = rng.choice([True, False])

        if is_same_class:
            # --- POSITIVE PAIR (Same Class) ---
            # The partner comes from the precomputed list (same class, different patient)
            paired_idx = rng.choice(self.positive_candidates[idx])

            label = 0.0  # 0 distance for similar images

        else:
            # --- NEGATIVE PAIR (Different Class) ---
            # No infinite loop risk here because the Anchor and Paired patient will always
            # have different CLASS_TYPEs, inherently making them different patients.
            pool = self.parkinson_indices if anchor_class == 1 else self.healthy_indices
            paired_idx = rng.choice(pool)
            label = 1.0  # High distance for dissimilar images

        # clean_and_threshold_image():
        # TAKES: A string path to an image file.
        # RETURNS: A 224x224 grayscale PIL image (white strokes on black), same for training and inference.
        # 3. Get both images (already cleaned + transformed in __init__ when cache=True)
        img_a = self._get_image(idx)
        img_b = self._get_image(paired_idx)

        # torch.tensor():
        # TAKES: Data (like a list or number) and optionally a specific data type (dtype).
        # RETURNS: A PyTorch tensor containing the data, ready for neural network calculations.
        return img_a, img_b, torch.tensor([label], dtype=torch.float32)


def split_patients(img_dir, subfolder, val_ratio=0.2, seed=42):
    """
    Splits PATIENT ids (not images) into train and validation sets.
    TAKES: img_dir, subfolder, val_ratio (share of patients for validation), seed (repeatable shuffle).
    RETURNS: (train_ids, val_ids), two sets of ids like {"H1", "P4"}.
    Healthy and Parkinson's patients are split separately, so both sets always contain both classes.
    """
    ds = ParkinsonSiameseDataset(img_dir, subfolder, cache=False)  # only reads file names
    rng = random.Random(seed)
    class_groups = [
        {ds.group_of.get(patient_id, patient_id) for patient_id in ds.healthy_patients},
        {ds.group_of.get(patient_id, patient_id) for patient_id in ds.parkinson_patients},
    ]
    for groups in class_groups:
        if len(groups) < 2:
            raise ValueError(
                f"Need at least 2 patient groups per class to split {subfolder}; "
                f"found {len(groups)}."
            )

    all_groups = set().union(*class_groups)
    val_groups = set()
    for groups in class_groups:
        shuffled = sorted(groups)
        rng.shuffle(shuffled)
        n_val = min(len(shuffled) - 1, max(1, round(len(shuffled) * val_ratio)))
        val_groups.update(shuffled[:n_val])

    # Keep at least one group for each class in both partitions.
    for groups in class_groups:
        if not groups - val_groups:
            removable = sorted(val_groups & groups)
            val_groups.remove(removable[-1])
        if not val_groups & groups:
            candidates = sorted(groups - val_groups)
            val_groups.add(candidates[0])

    train_groups = all_groups - val_groups
    return train_groups, val_groups


if __name__ == "__main__":
    # Quick self-test:  python -m ml_engine.write.dataset
    root = "ml_engine/data"
    for kind in ("spiral", "meander"):
        tr, va = split_patients(root, kind)
        print(kind, "| train patients:", len(tr), "| val patients:", len(va), "| overlap:", len(tr & va))
        train_ds = ParkinsonSiameseDataset(root, kind, patient_ids=tr)
        val_ds = ParkinsonSiameseDataset(root, kind, patient_ids=va, fixed_pairs=True)
        h = lambda ds: {hashlib.md5(open(d['FULL_PATH'], 'rb').read()).hexdigest() for d in ds.data}
        print("identical images shared by train and val:", len(h(train_ds) & h(val_ds)))
        a, b, y = val_ds[0]
        print("sample:", a.shape, b.shape, y.item(), "| val pair repeatable:", y.item() == val_ds[0][2].item())