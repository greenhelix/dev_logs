# English comments are provided for all code.

import tkinter as tk
from tkinter import filedialog, messagebox, ttk, scrolledtext, simpledialog
import subprocess
import threading
import platform
import os
import datetime
import queue
import re

class DeviceFileExplorer(tk.Toplevel):
    """
    A Toplevel window that acts as a file explorer for a selected Android device.
    Allows browsing directories and deleting files/folders.
    """
    def __init__(self, parent, serial, threaded_log_func):
        super().__init__(parent)
        self.title(f"File Explorer - {serial}")
        self.geometry("600x400")
        self.serial = serial
        self.threaded_log = threaded_log_func
        self.current_path = "/sdcard/"

        # --- UI Elements ---
        path_frame = tk.Frame(self)
        path_frame.pack(fill=tk.X, padx=5, pady=5)

        self.path_var = tk.StringVar(value=self.current_path)
        path_entry = tk.Entry(path_frame, textvariable=self.path_var)
        path_entry.pack(side=tk.LEFT, fill=tk.X, expand=True)
        path_entry.bind("<Return>", self.navigate_to_path)

        go_button = tk.Button(path_frame, text="Go", command=self.navigate_to_path)
        go_button.pack(side=tk.LEFT)

        self.listbox = tk.Listbox(self, selectmode=tk.EXTENDED)
        self.listbox.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        self.listbox.bind("<Double-1>", self.on_double_click)
        
        button_frame = tk.Frame(self)
        button_frame.pack(fill=tk.X, padx=5, pady=5)

        up_button = tk.Button(button_frame, text="Up", command=self.go_up)
        up_button.pack(side=tk.LEFT)

        refresh_button = tk.Button(button_frame, text="Refresh", command=self.refresh_list)
        refresh_button.pack(side=tk.LEFT)
        
        delete_button = tk.Button(button_frame, text="Delete Selected", command=self.delete_selected)
        delete_button.pack(side=tk.RIGHT)

        self.refresh_list()

    def run_adb_command(self, command):
        """Runs an ADB command specifically for the file explorer."""
        full_command = f"adb -s {self.serial} shell {command}"
        try:
            return subprocess.check_output(full_command, shell=True, text=True, stderr=subprocess.STDOUT, encoding='utf-8')
        except subprocess.CalledProcessError as e:
            messagebox.showerror("Error", f"Command failed: {command}\n\n{e.output}")
            return None

    def refresh_list(self):
        """Refreshes the file and directory listing."""
        self.listbox.delete(0, tk.END)
        # Use `ls -F` to distinguish files and directories
        output = self.run_adb_command(f'ls -F "{self.current_path}"')
        if output:
            items = sorted(output.strip().split('\n'), key=lambda s: s.lower())
            for item in items:
                if item:
                    self.listbox.insert(tk.END, item)
        self.path_var.set(self.current_path)

    def navigate_to_path(self, event=None):
        """Navigates to the path specified in the entry box."""
        self.current_path = self.path_var.get()
        if not self.current_path.endswith('/'):
            self.current_path += '/'
        self.refresh_list()

    def on_double_click(self, event):
        """Handles double-click events on listbox items."""
        selection = self.listbox.curselection()
        if not selection:
            return
        
        item = self.listbox.get(selection[0])
        if item.endswith('/'): # It's a directory
            # Construct new path, handling root case
            if self.current_path == '/':
                self.current_path += item
            else:
                self.current_path += item
            self.refresh_list()
            
    def go_up(self):
        """Navigates to the parent directory."""
        if self.current_path != "/":
            # Go to parent directory
            parts = self.current_path.strip('/').split('/')
            self.current_path = '/' + '/'.join(parts[:-1]) + '/'
            if self.current_path == "//": # Handle root's parent
                self.current_path = "/"
            self.refresh_list()

    def delete_selected(self):
        """Deletes selected files or folders after confirmation."""
        selection = self.listbox.curselection()
        if not selection:
            messagebox.showwarning("No Selection", "Please select items to delete.")
            return

        items_to_delete = [self.listbox.get(i) for i in selection]
        
        confirm = messagebox.askyesno("Confirm Deletion", f"Are you sure you want to permanently delete these {len(items_to_delete)} items?\n\n{', '.join(items_to_delete[:3])}{'...' if len(items_to_delete) > 3 else ''}")

        if confirm:
            for item in items_to_delete:
                full_path = os.path.join(self.current_path, item).replace("\\", "/")
                self.threaded_log(f"[{self.serial}] Deleting: {full_path}")
                output = self.run_adb_command(f'rm -rf "{full_path}"')
                if output: # rm -rf usually has no output on success
                    self.threaded_log(f"[{self.serial}] Deletion output: {output}")
            self.refresh_list()


class AdbTool:
    """Main ADB Tool application class."""
    def __init__(self, root):
        self.root = root
        self.root.title("ADB Control Program")
        self.root.geometry("800x650")

        self.command_history = []
        self.log_size_limit_bytes = 100 * 1024 * 1024
        self.log_limit_warned = False
        self.log_queue = queue.Queue()
        self.active_threads = 0

        # --- Layout ---
        main_frame = tk.Frame(root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        left_frame = tk.Frame(main_frame, width=300)
        left_frame.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 10))
        right_frame = tk.Frame(main_frame)
        right_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        # --- Widgets (condensed for brevity) ---
        device_frame = tk.LabelFrame(left_frame, text="Connected Devices")
        device_frame.pack(fill=tk.X, pady=(0, 10))
        self.device_listbox = tk.Listbox(device_frame, selectmode=tk.EXTENDED, height=8)
        self.device_listbox.pack(side=tk.LEFT, fill=tk.X, expand=True)
        tk.Scrollbar(device_frame, orient=tk.VERTICAL, command=self.device_listbox.yview).pack(side=tk.RIGHT, fill=tk.Y)
        self.device_listbox.config(yscrollcommand=tk.Scrollbar(device_frame).set)
        
        tk.Button(left_frame, text="Refresh Devices", command=self.refresh_devices).pack(fill=tk.X, pady=(0, 10))
        tk.Button(left_frame, text="Push ZIP to Device(s)", command=self.push_zip_file).pack(fill=tk.X, pady=(0, 10))
        
        key_event_frame = tk.LabelFrame(left_frame, text="Key Events")
        key_event_frame.pack(fill=tk.X, pady=(0, 10))
        keys = {"UP": "DPAD_UP", "LEFT": "DPAD_LEFT", "ENTER": "ENTER", "RIGHT": "DPAD_RIGHT", "DOWN": "DPAD_DOWN", "BACK": "BACK", "HOME": "HOME"}
        dpad_frame = tk.Frame(key_event_frame); dpad_frame.pack(pady=5)
        tk.Button(dpad_frame, text="▲", command=lambda: self.send_key_event(keys["UP"])).grid(row=0, column=1)
        tk.Button(dpad_frame, text="◀", command=lambda: self.send_key_event(keys["LEFT"])).grid(row=1, column=0)
        tk.Button(dpad_frame, text="OK", command=lambda: self.send_key_event(keys["ENTER"])).grid(row=1, column=1)
        tk.Button(dpad_frame, text="▶", command=lambda: self.send_key_event(keys["RIGHT"])).grid(row=1, column=2)
        tk.Button(dpad_frame, text="▼", command=lambda: self.send_key_event(keys["DOWN"])).grid(row=2, column=1)
        nav_frame = tk.Frame(key_event_frame); nav_frame.pack(pady=5)
        tk.Button(nav_frame, text="Back", command=lambda: self.send_key_event(keys["BACK"])).pack(side=tk.LEFT, padx=5)
        tk.Button(nav_frame, text="Home", command=lambda: self.send_key_event(keys["HOME"])).pack(side=tk.LEFT, padx=5)
        
        actions_frame = tk.LabelFrame(left_frame, text="Other Actions")
        actions_frame.pack(fill=tk.X, pady=(0, 10))
        tk.Button(actions_frame, text="Capture Screenshot", command=self.capture_screenshot).pack(fill=tk.X, pady=2)
        tk.Button(actions_frame, text="Restart ADB Server", command=self.restart_adb_server).pack(fill=tk.X, pady=2)
        tk.Button(actions_frame, text="Check ADB Version", command=self.check_adb_version).pack(fill=tk.X, pady=2)
        tk.Button(actions_frame, text="Device File Explorer", command=self.open_file_explorer).pack(fill=tk.X, pady=2)
        
        command_frame = tk.LabelFrame(right_frame, text="Custom ADB Command")
        command_frame.pack(fill=tk.X, pady=(0, 10))
        self.command_entry = ttk.Combobox(command_frame)
        self.command_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 5))
        tk.Button(command_frame, text="Execute", command=self.execute_custom_command).pack(side=tk.RIGHT)
        
        log_frame = tk.LabelFrame(right_frame, text="Log")
        log_frame.pack(fill=tk.BOTH, expand=True)
        self.log_text = scrolledtext.ScrolledText(log_frame, wrap=tk.WORD, state=tk.DISABLED)
        self.log_text.pack(fill=tk.BOTH, expand=True)
        log_button_frame = tk.Frame(log_frame); log_button_frame.pack(fill=tk.X, pady=5)
        tk.Button(log_button_frame, text="Save Log", command=self.save_log).pack(side=tk.LEFT, padx=5)
        tk.Button(log_button_frame, text="Clear Log", command=self.clear_log).pack(side=tk.LEFT, padx=5)

        self.check_environment()
        self.refresh_devices()
        self.root.after(100, self.process_log_queue)

    def process_log_queue(self):
        try:
            while True: self.log(self.log_queue.get_nowait())
        except queue.Empty: pass
        finally: self.root.after(100, self.process_log_queue)

    def threaded_log(self, message):
        self.log_queue.put(message)

    def open_file_explorer(self):
        """Opens the file explorer for the selected device."""
        selected_devices = self.get_selected_devices()
        if len(selected_devices) != 1:
            messagebox.showwarning("Selection Error", "Please select exactly one device for the File Explorer.")
            return
        DeviceFileExplorer(self.root, selected_devices[0], self.threaded_log)

    def push_file_to_device(self, serial, filepath):
        """Pushes a file and reports real-time progress using stdbuf where available."""
        command = f'adb -s {serial} push "{filepath}" /sdcard/'
        
        # Use stdbuf on Linux/macOS to disable output buffering for real-time progress
        if platform.system() != "Windows":
            command = f"stdbuf -o0 {command}"
            
        self.threaded_log(f"[{serial}] Starting push for {os.path.basename(filepath)}...")
        
        try:
            process = subprocess.Popen(command, shell=True, stderr=subprocess.PIPE, stdout=subprocess.PIPE, text=True, encoding='utf-8', errors='replace')
            
            for line in iter(process.stderr.readline, ''):
                line = line.strip()
                if line and '%' in line:
                    progress_part = re.search(r'\[\s*(\d+)%\]', line)
                    if progress_part:
                        self.threaded_log(f"[{serial}] Progress: {progress_part.group(0)}")

            process.wait()
            if process.returncode == 0:
                self.threaded_log(f"[{serial}] Push completed successfully.")
            else:
                 _, stderr_output = process.communicate()
                 self.threaded_log(f"[{serial}] Push failed. Error: {stderr_output.strip()}")

        except Exception as e:
            self.threaded_log(f"[{serial}] An exception occurred during push: {e}")
        finally:
            self.active_threads -= 1
            if self.active_threads == 0:
                self.threaded_log("All file transfers are complete.")
                self.root.after(10, lambda: messagebox.showinfo("Success", "All file transfers are complete."))

    def push_zip_file(self):
        if self.active_threads > 0:
            messagebox.showwarning("Busy", "A file push operation is already in progress."); return
        selected_devices = self.get_selected_devices()
        if not selected_devices:
            messagebox.showwarning("No Device Selected", "Please select at least one device."); return
        filepath = filedialog.askopenfilename(title="Select a file to push", filetypes=(("ZIP files", "*.zip"), ("All files", "*.*")))
        if not filepath: return
        self.active_threads = len(selected_devices)
        self.log(f"Starting parallel push for {self.active_threads} device(s).")
        for serial in selected_devices:
            threading.Thread(target=self.push_file_to_device, args=(serial, filepath), daemon=True).start()

    def send_key_event(self, key_name):
        selected_devices = self.get_selected_devices()
        if not selected_devices: messagebox.showwarning("No Device Selected", "Please select at least one device."); return
        self.threaded_log(f"Sending key event '{key_name}' to {len(selected_devices)} device(s)...")
        for serial in selected_devices:
            command = f"adb -s {serial} shell input keyevent {key_name}"
            threading.Thread(target=self.run_command, args=(command,), daemon=True).start()

    # --- Other methods remain largely the same, using threaded_log where appropriate ---
    def log(self, message):
        self.log_text.config(state=tk.NORMAL)
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)
        self.log_text.config(state=tk.DISABLED)
        if not self.log_limit_warned: threading.Thread(target=self.check_log_size, daemon=True).start()

    def check_log_size(self):
        if len(self.log_text.get("1.0", tk.END).encode('utf-8')) > self.log_size_limit_bytes:
            self.log_limit_warned = True
            self.root.after(0, self._ask_save_log)

    def _ask_save_log(self):
        answer = messagebox.askyesno("Log Size Warning", "Log > 100MB. Save and clear?")
        if answer: self.save_log(clear_after=True)
        else: self.clear_log(); self.threaded_log("Log cleared due to size limit.")

    def save_log(self, clear_after=False):
        log_content = self.log_text.get("1.0", tk.END)
        if not log_content.strip(): messagebox.showinfo("Empty Log", "Nothing to save."); return
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filepath = filedialog.asksaveasfilename(initialfile=f"adb_tool_log_{timestamp}.log", defaultextension=".log", filetypes=[("Log files", "*.log")], title="Save Log As")
        if filepath:
            try:
                with open(filepath, 'w', encoding='utf-8') as f: f.write(log_content)
                self.threaded_log(f"Log saved to {filepath}")
                if clear_after: self.clear_log(); self.log_limit_warned = False
            except Exception as e: self.threaded_log(f"Error saving log: {e}"); messagebox.showerror("Error", f"Failed to save log: {e}")

    def clear_log(self):
        self.log_text.config(state=tk.NORMAL); self.log_text.delete("1.0", tk.END); self.log_text.config(state=tk.DISABLED)
        self.log_limit_warned = False

    def run_command(self, command, check_output=False):
        try:
            if check_output:
                return subprocess.check_output(command, shell=True, text=True, stderr=subprocess.STDOUT, encoding='utf-8')
            else:
                subprocess.run(command, shell=True, check=True, text=True, capture_output=True, encoding='utf-8'); return ""
        except FileNotFoundError: self.threaded_log(f"Error: Command not found."); return None
        except subprocess.CalledProcessError as e: self.threaded_log(f"Error: {command}\n{e.stdout or e.stderr}"); return e.stdout or e.stderr

    def check_environment(self): self.threaded_log("Checking environment..."); self.threaded_log(f"OS: {platform.system()}"); self.run_command("adb version")
    def refresh_devices(self):
        self.threaded_log("Refreshing devices..."); self.device_listbox.delete(0, tk.END)
        output = self.run_command("adb devices", True)
        if output:
            devices = [line.split('\t')[0] for line in output.strip().split('\n')[1:] if '\tdevice' in line]
            for device in devices: self.device_listbox.insert(tk.END, device)
            self.threaded_log(f"Found {len(devices)} device(s).")

    def get_selected_devices(self): return [self.device_listbox.get(i) for i in self.device_listbox.curselection()]
    def execute_custom_command(self):
        command_str = self.command_entry.get();
        if not command_str: return
        if command_str not in self.command_history: self.command_history.append(command_str); self.command_entry['values'] = self.command_history
        selected_devices = self.get_selected_devices()
        if not selected_devices:
            if "adb" in command_str: self.threaded_log(f"Global cmd: {command_str}"); threading.Thread(target=lambda: self.threaded_log(self.run_command(command_str, True)), daemon=True).start()
            else: messagebox.showwarning("No Device", "Select a device."); return
        for serial in selected_devices:
            full_cmd = f"adb -s {serial} shell {command_str}"
            threading.Thread(target=lambda s=serial, c=full_cmd: self.threaded_log(f"[{s}]\n{self.run_command(c, True)}"), daemon=True).start()

    def capture_screenshot(self):
        selected = self.get_selected_devices();
        if len(selected) != 1: messagebox.showwarning("Selection Error", "Select exactly one device."); return
        serial = selected[0]
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        pc_path = filedialog.asksaveasfilename(initialfile=f"screenshot_{serial}_{timestamp}.png", defaultextension=".png", filetypes=[("PNG", "*.png")], title="Save Screenshot")
        if not pc_path: return
        def task():
            dev_path = f"/sdcard/screenshot_{timestamp}.png";
            self.run_command(f"adb -s {serial} shell screencap -p {dev_path}");
            self.run_command(f'adb -s {serial} pull "{dev_path}" "{pc_path}"');
            self.run_command(f"adb -s {serial} shell rm {dev_path}");
            self.threaded_log(f"Screenshot saved to {pc_path}");
            self.root.after(0, lambda: messagebox.showinfo("Success", f"Screenshot saved:\n{pc_path}"))
        threading.Thread(target=task, daemon=True).start()

    def restart_adb_server(self):
        self.threaded_log("Restarting ADB server...")
        def task():
            self.run_command("adb kill-server"); self.threaded_log("ADB server killed.")
            self.run_command("adb start-server"); self.threaded_log("ADB server started.")
            self.root.after(0, lambda: messagebox.showinfo("ADB Restarted", "ADB server restarted."))
        threading.Thread(target=task, daemon=True).start()

    def check_adb_version(self): self.threaded_log("Checking ADB version..."); self.run_command("adb version", True)

if __name__ == "__main__":
    if platform.system() == "Windows":
        from ctypes import windll
        try: windll.shcore.SetProcessDpiAwareness(1)
        except: pass
    root = tk.Tk()
    app = AdbTool(root)
    root.mainloop()
