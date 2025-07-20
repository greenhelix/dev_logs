# BIST - Diagram Auto Maker v8 (TclError Hotfix)
# Made by ikhwan
# 'bad screen distance' TclError를 수정한 최종 안정화 버전입니다.

import tkinter as tk
from tkinter import filedialog, messagebox, PanedWindow, Text, Scrollbar, END, Label, Canvas
import re
import os
import sys
import subprocess
import io
from collections import defaultdict
from PIL import Image, ImageTk

# --- 1. 환경 검사 및 설정 ---

PLANTUML_JAR_PATH = "plantuml.jar"

def check_environment():
    """실행 환경이 프로그램 구동에 적합한지 확인합니다."""
    if not os.path.exists(PLANTUML_JAR_PATH):
        messagebox.showwarning("경고", f"'{PLANTUML_JAR_PATH}' 파일을 찾을 수 없습니다.\n미리보기 기능이 작동하지 않을 수 있습니다.\n스크립트와 동일한 위치에 두거나 경로를 수정해주세요.")
    try:
        subprocess.run(['java', '-version'], check=True, capture_output=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        messagebox.showerror("오류", "'Java'가 설치되지 않았거나 시스템 경로에 없습니다.\n미리보기 기능을 사용하려면 Java를 설치해야 합니다.")
        sys.exit(1)
    try:
        from PIL import Image, ImageTk
    except ImportError:
        messagebox.showerror("오류", "'Pillow' 라이브러리가 설치되지 않았습니다.\n'pip install Pillow' 명령어로 설치해주세요.")
        sys.exit(1)

# --- 2. 코드 파싱 로직 (이전 버전과 동일) ---

class CodeParser:
    LIFECYCLE_METHODS = {
        'onCreate', 'onStart', 'onResume', 'onPause', 'onStop', 'onDestroy',
        'onCreateView', 'onViewCreated', 'onDestroyView', 'onAttach', 'onDetach',
        'onSaveInstanceState', 'onRestoreInstanceState', 'onRequestPermissionsResult',
        'dispatchKeyEvent', 'getTargetFocusId'
    }

    def _find_matching_brace(self, content, start_pos):
        brace_level = 1
        pos = start_pos + 1
        while pos < len(content):
            if content[pos] == '{':
                brace_level += 1
            elif content[pos] == '}':
                brace_level -= 1
                if brace_level == 0:
                    return pos
            pos += 1
        return -1

    def parse_files(self, file_paths, base_dir=None):
        structures = {}
        class_to_package = {}
        all_class_names = set()
        file_contents = {}
        
        if base_dir is None and file_paths:
             base_dir = os.path.dirname(os.path.commonpath(file_paths))

        for file_path in file_paths:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                file_contents[file_path] = content
                matches = re.findall(r'(?:class|interface)\s+(\w+)', content)
                all_class_names.update(matches)

        for file_path, content in file_contents.items():
            relative_path = os.path.relpath(os.path.dirname(file_path), base_dir)
            package_name = relative_path.replace(os.sep, '.') if relative_path != '.' else 'default'

            class_definitions = re.finditer(r'((?:public|private|protected|abstract|static|\s)*)(class|interface)\s+(\w+)(?:\s*extends\s+([\w\s,.<>]+))?(?:\s*implements\s+([\w\s,.<>]+))?\s*{', content)

            for match in class_definitions:
                _, keyword, name, extends, implements = match.groups()
                if name not in all_class_names: continue

                class_to_package[name] = package_name
                class_body_start = match.end()
                class_body_end = self._find_matching_brace(content, class_body_start -1)
                
                if class_body_end == -1: continue
                body = content[class_body_start:class_body_end]

                structures[name] = {
                    'type': keyword, 'methods': [],
                    'relations': defaultdict(list)
                }

                if extends:
                    for parent in re.split(r',\s*', extends.strip()):
                        clean_parent = re.sub(r'<.*?>', '', parent).strip()
                        if clean_parent in all_class_names:
                            structures[name]['relations']['extends'].append(clean_parent)
                if implements:
                    for interface in re.split(r',\s*', implements.strip()):
                        clean_interface = re.sub(r'<.*?>', '', interface).strip()
                        if clean_interface in all_class_names:
                            structures[name]['relations']['implements'].append(clean_interface)
                
                method_pattern = r'^\s*(?:@\w+\s*)*\s*(public|private|protected|static|abstract|synchronized|final|\s)*([\w<>.\[\]]+)\s+(\w+)\s*\(([^)]*)\)\s*(?:{|throws)'
                for m_match in re.finditer(method_pattern, body, re.MULTILINE):
                    visibility_group, _, method_name, params = m_match.groups()
                    if method_name in self.LIFECYCLE_METHODS or method_name in {'if', 'for', 'while', 'switch', 'catch'}:
                        continue
                    
                    vis_symbol = '+' if 'public' in str(visibility_group) else '-'
                    structures[name]['methods'].append(f'{vis_symbol} {method_name}()')

                    for class_name_dep in all_class_names:
                        if re.search(r'\b' + re.escape(class_name_dep) + r'\b', params):
                            if class_name_dep != name:
                                structures[name]['relations']['dependency'].append(class_name_dep)
                
                var_pattern = r'^\s*(private|public|protected|final|static|\s)*([\w<>.\[\]]+)\s+([\w\[\]]+)\s*(?:=.*?;|;)'
                for v_match in re.finditer(var_pattern, body, re.MULTILINE):
                    _, var_type, _ = v_match.groups()
                    clean_type = re.sub(r'<.*?>', '', var_type).strip()
                    if clean_type in all_class_names and clean_type != name:
                         structures[name]['relations']['composition'].append(clean_type)
        return structures, class_to_package

# --- 3. PUML 생성 로직 (이전 버전과 동일) ---

class PumlGenerator:
    """파싱된 데이터 구조를 바탕으로 고급 기능이 적용된 PlantUML(.puml) 파일 내용을 생성합니다."""
    
    def generate(self, title, structures, packages_info):
        puml_content = f"""@startuml
!theme cloudscape-design
title {title}

' A4 사이즈 출력을 위한 레이아웃 최적화
top to bottom direction
skinparam linetype ortho
skinparam wrapWidth 200
scale max 1200 width

' 기타 스타일링
skinparam packageStyle folder
skinparam classAttributeIconSize 0

"""
        grouped_by_package = defaultdict(list)
        for class_name, pkg_name in packages_info.items():
            if class_name in structures:
                grouped_by_package[pkg_name].append(class_name)

        for pkg_name, class_list in sorted(grouped_by_package.items()):
            puml_content += f'package "{pkg_name}" {{\n'
            for name in sorted(class_list):
                data = structures[name]
                puml_content += f"  {data['type']} \"{name}\" {{\n"
                for method in sorted(list(set(data['methods']))):
                    puml_content += f"    {method}\n"
                puml_content += "  }\n"
            puml_content += "}\n\n"

        puml_content += "' --- 관계 정의 ---\n\n"
        added_relations = set()
        for name, data in structures.items():
            relations = data.get('relations', {})
            for rel_type in ['extends', 'implements', 'composition', 'dependency']:
                for target in set(relations.get(rel_type, [])):
                    rel_tuple = tuple(sorted((name, target))) + (rel_type,)
                    if rel_type in ['extends', 'implements']: rel_tuple = (name, target, rel_type)
                    if rel_tuple in added_relations: continue

                    if rel_type == 'extends':
                        puml_content += f"{target} <|-- {name}\n"
                    elif rel_type == 'implements':
                        puml_content += f"{target} <|.. {name}\n"
                    elif rel_type == 'composition':
                        puml_content += f"{name} *-- {target}\n"
                    elif rel_type == 'dependency':
                        is_strongly_related = any(target in l for l in [relations.get(k, []) for k in ['extends', 'implements', 'composition']])
                        if not is_strongly_related:
                            puml_content += f"{name} ..> {target}\n"
                    
                    added_relations.add(rel_tuple)

        puml_content += "\n@enduml\n"
        return puml_content

# --- 4. GUI 애플리케이션 (오류 수정) ---

class DiagramApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("BIST - Diagram Auto Maker v8")
        self.geometry("1200x800")

        self.parser = CodeParser()
        self.generator = PumlGenerator()
        self.file_paths = []
        self.base_dir = None
        self.puml_content = None
        self.preview_image_tk = None

        self.create_widgets()
        self.log("프로그램이 시작되었습니다. 분석할 파일이나 폴더를 추가해주세요.")

    def create_widgets(self):
        main_pane = PanedWindow(self, orient=tk.HORIZONTAL, sashrelief=tk.RAISED)
        main_pane.pack(fill=tk.BOTH, expand=True)

        # --- 왼쪽 제어판 ---
        control_frame = tk.Frame(main_pane, padx=10, pady=10, width=400)
        
        title_frame = tk.Frame(control_frame)
        title_frame.pack(fill=tk.X, pady=(0, 10))
        tk.Label(title_frame, text="다이어그램 제목:", font=("Helvetica", 10)).pack(side=tk.LEFT)
        self.title_entry = tk.Entry(title_frame, font=("Helvetica", 10))
        self.title_entry.pack(fill=tk.X, expand=True, side=tk.LEFT, padx=5)

        list_frame = tk.Frame(control_frame)
        list_frame.pack(fill=tk.BOTH, expand=True)
        self.file_listbox = tk.Listbox(list_frame, selectmode=tk.EXTENDED, font=("Courier New", 9))
        self.file_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        list_scrollbar = tk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.file_listbox.yview)
        list_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.file_listbox.config(yscrollcommand=list_scrollbar.set)
        
        button_frame = tk.Frame(control_frame)
        button_frame.pack(fill=tk.X, pady=5)
        tk.Button(button_frame, text="파일 추가", command=self.add_files).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        tk.Button(button_frame, text="폴더 추가", command=self.add_folder).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        tk.Button(button_frame, text="선택 삭제", command=self.delete_selected_files).pack(side=tk.LEFT, padx=2, expand=True, fill=tk.X)
        
        action_frame = tk.Frame(control_frame)
        action_frame.pack(fill=tk.X, pady=(10,0))
        tk.Button(action_frame, text="미리보기 생성", command=self.generate_preview, bg="#007ACC", fg="white", font=("Helvetica", 11, "bold")).pack(fill=tk.X, ipady=5)
        tk.Button(action_frame, text="PUML 파일로 추출", command=self.export_puml, bg="#2E8B57", fg="white", font=("Helvetica", 11, "bold")).pack(fill=tk.X, ipady=5, pady=(5,0))

        main_pane.add(control_frame, stretch="never")

        # --- 오른쪽 패널 ---
        right_container = tk.Frame(main_pane)

        # 로그 영역 (하단에 고정)
        log_frame = tk.Frame(right_container, height=150)
        log_frame.pack(side=tk.BOTTOM, fill=tk.X, expand=False, padx=10, pady=5)
        log_frame.pack_propagate(False)

        tk.Label(log_frame, text="진행률 및 오류 로그", font=("Helvetica", 10, "bold")).pack(anchor="w")
        self.log_text = Text(log_frame, font=("Courier New", 9), wrap=tk.WORD)
        log_scrollbar = Scrollbar(log_frame, command=self.log_text.yview)
        self.log_text.config(yscrollcommand=log_scrollbar.set)
        log_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.log_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        # 미리보기 영역 (나머지 공간 채우기)
        # === 오류 수정 부분 ===
        # 위젯 생성 시에는 여백을 지정하지 않고, pack() 메소드에서 여백을 지정하도록 변경
        preview_container = tk.Frame(right_container)
        preview_container.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=10, pady=(10, 0))

        self.preview_canvas = Canvas(preview_container, bg="white", highlightthickness=0)
        v_scroll = Scrollbar(preview_container, orient=tk.VERTICAL, command=self.preview_canvas.yview)
        h_scroll = Scrollbar(preview_container, orient=tk.HORIZONTAL, command=self.preview_canvas.xview)
        self.preview_canvas.config(yscrollcommand=v_scroll.set, xscrollcommand=h_scroll.set)

        v_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        h_scroll.pack(side=tk.BOTTOM, fill=tk.X)
        self.preview_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        self.initial_preview_text = self.preview_canvas.create_text(
            10, 10, anchor='nw', text="미리보기가 여기에 표시됩니다.", fill="gray", font=("Helvetica", 12))
        
        main_pane.add(right_container)
        main_pane.sash_place(0, 400, 0)

    def log(self, message):
        self.log_text.insert(END, f"{message}\n")
        self.log_text.see(END)
        self.update_idletasks()

    def add_files(self, *args):
        files = filedialog.askopenfilenames(title="Java 또는 Kotlin 파일 선택", filetypes=[("Java/Kotlin Files", "*.java *.kt"), ("All files", "*.*")])
        if not files: return
        
        new_files = 0
        for file in files:
            if file not in self.file_paths:
                self.file_paths.append(file)
                self.file_listbox.insert(tk.END, os.path.basename(file))
                new_files += 1
        self.log(f"{new_files}개 파일을 추가했습니다.")
        if self.base_dir is None:
            self.base_dir = os.path.dirname(os.path.commonpath(self.file_paths))

    def add_folder(self, *args):
        folder = filedialog.askdirectory(title="분석할 소스 코드가 있는 폴더를 선택하세요")
        if not folder: return

        if self.base_dir is None: self.base_dir = folder
        
        new_files = 0
        for root, _, files in os.walk(folder):
            for file in files:
                if file.endswith((".java", ".kt")):
                    full_path = os.path.join(root, file)
                    if full_path not in self.file_paths:
                        self.file_paths.append(full_path)
                        self.file_listbox.insert(tk.END, os.path.basename(full_path))
                        new_files += 1
        self.log(f"'{os.path.basename(folder)}' 폴더에서 {new_files}개 파일을 추가했습니다.")
    
    def delete_selected_files(self, *args):
        selected_indices = self.file_listbox.curselection()
        if not selected_indices:
            self.log("삭제할 파일을 선택해주세요.")
            return
            
        for i in sorted(selected_indices, reverse=True):
            self.file_listbox.delete(i)
            del self.file_paths[i]
        self.log(f"{len(selected_indices)}개 파일을 목록에서 삭제했습니다.")
        
        if not self.file_paths: self.base_dir = None

    def generate_preview(self, *args):
        title = self.title_entry.get().strip()
        if not title or not self.file_paths:
            messagebox.showwarning("경고", "제목을 입력하고 최소 하나 이상의 파일을 추가해야 합니다.")
            return

        self.log("="*50 + f"\n'{title}' 다이어그램 생성을 시작합니다...")
        self.preview_canvas.delete("all")
        self.preview_canvas.create_text(10, 10, anchor='nw', text="미리보기를 생성하는 중입니다...", fill="gray")

        try:
            self.log("소스 파일 분석 중...")
            structures, packages_info = self.parser.parse_files(self.file_paths, self.base_dir)
            if not structures: raise ValueError("분석 가능한 클래스/인터페이스를 찾지 못했습니다.")
            self.log(f"{len(structures)}개의 클래스/인터페이스를 분석했습니다.")

            self.log("PlantUML 코드 생성 중...")
            self.puml_content = self.generator.generate(title, structures, packages_info)
            self.log("PlantUML 코드 생성을 완료했습니다.")

            self.log(f"'{PLANTUML_JAR_PATH}'를 사용하여 이미지 렌더링 중...")
            if not os.path.exists(PLANTUML_JAR_PATH): raise FileNotFoundError(f"'{PLANTUML_JAR_PATH}'를 찾을 수 없습니다.")
                
            proc = subprocess.Popen(
                ['java', '-jar', PLANTUML_JAR_PATH, '-tpng', '-pipe'],
                stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE
            )
            png_data, stderr = proc.communicate(input=self.puml_content.encode('utf-8'))

            if proc.returncode != 0: raise RuntimeError(f"PlantUML 오류:\n{stderr.decode('utf-8', 'ignore')}")

            self.log("이미지 렌더링 완료. 미리보기를 표시합니다.")
            image_stream = io.BytesIO(png_data)
            image = Image.open(image_stream)
            
            self.preview_image_tk = ImageTk.PhotoImage(image)
            self.preview_canvas.delete("all")
            self.preview_canvas.create_image(0, 0, anchor='nw', image=self.preview_image_tk)
            self.preview_canvas.config(scrollregion=self.preview_canvas.bbox("all"))

            self.log("미리보기 생성에 성공했습니다.")

        except Exception as e:
            self.puml_content = None
            error_msg = f"미리보기 생성 오류: {e}"
            self.log(error_msg)
            messagebox.showerror("생성 오류", error_msg)
            self.preview_canvas.delete("all")
            self.preview_canvas.create_text(10, 10, anchor='nw', text="미리보기 생성 중 오류 발생", fill="red")

    def export_puml(self, *args):
        if not self.puml_content:
            messagebox.showwarning("경고", "먼저 '미리보기 생성'을 실행해야 합니다.")
            return

        results_dir = "results"
        os.makedirs(results_dir, exist_ok=True)
        
        title = self.title_entry.get().strip().replace(' ', '_') or "diagram"
        file_name = f"{title}.puml"

        save_path = filedialog.asksaveasfilename(
            initialdir=results_dir, initialfile=file_name,
            defaultextension=".puml", filetypes=[("PlantUML File", "*.puml")]
        )

        if save_path:
            try:
                with open(save_path, 'w', encoding='utf-8') as f: f.write(self.puml_content)
                self.log(f"다이어그램이 '{os.path.basename(save_path)}' 파일로 저장되었습니다.")
                messagebox.showinfo("성공", f"'{os.path.basename(save_path)}' 파일로 저장되었습니다.")
            except Exception as e:
                error_msg = f"파일 저장 오류: {e}"
                self.log(error_msg)
                messagebox.showerror("저장 오류", error_msg)

# --- 5. 프로그램 실행 ---

if __name__ == "__main__":
    check_environment()
    app = DiagramApp()
    app.mainloop()
