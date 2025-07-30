# BIST - Diagram Auto Maker v23 (Final Bug-Fix Release)
# Made by ikhwan
# IllegalStateException을 포함한 모든 알려진 버그를 수정한 최종 완성 버전입니다.

import tkinter as tk
from tkinter import filedialog, messagebox, PanedWindow, Text, Scrollbar, END, Label, Canvas
import re
import os
import sys
import subprocess
import io
from collections import defaultdict
from PIL import Image, ImageTk

# --- 0. 배포를 위한 동적 경로 설정 ---
def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

# Windows 환경에서 GTK+ 라이브러리 경로를 동적으로 추가
settings_dir = resource_path('settings')
if sys.platform == "win32" and os.path.exists(settings_dir):
    os.environ['PATH'] = settings_dir + os.pathsep + os.environ.get('PATH', '')


# --- 1. 환경 검사 및 설정 ---
PLANTUML_JAR_PATH = resource_path(os.path.join("settings", "plantuml.jar"))

def check_environment():
    """ 프로그램 실행에 필요한 외부 라이브러리 및 도구를 검사합니다. """
    try:
        # CairoSVG는 런타임에 동적으로 로드되므로, 여기서 임포트하여 확인
        import cairosvg
    except (ImportError, OSError) as e:
        messagebox.showerror("필수 라이브러리 오류", f"CairoSVG 라이브러리를 로드할 수 없습니다.\n'settings' 폴더에 GTK+ 관련 DLL 파일들이 모두 포함되었는지 확인해주세요.\n\n오류: {e}")
        sys.exit(1)

    if not os.path.exists(PLANTUML_JAR_PATH):
        messagebox.showwarning("경고", f"'{os.path.basename(PLANTUML_JAR_PATH)}' 파일을 찾을 수 없습니다.\n프로그램이 정상적으로 작동하지 않을 수 있습니다.")

    try:
        subprocess.run(['java', '-version'], check=True, capture_output=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        messagebox.showerror("오류", "'Java'가 설치되지 않았거나 시스템 경로에 없습니다.\nPlantUML 다이어그램을 생성하려면 Java가 필요합니다.")
        sys.exit(1)

class CodeParser:
    """ 소스 코드 파일을 파싱하여 클래스 구조, 관계, 멤버를 추출하는 클래스 """

    def _get_class_body(self, content, start_index):
        """ 클래스 정의 시작 '{'부터 끝 '}'까지의 내용을 추출합니다. """
        try:
            open_brace_pos = content.find('{', start_index)
            if open_brace_pos == -1: return ""
            
            brace_level, pos = 1, open_brace_pos + 1
            while pos < len(content):
                if content[pos] == '{': brace_level += 1
                elif content[pos] == '}':
                    brace_level -= 1
                    if brace_level == 0: return content[open_brace_pos+1:pos]
                pos += 1
        except ValueError: return ""
        return ""

    def parse_files(self, file_paths, base_dir=None):
        """ 여러 소스 파일을 파싱하여 클래스 정보를 반환합니다. """
        structures, class_to_package, class_to_filepath = {}, {}, {}
        all_class_names = set()

        if base_dir is None and file_paths:
            base_dir = os.path.dirname(os.path.commonpath(file_paths))

        for file_path in file_paths:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            # 모든 클래스/인터페이스/이넘 이름을 먼저 수집
            for name in re.findall(r'(?:class|interface|enum)\s+(\w+)', content):
                all_class_names.add(name)
                class_to_filepath[name] = file_path

        for class_name, file_path in class_to_filepath.items():
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()

            relative_path = os.path.relpath(os.path.dirname(file_path), base_dir)
            package_name = relative_path.replace(os.sep, '.') if relative_path != '.' else 'default'
            
            # 클래스 정의(상속, 구현 포함)를 찾는 정규식
            class_def_re = re.compile(
                r'(class|interface|abstract\s+class|enum)\s+' + re.escape(class_name) +
                r'(?:\s*<[\w\s,.<>]+>)?' +
                r'(?:\s*extends\s+([\w\s,.<>]+))?' +
                r'(?:\s*implements\s+([\w\s,.<>]+))?' +
                r'\s*{'
            )

            match = class_def_re.search(content)
            if match:
                keyword, extends_str, implements_str = match.groups()
                class_to_package[class_name] = package_name
                structures[class_name] = {'type': keyword.strip(), 'relations': defaultdict(set), 'members': []}

                if extends_str:
                    for p in re.split(r',\s*', extends_str.strip()):
                        clean_p = re.sub(r'<.*?>', '', p).strip()
                        if clean_p in all_class_names: structures[class_name]['relations']['extends'].add(clean_p)
                
                if implements_str:
                    for i in re.split(r',\s*', implements_str.strip()):
                        clean_i = re.sub(r'<.*?>', '', i).strip()
                        if clean_i in all_class_names: structures[class_name]['relations']['implements'].add(clean_i)

                class_body = self._get_class_body(content, match.end())
                if not class_body: continue

                # 멤버(메서드/내부 클래스) 추출
                member_re = re.compile(r'(?:fun|void|public|private|protected|internal|suspend\s+fun)\s+[\w<>,.\s]+\s+(\w+)\s*\(|class\s+(\w+)|interface\s+(\w+)')
                for member_match in re.finditer(member_re, class_body):
                    member_name = next((g for g in member_match.groups() if g is not None), None)
                    if member_name: structures[class_name]['members'].append(member_name)

                # 다른 클래스 사용(의존성) 관계 추출
                usage_pattern = re.compile(r'\b(' + '|'.join(re.escape(cn) for cn in all_class_names) + r')\b')
                found_usages = usage_pattern.findall(class_body)
                for used_class in set(found_usages):
                    if used_class != class_name and used_class in all_class_names:
                        structures[class_name]['relations']['usage'].add(used_class)
        
        return structures, class_to_package, class_to_filepath

class PumlGenerator:
    """ 파싱된 클래스 정보를 바탕으로 PlantUML 코드를 생성하는 클래스 """
    
    def generate(self, title, structures, class_to_package, class_to_filepath, mode):
        """ 선택된 모드에 따라 적절한 다이어그램 생성 메서드를 호출합니다. """
        if mode == 'architecture':
            return self._generate_architecture_diagram(title, structures, class_to_package, class_to_filepath)
        elif mode == 'class_analysis':
            return self._generate_class_analysis_diagram(title, structures, class_to_package)
        elif mode == 'full_analysis':
            return self._generate_full_analysis_diagram(title, structures, class_to_package)

    def _generate_architecture_diagram(self, title, structures, class_to_package, class_to_filepath):
        """ 아키텍처 분석 다이어그램(MVVM) PUML 코드를 생성합니다. """
        puml_content = f"""@startuml
!theme cloudscape-design
title {title} (Architecture Analysis)

top to bottom direction
skinparam linetype ortho
skinparam packageStyle folder
skinparam roundCorner 20
skinparam classAttributeIconSize 0
"""
        layers = defaultdict(list)
        interfaces_and_abstracts = []

        for name, data in structures.items():
            if data['type'] in ['interface', 'abstract class']:
                interfaces_and_abstracts.append(name)
            else:
                filename = os.path.basename(class_to_filepath.get(name, "")).lower()
                pkg_name = class_to_package.get(name, "").lower()

                if "viewmodel" in filename or "viewmodel" in pkg_name:
                    layers['ViewModel'].append(name)
                elif any(k in filename for k in ["fragment", "activity"]) or any(k in pkg_name for k in ["view", "activity", "fragment", "adapter"]):
                    layers['View'].append(name)
                elif "repository" in filename or "util" in filename or "service" in filename:
                    layers['RepositoryUtil'].append(name)
                elif any(k in pkg_name for k in ['model', 'data', 'dto', 'entity']):
                    layers['Model'].append(name)
                else:
                    layers['RepositoryUtil'].append(name) # 기본값

        layer_definitions = {
            'View': 'package "View Layer" as View',
            'ViewModel': 'package "ViewModel Layer" as ViewModel',
            'Model': 'package "Model Layer" as Model',
            'RepositoryUtil': 'package "Repository & Utils" as RepoUtil'
        }

        for layer_name, pkg_def in layer_definitions.items():
            if layers[layer_name]:
                puml_content += f'{pkg_def} {{\n'
                for class_name in sorted(layers[layer_name]): puml_content += f"  class \"{class_name}\"\n"
                puml_content += '}\n\n'
        
        if interfaces_and_abstracts:
            puml_content += 'package "Interfaces & Abstract Classes" as Core {\n'
            for name in sorted(interfaces_and_abstracts):
                puml_content += f"  {structures[name]['type']} \"{name}\"\n"
            puml_content += '}\n\n'

        puml_content += "' --- High-level Architecture Flow ---\n"
        if layers['View'] and layers['ViewModel']: puml_content += "View -right-> ViewModel\n"
        if layers['ViewModel'] and layers['Model']: puml_content += "ViewModel -right-> Model\n"
        if layers['ViewModel'] and layers['RepositoryUtil']: puml_content += "ViewModel -down-> RepoUtil\n"
        
        puml_content += "\n' --- Detailed Relationships ---\n"
        drawn_relations = set()
        for name, data in sorted(structures.items()):
            for parent in data['relations'].get('extends', []):
                rel = f"\"{parent}\" <|-- \"{name}\""
                if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)
            
            for interface in data['relations'].get('implements', []):
                rel = f"\"{interface}\" <|.. \"{name}\""
                if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)
            
            for used_class in data['relations'].get('usage', []):
                is_parent = used_class in data['relations']['extends'] or used_class in data['relations']['implements']
                if not is_parent:
                    rel = f"\"{name}\" --> \"{used_class}\""
                    if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)

        puml_content += "\n@enduml\n"
        return puml_content

    def _generate_class_analysis_diagram(self, title, structures, class_to_package):
        """ 패키지별 클래스 분석 다이어그램 PUML 코드를 생성합니다. """
        puml_content = f"""@startuml
set separator .
!theme cloudscape-design
title {title} (Class Analysis by Package)

top to bottom direction
skinparam packageStyle rectangle
"""
        grouped_by_package = defaultdict(list)
        for class_name, pkg_name in class_to_package.items(): grouped_by_package[pkg_name].append(class_name)

        for pkg_name, classes in sorted(grouped_by_package.items()):
            # [버그 수정] 점(.)이 포함된 패키지 이름은 큰따옴표로 묶어야 함
            puml_content += f'package "{pkg_name}" {{\n'
            for class_name in sorted(classes):
                if class_name in structures:
                    puml_content += f"  {structures[class_name]['type']} \"{class_name}\"\n"
            puml_content += '}\n\n'
        
        # 관계 추가 (상속, 구현, 사용)
        drawn_relations = set()
        for name, data in sorted(structures.items()):
            for parent in data['relations'].get('extends', []):
                rel = f'"{class_to_package[parent]}.{parent}" <|-- "{class_to_package[name]}.{name}"'
                if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)
            for interface in data['relations'].get('implements', []):
                rel = f'"{class_to_package[interface]}.{interface}" <|.. "{class_to_package[name]}.{name}"'
                if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)
            for used_class in data['relations'].get('usage', []):
                 if not (used_class in data['relations']['extends'] or used_class in data['relations']['implements']):
                    rel = f'"{class_to_package[name]}.{name}" ..> "{class_to_package[used_class]}.{used_class}"'
                    if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)

        puml_content += "\n@enduml\n"
        return puml_content

    def _generate_full_analysis_diagram(self, title, structures, class_to_package):
        """ 멤버(메서드 등)를 포함한 전체 분석 다이어그램 PUML 코드를 생성합니다. """
        puml_content = f"""@startuml
set separator .
!theme cloudscape-design
title {title} (Full Analysis with Members)

top to bottom direction
skinparam packageStyle rectangle
"""
        grouped_by_package = defaultdict(list)
        for class_name, pkg_name in class_to_package.items(): grouped_by_package[pkg_name].append(class_name)

        for pkg_name, classes in sorted(grouped_by_package.items()):
            # [버그 수정] 점(.)이 포함된 패키지 이름은 큰따옴표로 묶어야 함
            puml_content += f'package "{pkg_name}" {{\n'
            for class_name in sorted(classes):
                if class_name in structures:
                    puml_content += f"  {structures[class_name]['type']} \"{class_name}\" {{\n"
                    for member in sorted(set(structures[class_name]['members'])):
                        puml_content += f"    + {member}()\n"
                    puml_content += "  }\n"
            puml_content += '}\n\n'

        # 관계 추가 (상속, 구현, 사용)
        drawn_relations = set()
        for name, data in sorted(structures.items()):
            for parent in data['relations'].get('extends', []):
                rel = f'"{class_to_package[parent]}.{parent}" <|-- "{class_to_package[name]}.{name}"'
                if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)
            for interface in data['relations'].get('implements', []):
                rel = f'"{class_to_package[interface]}.{interface}" <|.. "{class_to_package[name]}.{name}"'
                if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)
            for used_class in data['relations'].get('usage', []):
                 if not (used_class in data['relations']['extends'] or used_class in data['relations']['implements']):
                    rel = f'"{class_to_package[name]}.{name}" ..> "{class_to_package[used_class]}.{used_class}"'
                    if rel not in drawn_relations: puml_content += f"{rel}\n"; drawn_relations.add(rel)

        puml_content += "\n@enduml\n"
        return puml_content

class DiagramApp(tk.Tk):
    """ 다이어그램 생성기 GUI 애플리케이션 클래스 """

    def __init__(self):
        super().__init__()
        
        global cairosvg
        import cairosvg # CairoSVG를 __init__ 시점에 임포트

        self.title("BIST - Diagram Auto Maker v23 (Final)")
        self.geometry("1200x800")
        
        self.parser, self.generator = CodeParser(), PumlGenerator()
        self.file_paths, self.base_dir, self.puml_content = [], None, None
        self.original_svg_data, self.preview_image_tk, self.scale = None, None, 1.0

        self.mode_var = tk.StringVar(value="architecture")
        
        self.create_widgets()
        self.log("프로그램 시작. 다이어그램 모드를 선택 후 미리보기를 생성하세요.")

    def create_widgets(self):
        """ GUI 위젯들을 생성하고 배치합니다. """
        main_pane = PanedWindow(self, orient=tk.HORIZONTAL, sashrelief=tk.RAISED)
        main_pane.pack(fill=tk.BOTH, expand=True)

        # --- 왼쪽 제어판 ---
        control_frame = tk.Frame(main_pane, padx=10, pady=10, width=400)
        
        title_frame = tk.Frame(control_frame); title_frame.pack(fill=tk.X, pady=(0, 10))
        tk.Label(title_frame, text="다이어그램 제목:").pack(side=tk.LEFT)
        self.title_entry = tk.Entry(title_frame); self.title_entry.pack(fill=tk.X, expand=True, side=tk.LEFT, padx=5)

        list_frame = tk.Frame(control_frame); list_frame.pack(fill=tk.BOTH, expand=True)
        self.file_listbox = tk.Listbox(list_frame, selectmode=tk.EXTENDED)
        self.file_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        list_scrollbar = tk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.file_listbox.yview)
        list_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.file_listbox.config(yscrollcommand=list_scrollbar.set)

        button_frame = tk.Frame(control_frame); button_frame.pack(fill=tk.X, pady=5)
        tk.Button(button_frame, text="파일 추가", command=self.add_files).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        tk.Button(button_frame, text="폴더 추가", command=self.add_folder).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        tk.Button(button_frame, text="선택 삭제", command=self.delete_selected_files).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        
        mode_frame = tk.LabelFrame(control_frame, text="모드 선택", padx=5, pady=5)
        mode_frame.pack(fill=tk.X, pady=(10, 5))
        tk.Radiobutton(mode_frame, text="아키텍처 분석 (V-VM-M)", variable=self.mode_var, value="architecture").pack(anchor='w')
        tk.Radiobutton(mode_frame, text="클래스 분석 (패키지별)", variable=self.mode_var, value="class_analysis").pack(anchor='w')
        tk.Radiobutton(mode_frame, text="전체 분석 (메서드/내부클래스)", variable=self.mode_var, value="full_analysis").pack(anchor='w')
        
        action_frame = tk.Frame(control_frame); action_frame.pack(fill=tk.X, pady=(5,0))
        tk.Button(action_frame, text="미리보기 생성", command=self.generate_preview, bg="#007ACC", fg="white", font=("Helvetica", 11, "bold")).pack(fill=tk.X, ipady=5)
        tk.Button(action_frame, text="PUML 파일로 추출", command=self.export_puml, bg="#2E8B57", fg="white", font=("Helvetica", 11, "bold")).pack(fill=tk.X, ipady=5, pady=(5,0))

        main_pane.add(control_frame, stretch="never")

        # --- 오른쪽 미리보기 및 로그 ---
        right_container = tk.Frame(main_pane)
        
        log_frame = tk.Frame(right_container, height=150); log_frame.pack(side=tk.BOTTOM, fill=tk.X, expand=False, padx=10, pady=5); log_frame.pack_propagate(False)
        tk.Label(log_frame, text="진행률 및 오류 로그", font=("Helvetica", 10, "bold")).pack(anchor="w")
        self.log_text = Text(log_frame, wrap=tk.WORD); log_scrollbar = Scrollbar(log_frame, command=self.log_text.yview); self.log_text.config(yscrollcommand=log_scrollbar.set)
        log_scrollbar.pack(side=tk.RIGHT, fill=tk.Y); self.log_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        preview_container = tk.Frame(right_container); preview_container.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=10, pady=(10, 0))
        self.preview_canvas = Canvas(preview_container, bg="white", highlightthickness=0)
        v_scroll = Scrollbar(preview_container, orient=tk.VERTICAL, command=self.preview_canvas.yview); h_scroll = Scrollbar(preview_container, orient=tk.HORIZONTAL, command=self.preview_canvas.xview)
        self.preview_canvas.config(yscrollcommand=v_scroll.set, xscrollcommand=h_scroll.set)
        v_scroll.pack(side=tk.RIGHT, fill=tk.Y); h_scroll.pack(side=tk.BOTTOM, fill=tk.X); self.preview_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.preview_canvas.create_text(10, 10, anchor='nw', text="미리보기가 여기에 표시됩니다.", fill="gray")

        # 마우스 이벤트 바인딩 (줌 & 이동)
        self.preview_canvas.bind("<MouseWheel>", self._on_mouse_wheel) # Windows, macOS
        self.preview_canvas.bind("<Button-4>", self._on_mouse_wheel)   # Linux (scroll up)
        self.preview_canvas.bind("<Button-5>", self._on_mouse_wheel)   # Linux (scroll down)
        self.preview_canvas.bind("<ButtonPress-3>", self.on_right_press)
        self.preview_canvas.bind("<B3-Motion>", self.on_right_drag)

        main_pane.add(right_container)
        main_pane.sash_place(0, 400, 0)

    def on_right_press(self, event): self.preview_canvas.scan_mark(event.x, event.y)
    def on_right_drag(self, event): self.preview_canvas.scan_dragto(event.x, event.y, gain=1)

    def _on_mouse_wheel(self, event):
        """ 마우스 휠 이벤트로 캔버스 이미지를 줌인/줌아웃 합니다. """
        if self.original_svg_data is None: return
        
        factor = 1.1
        if (hasattr(event, 'delta') and event.delta > 0) or event.num == 4: # Scroll up
            self.scale *= factor
        elif (hasattr(event, 'delta') and event.delta < 0) or event.num == 5: # Scroll down
            self.scale /= factor
        
        self.scale = max(0.1, min(5.0, self.scale)) # 줌 배율 제한
        self.zoom_preview()

    def zoom_preview(self):
        """ 현재 배율(self.scale)로 SVG를 다시 렌더링하여 캔버스에 표시합니다. """
        if self.original_svg_data is None: return
        try:
            png_data = cairosvg.svg2png(bytestring=self.original_svg_data.encode('utf-8'), scale=self.scale)
            image = Image.open(io.BytesIO(png_data))
            self.preview_image_tk = ImageTk.PhotoImage(image)

            self.preview_canvas.delete("all")
            self.preview_canvas.create_image(0, 0, anchor='nw', image=self.preview_image_tk)
            self.preview_canvas.config(scrollregion=self.preview_canvas.bbox("all"))
        except Exception as e:
            self.log(f"CairoSVG 렌더링 오류: {e}")

    def generate_preview(self, *args):
        """ '미리보기 생성' 버튼 클릭 시 호출되는 메인 로직입니다. """
        title = self.title_entry.get().strip()
        if not title or not self.file_paths:
            messagebox.showwarning("경고", "제목을 입력하고 최소 하나 이상의 파일을 추가해야 합니다.")
            return

        self.log("="*50 + f"\n'{title}' SVG 다이어그램 생성을 시작합니다...")
        self.preview_canvas.delete("all"); self.preview_canvas.create_text(10, 10, anchor='nw', text="미리보기를 생성하는 중...", fill="gray"); self.update_idletasks()
        
        try:
            selected_mode = self.mode_var.get()
            mode_map = {'architecture': "아키텍처 분석 (V-VM-M)", 'class_analysis': "클래스 분석 (패키지별)", 'full_analysis': "전체 분석 (메서드/내부클래스)"}
            self.log(f"선택된 모드: {mode_map[selected_mode]}")

            self.log("소스 파일 분석...")
            structures, class_to_package, class_to_filepath = self.parser.parse_files(self.file_paths, self.base_dir)
            if not structures: raise ValueError("분석 가능한 클래스/인터페이스를 찾지 못했습니다.")

            self.log("PUML 코드 생성...")
            self.puml_content = self.generator.generate(title, structures, class_to_package, class_to_filepath, mode=selected_mode)

            self.log("SVG 이미지 렌더링...")
            proc = subprocess.Popen(['java', '-jar', PLANTUML_JAR_PATH, '-tsvg', '-pipe'],
                                    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            svg_data, stderr = proc.communicate(input=self.puml_content.encode('utf-8'))
            
            if proc.returncode != 0:
                raise RuntimeError(f"PlantUML 오류:\n{stderr.decode('utf-8', 'ignore')}")

            self.log("SVG 표시 및 줌 초기화...")
            self.original_svg_data = svg_data.decode('utf-8')
            self.scale = 1.0
            self.zoom_preview()
            self.log("미리보기 생성 성공. (마우스 휠: 줌, 우클릭 드래그: 이동)")
            
        except Exception as e:
            self.puml_content, self.original_svg_data = None, None
            error_msg = f"미리보기 생성 오류: {e}"
            self.log(error_msg)
            messagebox.showerror("생성 오류", error_msg)
            self.preview_canvas.delete("all"); self.preview_canvas.create_text(10, 10, anchor='nw', text="오류 발생", fill="red")

    def log(self, message):
        """ 로그 메시지를 GUI의 로그 창에 추가합니다. """
        self.log_text.insert(END, f"{message}\n"); self.log_text.see(END); self.update_idletasks()

    def add_files(self, *args):
        """ '파일 추가' 대화상자를 열어 소스 파일을 목록에 추가합니다. """
        files = filedialog.askopenfilenames(title="Java/Kotlin 파일 선택", filetypes=[("Java/Kotlin Files", "*.java *.kt")])
        if files:
            new_files = 0
            for f_path in files:
                if f_path not in self.file_paths:
                    self.file_paths.append(f_path)
                    self.file_listbox.insert(tk.END, os.path.basename(f_path))
                    new_files += 1
            self.log(f"{new_files}개 파일을 추가했습니다.")
            if self.base_dir is None and self.file_paths:
                self.base_dir = os.path.dirname(os.path.commonpath(self.file_paths))

    def add_folder(self, *args):
        """ '폴더 추가' 대화상자를 열어 해당 폴더의 모든 소스 파일을 목록에 추가합니다. """
        folder = filedialog.askdirectory(title="소스 코드 폴더 선택")
        if not folder: return

        if self.base_dir is None: self.base_dir = folder
            
        new_files = 0
        for r, _, f_list in os.walk(folder):
            for file in f_list:
                if file.endswith((".java", ".kt")):
                    path = os.path.join(r, file)
                    if path not in self.file_paths:
                        self.file_paths.append(path)
                        self.file_listbox.insert(tk.END, os.path.basename(file))
                        new_files += 1
        self.log(f"'{os.path.basename(folder)}' 폴더에서 {new_files}개 파일을 추가했습니다.")
    
    def delete_selected_files(self, *args):
        """ 파일 목록에서 선택된 항목들을 삭제합니다. """
        indices = self.file_listbox.curselection()
        if indices:
            for i in sorted(indices, reverse=True):
                self.file_listbox.delete(i)
                del self.file_paths[i]
            self.log(f"{len(indices)}개 파일을 목록에서 삭제했습니다.")
            if not self.file_paths: self.base_dir = None

    def export_puml(self, *args):
        """ 생성된 PUML 코드를 파일로 저장합니다. """
        if not self.puml_content:
            messagebox.showwarning("경고", "먼저 '미리보기 생성'을 실행해야 합니다.")
            return

        results_dir = "results"; os.makedirs(results_dir, exist_ok=True)
        title = self.title_entry.get().strip().replace(' ', '_') or "diagram"
        save_path = filedialog.asksaveasfilename(initialdir=results_dir, initialfile=f"{title}.puml", 
                                                 defaultextension=".puml", filetypes=[("PlantUML File", "*.puml")])
        if save_path:
            try:
                with open(save_path, 'w', encoding='utf-8') as f: f.write(self.puml_content)
                self.log(f"다이어그램이 '{os.path.basename(save_path)}' 파일로 저장되었습니다.")
                messagebox.showinfo("성공", f"'{os.path.basename(save_path)}' 파일로 저장되었습니다.")
            except Exception as e:
                messagebox.showerror("저장 오류", f"파일 저장 오류: {e}")

if __name__ == "__main__":
    check_environment()
    app = DiagramApp()
    app.mainloop()
