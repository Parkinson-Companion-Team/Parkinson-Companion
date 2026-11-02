"""
=============================================================================
FLOWCHART: EVALUATE STABILITY
=============================================================================
[START]
   |
   v
1. [Initialize AI] -----------> Check hardware (CPU/GPU)
   |                            Load SiameseNetwork structure
   |                            Load saved .pth weights
   |                            Lock brain with .eval()
   v
2. [Retrieve Anchor] ---------> Locate raw data folder
   |                            Find healthy baseline image
   v
3. [Clean Images] ------------> clean_and_threshold_image(user_image)
   |                            clean_and_threshold_image(anchor_image)
   v
4. [Tensor Conversion] -------> Apply standard transforms (Normalize)
   |                            Add fake batch dimension with .unsqueeze(0)
   v
5. [Feature Extraction] ------> Disable gradients with torch.no_grad()
   |                            Pass both tensors through AI framework
   v
6. [Calculate Distance] ------> Measure Euclidean distance between outputs
   |                            using F.pairwise_distance()
   v
7. [Calculate Score] ---------> Apply exponential decay formula
   |                            Clamp to 0-100 scale, round to 2 decimals
   v
[END] <------------------------ Return Stability Score
=============================================================================
"""

import os
import math
import torch
import torch.nn.functional as F
import torchvision.transforms as transforms

from ml_engine.write.architecture import SiameseParkinsonNetwork
from ml_engine.write.preprocess import clean_and_threshold_image

# transforms.Compose: Takes a list of Transform objects | Returns a callable that applies them sequentially
# transforms.ToTensor: Takes a PIL Image or numpy array | Returns a PyTorch Tensor (squished to 0.0-1.0)
# transforms.Normalize: Takes sequences of means and standard deviations | Returns a mathematically normalized Tensor
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.5], std=[0.5])
])

def evaluate_stability(uploaded_image_path, test_type):
    # torch.cuda.is_available: Takes nothing | Returns boolean True if GPU is accessible
    # torch.device: Takes a string device name ("cuda" or "cpu") | Returns a PyTorch device object
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    
    # .to: Takes a device object | Returns the model moved to the specified hardware
    model = SiameseParkinsonNetwork().to(device)
    
    # __file__: Built-in Python variable | Returns string of the current script's file path
    # os.path.dirname: Takes a file path string | Returns string of the directory containing that file
    # os.path.join: Takes multiple path strings | Returns a single concatenated path string
    weights_path = os.path.join(os.path.dirname(__file__), 'weights', f'siamese_{test_type}.pth')
    
    # os.path.exists: Takes a file/folder path string | Returns boolean True if it exists
    if not os.path.exists(weights_path):
        raise FileNotFoundError(f"Missing weight metrics for '{test_type}' at {weights_path}. Run training first.")

    # torch.load: Takes a file path and a map_location | Returns the deserialized Python object (State Dictionary)
    # model.load_state_dict: Takes a State Dictionary | Returns nothing (injects parameters into the model in-place)
    model.load_state_dict(torch.load(weights_path, map_location=device))
    
    # model.eval: Takes nothing | Returns the model set to evaluation mode (locks gradients/learning)
    model.eval()

    base_dir = os.path.dirname(__file__)
    anchor_dir = os.path.join(base_dir, 'data', 'raw', test_type, 'healthy')
    
    if not os.path.exists(anchor_dir):
        raise FileNotFoundError(f"Reference baseline template directory not found at: {anchor_dir}")
        
    # os.listdir: Takes a directory path string | Returns a list of filename strings inside that directory
    # str.endswith: Takes a tuple of string extensions | Returns boolean True if string ends with any of them
    valid_anchors = [f for f in os.listdir(anchor_dir) if f.endswith(('.jpg', '.png', '.jpeg'))]
    if not valid_anchors:
        raise FileNotFoundError(f"No baseline image templates found inside: {anchor_dir}")
        
    anchor_full_path = os.path.join(anchor_dir, valid_anchors[0])

    img_user = clean_and_threshold_image(uploaded_image_path)
    img_anchor = clean_and_threshold_image(anchor_full_path)
    
    # transform: Takes a PIL Image | Returns a PyTorch Tensor
    # tensor.unsqueeze: Takes an integer dimension index | Returns a new Tensor with a dimension added at that index
    # tensor.to: Takes a device object | Returns the Tensor moved to the specified hardware
    tensor_user = transform(img_user).unsqueeze(0).to(device)
    tensor_anchor = transform(img_anchor).unsqueeze(0).to(device)

    # torch.no_grad: Takes nothing | Returns a context manager that disables gradient calculation to save memory
    with torch.no_grad():
        output_user, output_anchor = model(tensor_user, tensor_anchor)
        
        # F.pairwise_distance: Takes two Tensors | Returns a Tensor containing their Euclidean distance
        # tensor.item: Takes a single-value Tensor | Returns a standard Python number (float)
        distance = F.pairwise_distance(output_user, output_anchor).item()

    k = 1.5
    # math.exp: Takes a number (float/int) | Returns the exponential value e^x (float)
    stability_score = 100.0 * math.exp(-k * distance)
    
    # round: Takes a number and integer for decimal places | Returns the rounded number
    return round(stability_score, 2)