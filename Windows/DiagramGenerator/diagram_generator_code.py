# BIST - Diagram Auto Maker v3
# Made by ik

import tkinter as tk
from tkinter import filedialog, messagebox
import re
import os
import sys
from collections import defaultdict

# --- 1. 환경 검사 ---
def check_environment():
    """실행 환경이 프로그램 구동에 적합한지 확인합니다."""
    if sys.version_info < (3, 6):
        print("오류: 이 프로그램을 실행하려면 Python 3.6 이상의 버전이 필요합니다.")
        input("계속하려면 Enter 키를 누르세요...")
        sys.exit(1)

# --- 2. 코드 파싱 로직 (대폭 개선) ---
class CodeParser:
    """
    Android 코드(Java/Kotlin)의 특성을 고려하여 구조를 분석하는 클래스입니다.
    - 생명주기 메서드, TAG 변수를 필터링합니다.
    - 정확한 메서드 선언만을 추출합니다.
    """
    # 생략할 안드로이드 생명주기 및 일반 메서드 목록
    LIFECYCLE_METHODS = {
        'onCreate', 'onStart', 'onResume', 'onPause', 'onStop', 'onDestroy',
        'onCreateView', 'onViewCreated', 'onDestroyView', 'onAttach', 'onDetach',
        'onSaveInstanceState', 'onRestoreInstanceState', 'onRequestPermissionsResult',
        'dispatchKeyEvent', 'getTargetFocusId'
    }

    def _find_matching_brace(self, content, start_pos):
        """지정된 시작 괄호 '{'에 대응하는 닫는 괄호 '}'의 위치를 찾습니다."""
        brace_level = 1
        pos = start_pos
        while pos < len(content):
            if content[pos] == '{':
                brace_level += 1
            elif content[pos] == '}':
                brace_level -= 1
                if brace_level == 0:
                    return pos
            pos += 1
        return -1

    def parse_files(self, file_paths):
        """여러 소스 파일을 파싱하고 구조화된 데이터를 반환합니다."""
        structures = {}
        all_class_names = set()
        file_contents = {}

        # 1. 모든 파일 내용 읽기 및 클래스/인터페이스 이름 수집
        for file_path in file_paths:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                file_contents[file_path] = content
                matches = re.findall(r'(?:class|interface)\s+(\w+)', content)
                all_class_names.update(matches)

        # 2. 각 파일의 구조를 상세히 파싱
        for file_path, content in file_contents.items():
            class_definitions = re.finditer(r'((?:public|private|protected|abstract|static|\s)*)(class|interface)\s+(\w+)(?:\s*extends\s+([\w\s,.<>]+))?(?:\s*implements\s+([\w\s,.<>]+))?\s*{', content)

            for match in class_definitions:
                _, keyword, name, extends, implements = match.groups()
                if name not in all_class_names: continue

                class_body_start = match.end()
                class_body_end = self._find_matching_brace(content, class_body_start)
                if class_body_end == -1: continue

                body = content[class_body_start:class_body_end]
                
                # 클래스 이름을 키로 사용하여 구조 초기화
                structures[name] = {
                    'type': keyword, 'variables': [], 'methods': [],
                    'relations': defaultdict(list)
                }

                # 상속 및 구현 관계 분석
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

                # 변수 추출 (TAG 변수 필터링)
                var_pattern = r'^\s*(private|public|protected|final|static|\s)*([\w<>.\[\]]+)\s+([\w\[\]]+)\s*(?:=.*?;|;)'
                for v_match in re.finditer(var_pattern, body, re.MULTILINE):
                    visibility_group, var_type, var_name = v_match.groups()
                    if 'static' in str(visibility_group) and 'final' in str(visibility_group) and var_name == 'TAG':
                        continue # TAG 변수는 건너뜀
                    
                    vis_symbol = '+' if 'public' in str(visibility_group) else '-'
                    structures[name]['variables'].append(f'{vis_symbol} {var_name}: {var_type.strip()}')
                    
                    clean_type = re.sub(r'<.*?>', '', var_type).strip()
                    if clean_type in all_class_names and clean_type != name:
                        structures[name]['relations']['composition'].append(clean_type)

                # 메서드 추출 (생명주기 메서드 필터링 및 정확도 향상)
                method_pattern = r'^\s*(?:@\w+\s*)*\s*(public|private|protected|static|abstract|synchronized|final|\s)*([\w<>.\[\]]+)\s+(\w+)\s*\(([^)]*)\)\s*(?:{|throws)'
                for m_match in re.finditer(method_pattern, body, re.MULTILINE):
                    _, _, return_type, method_name, params = m_match.groups()
                    # 제어문 키워드 및 생명주기 메서드 필터링
                    if method_name in self.LIFECYCLE_METHODS or method_name in {'if', 'for', 'while', 'switch', 'catch'}:
                        continue
                    
                    # 생성자는 유지 (메서드 이름과 클래스 이름이 같음)
                    if method_name == name:
                         vis_symbol = '+' if 'public' in str(m_match.group(1)) else '-'
                         structures[name]['methods'].append(f'{vis_symbol} {method_name}()')
                         continue

                    # 일반 메서드
                    vis_symbol = '+' if 'public' in str(m_match.group(1)) else '-'
                    structures[name]['methods'].append(f'{vis_symbol} {method_name}()')

                    # 파라미터에서 Dependency 관계 분석
                    for class_name in all_class_names:
                        if re.search(r'\b' + re.escape(class_name) + r'\b', params):
                             if class_name != name:
                                structures[name]['relations']['dependency'].append(class_name)
        return structures


# --- 3. PUML 생성 로직 ---
class PumlGenerator:
    """파싱된 데이터 구조를 바탕으로 PlantUML(.puml) 파일 내용을 생성합니다."""
    def generate(self, title, structures):
        puml_content = f"@startuml\n\ntitle {title}\n\nskinparam classAttributeIconSize 0\n\n"
        for name, data in structures.items():
            puml_content += f"{data['type']} \"{name}\" {{\n"
            for method in sorted(list(set(data['methods']))):
                puml_content += f"  {method}\n"
            if data['methods'] and data['variables']:
                 puml_content += "--\n"
            for var in sorted(list(set(data['variables']))):
                puml_content += f"  {var}\n"
            puml_content += "}\n\n"

        puml_content += "' --- 관계 정의 ---\n\n"
        added_relations = set()
        for name, data in structures.items():
            relations = data.get('relations', {})
            # 상속, 구현, 컴포지션, 의존 순으로 관계 정의
            for rel_type in ['extends', 'implements', 'composition', 'dependency']:
                for target in set(relations.get(rel_type, [])):
                    # 관계 중복 방지
                    rel_tuple = tuple(sorted((name, target))) + (rel_type,)
                    if rel_type in ['extends', 'implements']: rel_tuple = (name, target, rel_type)
                    
                    if rel_tuple in added_relations: continue

                    if rel_type == 'extends':
                        puml_content += f"{target} <|-- {name}\n"
                    elif rel_type == 'implements':
                        puml_content += f"{target} <|.. {name}\n"
                    elif rel_type == 'composition':
                        puml_content += f"{name} \"1\" *-- \"*\" {target}\n"
                    elif rel_type == 'dependency':
                        is_strongly_related = any(target in l for l in [relations.get(k,[]) for k in ['extends','implements','composition']])
                        if not is_strongly_related:
                            puml_content += f"{name} ..> {target} : uses\n"
                    
                    added_relations.add(rel_tuple)

        puml_content += "\n@enduml\n"
        return puml_content

# --- 4. GUI 애플리케이션 ---
class DiagramApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("BIST - Diagram Auto Maker v3")
        self.geometry("700x500")
        self.parser = CodeParser()
        self.generator = PumlGenerator()
        self.file_paths = []
        self.create_widgets()

    def create_widgets(self):
        top_frame = tk.Frame(self, padx=10, pady=10)
        top_frame.pack(fill=tk.X)
        tk.Label(top_frame, text="다이어그램 제목:", font=("Helvetica", 10)).pack(side=tk.LEFT)
        self.title_entry = tk.Entry(top_frame, font=("Helvetica", 10))
        self.title_entry.pack(fill=tk.X, expand=True, side=tk.LEFT, padx=5)

        mid_frame = tk.Frame(self, padx=10, pady=5)
        mid_frame.pack(fill=tk.BOTH, expand=True)

        list_frame = tk.Frame(mid_frame)
        list_frame.pack(pady=5, fill=tk.BOTH, expand=True)
        self.file_listbox = tk.Listbox(list_frame, selectmode=tk.EXTENDED, font=("Courier New", 9))
        self.file_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        scrollbar = tk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.file_listbox.yview)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.file_listbox.config(yscrollcommand=scrollbar.set)

        button_frame = tk.Frame(mid_frame)
        button_frame.pack(pady=5)
        tk.Button(button_frame, text="파일 추가", command=self.add_files).pack(side=tk.LEFT, padx=5)
        tk.Button(button_frame, text="선택 삭제", command=self.delete_selected_files).pack(side=tk.LEFT, padx=5)

        tk.Button(self, text="PUML 다이어그램 생성", command=self.generate_diagram, bg="#2E8B57", fg="white", font=("Helvetica", 12, "bold"), relief=tk.GROOVE, padx=10, pady=10).pack(pady=10)
        
        tk.Label(self, text="made by ik", fg="gray", font=("Helvetica", 8)).pack(side=tk.BOTTOM, pady=5)

    def add_files(self):
        files = filedialog.askopenfilenames(title="Java 또는 Kotlin 파일 선택", filetypes=[("Java/Kotlin Files", "*.java *.kt"), ("All files", "*.*")])
        for file in files:
            if file not in self.file_paths:
                self.file_paths.append(file)
                self.file_listbox.insert(tk.END, os.path.basename(file))
    
    def delete_selected_files(self):
        selected_indices = self.file_listbox.curselection()
        for i in sorted(selected_indices, reverse=True):
            self.file_listbox.delete(i)
            del self.file_paths[i]

    def generate_diagram(self):
        title = self.title_entry.get().strip()
        if not title:
            messagebox.showwarning("경고", "다이어그램 제목을 반드시 입력해야 합니다.")
            return
        if not self.file_paths:
            messagebox.showwarning("경고", "최소 하나 이상의 소스 파일을 추가해야 합니다.")
            return

        try:
            structures = self.parser.parse_files(self.file_paths)
            if not structures:
                messagebox.showerror("오류", "파일에서 클래스나 인터페이스를 찾을 수 없습니다.")
                return

            puml_content = self.generator.generate(title, structures)
            
            file_name = f"{title.replace(' ', '_')}.puml"
            save_path = filedialog.asksaveasfilename(defaultextension=".puml", initialfile=file_name, filetypes=[("PlantUML File", "*.puml")])
            if save_path:
                with open(save_path, 'w', encoding='utf-8') as f:
                    f.write(puml_content)
                messagebox.showinfo("성공", f"다이어그램이 성공적으로 '{os.path.basename(save_path)}' 파일로 저장되었습니다.")
        except Exception as e:
            messagebox.showerror("생성 오류", f"다이어그램 생성 중 오류가 발생했습니다:\n{e}")

# --- 5. 프로그램 실행 ---
if __name__ == "__main__":
    check_environment()
    app = DiagramApp()
    app.mainloop()
