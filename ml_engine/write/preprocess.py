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
from PIL import Image

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
    img = cv2.imread(str(image_path), cv2.IMREAD_GRAYSCALE)
    
    if img is None:
        raise FileNotFoundError(f"Could not read image at path: {image_path}")

    # cv2.adaptiveThreshold():
    # TAKES: The source grayscale image, maximum pixel value (255), adaptive method (GAUSSIAN_C), threshold style (BINARY_INV), block size (11), and a fine-tuning constant (2).
    # RETURNS: A binarized NumPy array where the background is pure black (0) and pen strokes are pure white (255) to strip away real-world shadows.
    binary_img = cv2.adaptiveThreshold(
        img, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY_INV, 11, 2
    )
    
    # cv2.resize():
    # TAKES: The source image, the target dimensions (224x224), and the mathematical interpolation method (INTER_AREA for safe shrinking).
    # RETURNS: A new NumPy array resized exactly to the target dimensions without losing thin pen stroke details.
    resized_img = cv2.resize(binary_img, (224, 224), interpolation=cv2.INTER_AREA)
    
    # Image.fromarray():
    # TAKES: A raw NumPy array of numbers (from OpenCV).
    # RETURNS: A standard PIL Image object that can be seamlessly passed into your PyTorch transforms.
    return Image.fromarray(resized_img)