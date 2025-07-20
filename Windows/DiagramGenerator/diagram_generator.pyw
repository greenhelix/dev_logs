# BIST - Diagram Auto Maker v14 (Clean Dependency Folder Structure)
# Made by ikhwan
# 'settings' 폴더에 모든 의존성을 분리하여 깔끔하게 배포할 수 있는 최종 버전입니다.

import tkinter as tk
from tkinter import filedialog, messagebox, PanedWindow, Text, Scrollbar, END, Label, Canvas
import re
import os
import sys
import subprocess
import io
from collections import defaultdict
from PIL import Image, ImageTk

# --- 0. 배포를 위한 동적 경로 설정 (가장 먼저 실행되어야 함) ---

def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        # PyInstaller는 _MEIPASS라는 임시 폴더에 경로를 저장합니다.
        base_path = sys._MEIPASS
    except Exception:
        # 일반 파이썬 스크립트로 실행될 경우, 현재 파일의 디렉토리를 사용합니다.
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

# 'settings' 폴더의 절대 경로를 만듭니다.
settings_dir = resource_path('settings')

# Windows 환경 변수 PATH에 'settings' 폴더 경로를 일시적으로 추가합니다.
# 이 코드는 cairosvg가 임포트되기 전에 실행되어야 합니다.
if sys.platform == "win32" and os.path.exists(settings_dir):
    os.environ['PATH'] = settings_dir + os.pathsep + os.environ.get('PATH', '')

# --- 1. 환경 검사 및 설정 ---

# plantuml.jar 파일의 경로도 'settings' 폴더를 기준으로 설정합니다.
PLANTUML_JAR_PATH = resource_path(os.path.join("settings", "plantuml.jar"))

def check_environment():
    """실행 환경에 필요한 Java, PlantUML, 라이브러리가 있는지 확인합니다."""
    try:
        import cairosvg
    except (ImportError, OSError) as e:
        messagebox.showerror("필수 라이브러리 오류", f"CairoSVG 라이브러리를 로드할 수 없습니다.\n'settings' 폴더에 GTK+ 관련 DLL 파일들이 모두 포함되었는지 확인해주세요.\n\n오류: {e}")
        sys.exit(1)
    if not os.path.exists(PLANTUML_JAR_PATH):
        messagebox.showwarning("경고", f"'{os.path.basename(PLANTUML_JAR_PATH)}' 파일을 찾을 수 없습니다.\n'settings' 폴더 안에 파일이 있는지 확인해주세요.")
    try:
        subprocess.run(['java', '-version'], check=True, capture_output=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        messagebox.showerror("오류", "'Java'가 설치되지 않았거나 시스템 경로에 없습니다.")
        sys.exit(1)

# --- (CodeParser, PumlGenerator, DiagramApp 클래스는 이전 v13 버전과 동일합니다) ---
# ... (이하 모든 코드는 이전과 동일)
class CodeParser:
    def _find_matching_brace(self, content, start_pos):
        brace_level, pos = 1, start_pos + 1
        while pos < len(content):
            if content[pos] == '{': brace_level += 1
            elif content[pos] == '}':
                brace_level -= 1
                if brace_level == 0: return pos
            pos += 1
        return -1
    def parse_files(self, file_paths, base_dir=None):
        structures, class_to_package, all_class_names, file_contents = {}, {}, set(), {}
        if base_dir is None and file_paths:
             base_dir = os.path.dirname(os.path.commonpath(file_paths))
        for file_path in file_paths:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read(); file_contents[file_path] = content
                all_class_names.update(re.findall(r'(?:class|interface)\s+(\w+)', content))
        for file_path, content in file_contents.items():
            relative_path = os.path.relpath(os.path.dirname(file_path), base_dir)
            package_name = relative_path.replace(os.sep, '.') if relative_path != '.' else 'default'
            class_defs_re = r'(class|interface)\s+(\w+)(?:\s*extends\s+([\w\s,.<>]+))?(?:\s*implements\s+([\w\s,.<>]+))?\s*{'
            for match in re.finditer(class_defs_re, content):
                keyword, name, extends, implements = match.groups()
                if name not in all_class_names: continue
                class_to_package[name] = package_name
                structures[name] = {'type': keyword, 'relations': defaultdict(list)}
                if extends:
                    for p in re.split(r',\s*', extends.strip()):
                        clean_p = re.sub(r'<.*?>', '', p).strip()
                        if clean_p in all_class_names: structures[name]['relations']['extends'].append(clean_p)
                if implements:
                    for i in re.split(r',\s*', implements.strip()):
                        clean_i = re.sub(r'<.*?>', '', i).strip()
                        if clean_i in all_class_names: structures[name]['relations']['implements'].append(clean_i)
        return structures, class_to_package
class PumlGenerator:
    PACKAGE_ALIASES = {"default": "App", "fragment": "View", "viewmodel": "ViewModel","test": "Model", "util": "Util"}
    def get_alias(self, package_name): return self.PACKAGE_ALIASES.get(package_name, package_name.capitalize())
    def generate(self, title, structures, packages_info):
        puml_content = f"""@startuml
!theme cloudscape-design
title {title}
top to bottom direction
skinparam linetype ortho
skinparam wrapWidth 250
scale max 1200 width
skinparam packageStyle folder
skinparam classAttributeIconSize 0
skinparam groupInheritance 2
"""
        grouped_by_package, package_to_alias = defaultdict(list), {}
        for class_name, pkg_name in packages_info.items():
            if class_name in structures:
                alias = self.get_alias(pkg_name)
                grouped_by_package[alias].append(class_name)
                if alias not in package_to_alias: package_to_alias[alias] = pkg_name
        for alias in sorted(grouped_by_package.keys()):
            puml_content += f'package "{alias} ({package_to_alias[alias]})" as {alias} {{\n'
            for name in sorted(grouped_by_package[alias]):
                puml_content += f"  {structures[name]['type']} \"{name}\"\n"
            puml_content += "}\n\n"
        puml_content += "' --- Architecture Level Relations ---\n"
        puml_content += "View ..> ViewModel : uses\nViewModel ..> Model : uses\n"
        puml_content += "ViewModel ..> Util : uses\nApp ..> ViewModel : uses\n\n"
        puml_content += "' Vertical Layout Alignment\n"
        puml_content += "View -[hidden]down-> ViewModel\nViewModel -[hidden]down-> Model\nModel -[hidden]down- Util\n\n"
        puml_content += "' --- Key Inheritance & Implementation ---\n"
        for name, data in structures.items():
            src_alias = self.get_alias(packages_info.get(name, ''))
            if 'Test' in data['relations'].get('implements', []): puml_content += f"Model.Test <|.. {src_alias}.{name}\n"
            if 'BaseTestViewModel' in data['relations'].get('extends', []): puml_content += f"ViewModel.BaseTestViewModel <|-- {src_alias}.{name}\n"
        puml_content += "\n@enduml\n"
        return puml_content
class DiagramApp(tk.Tk):
    def __init__(self):
        super().__init__()
        global cairosvg
        import cairosvg
        self.title("BIST - Diagram Auto Maker v14"); self.geometry("1200x800")
        self.parser, self.generator = CodeParser(), PumlGenerator()
        self.file_paths, self.base_dir, self.puml_content = [], None, None
        self.original_svg_data, self.preview_image_tk, self.scale = None, None, 1.0
        self.create_widgets()
        self.log("프로그램이 시작되었습니다. CairoSVG 고화질 미리보기가 적용되었습니다.")
    def create_widgets(self):
        main_pane = PanedWindow(self, orient=tk.HORIZONTAL, sashrelief=tk.RAISED); main_pane.pack(fill=tk.BOTH, expand=True)
        control_frame = tk.Frame(main_pane, padx=10, pady=10, width=400)
        title_frame = tk.Frame(control_frame); title_frame.pack(fill=tk.X, pady=(0, 10))
        tk.Label(title_frame, text="다이어그램 제목:").pack(side=tk.LEFT)
        self.title_entry = tk.Entry(title_frame); self.title_entry.pack(fill=tk.X, expand=True, side=tk.LEFT, padx=5)
        list_frame = tk.Frame(control_frame); list_frame.pack(fill=tk.BOTH, expand=True)
        self.file_listbox = tk.Listbox(list_frame, selectmode=tk.EXTENDED); self.file_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        list_scrollbar = tk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.file_listbox.yview); list_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.file_listbox.config(yscrollcommand=list_scrollbar.set)
        button_frame = tk.Frame(control_frame); button_frame.pack(fill=tk.X, pady=5)
        tk.Button(button_frame, text="파일 추가", command=self.add_files).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        tk.Button(button_frame, text="폴더 추가", command=self.add_folder).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        tk.Button(button_frame, text="선택 삭제", command=self.delete_selected_files).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        action_frame = tk.Frame(control_frame); action_frame.pack(fill=tk.X, pady=(10,0))
        tk.Button(action_frame, text="미리보기 생성", command=self.generate_preview, bg="#007ACC", fg="white", font=("Helvetica", 11, "bold")).pack(fill=tk.X, ipady=5)
        tk.Button(action_frame, text="PUML 파일로 추출", command=self.export_puml, bg="#2E8B57", fg="white", font=("Helvetica", 11, "bold")).pack(fill=tk.X, ipady=5, pady=(5,0))
        main_pane.add(control_frame, stretch="never")
        right_container = tk.Frame(main_pane)
        log_frame = tk.Frame(right_container, height=150); log_frame.pack(side=tk.BOTTOM, fill=tk.X, expand=False, padx=10, pady=5); log_frame.pack_propagate(False)
        tk.Label(log_frame, text="진행률 및 오류 로그", font=("Helvetica", 10, "bold")).pack(anchor="w")
        self.log_text = Text(log_frame, wrap=tk.WORD); log_scrollbar = Scrollbar(log_frame, command=self.log_text.yview); self.log_text.config(yscrollcommand=log_scrollbar.set); log_scrollbar.pack(side=tk.RIGHT, fill=tk.Y); self.log_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        preview_container = tk.Frame(right_container); preview_container.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=10, pady=(10, 0))
        self.preview_canvas = Canvas(preview_container, bg="white", highlightthickness=0)
        v_scroll = Scrollbar(preview_container, orient=tk.VERTICAL, command=self.preview_canvas.yview); h_scroll = Scrollbar(preview_container, orient=tk.HORIZONTAL, command=self.preview_canvas.xview)
        self.preview_canvas.config(yscrollcommand=v_scroll.set, xscrollcommand=h_scroll.set)
        v_scroll.pack(side=tk.RIGHT, fill=tk.Y); h_scroll.pack(side=tk.BOTTOM, fill=tk.X); self.preview_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.preview_canvas.create_text(10, 10, anchor='nw', text="미리보기가 여기에 표시됩니다.", fill="gray")
        self.preview_canvas.bind("<Control-MouseWheel>", self._on_mouse_wheel)
        self.preview_canvas.bind("<Control-Button-4>", self._on_mouse_wheel)
        self.preview_canvas.bind("<Control-Button-5>", self._on_mouse_wheel)
        main_pane.add(right_container); main_pane.sash_place(0, 400, 0)
    def _on_mouse_wheel(self, event):
        if self.original_svg_data is None: return
        factor = 1.1
        if event.delta > 0 or event.num == 4: self.scale *= factor
        elif event.delta < 0 or event.num == 5: self.scale /= factor
        self.scale = max(0.1, min(5.0, self.scale))
        self.zoom_preview()
    def zoom_preview(self):
        if self.original_svg_data is None: return
        try:
            png_data = cairosvg.svg2png(bytestring=self.original_svg_data.encode('utf-8'), scale=self.scale)
            image = Image.open(io.BytesIO(png_data))
            self.preview_image_tk = ImageTk.PhotoImage(image)
            self.preview_canvas.delete("all")
            self.preview_canvas.create_image(0, 0, anchor='nw', image=self.preview_image_tk)
            self.preview_canvas.config(scrollregion=self.preview_canvas.bbox("all"))
        except Exception as e: self.log(f"CairoSVG 렌더링 오류: {e}")
    def generate_preview(self, *args):
        title = self.title_entry.get().strip()
        if not title or not self.file_paths: messagebox.showwarning("경고", "제목을 입력하고 최소 하나 이상의 파일을 추가해야 합니다."); return
        self.log("="*50 + f"\n'{title}' SVG 다이어그램 생성을 시작합니다...")
        self.preview_canvas.delete("all"); self.preview_canvas.create_text(10, 10, anchor='nw', text="미리보기를 생성하는 중...", fill="gray")
        self.original_svg_data = None
        try:
            self.log("소스 파일 분석..."); structures, packages_info = self.parser.parse_files(self.file_paths, self.base_dir)
            if not structures: raise ValueError("분석 가능한 클래스/인터페이스를 찾지 못했습니다.")
            self.log("PUML 코드 생성..."); self.puml_content = self.generator.generate(title, structures, packages_info)
            self.log("SVG 이미지 렌더링..."); proc = subprocess.Popen(['java', '-jar', PLANTUML_JAR_PATH, '-tsvg', '-pipe'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            svg_data, stderr = proc.communicate(input=self.puml_content.encode('utf-8'))
            if proc.returncode != 0: raise RuntimeError(f"PlantUML 오류:\n{stderr.decode('utf-8', 'ignore')}")
            self.log("SVG 표시 및 줌 초기화...")
            self.original_svg_data = svg_data.decode('utf-8')
            self.scale = 1.0; self.zoom_preview()
            self.log("고화질 미리보기 생성 성공.")
        except Exception as e:
            self.puml_content, self.original_svg_data = None, None
            error_msg = f"미리보기 생성 오류: {e}"; self.log(error_msg); messagebox.showerror("생성 오류", error_msg)
            self.preview_canvas.delete("all"); self.preview_canvas.create_text(10, 10, anchor='nw', text="오류 발생", fill="red")
    def log(self, message): self.log_text.insert(END, f"{message}\n"); self.log_text.see(END); self.update_idletasks()
    def add_files(self, *args):
        files = filedialog.askopenfilenames(title="Java/Kotlin 파일 선택", filetypes=[("Java/Kotlin Files", "*.java *.kt")])
        if files:
            new_files=sum(1 for f in files if f not in self.file_paths and (self.file_paths.append(f) or self.file_listbox.insert(tk.END, os.path.basename(f))))
            self.log(f"{new_files}개 파일을 추가했습니다.")
            if self.base_dir is None and self.file_paths: self.base_dir = os.path.dirname(os.path.commonpath(self.file_paths))
    def add_folder(self, *args):
        folder = filedialog.askdirectory(title="소스 코드 폴더 선택")
        if folder:
            if self.base_dir is None: self.base_dir = folder
            new_files = 0
            for r, _, f in os.walk(folder):
                for file in f:
                    if file.endswith((".java", ".kt")):
                        path = os.path.join(r, file)
                        if path not in self.file_paths: self.file_paths.append(path); self.file_listbox.insert(tk.END, os.path.basename(file)); new_files += 1
            self.log(f"'{os.path.basename(folder)}' 폴더에서 {new_files}개 파일을 추가했습니다.")
    def delete_selected_files(self, *args):
        indices = self.file_listbox.curselection()
        if indices:
            for i in sorted(indices, reverse=True): self.file_listbox.delete(i); del self.file_paths[i]
            self.log(f"{len(indices)}개 파일을 목록에서 삭제했습니다.")
            if not self.file_paths: self.base_dir = None
    def export_puml(self, *args):
        if not self.puml_content: messagebox.showwarning("경고", "먼저 '미리보기 생성'을 실행해야 합니다."); return
        results_dir = "results"; os.makedirs(results_dir, exist_ok=True)
        title = self.title_entry.get().strip().replace(' ', '_') or "diagram"
        save_path = filedialog.asksaveasfilename(initialdir=results_dir, initialfile=f"{title}.puml", defaultextension=".puml", filetypes=[("PlantUML File", "*.puml")])
        if save_path:
            try:
                with open(save_path, 'w', encoding='utf-8') as f: f.write(self.puml_content)
                self.log(f"다이어그램이 '{os.path.basename(save_path)}' 파일로 저장되었습니다.")
                messagebox.showinfo("성공", f"'{os.path.basename(save_path)}' 파일로 저장되었습니다.")
            except Exception as e: messagebox.showerror("저장 오류", f"파일 저장 오류: {e}")

if __name__ == "__main__":
    check_environment()
    app = DiagramApp()
    app.mainloop()

