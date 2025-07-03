#!/usr/bin/env python3
import tkinter as tk
from tkinter import filedialog, messagebox, scrolledtext
import firebase_admin
from firebase_admin import credentials, firestore
import csv
import json
import os
import platform
import sys
import uuid # Import uuid for generating random document IDs

# --- Global Variables for GUI Elements ---
log_text_widget = None
root_window = None

# --- Firebase Initialization Configuration ---
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SERVICE_ACCOUNT_KEY_FILENAME = 'serviceAccount.json'
SERVICE_ACCOUNT_KEY_PATH = os.path.join(SCRIPT_DIR, SERVICE_ACCOUNT_KEY_FILENAME)

PROJECT_ID = 'innopia-ef948'
FIRESTORE_DATABASE_ID = 'google-cts' # Your specific named Firestore database ID
db = None # Initialize db as None, will be set after successful connection
firebase_app_instance = None # To hold the initialized Firebase App instance

def log_message(message, tag=""):
    if log_text_widget:
        log_text_widget.insert(tk.END, message + "\n", tag)
        log_text_widget.see(tk.END) # Scroll to the end
        if root_window:
            root_window.update_idletasks() # Force GUI update
    else:
        print(f"LOG (no GUI yet): {message}")

def initialize_firebase():
    """Initializes Firebase Admin SDK and connects to the specified Firestore database."""
    global db, firebase_app_instance # Declare global variables
    log_message("Attempting to initialize Firebase...")
    try:
        if not os.path.exists(SERVICE_ACCOUNT_KEY_PATH):
            error_msg = f"Service account key not found at: {SERVICE_ACCOUNT_KEY_PATH}"
            log_message(f"Error: {error_msg}", "error")
            messagebox.showerror("Firebase Error", error_msg + "\nPlease ensure 'serviceAccount.json' is in the same directory as the script.")
            return False

        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        
        # Initialize Firebase App if not already initialized
        # Use a named app instance for clarity and to prevent re-initialization errors
        if not firebase_admin._apps: # Check if any app is initialized by default
            firebase_app_instance = firebase_admin.initialize_app(cred, name='default_app_for_' + PROJECT_ID)
            log_message(f"Firebase app initialized: {firebase_app_instance.name}", "info")
        else:
            try:
                # Try to get the default app if it exists (e.g., if initialized elsewhere)
                firebase_app_instance = firebase_admin.get_app()
                log_message(f"Using existing Firebase app: {firebase_app_instance.name}", "info")
            except ValueError:
                # If no default app, but _apps is not empty, it means a named app exists but not 'default_app_for_PROJECT_ID'
                # Initialize it explicitly
                firebase_app_instance = firebase_admin.initialize_app(cred, name='default_app_for_' + PROJECT_ID)
                log_message(f"Firebase app initialized (as non-default): {firebase_app_instance.name}", "info")
            
        # Get the Firestore client for the *specific app instance* and *named* database
        # THIS IS THE CRUCIAL LINE for connecting to 'innopia1'
        # db = firestore.client(app=firebase_app_instance, database=FIRESTORE_DATABASE_ID) 
        db = firestore.client(app=firebase_app_instance) 
        db = firestore.firestore(app=firebase_app_instance, db_id) 
        
        log_message(f"Firestore client connected to database: {FIRESTORE_DATABASE_ID}", "success")
        return True
    except Exception as e:
        error_msg = (f"Failed to initialize Firebase or connect to database '{FIRESTORE_DATABASE_ID}': {e}\n"
                     "Please ensure:\n"
                     f"1. 'serviceAccount.json' is correct and readable at {SERVICE_ACCOUNT_KEY_PATH}.\n"
                     f"2. Cloud Firestore database '{FIRESTORE_DATABASE_ID}' is enabled for project '{PROJECT_ID}' in the Firebase Console.\n"
                     "   Visit: https://console.cloud.google.com/datastore/setup?project=" + PROJECT_ID)
        log_message(f"Firebase Init Error: {e}", "error")
        messagebox.showerror("Firebase Error", error_msg)
        return False

# --- Functions for Firestore Upload ---

def upload_data_to_firestore(data_to_upload, collection_id, module, testcase, tool_version): # Added arguments for flexibility
    """
    Uploads data to Firestore with the structure: Collection (CTS) -> Random Document ID.
    The document contains Module, TestCase, ToolVersion, and the actual data.
    """
    if db is None:
        log_message("Firestore connection not established. Please check Firebase initialization.", "error")
        messagebox.showerror("Upload Error", "Firestore connection not established.")
        return

    if not all([collection_id, module, testcase, tool_version]):
        log_message("Collection ID, Module, TestCase, and ToolVersion fields cannot be empty for upload.", "warning")
        messagebox.showerror("Upload Error", "Missing Required Fields", "Collection ID, Module, TestCase, and ToolVersion fields cannot be empty.")
        return

    # Generate a random document ID (UUID)
    doc_id = str(uuid.uuid4())
    doc_path = f"{collection_id}/{doc_id} (in '{FIRESTORE_DATABASE_ID}')"
    log_message(f"Attempting to upload data to: {doc_path}")

    try:
        doc_ref = db.collection(collection_id).document(doc_id)

        # Prepare the data for the document
        final_document_data = {
            "Module": module,
            "TestCase": testcase,
            "ToolVersion": tool_version,
            "Timestamp": firestore.SERVER_TIMESTAMP,
            "Data": data_to_upload # The actual content from input or file
        }

        log_message("Uploading single document...", "info")
        doc_ref.set(final_document_data)
        log_message(f"Successfully uploaded document to {doc_path}", "success")
        messagebox.showinfo("Upload Successful", f"Data uploaded to Firestore at:\n{doc_path}")
    except Exception as e:
        log_message(f"Error uploading data to Firestore: {e}", "error")
        messagebox.showerror("Upload Error", f"Error uploading data to Firestore: {e}\n"
                                              "Please check your Firebase project and Firestore setup, "
                                              "especially permissions for the service account.")

# --- GUI Functions ---

def upload_from_inputs():
    """Uploads data from the input fields directly to Firestore."""
    log_message("Initiating upload from input fields...")
    
    # Data to be embedded under the "Data" field in the Firestore document
    input_data_payload = {
        "UserProvidedType": entry_type.get(), # Renamed to avoid conflict with collection_id
        # You can add other fields here if they are part of the 'Data' payload
    }
    
    # Fields that define the document's location and core metadata
    collection_id_val = entry_type.get() # Now used as the collection ID
    module_val = entry_module.get()
    testcase_val = entry_test_case.get()
    tool_version_val = entry_tool_version.get()

    upload_data_to_firestore(input_data_payload, collection_id_val, module_val, testcase_val, tool_version_val)

def upload_file():
    """Opens a file dialog, parses the chosen file, and uploads its content."""
    log_message("Opening file dialog...", "info")
    file_path = filedialog.askopenfilename(
        filetypes=[("CSV files", "*.csv"), ("JSON files", "*.json")]
    )
    if not file_path:
        log_message("File selection cancelled.", "info")
        return

    log_message(f"Selected file: {os.path.basename(file_path)}", "info")
    file_extension = os.path.splitext(file_path)[1].lower()
    parsed_data = None

    if file_extension == '.csv':
        log_message("Parsing CSV file...", "info")
        try:
            with open(file_path, mode='r', encoding='utf-8') as file:
                csv_reader = csv.DictReader(file)
                parsed_data = [row for row in csv_reader]
            log_message(f"Successfully parsed CSV file: {os.path.basename(file_path)}", "success")
            messagebox.showinfo("File Parsed", f"Successfully parsed CSV file: {os.path.basename(file_path)}")
        except Exception as e:
            log_message(f"Error reading CSV file: {e}", "error")
            messagebox.showerror("File Error", f"Error reading CSV file: {e}")
            return
    elif file_extension == '.json':
        log_message("Parsing JSON file...", "info")
        try:
            with open(file_path, mode='r', encoding='utf-8') as file:
                parsed_data = json.load(file)
            log_message(f"Successfully parsed JSON file: {os.path.basename(file_path)}", "success")
            messagebox.showinfo("File Parsed", f"Successfully parsed JSON file: {os.path.basename(file_path)}")
        except Exception as e:
            log_message(f"Error reading JSON file: {e}", "error")
            messagebox.showerror("File Error", f"Error reading JSON file: {e}")
            return
    else:
        log_message(f"Unsupported file type selected: {file_extension}", "warning")
        messagebox.showwarning("Unsupported File Type", "Please select a .csv or .json file.")
        return

    if parsed_data:
        # Default values for file uploads
        collection_id_val = "FileUploads" # A generic collection for files
        module_val = "UnknownModule"   
        testcase_val = os.path.basename(file_path).split('.')[0] # Filename as test case
        tool_version_val = entry_tool_version.get() # Get from GUI input

        # Attempt to extract metadata from the parsed file data itself
        if isinstance(parsed_data, list) and parsed_data:
            if parsed_data and isinstance(parsed_data[0], dict):
                if 'Type' in parsed_data[0]:
                    collection_id_val = parsed_data[0]['Type']
                if 'Module' in parsed_data[0]:
                    module_val = parsed_data[0]['Module']
                if 'TestCase' in parsed_data[0]:
                    testcase_val = parsed_data[0]['TestCase']
                if 'ToolVersion' in parsed_data[0]: # Also try to get tool version from file
                    tool_version_val = parsed_data[0]['ToolVersion']
        elif isinstance(parsed_data, dict):
            if 'Type' in parsed_data:
                collection_id_val = parsed_data['Type']
            if 'Module' in parsed_data:
                module_val = parsed_data['Module']
            if 'TestCase' in parsed_data:
                testcase_val = parsed_data['TestCase']
            if 'ToolVersion' in parsed_data:
                tool_version_val = parsed_data['ToolVersion']
        
        # Update input fields for user's reference
        entry_type.delete(0, tk.END)
        entry_type.insert(0, collection_id_val)
        entry_module.delete(0, tk.END)
        entry_module.insert(0, module_val)
        entry_test_case.delete(0, tk.END)
        entry_test_case.insert(0, testcase_val)
        entry_tool_version.delete(0, tk.END)
        entry_tool_version.insert(0, tool_version_val) # Update tool version too
        
        upload_data_to_firestore(parsed_data, collection_id_val, module_val, testcase_val, tool_version_val)


# --- Main Application Window Setup ---

def create_gui():
    global root_window, log_text_widget, entry_type, entry_tool_version, entry_module, entry_test_case

    root_window = tk.Tk()
    root_window.title("Firestore Data Uploader - " + PROJECT_ID + " (DB: " + FIRESTORE_DATABASE_ID + ")")
    root_window.geometry("500x700")

    input_frame = tk.Frame(root_window, padx=10, pady=10)
    input_frame.pack(fill=tk.X)

    tk.Label(input_frame, text="Collection ID:").grid(row=0, column=0, sticky="w", pady=2) # Changed label
    entry_type = tk.Entry(input_frame, width=40)
    entry_type.grid(row=0, column=1, pady=2, padx=5)
    entry_type.insert(0, "CTS")

    tk.Label(input_frame, text="ToolVersion:").grid(row=1, column=0, sticky="w", pady=2)
    entry_tool_version = tk.Entry(input_frame, width=40)
    entry_tool_version.grid(row=1, column=1, pady=2, padx=5)
    entry_tool_version.insert(0, "cts-r8")

    tk.Label(input_frame, text="Module:").grid(row=2, column=0, sticky="w", pady=2)
    entry_module = tk.Entry(input_frame, width=40)
    entry_module.grid(row=2, column=1, pady=2, padx=5)
    entry_module.insert(0, "CtsTestModule1")

    tk.Label(input_frame, text="TestCase:").grid(row=3, column=0, sticky="w", pady=2)
    entry_test_case = tk.Entry(input_frame, width=40)
    entry_test_case.grid(row=3, column=1, pady=2, padx=5)
    entry_test_case.insert(0, "CtsTestCase1")

    button_frame = tk.Frame(root_window, pady=10)
    button_frame.pack()

    upload_input_btn = tk.Button(button_frame, text="Upload Input Data to Firestore", command=upload_from_inputs, height=2, width=30)
    upload_input_btn.pack(side=tk.LEFT, padx=10)

    upload_file_btn = tk.Button(button_frame, text="Upload CSV/JSON File", command=upload_file, height=2, width=30)
    upload_file_btn.pack(side=tk.RIGHT, padx=10)

    tk.Label(root_window, text="Activity Log:").pack(pady=5, anchor="w", padx=10)
    log_text_widget = scrolledtext.ScrolledText(root_window, wrap=tk.WORD, height=15, width=60)
    log_text_widget.pack(padx=10, pady=5, fill=tk.BOTH, expand=True)

    log_text_widget.tag_config("info", foreground="blue")
    log_text_widget.tag_config("success", foreground="green", font=("Arial", 9, "bold"))
    log_text_widget.tag_config("warning", foreground="orange")
    log_text_widget.tag_config("error", foreground="red", font=("Arial", 9, "bold"))

    footer_frame = tk.Frame(root_window)
    footer_frame.pack(side=tk.BOTTOM, fill=tk.X, pady=5)

    tk.Label(footer_frame, text="created by ik", font=("Arial", 9, "italic")).pack(side=tk.LEFT, padx=10)

    os_info_label = tk.Label(footer_frame, text=f"Running on: {platform.system()} {platform.release()} ({platform.machine()})")
    os_info_label.pack(side=tk.RIGHT, padx=10)
    
    if not initialize_firebase():
        log_message("Firebase initialization failed at startup. Please review errors above.", "error")

    root_window.mainloop()

if __name__ == "__main__":
    create_gui()