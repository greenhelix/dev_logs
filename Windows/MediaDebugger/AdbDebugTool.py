# -*- coding: utf-8 -*-

import subprocess
import threading
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
from tkinter.font import Font
import sys
import re

class AdbDebugTool(tk.Tk):
    """
    v5.0: 3단계 교차 검증을 통해 현재 재생 상태 모니터링 정확도를 높인 최종 버전입니다.
    """
    def __init__(self):
        super().__init__()

        # --- 1. 창 설정 및 스타일 ---
        self.title("ADB 미디어 디버깅 도구 (v5.0 - 최종)")
        self.geometry("1200x900")
        self.minsize(1000, 700)
        self.configure(bg="#2c2f33")

        style = ttk.Style(self)
        style.theme_use('clam')
        style.configure("TCombobox", fieldbackground="#3c3f41", background="#7289da", foreground="white", arrowcolor="white")
        style.map('TCombobox', fieldbackground=[('readonly', '#3c3f41')])
        self.label_font = Font(family="맑은 고딕", size=10)
        self.title_font = Font(family="맑은 고딕", size=13, weight="bold")
        self.info_font = Font(family="맑은 고딕", size=11)
        self.log_font = Font(family="D2Coding", size=10)

        self.commands = {
            "dumpsys display": "디스플레이 기본 정보 (HDR 지원 등)",
            "dumpsys media.codec": "지원 미디어 코덱 목록",
            "dumpsys media.audio_policy": "오디오 정책 및 장치 정보",
            "getprop": "시스템 속성 전체 목록",
        }

        # --- 2. 위젯 생성 및 초기화 ---
        self.create_widgets()
        self.refresh_devices()

    def create_widgets(self):
        top_frame = tk.Frame(self, bg="#23272a", padx=10, pady=10)
        top_frame.pack(fill=tk.X)

        tk.Label(top_frame, text="디바이스:", bg="#23272a", fg="white", font=self.label_font).pack(side=tk.LEFT)
        self.device_combo = ttk.Combobox(top_frame, state="readonly", width=20, font=self.label_font)
        self.device_combo.pack(side=tk.LEFT, padx=(5, 15))
        self.device_refresh_button = tk.Button(top_frame, text="⟳", command=self.refresh_devices, bg="#5865f2", fg="white", relief="flat", font=self.label_font)
        self.device_refresh_button.pack(side=tk.LEFT)

        tk.Label(top_frame, text="명령어:", bg="#23272a", fg="white", font=self.label_font).pack(side=tk.LEFT, padx=(15,5))
        command_display_list = [f"{cmd}  ({desc})" for cmd, desc in self.commands.items()]
        self.command_combo = ttk.Combobox(top_frame, state="readonly", font=self.label_font)
        self.command_combo['values'] = command_display_list
        self.command_combo.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=5)
        self.command_combo.current(0)
        self.run_button = tk.Button(top_frame, text="실행", command=self.run_command_thread, bg="#7289da", fg="white", relief="flat", font=self.label_font, padx=10)
        self.run_button.pack(side=tk.RIGHT)

        main_pane = tk.PanedWindow(self, orient=tk.HORIZONTAL, bg="#2c2f33", sashwidth=8, sashrelief=tk.RAISED)
        main_pane.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        left_pane_frame = tk.Frame(main_pane, bg="#2c2f33")
        
        status_frame = tk.LabelFrame(left_pane_frame, text="현재 재생 상태 모니터링", bg="#2c2f33", fg="white", font=self.label_font, padx=10, pady=10)
        status_frame.pack(fill=tk.X, pady=(0, 10))
        self.current_playback_label = tk.Label(status_frame, text="재생 포맷: 확인 필요", bg="#2c2f33", fg="#43b581", font=self.title_font)
        self.current_playback_label.pack(pady=5)
        self.status_refresh_button = tk.Button(status_frame, text="현재 상태 갱신", command=self.thread_refresh_playback_status, bg="#5865f2", fg="white", relief="flat", font=self.label_font)
        self.status_refresh_button.pack(pady=5)

        info_frame = tk.LabelFrame(left_pane_frame, text="디바이스 정보", bg="#2c2f33", fg="white", font=self.label_font, padx=10, pady=10)
        info_frame.pack(fill=tk.BOTH, expand=True)
        self.info_text = tk.Text(info_frame, wrap=tk.WORD, bg="#23272a", fg="#dcddde", relief="flat", font=self.info_font, state=tk.DISABLED)
        self.info_text.pack(fill=tk.BOTH, expand=True)

        lights_frame = tk.LabelFrame(left_pane_frame, text="HDR 지원 여부", bg="#2c2f33", fg="white", font=self.label_font, padx=10, pady=10)
        lights_frame.pack(fill=tk.X, pady=(10, 0))
        self.hdr_lights = {}
        hdr_formats = ["HDR10", "HDR10+", "HLG", "Dolby Vision"]
        for fmt in hdr_formats:
            f = tk.Frame(lights_frame, bg="#2c2f33")
            f.pack(side=tk.LEFT, expand=True, fill=tk.X)
            canvas = tk.Canvas(f, width=24, height=24, bg="#2c2f33", highlightthickness=0)
            oval = canvas.create_oval(2, 2, 22, 22, fill="#4f545c", outline="")
            canvas.pack()
            label = tk.Label(f, text=fmt, bg="#2c2f33", fg="white", font=self.label_font)
            label.pack()
            self.hdr_lights[fmt] = (canvas, oval)
        
        main_pane.add(left_pane_frame, minsize=400)

        log_frame = tk.LabelFrame(main_pane, text="원본 로그", bg="#2c2f33", fg="white", font=self.label_font, padx=10, pady=10)
        self.log_text = scrolledtext.ScrolledText(log_frame, wrap=tk.NONE, bg="#23272a", fg="#dcddde", relief="flat", font=self.log_font, state=tk.DISABLED)
        self.log_text.pack(fill=tk.BOTH, expand=True)
        main_pane.add(log_frame, minsize=500)

    def log(self, message):
        self.log_text.configure(state=tk.NORMAL)
        self.log_text.insert(tk.END, message)
        self.log_text.see(tk.END)
        self.log_text.configure(state=tk.DISABLED)
        self.update_idletasks()

    def update_info(self, content):
        self.info_text.configure(state=tk.NORMAL)
        self.info_text.delete('1.0', tk.END)
        self.info_text.insert('1.0', content)
        self.info_text.configure(state=tk.DISABLED)

    def refresh_devices(self):
        self.log("[INFO] 연결된 ADB 디바이스를 검색합니다...\n$ adb devices\n")
        try:
            startupinfo = None
            if sys.platform == "win32":
                startupinfo = subprocess.STARTUPINFO()
                startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            
            output = subprocess.check_output(['adb', 'devices'], text=True, startupinfo=startupinfo, stderr=subprocess.STDOUT)
            self.log(output + "\n")
            
            devices = [line.split('\t')[0] for line in output.strip().splitlines()[1:] if '\tdevice' in line]
            
            if devices:
                self.device_combo['values'] = devices
                self.device_combo.current(0)
                self.log(f"[SUCCESS] '{len(devices)}'개의 활성 디바이스를 찾았습니다.\n")
            else:
                self.device_combo['values'] = []
                self.device_combo.set('')
                self.log("[WARNING] 연결된 활성(device) 상태의 ADB 디바이스가 없습니다. 오른쪽 로그에서 'unauthorized' 등 다른 상태가 아닌지 확인하세요.\n")
                
        except FileNotFoundError:
            messagebox.showerror("오류", "'adb'를 찾을 수 없습니다. PATH 환경변수를 확인하세요.")
            self.log("[ERROR] 'adb' 명령을 찾을 수 없습니다.\n")
        except Exception as e:
            messagebox.showerror("오류", f"디바이스 검색 중 오류 발생: {e}")
            self.log(f"[ERROR] 디바이스 검색 실패: {e}\n")

    def run_command_thread(self):
        device = self.device_combo.get()
        if not device:
            messagebox.showwarning("경고", "디바이스를 선택해주세요.")
            return

        selected_display_text = self.command_combo.get()
        if not selected_display_text:
            messagebox.showwarning("경고", "명령어를 선택해주세요.")
            return
            
        actual_command = selected_display_text.split("  (")[0]
        
        thread = threading.Thread(target=self.execute_adb_command, args=(device, actual_command))
        thread.daemon = True
        thread.start()

    def execute_adb_command(self, device, command, silent=False):
        if not silent:
            self.update_hdr_lights([])
            self.update_info("명령어 실행 중...")
            self.log(f"\n$ adb -s {device} shell {command}\n\n")

        try:
            startupinfo = None
            if sys.platform == "win32":
                startupinfo = subprocess.STARTUPINFO()
                startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW

            full_command_list = ['adb', '-s', device, 'shell'] + command.split()
            process = subprocess.Popen(full_command_list, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace', startupinfo=startupinfo)
            
            output_str = process.communicate()[0]

            if silent:
                return output_str

            self.log(output_str)
            self.log(f"\n--- 실행 완료 (종료 코드: {process.returncode}) ---\n")
            
            if "dumpsys display" in command:
                self.parse_display_info(output_str)
            elif "dumpsys media.codec" in command:
                self.update_info(self.parse_media_codec_info(output_str))
            else:
                self.update_info("이 명령어는 요약 정보를 지원하지 않습니다.")

        except Exception as e:
            if not silent:
                self.log(f"\n[ERROR] 명령어 실행 중 오류 발생: {e}\n")
            return None

    def update_hdr_lights(self, supported_formats):
        color_on = "#43b581"
        color_off = "#4f545c"

        for fmt, (canvas, oval) in self.hdr_lights.items():
            if fmt in supported_formats:
                canvas.itemconfig(oval, fill=color_on)
            else:
                canvas.itemconfig(oval, fill=color_off)

    def parse_display_info(self, text):
        info = {}
        res_match = re.search(r'mDefaultViewport=.*?deviceWidth=(\d+).*?deviceHeight=(\d+)', text)
        if res_match: info['기본 해상도'] = f"{res_match.group(1)}x{res_match.group(2)}"
        state_match = re.search(r'mState=([A-Z_]+)', text)
        if state_match: info['화면 상태'] = state_match.group(1)

        hdr_map = {"1": "Dolby Vision", "2": "HDR10", "3": "HLG", "4": "HDR10+"}
        supported_formats = []
        hdr_match = re.search(r'supportedHdrTypes=\[([^\]]+)\]', text)
        if hdr_match:
            type_ids = hdr_match.group(1).split(',')
            supported_formats = [hdr_map.get(t.strip()) for t in type_ids if hdr_map.get(t.strip())]
        
        info['지원 HDR'] = ', '.join(supported_formats) if supported_formats else '정보 없음'
        self.update_hdr_lights(supported_formats)
        display_text = "\n\n".join([f"▶ {key}\n   {value}" for key, value in info.items()])
        self.update_info(display_text)
        
    def parse_media_codec_info(self, text):
        decoders = set(re.findall(r'^\s*decoder: name=([\w.-]+)', text, re.MULTILINE))
        encoders = set(re.findall(r'^\s*encoder: name=([\w.-]+)', text, re.MULTILINE))
        decoder_str = "▶ 디코더 (재생 가능)\n   " + ('\n   '.join(sorted(list(decoders))) or '정보 없음')
        encoder_str = "▶ 인코더 (녹화/변환 가능)\n   " + ('\n   '.join(sorted(list(encoders))) or '정보 없음')
        return f"{decoder_str}\n\n{encoder_str}"
        
    def thread_refresh_playback_status(self):
        self.current_playback_label.config(text="재생 포맷: 확인 중...")
        thread = threading.Thread(target=self.refresh_playback_status)
        thread.daemon = True
        thread.start()

    def refresh_playback_status(self):
        device = self.device_combo.get()
        if not device:
            self.current_playback_label.config(text="재생 포맷: 디바이스 없음")
            return
        
        # 1단계: media_session에서 정보 가져오기
        output_session = self.execute_adb_command(device, "dumpsys media_session", silent=True)
        if output_session:
            playback_format, source = self.parse_current_playback_from_session(output_session)
            if playback_format != "정보 없음":
                self.current_playback_label.config(text=f"재생 포맷: {playback_format} ({source})")
                return

        # 2단계: SurfaceFlinger에서 정보 가져오기
        output_flinger = self.execute_adb_command(device, "dumpsys SurfaceFlinger", silent=True)
        if output_flinger:
            playback_format, source = self.parse_current_playback_from_flinger(output_flinger)
            if playback_format != "정보 없음":
                self.current_playback_label.config(text=f"재생 포맷: {playback_format} ({source})")
                return
        
        self.current_playback_label.config(text="재생 포맷: 정보 없음 / SDR")

    def parse_current_playback_from_session(self, text):
        active_sessions = text.split('MediaSessionRecord')
        for session in active_sessions:
            if 'state=PlaybackState {state=3' in session: # state=3은 재생 중
                hdr_match = re.search(r'android.media.metadata.HDR_FORMAT=([\w+]+)', session)
                if hdr_match:
                    return hdr_match.group(1), "media_session"
        return "정보 없음", "media_session"

    def parse_current_playback_from_flinger(self, text):
        active_layers = re.findall(r'\+ Layer.*?dataspace=([\w_]+)', text)
        if not active_layers: return "정보 없음", "SurfaceFlinger"
        
        last_dataspace = active_layers[-1]
        self.log(f"[DEBUG] 현재 활성 데이터스페이스: {last_dataspace}\n")
        
        if "BT2020_PQ" in last_dataspace: return "HDR10/HDR10+", "SurfaceFlinger"
        elif "BT2020_HLG" in last_dataspace: return "HLG", "SurfaceFlinger"
        elif "DOLBY_VISION" in last_dataspace: return "Dolby Vision", "SurfaceFlinger"
        elif "BT709" in last_dataspace: return "SDR", "SurfaceFlinger"
        else: return "알 수 없음", "SurfaceFlinger"


if __name__ == '__main__':
    if sys.platform == "win32":
        try:
            from ctypes import windll
            windll.shcore.SetProcessDpiAwareness(1)
        except Exception:
            pass
            
    app = AdbDebugTool()
    app.mainloop()

