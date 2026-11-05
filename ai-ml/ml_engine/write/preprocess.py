"""
What changed from the old version: the read flag is now cv2.IMREAD_GRAYSCALE. The old cv2.COLOR_BGR2GRAY is a colour-conversion code, not a read flag. Its numeric value happens to be 6, so colour images came back with 3 channels and adaptiveThreshold crashed.
==========================================
LOGIC FLOWCHART
==========================================
[ Raw Image Path ] 
        ▼
[ Load as Grayscale Array (cv2.imread) ]
        ▼
[ Remove Shadows & Binarize (cv2.adaptiveThreshold) ]
        ▼
[ Shrink to Target 224x224 Size (cv2.resize) ]
        ▼
[ Convert back to Python Image Object (Image.fromarray) ]
==========================================
"""

import cv2
import numpy as np
from PIL import Image


class InvalidImageError(ValueError):
    """Raised when an uploaded image cannot be decoded safely."""


def _read_gray(image_path):
    try:
        flags = cv2.IMREAD_UNCHANGED if str(image_path).lower().endswith(".png") else cv2.IMREAD_GRAYSCALE
        image = cv2.imread(str(image_path), flags)
        if image is None:
            raise InvalidImageError(f"Could not read image at path: {image_path}")
        if image.dtype != np.uint8:
            image = cv2.normalize(image, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
        if image.ndim == 3 and image.shape[2] == 4:
            rgb = image[:, :, :3].astype(np.float32)
            alpha = image[:, :, 3:4].astype(np.float32) / 255.0
            image = np.clip(rgb * alpha + 255.0 * (1.0 - alpha), 0, 255).astype(np.uint8)
        if image.ndim == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        return image
    except cv2.error as exc:
        raise InvalidImageError("Could not decode image") from exc


def _binarize(gray):
    blur = cv2.GaussianBlur(gray, (5, 5), 0)
    _, binary = cv2.threshold(blur, 0, 255, cv2.THRESH_BINARY_INV | cv2.THRESH_OTSU)
    return binary


def _needs_flip(binary):
    return (binary > 0).mean() > 0.5


def clean_and_threshold_image(image_path):
    """
    Single source of truth for handwriting preprocessing.
    Used by BOTH training (dataset.py) and inference (predictor.py).
    Returns a 224x224 grayscale PIL image: white strokes on black.
    """
    
    # str():
    # TAKES: An object (like a Pathlib path object or text variable).
    # RETURNS: The standard string representation of that object.
    
    # cv2.imread():
    # TAKES: A string file path and a color mode flag (IMREAD_GRAYSCALE strips colors).
    # RETURNS: A 2D NumPy array representing the image pixels, or None if the file is missing/corrupted.
    img = _read_gray(image_path)
    binary_img = _binarize(img)
    if _needs_flip(binary_img):
        binary_img = cv2.bitwise_not(binary_img)
    
    # cv2.resize():
    # TAKES: The source image, the target dimensions (224x224), and the mathematical interpolation method (INTER_AREA for safe shrinking).
    # RETURNS: A new NumPy array resized exactly to the target dimensions without losing thin pen stroke details.
    resized_img = cv2.resize(binary_img, (224, 224), interpolation=cv2.INTER_AREA)
    
    # Image.fromarray():
    # TAKES: A raw NumPy array of numbers (from OpenCV).
    # RETURNS: A standard PIL Image object that can be seamlessly passed into your PyTorch transforms.
    return Image.fromarray(resized_img)