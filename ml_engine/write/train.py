"""
=============================================================================
FLOWCHART OF THIS SCRIPT
=============================================================================
[Start: Terminal Command]
          |
          v
[Parse Terminal Arguments] ---> (Extracts '--type' folder name, e.g., 'spiral')
          |
          v
[train_model(test_type)]
          |
          v
[Check Device] ---> (Sets to 'cuda' if GPU available, else 'cpu')
          |
          v
[Init Dataset & DataLoader] ---> (Loads images from specified subfolder)
          |
          v
[Init Network, Loss, Optimizer] ---> (Siamese Net, Contrastive Loss, Adam)
          |
          v
================= [START EPOCH LOOP: 20 Epochs] =================
          |
          v
  =============== [START BATCH LOOP: Dataloader] ===============
            |
            v
  [Move Images & Labels to Device]
            |
            v
  [optimizer.zero_grad()] ---> (Clears previous gradients)
            |
            v
  [Forward Pass: net(img0, img1)] ---> (Gets output feature vectors)
            |
            v
  [Calculate Contrastive Loss] ---> (Checks similarity/dissimilarity)
            |
            v
  [loss.backward()] ---> (Computes gradients via calculus)
            |
            v
  [optimizer.step()] ---> (Updates model weights based on gradients)
            |
            v
  [Accumulate epoch_loss] ---> (using loss.item())
            |
  ================ [END BATCH LOOP] ============================
          |
          v
[Calculate & Print Average Epoch Loss]
          |
================= [END EPOCH LOOP] ==============================
          |
          v
[os.makedirs] ---> (Ensure 'weights/' directory exists)
          |
          v
[torch.save()] ---> (Saves trained model weights to .pth file)
          |
          v
[End]
=============================================================================
"""

import os
import argparse #for terminal use
import random
import torch
import torch.nn as nn #nural network(nn) layer
import torch.optim as optim
import torch.nn.functional as F
#F contains all the raw mathematical operations and functions that a neural network needs to do its job
from torch.utils.data import DataLoader

from ml_engine.write.dataset import ParkinsonSiameseDataset, split_patients
from ml_engine.write.architecture import SiameseParkinsonNetwork

# Folders are built from THIS file's location, so the script works no matter which
# folder you run it from, and the weights land exactly where predictor.py looks for them.
WRITE_DIR = os.path.dirname(os.path.abspath(__file__))          # .../ml_engine/write
DATA_DIR = os.path.normpath(os.path.join(WRITE_DIR, "..", "data"))  # .../ml_engine/data
WEIGHTS_DIR = os.path.join(WRITE_DIR, "weights")                 # .../ml_engine/write/weights

class ContrastiveLoss(nn.Module):
    def __init__(self, margin=1.0):
        super().__init__() # to inherit parent constractor
        self.margin = margin
        # here margin is distance between healthy and patient data 


# 1.this fn calculate euclidean distance and constractive loss
    def forward(self, output1, output2, label):
        #p_d funtion take pair of image and calculate Distance = \sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2 +...} after that keepdim force the result to column.
        euclidean_distance = F.pairwise_distance(output1, output2, keepdim=True )
        #The constractive loss formula where Y=label,D= euclidean distance and m= slef.margin

        loss_contrastive = torch.mean(
            (1 - label) * torch.pow(euclidean_distance, 2) +
            (label) * torch.pow(torch.clamp(self.margin - euclidean_distance, min=0.0), 2)
        )
        return loss_contrastive

        # Contrastive Loss teaches a machine learning model how to understand similarity rather than forcing it to memorize specific categories.
        #
        # Instead of asking a model, "Is this a picture of a cat?" (which standard classification does), contrastive loss asks, "Are these two pictures of the same thing?"

# Notice we now pass 'test_type' directly into the function

def validate(net, val_loader, criterion, device):
    """
    Runs the validation patients through the network WITHOUT learning from them.
    TAKES: net, val_loader (fixed pairs), criterion (the loss), device.
    RETURNS: (average loss, mean distance of SIMILAR pairs, mean distance of DIFFERENT pairs).
    A healthy model has a small first distance and a large second distance.
    """
    net.eval()  # lock the brain: no dropout, BatchNorm uses its saved statistics
    total_loss = 0.0
    same_dists, diff_dists = [], []

    with torch.no_grad():  # no calculus engine needed, saves memory and time
        for img0, img1, label in val_loader:
            img0, img1, label = img0.to(device), img1.to(device), label.to(device)
            output1, output2 = net(img0, img1)
            total_loss += criterion(output1, output2, label).item()

            d = F.pairwise_distance(output1, output2)   # one distance per pair
            lab = label.view(-1)                         # 0.0 = similar, 1.0 = different
            same_dists.extend(d[lab == 0].tolist())
            diff_dists.extend(d[lab == 1].tolist())

    mean = lambda xs: sum(xs) / len(xs) if xs else float('nan')
    return total_loss / len(val_loader), mean(same_dists), mean(diff_dists)


#this funciton check device and do dataload and train
def train_model(test_type, epochs=20):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"--- Training Initialization ---")
    print(f"Device: {device}")
    print(f"Targeting Dataset: {test_type.upper()}")

    # Fixed seeds -> the same split and the same starting weights every run, so results can be compared
    random.seed(42)
    torch.manual_seed(42)

    image_dir_path = DATA_DIR
    weights_dir = WEIGHTS_DIR

    # Split PATIENTS (not images) into train and validation, so all 4 images of a person
    # stay on the same side and validation really means "people the model has never seen".
    train_ids, val_ids = split_patients(image_dir_path, test_type)

    # The dataset dynamically uses whatever folder you typed in the terminal
    train_dataset = ParkinsonSiameseDataset(
        img_dir=image_dir_path,
        subfolder=test_type,
        patient_ids=train_ids
    )
    # Validation uses fixed pairs: the same pairs every epoch, so a falling val loss
    # means the model really improved, not that it got easier pairs.
    val_dataset = ParkinsonSiameseDataset(
        img_dir=image_dir_path,
        subfolder=test_type,
        patient_ids=val_ids,
        fixed_pairs=True
    )

    train_dataloader = DataLoader(train_dataset, shuffle=True, num_workers=0, batch_size=32)
    val_dataloader = DataLoader(val_dataset, shuffle=False, num_workers=0, batch_size=32)
# net=network
    net = SiameseParkinsonNetwork().to(device)
    criterion = ContrastiveLoss(margin=1.0)
    optimizer = optim.Adam(net.parameters(), lr=0.0005)
    #Adam is the "mechanic." When the grader says the AI made a mistake, Adam goes inside the network and turns the mathematical dials to fix it. lr=0.0005 (Learning Rate) tells Adam to make very tiny, careful adjustments.

    print("Starting training phase...")
    net.train() 


    """new for structure knowledgeconveyor_belt = [
    ("Alice", "Bob", 5.0),
    ("Charlie", "David", 2.5),
    ("Eve", "Frank", 10.0)
]
    for package in conveyor_belt:
    print(package)
    
    # Output:
    # ('Alice', 'Bob', 5.0)
    # ('Charlie', 'David', 2.5)

    for sender, receiver, weight in conveyor_belt:
        print(f"{sender} is sending {weight}kg to {receiver}")

    # Output:
    # Alice is sending 5.0kg to Bob
    # Charlie is sending 2.5kg to David

    enumerate() is a built-in Python function that takes a collection of items (like a list or a dataloader) and attaches an automatic counter to each item as you loop through them.

    for i, package in enumerate(conveyor_belt):
        print(f"Package number {i} is {package}")

    # Output:
    # Package number 0 is ('Alice', 'Bob', 5.0)
    # Package number 1 is ('Charlie', 'David', 2.5)

    for i, (sender, receiver, weight) in enumerate(conveyor_belt):
        print(f"Batch {i}: {sender} -> {receiver}")

    # Output:
    # Batch 0: Alice -> Bob
    # Batch 1: Charlie -> David
    # Batch 2: Eve -> Frank
    """

    os.makedirs(weights_dir, exist_ok=True)

    # Dynamically names the output file based on your input!
    save_path = os.path.join(weights_dir, f"siamese_{test_type}.pth") 

    best_val_loss = float("inf")  # lowest validation loss seen so far

    for epoch in range(epochs):
        net.train()  # validate() switches the net to eval mode, so switch back to training every epoch
        epoch_loss = 0.0
        for i, (img0, img1, label) in enumerate(train_dataloader, 0):#0 is by default index
            img0, img1, label = img0.to(device), img1.to(device), label.to(device)

            """The Whiteboard Analogy
            Imagine a student solving a complex math problem on a whiteboard.

            The student looks at a problem (a batch of data).

            They do the math and write the answer on the board (the gradients).

            The teacher checks it and adjusts the student's grade (the optimizer updates the weights).

            If the student moves on to the second math problem without erasing the whiteboard first, they will end up writing the new numbers right on top of the old ones. The math becomes a chaotic, unreadable mess, and they will get the wrong answer.

            optimizer.zero_grad() is the eraser. It wipes the mathematical whiteboard completely clean so the model can calculate fresh, accurate directions for the new batch of images."""

            optimizer.zero_grad()

            """In Python, if you put a magic method named __call__ inside a class, it gives the resulting object the special ability to be triggered exactly like a function.

            Because your SiameseParkinsonNetwork inherited from PyTorch's nn.Module, it silently inherited PyTorch's highly complex __call__ function.

            2. The Secret Handshake (The forward function)
            When you write net(img0, img1), here is the exact chain of events that happens in the fraction of a millisecond:

            Python sees the parentheses and triggers the hidden __call__ function inherited from PyTorch.

            PyTorch briefly takes control. It sets up the invisible mathematical tracking system (the computational graph) so it can calculate the calculus gradients later.

            Once the setup is done, PyTorch automatically looks inside your code for a function named forward and passes img0 and img1 directly into it.

            Your forward function does the actual work (passing the images through the convolution layers), and hands the results (output1, output2) back out."""
            
            output1, output2 = net(img0, img1)
            
            final_loss_contrastive = criterion(output1, output2, label)
            final_loss_contrastive.backward()
            optimizer.step()

            """loss_contrastive isn't just a number? It is a PyTorch Tensor with a massive, invisible "breadcrumb trail" (computational graph) attached to it so .backward() can do its calculus.

            That breadcrumb trail takes up a huge amount of memory (RAM/VRAM) on your computer.

            If you just wrote epoch_loss += loss_contrastive, you wouldn't just be throwing the number 2.45 into the bucket. You would be throwing the number plus the massive breadcrumb trail. If you do this for 1,000 batches, you will stack up 1,000 massive breadcrumb trails in your bucket.

            Your computer's memory will fill up completely, and your program will crash with an "Out of Memory" (OOM) error.

            3. The "Safety Scissors" (.item())
            The .item() command acts like a pair of safety scissors.

            When you call loss_contrastive.item(), you are telling PyTorch:

            "I only want the raw, standard Python number (like 2.45). Cut off the massive PyTorch breadcrumb trail and leave it behind."

            By using .item(), you safely drop a lightweight, normal number into your epoch_loss bucket. The heavy breadcrumb trail gets deleted by the computer, keeping your memory completely clean for the next batch of images."""
            
            epoch_loss += final_loss_contrastive.item()

        train_loss = epoch_loss / len(train_dataloader)

        # After every epoch: test on the validation patients (people the model never trained on)
        val_loss, same_dist, diff_dist = validate(net, val_dataloader, criterion, device)

        print(f"Epoch [{epoch+1}/{epochs}] | Train Loss: {train_loss:.4f} | Val Loss: {val_loss:.4f} "
              f"| same-pair dist: {same_dist:.3f} | different-pair dist: {diff_dist:.3f}")

        # Keep only the BEST version of the model (lowest validation loss), so later epochs
        # that start memorizing the training data cannot overwrite a better model.
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            torch.save(net.state_dict(), save_path)
            print(f"  -> New best model saved (val loss {val_loss:.4f})")

    print(f"Success! Best weights (val loss {best_val_loss:.4f}) saved to: {save_path}\n")

if __name__ == "__main__":
    #This line tells Python: "Only run the code below this line IF I am running this specific file directly. If another file is just importing me to borrow my functions, ignore this section completely."
    # This captures what you type in the terminal
    parser = argparse.ArgumentParser(description="Train Parkinson's Siamese Network")


    """strict rules about what the user is allowed to type.

    '--type': The user must use this exact flag.

    type=str: Whatever comes after the flag must be treated as text (a string).

    required=True: The script will refuse to run if the user forgets this flag.

    help: If the user types python script.py --help, this exact message will pop up to guide them."""
    parser.add_argument(
        '--type', 
        type=str, 
        required=True, 
        help="The name of the subfolder to train (e.g., spiral, meander, circle)"
    )
    parser.add_argument(
        '--epochs',
        type=int,
        default=20,
        help="How many passes over the training data (default 20)"
    )
    args = parser.parse_args()
    #What it does: This line actually executes the rules. It reads the terminal, checks that you followed the rules (meaning you included --type), grabs the word you typed, and saves it inside the args box.
    
    # Pass the terminal argument into the training function
    train_model(args.type, args.epochs)