# Python script for a media analysis tool with a tkinter UI.
# This version uses 'dumpsys SurfaceFlinger' to detect active video resolution.

import subprocess
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext, filedialog
import threading
import re

# Function to run shell commands.
def run_cmd(cmd):
    try:
        result = subprocess.run(
            cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True,
            text=True, encoding='utf-8', errors='ignore', timeout=15
        )
        return result.stdout if result.returncode == 0 else f"Error:\n{result.stderr}"
    except Exception as e:
        return f"Error: {str(e)}"

# Get connected ADB devices.
def get_adb_devices():
    devices = []
    for line in run_cmd('adb devices').splitlines():
        if '\tdevice' in line:
            devices.append(line.split('\t')[0])
    return devices

# Get output from specified dumpsys service.
def get_dumpsys_info(device, service):
    return run_cmd(f'adb -s {device} shell dumpsys {service}')

# Official Android HDR Type constants.
# Source: https://developer.android.com/reference/android/view/Display.HdrCapabilities
HDR_HARDWARE_MAP = {
    '1': 'Dolby Vision',
    '2': 'HDR10',
    '3': 'HLG',
    '4': 'HDR10+',
}

# Parse display and media info.
def parse_media_info(device):
    display_output = get_dumpsys_info(device, 'display')
    surfaceflinger_output = get_dumpsys_info(device, 'SurfaceFlinger')

    # --- 1. Parse Display Capabilities (Hardware Support) ---
    system_resolution = 'Unknown'
    hdr_support = {name: 'No' for name in HDR_HARDWARE_MAP.values()}
    hdr_support['DolbyAtmos'] = 'No (Audio Format)'

    active_mode_match = re.search(r'mActiveSfDisplayMode=DisplayMode{.*?width=(\d+), height=(\d+).*?}', display_output)
    if active_mode_match:
        width, height = int(active_mode_match.group(1)), int(active_mode_match.group(2))
        res_str = f"{width}x{height}"
        system_resolution = f'4K ({res_str})' if width >= 3840 else f'1080p ({res_str})' if width >= 1920 else res_str

    hdr_caps_match = re.search(r'hdrCapabilities HdrCapabilities{mSupportedHdrTypes=\[(.*?)\],', display_output)
    if hdr_caps_match:
        for type_id in hdr_caps_match.group(1).replace(' ', '').split(','):
            if type_id in HDR_HARDWARE_MAP:
                hdr_support[HDR_HARDWARE_MAP[type_id]] = 'Yes (Supported)'

    # --- 2. Parse Active Video Stream Resolution from SurfaceFlinger ---
    playing_resolution = "N/A"
    
    # Find layers that look like video surfaces (e.g., from YouTube, Netflix)
    # They often have names like "SurfaceView" or contain the package name of the video app.
    # The regex looks for a layer with a buffer size (e.g., 3840x2160).
    # We prioritize larger buffers as they are more likely to be video content.
    video_layers = re.findall(r'\|\s+SurfaceView.*?\n.*?buffer.*?\s+(\d+)\s+(\d+)', surfaceflinger_output, re.DOTALL)
    
    max_res = 0
    for width_str, height_str in video_layers:
        width, height = int(width_str), int(height_str)
        if width * height > max_res:
            max_res = width * height
            playing_resolution = f"{width}x{height}"

    return system_resolution, hdr_support, playing_resolution

# Main application class.
class MediaAnalysisApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Media Analysis Tool")
        self.geometry("1000x800")

        # UI Setup
        top_frame = ttk.Frame(self, padding="10")
        top_frame.pack(fill=tk.X)
        ttk.Label(top_frame, text="Select ADB Device:").pack(side=tk.LEFT, padx=(0, 5))
        self.device_combo = ttk.Combobox(top_frame, state='readonly', width=30)
        self.device_combo.pack(side=tk.LEFT, fill=tk.X, expand=True)
        ttk.Button(top_frame, text="Refresh Devices", command=self.refresh_devices).pack(side=tk.LEFT, padx=5)

        main_pane = ttk.PanedWindow(self, orient=tk.VERTICAL)
        main_pane.pack(fill=tk.BOTH, expand=True, padx=10, pady=(0, 10))

        top_content = ttk.Frame(main_pane)
        main_pane.add(top_content, weight=1)
        
        action_frame = ttk.Frame(top_content, padding=(0, 10))
        action_frame.pack(fill=tk.X)
        ttk.Button(action_frame, text="Refresh Status", command=self.refresh_status).pack(side=tk.LEFT)

        info_frame = ttk.LabelFrame(top_content, text="Display & Media Status", padding="10")
        info_frame.pack(fill=tk.BOTH, expand=True)
        self.info_tree = ttk.Treeview(info_frame, columns=('Property', 'Value'), show='headings')
        self.info_tree.heading('Property', text='Property')
        self.info_tree.heading('Value', text='Value')
        self.info_tree.pack(fill=tk.BOTH, expand=True)

        log_frame = ttk.LabelFrame(main_pane, text="Log", padding="10")
        main_pane.add(log_frame, weight=2)
        self.log_text = scrolledtext.ScrolledText(log_frame, state='normal', wrap='word', height=15)
        self.log_text.pack(fill=tk.BOTH, expand=True, side=tk.LEFT)
        ttk.Button(log_frame, text="Clear Log", command=self.clear_log).pack(side=tk.RIGHT, anchor='n', padx=(5,0))

        self.refresh_devices()

    def log(self, message):
        self.log_text.insert(tk.END, message + '\n')
        self.log_text.see(tk.END)

    def refresh_devices(self):
        self.log("Refreshing device list...")
        devices = get_adb_devices()
        self.device_combo['values'] = devices
        if devices:
            self.device_combo.current(0)
            self.log(f"Found devices: {', '.join(devices)}")
        else:
            self.log("No ADB devices found.")

    def clear_log(self):
        self.log_text.delete('1.0', tk.END)

    def refresh_status(self):
        device = self.device_combo.get()
        if not device:
            messagebox.showwarning("No Device", "Please select a device.")
            return
        self.log(f"Refreshing status for {device}...")
        threading.Thread(target=self._update_status_thread, args=(device,), daemon=True).start()

    def _update_status_thread(self, device):
        try:
            system_res, hdr_support, playing_res = parse_media_info(device)
            self.after(0, self._update_ui, system_res, hdr_support, playing_res)
            self.log("Status refreshed successfully.")
        except Exception as e:
            self.log(f"Error refreshing status: {e}")

    def _update_ui(self, system_res, hdr_support, playing_res):
        for i in self.info_tree.get_children():
            self.info_tree.delete(i)
        
        self.info_tree.insert('', 'end', values=('System Resolution', system_res))
        self.info_tree.insert('', 'end', values=('Active Video Resolution', playing_res))
        self.info_tree.insert('', 'end', values=('', ''))
        self.info_tree.insert('', 'end', values=('HDR Hardware Support', '---'))
        for key, value in hdr_support.items():
            self.info_tree.insert('', 'end', values=(f"  {key}", value))

if __name__ == '__main__':
    app = MediaAnalysisApp()
    app.mainloop()
