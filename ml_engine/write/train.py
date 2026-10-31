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
import torch
import torch.nn as nn #nural network(nn) layer
import torch.optim as optim
import torch.nn.functional as F
#F contains all the raw mathematical operations and functions that a neural network needs to do its job
from torch.utils.data import DataLoader

from ml_engine.write.dataset import ParkinsonSiameseDataset
from ml_engine.write.architecture import SiameseParkinsonNetwork

class ContrastiveLoss(nn.Module):
    def __init__(self, margin=1.0):
        # super().__init__(): Takes: None (implicitly self). Returns: None. Calls the constructor of the parent class (nn.Module) to initialize its internal state.
        super().__init__() # to inherit parent constractor
        self.margin = margin
        # here margin is distance between healthy and patient data 


# 1.this fn calculate euclidean distance and constractive loss
    def forward(self, output1, output2, label):
        #p_d funtion take pair of image and calculate Distance = \sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2 +...} after that keepdim force the result to column.
        
        # F.pairwise_distance(): Takes: tensor1, tensor2, keepdim (bool). Returns: A tensor containing the calculated distance between the two input tensors.
        euclidean_distance = F.pairwise_distance(output1, output2, keepdim=True )
        
        #The constractive loss formula where Y=label,D= euclidean distance and m= slef.margin

        # torch.clamp(): Takes: input tensor, min value (optional), max value (optional). Returns: A tensor with all elements clamped into the range [min, max].
        # torch.pow(): Takes: input tensor, exponent (number). Returns: A tensor with the elements of the input raised to the power of the exponent.
        # torch.mean(): Takes: input tensor. Returns: A scalar tensor representing the mean (average) of all elements in the input tensor.
        loss_contrastive = torch.mean(
            (1 - label) * torch.pow(euclidean_distance, 2) +
            (label) * torch.pow(torch.clamp(self.margin - euclidean_distance, min=0.0), 2)
        )
        return loss_contrastive

        """Contrastive Loss teaches a machine learning model how to understand similarity rather than forcing it to memorize specific categories.

        Instead of asking a model, "Is this a picture of a cat?" (which standard classification does), contrastive loss asks, "Are these two pictures of the same thing?"""

# Notice we now pass 'test_type' directly into the function

#this funciton check device and do dataload and train
def train_model(test_type):
    # torch.cuda.is_available(): Takes: None. Returns: Boolean (True if GPU is available, False if not).
    # torch.device(): Takes: A string representing the device type ("cuda" or "cpu"). Returns: A torch.device object representing the device where computations will happen.
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    
    # print(): Takes: A string or objects to print. Returns: None (outputs the text to the terminal).
    print(f"--- Training Initialization ---")
    print(f"Device: {device}")
    
    # str.upper(): Takes: None (it's a string method). Returns: A new string with all characters converted to uppercase.
    print(f"Targeting Dataset: {test_type.upper()}")
    
    image_dir_path = "data/raw/"
    weights_dir = "weights/"

    # The dataset dynamically uses whatever folder you typed in the terminal
    siamese_dataset = ParkinsonSiameseDataset(
        img_dir=image_dir_path,
        subfolder=test_type  
    )
    
    # DataLoader(): Takes: dataset object, batch_size (int), shuffle (bool), num_workers (int). Returns: An iterable DataLoader object that batches and shuffles the dataset.
    train_dataloader = DataLoader(siamese_dataset, shuffle=True, num_workers=0, batch_size=32)
# model=network

    # .to(device): Takes: A torch.device object. Returns: The PyTorch model (or tensor) moved to the specified device (CPU or GPU memory).
    model = SiameseParkinsonNetwork().to(device)
    criterion = ContrastiveLoss(margin=1.0)
    
    # model.parameters(): Takes: None. Returns: An iterator containing all the learnable weights and biases (parameters) of the neural network.
    # optim.Adam(): Takes: model parameters, lr (learning rate as float). Returns: An Adam optimizer object that will update the network weights.
    optimizer = optim.Adam(net.parameters(), lr=0.0005)
    #Adam is the "mechanic." When the grader says the AI made a mistake, Adam goes inside the network and turns the mathematical dials to fix it. lr=0.0005 (Learning Rate) tells Adam to make very tiny, careful adjustments.

    epochs = 20
    
    print("Starting training phase...")
    
    # model.train(): Takes: None. Returns: The module itself. Sets the neural network to "training mode" (activates features like Dropout and BatchNorm specific to training).
    model.train() 


    """new for structure
    knowledgeconveyor_belt = [
    ("Alice", "Bob", 5.0),
    ("Charlie", "David", 2.5),
    ("Eve", "Frank", 10.0)]
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

    # range(): Takes: an integer (stop value). Returns: An iterable sequence of numbers from 0 up to (but not including) the stop value.
    for epoch in range(epochs):
        epoch_loss = 0.0
        
        # enumerate(): Takes: an iterable (like train_dataloader) and an optional start index (0). Returns: An iterator that yields a tuple (index, item) for each loop iteration.
        for i, (img0, img1, label) in enumerate(train_dataloader, 0):#0 is by default index
            img0, img1, label = img0.to(device), img1.to(device), label.to(device)

            """The Whiteboard Analogy
            Imagine a student solving a complex math problem on a whiteboard.

            The student looks at a problem (a batch of data).

            They do the math and write the answer on the board (the gradients).

            The teacher checks it and adjusts the student's grade (the optimizer updates the weights).

            If the student moves on to the second math problem without erasing the whiteboard first, they will end up writing the new numbers right on top of the old ones. The math becomes a chaotic, unreadable mess, and they will get the wrong answer.

            optimizer.zero_grad() is the eraser. It wipes the mathematical whiteboard completely clean so the model can calculate fresh, accurate directions for the new batch of images."""

            # optimizer.zero_grad(): Takes: None. Returns: None. Clears the old gradients from the previous batch so they don't accumulate incorrectly.
            optimizer.zero_grad()

            """In Python, if you put a magic method named __call__ inside a class, it gives the resulting object the special ability to be triggered exactly like a function.

            Because our SiameseParkinsonNetwork inherited from PyTorch's nn.Module, it silently inherited PyTorch's highly complex __call__ function.

            2. The Secret Handshake (The forward function)
            When you write net(img0, img1), here is the exact chain of events that happens in the fraction of a millisecond:

            Python sees the parentheses and triggers the hidden __call__ function inherited from PyTorch.

            PyTorch briefly takes control. It sets up the invisible mathematical tracking system (the computational graph) so it can calculate the calculus gradients later.

            Once the setup is done, PyTorch automatically looks inside your code for a function named forward and passes img0 and img1 directly into it.

            Your forward function does the actual work (passing the images through the convolution layers), and hands the results (output1, output2) back out."""
            
            output1, output2 = model(img0, img1)
            
            final_loss_contrastive = criterion(output1, output2, label)
            
            # .backward(): Takes: None. Returns: None. Traverses the computational graph backward to calculate the gradients (calculus derivatives) for all network weights.
            final_loss_contrastive.backward()
            
            # optimizer.step(): Takes: None. Returns: None. Tells the optimizer to update the network's weights based on the gradients just calculated by .backward().
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
            
            # .item(): Takes: None. Returns: A standard Python float/integer extracted from a tensor that only contains a single value.
            epoch_loss += final_loss_contrastive.item()
            
        # len(): Takes: A collection (like train_dataloader). Returns: An integer representing the number of items (batches) in that collection.
        print(f"Epoch [{epoch+1}/{epochs}] | Average Loss: {epoch_loss / len(train_dataloader):.4f}")

    # os.makedirs(): Takes: A string path, and exist_ok (bool). Returns: None. Creates the specified directory structure on the hard drive.
    os.makedirs(weights_dir, exist_ok=True)
    
    # os.path.join(): Takes: Multiple string path components. Returns: A single string representing a properly formatted file path for the current operating system.
    # Dynamically names the output file based on your input!
    save_path = os.path.join(weights_dir, f"siamese_{test_type}.pth") 
    
    # model.state_dict(): Takes: None. Returns: A Python dictionary containing the current weights and biases of every layer in the model.
    # torch.save(): Takes: A Python object (like a state_dict) and a file path string. Returns: None. Saves the object to the hard drive at the specified path.
    torch.save(model.state_dict(), save_path)
    print(f"Success! Weights saved to: {save_path}\n")

if __name__ == "__main__":
    #This line tells Python: "Only run the code below this line IF I am running this specific file directly. If another file is just importing me to borrow my functions, ignore this section completely."
    # This captures what you type in the terminal
    
    # argparse.ArgumentParser(): Takes: a description string. Returns: An ArgumentParser object used to handle terminal commands.
    parser = argparse.ArgumentParser(description="Train Parkinson's Siamese Network")


    """strict rules about what the user is allowed to type.

    '--type': The user must use this exact flag.

    type=str: Whatever comes after the flag must be treated as text (a string).

    required=True: The script will refuse to run if the user forgets this flag.

    help: If the user types python script.py --help, this exact message will pop up to guide them."""
    
    # parser.add_argument(): Takes: Flag string, expected variable type, required boolean, help string. Returns: None. Configures the parser to expect specific terminal flags.
    parser.add_argument(
        '--type', 
        type=str, 
        required=True, 
        help="The name of the subfolder to train (e.g., spiral, meander, circle)"
    )
    
    # parser.parse_args(): Takes: None (reads from sys.argv automatically). Returns: A Namespace object containing all the parsed values from the terminal.
    args = parser.parse_args()
    #What it does: This line actually executes the rules. It reads the terminal, checks that you followed the rules (meaning you included --type), grabs the word you typed, and saves it inside the args box.
    
    # Pass the terminal argument into the training function
    train_model(args.type)