"""1.Initialize AI Framework (once per test type):The first time a test type is needed, the script checks your hardware (GPU vs CPU), builds the empty SiameseParkinsonNetwork structure, loads your saved .pth weights and the calibration_<type>.json file, and keeps both in memory so later requests reuse them. It immediately calls .eval() to lock the brain and prevent accidental learning.
2.Retrieve the Healthy Baseline (Anchor):The anchor is no longer one random healthy drawing. It is the centroid (mean embedding) of the healthy TRAINING patients, saved by calibrate.py inside calibration_<type>.json.
3.OpenCV Image Cleaning:The user's uploaded image is sent through clean_and_threshold_image() from preprocess.py (the same function used in training). It is converted to grayscale, blurred, separated into white strokes on black with Otsu thresholding, and rigidly resized to 224x224 pixels.
4.PyTorch Tensor Conversion:The clean image is passed through your transform. It is converted into a PyTorch Tensor, mathematically normalized, and given a "fake" batch dimension using .unsqueeze(0) so the AI thinks it's receiving a batch of size 1.
5.Feature Extraction (Forward Pass):The script turns off the calculus engine using torch.no_grad() to save memory. It passes the image tensor through the twin CNN to extract its final 128-dimensional mathematical coordinates (the embedding).
6.Calculate Euclidean Distance:Using torch.norm, the script measures the exact straight-line mathematical distance between the user's embedding and the healthy anchor.
7.Calculate Final Score:The raw distance is turned into a calibrated Stability Score with a two-point curve (healthy median distance -> 90, PD median distance -> 35), clamped between 0.0 and 100.0 and rounded to two decimal places.
8.Return the package:analyze_drawing() returns a dictionary (score, distance, embedding, model_version) so Django can store the embedding and compute day-to-day and week-to-week trends later. evaluate_stability() still returns just the score."""


import os
import json
import torch
import torchvision.transforms as transforms
#The Mathematician (torchvision): Takes that image, turns it into a PyTorch Tensor, and squishes all the colors into the perfect mathematical range (-1.0 to 1.0).
# Import the exact architecture you provided
from ml_engine.write.architecture import SiameseParkinsonNetwork
from ml_engine.write.preprocess import clean_and_threshold_image

MODEL_VERSION = "write_cnn_v1"
VALID_TEST_TYPES = ("spiral", "meander")   # test_type comes from the user, so only known values are allowed
WEIGHTS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "weights")
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

_loaded = {}  # test_type -> (model, calibration), loaded once and reused by every request

# Standard transform matching your training configuration
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.5], std=[0.5])
])
#here compose pass the output of transforms.ToTensor() to transforms.Normalize(mean=[0.5], std=[0.5])


def _load(test_type):
    """
    Loads the model weights and the calibration file once per test type.
    TAKES: test_type ("spiral" or "meander").
    RETURNS: (model, calibration dict). The dict holds the anchor tensor and the two calibration distances.
    """
    if test_type not in VALID_TEST_TYPES:
        raise ValueError(f"Unknown test_type '{test_type}'. Use one of {VALID_TEST_TYPES}.")
    if test_type in _loaded:
        return _loaded[test_type]

    weights_path = os.path.join(WEIGHTS_DIR, f'siamese_{test_type}.pth')
    calib_path = os.path.join(WEIGHTS_DIR, f'calibration_{test_type}.json')
    for path in (weights_path, calib_path):
        if not os.path.exists(path):
            raise FileNotFoundError(f"Missing '{path}'. Run train.py and calibrate.py for '{test_type}' first.")

    # 1. Instantiate the twin framework structure and load specific trained weights
    model = SiameseParkinsonNetwork().to(DEVICE)

    """This line of code performs a **"Brain Transplant"** on your AI.

    ### 1. `torch.load()` (The File Reader)

    This function is PyTorch's official file reader. It goes to your hard drive, opens the saved `.pth` file, and extracts the data.

    **What it TAKES:**

    * **`weights_path`:** The exact file path on your computer where the `.pth` file is located.
    * **`map_location=device`:** This is a brilliant safety feature. If you trained your AI on a massive cloud GPU, PyTorch saved the file as a "GPU file." If you try to open that file on a basic laptop that only has a CPU, PyTorch will crash. `map_location=device` tells PyTorch: *"I don't care what hardware this was trained on. Translate the data to perfectly match whatever hardware (`device`) I am currently using."*

    **What it RETURNS:**
    It returns a **State Dictionary** (`state_dict`). This is simply a massive, organized Python list containing millions of highly specific numbers (the "weights" and "biases") that the AI calculated during its 20 epochs of training.

    ### 2. `model.load_state_dict()` (The Surgeon)

    **What it TAKES:**

    * It takes the **State Dictionary** (the list of learned numbers) provided by `torch.load()`.

    **What it DOES:**
    It acts like a surgeon, taking every single number from that saved list and perfectly injecting it into the correct layers of your empty `model`. It maps the saved memory perfectly to the architecture.

    ---

    ### The Final Step: `model.eval()`

    In PyTorch, a model can be in one of two modes:

    1. **Training Mode (`model.train()`):** The model is actively updating its weights, changing its math, and "learning."
    2. **Evaluation Mode (`model.eval()`):** The model's brain is completely locked.

    By calling `model.eval()`, you are telling the AI: *"Your training is completely finished. Do not try to learn from this new patient's drawing. Just use your locked, saved memories to grade it."*"""

    model.load_state_dict(torch.load(weights_path, map_location=DEVICE))
    model.eval()

    # 2. Load the healthy anchor (centroid) and the two calibration distances saved by calibrate.py
    with open(calib_path) as f:
        calib = json.load(f)
    calib["anchor"] = torch.tensor(calib["anchor"], device=DEVICE)

    _loaded[test_type] = (model, calib)
    return _loaded[test_type]


def distance_to_stability(distance, calib):
    """
    Turns a raw distance into a 0-100 stability score.
    TAKES: distance (float) and the calibration dict.
    RETURNS: a float between 0 and 100. Same formula as make_score_fn() in calibrate.py:
    healthy median distance -> 90, PD median distance -> 35, smooth exponential curve in between.
    """
    d_healthy = calib["d_healthy"]
    d_pd = calib["d_pd"]
    if d_pd <= d_healthy:
        raise ValueError("Calibration requires d_pd to be greater than d_healthy.")
    ratio = calib["target_pd"] / calib["target_healthy"]
    exponent = (distance - d_healthy) / (d_pd - d_healthy)
    score = calib["target_healthy"] * (ratio ** exponent)
    return round(max(0.0, min(100.0, score)), 2)


def analyze_drawing(uploaded_image_path, test_type):
    """
    Compares a user upload against the healthy anchor of this test type.
    TAKES: path to the uploaded image, test_type ("spiral" or "meander").
    RETURNS: dict with stability_score (0-100), distance, embedding (128 numbers to store in the DB)
    and model_version.
    """
    model, calib = _load(test_type)

    # 3. Normalize and process the image through the shared OpenCV threshold pipeline
    """What it RETURNS (The Output)
    It returns a PIL.Image object (a standard Python image).

    When the function finishes running, it hands back an image that has been completely transformed. Because of the code in preprocess.py, the returned image is guaranteed to be:

    Grayscale (All color stripped away).

    High-Contrast Binary (Shadows removed, leaving only pure black background and pure white pen strokes).

    Perfectly Sized (Squished or stretched into a strict 224x224 pixel square).

    Translated (Converted from OpenCV's raw math array back into a standard Python image using Image.fromarray)."""
    img_user = clean_and_threshold_image(uploaded_image_path)

    # Convert the clean PIL frame into an analytical tensor and add a batch dimension

    """1. transform(img_user)
    This is the assembly line I built earlier using torchvision.transforms.Compose.

    What it TAKES: The clean PIL.Image 
    i just generated in the previous step.

    What it RETURNS: A 3D PyTorch Tensor.

    What it DOES: It translates the Python image into a mathematical matrix, squishes the colors between -1.0 and 1.0, and outputs a 3D shape of [1, 224, 224] (1 color channel, 224 width, 224 height).

    2. .unsqueeze(0)
    This is the "fake box" creator.

    What it TAKES: The number 0. This simply tells PyTorch where to add the fake dimension (index 0 means at the very front).

    What it RETURNS: A 4D PyTorch Tensor.

    What it DOES: Remember the factory analogy? The AI refuses to process a single loose image. It only accepts "boxes" (batches) of images. This wraps a fake batch dimension around your tensor, changing its shape from [1, 224, 224] to [1, 1, 224, 224] so the neural network won't crash."""

    tensor_user = transform(img_user).unsqueeze(0).to(DEVICE)

    # 4. Extract the 128-dimensional coordinate embedding and measure spatial distance to the anchor

    """The .item() command is a built-in PyTorch extractor. Its only job is to rip a standard Python number out of a PyTorch Tensor wrapper.
        If you printed it without .item(), it would look like this:

    Python
    # What PyTorch outputs
    tensor([3.1415], device='cuda:0')
        """

    with torch.no_grad():
        # Only ONE image goes through the twin CNN now: the other "twin" is the stored anchor.
        embedding = model.twin_cnn(tensor_user).squeeze(0)
        distance = torch.norm(embedding - calib["anchor"], p=2).item()

    # 5. Convert Euclidean distance into a human-readable Stability Percentage
    # A drawing at the healthy anchor scores 100%; the typical PD drawing scores about 35%.
    stability_score = distance_to_stability(distance, calib)

    return {
        "stability_score": round(stability_score, 2),
        "distance": round(distance, 4),
        "embedding": embedding.tolist(),   # store this in the database so trends can be recomputed later
        "model_version": MODEL_VERSION,
    }


def evaluate_stability(uploaded_image_path, test_type):
    """Backward-compatible wrapper: returns only the 0-100 stability score."""
    return analyze_drawing(uploaded_image_path, test_type)["stability_score"]


if __name__ == "__main__":
    # Quick test:  python -m ml_engine.write.predictor --type spiral --image <path to a drawing>
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--type', required=True)
    parser.add_argument('--image', required=True)
    args = parser.parse_args()
    result = analyze_drawing(args.image, args.type)
    result.pop("embedding")   # too long to print
    print(result)