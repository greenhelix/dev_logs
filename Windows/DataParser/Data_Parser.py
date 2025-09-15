# Data_Parser.py
import os
import csv
import sys
import html

# 1. 필수 라이브러리 설치 확인
try:
    import pandas as pd
except ImportError:
    print("[ERROR] 'pandas' library is not installed. Please run: pip install pandas")
    sys.exit(1)

try:
    import tabulate
except ImportError:
    print("[ERROR] 'tabulate' library is not installed. Please run: pip install tabulate")
    sys.exit(1)

# 2. GUI 환경 확인
try:
    from tkinter import Tk, filedialog
    GUI_MODE = True
except ImportError:
    GUI_MODE = False

def select_file_via_gui():
    """GUI 파일 선택창을 통해 파일 경로를 얻습니다."""
    print("[INFO] Opening file selection dialog...")
    root = Tk()
    root.withdraw()
    file_path = filedialog.askopenfilename(
        title='Select the input data file',
        filetypes=[('All Files', '*.*'), ('CSV files', '*.csv'), ('TSV files', '*.tsv'), ('Text files', '*.txt')]
    )
    return file_path

def select_file_via_console():
    """콘솔 입력을 통해 파일 경로를 얻습니다."""
    print("[INFO] GUI not available. Please enter the file path manually.")
    file_path = input("Enter the full path of the input data file: ").strip().strip("'\"")
    return file_path

def detect_file_format(lines):
    """파일 내용을 기반으로 형식을 지능적으로 탐지합니다."""
    if sum(1 for line in lines if line.strip().startswith('[[') and line.strip().endswith(']]')) > len(lines) * 0.8:
        return 'module_list'
    elif any('Module' in line and 'Test' in line and 'Result' in line for line in lines[:20]):
        return 'detailed_results'
    else:
        return 'data_format'

def parse_module_list(lines):
    """testmodule.txt와 같은 모듈 목록 파일을 파싱합니다."""
    print("[DEBUG] Parsing as 'module_list' format...")
    records = []
    for line in lines:
        line = line.strip()
        if line.startswith('[[') and line.endswith(']]'):
            records.append({"Module": line, "ABI": "", "Total Tests": 0})
    df = pd.DataFrame(records)
    return df

def parse_detailed_results(lines, delimiter):
    """상세 결과 파일을 파싱합니다."""
    print("[DEBUG] Parsing as 'detailed_results' or 'data_format'...")
    records = []
    current_module = "[[Unknown]]"
    current_abi = "Unknown"
    is_incomplete_section = False
    for line in lines:
        line = line.strip()
        if not line or line.startswith(('| Summary', ':---')):
            is_incomplete_section = False
            continue
        if "Incomplete Modules" in line:
            is_incomplete_section = True
            print("[DEBUG] Entered 'Incomplete Modules' section.")
            continue
        parts = line.split()
        if len(parts) >= 2 and ('armeabi' in parts[0] or 'arm64' in parts[0]):
            current_abi = parts[0]
            module_name = ' '.join(parts[1:]).replace('[instant]', '(instant)').strip(', ')
            current_module = f"[[{module_name}]]"
            print(f"[DEBUG] Context switch -> Module: {current_module}, ABI: {current_abi}")
            if is_incomplete_section:
                records.append({"Module": current_module, "Test": "", "Result": "incomplete", "Details": "", "ABI": current_abi})
            continue
        if is_incomplete_section or line.lower().startswith(('test', '| test')):
            continue
        fields = []
        try:
            if line.startswith('|') and line.endswith('|'):
                fields = [field.strip() for field in line.split('|')[1:-1]]
            elif delimiter in line:
                fields = [field.strip() for field in line.split(delimiter, 2)]
            if len(fields) >= 3:
                records.append({"Module": current_module, "Test": fields[0], "Result": fields[1], "Details": fields[2], "ABI": current_abi})
        except Exception:
            pass
    if not records:
        return pd.DataFrame()
    df = pd.DataFrame(records)
    print(f"[DEBUG] Parsed {len(df)} initial records.")
    group_cols = ['Module', 'Test', 'Result', 'Details']
    df_merged = df.groupby(group_cols, as_index=False).agg(
        ABI=('ABI', lambda x: ', '.join(sorted(set(abi.strip() for abi in x if abi and abi != 'nan'))))
    )
    return df_merged[['Module', 'Test', 'Result', 'Details', 'ABI']]

def save_as_html(df, filename):
    """DataFrame을 지정된 HTML 형식으로 저장합니다."""
    print(f"[INFO] Saving data to HTML file: {filename}")
    html_content = """
<table style="width:100%; table-layout:fixed; border-collapse: collapse; border: 1px solid #ddd;">
  <colgroup>
    <col style="width: 5%;">
    <col style="width: 20%;">
    <col style="width: 45%;">
    <col style="width: 10%;">
    <col style="width: 20%;">
  </colgroup>
  <thead style="background-color: #f2f2f2;">
    <tr>
      <th>No</th>
      <th>Module</th>
      <th>Test</th>
      <th>Result</th>
      <th>ABI</th>
    </tr>
  </thead>
  <tbody>
"""
    required_cols = ['No', 'Module', 'Test', 'Result', 'ABI']
    for col in required_cols:
        if col not in df.columns:
            # Details 열은 표시하지 않으므로, 누락되어도 괜찮습니다.
            if col not in ['No', 'Details']:
                 df[col] = ''
    
    # 표시할 열만 선택
    df_html = df[[col for col in required_cols if col in df.columns]]

    for row in df_html.itertuples(index=False):
        html_content += "    <tr>\n"
        # No: 번호
        html_content += f'      <td">{html.escape(str(row.No))}</td>\n'
        # Module: 항상 한 줄로 표시
        html_content += f'      <td">{html.escape(str(row.Module))}</td>\n'
        # Test: 길면 자동 줄바꿈
        html_content += f'      <td">{html.escape(str(row.Test))}</td>\n'
        # Result: 기본 스타일
        html_content += f'      <td">{html.escape(str(row.Result))}</td>\n'
        # ABI: 길면 자동 줄바꿈
        html_content += f'      <td">{html.escape(str(row.ABI))}</td>\n'
        html_content += "    </tr>\n"

    html_content += """
  </tbody>
</table>
"""
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(html_content)

def main():
    """메인 실행 함수"""
    print("[INFO] Starting Data_Parser.py...")
    file_path = select_file_via_gui() if GUI_MODE else select_file_via_console()
    if not file_path or not os.path.isfile(file_path):
        print(f"[ERROR] Invalid file path: '{file_path}'. Exiting.")
        sys.exit(1)

    print(f"[INFO] Selected file: {file_path}")
    delimiter = '\t' if file_path.lower().endswith('.tsv') else ','
    print(f"[INFO] Using delimiter: '{delimiter}'")

    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        lines = f.readlines()
    print(f"[INFO] Read {len(lines)} lines from file.")

    file_format = detect_file_format(lines)
    print(f"[INFO] Detected file format as: '{file_format}'")

    if file_format == 'module_list':
        df_final = parse_module_list(lines)
    else:
        df_final = parse_detailed_results(lines, delimiter)

    if df_final.empty:
        print("[ERROR] Parsing failed to produce any data. Exiting.")
        sys.exit(1)

    # 번호 열 추가
    if not df_final.empty:
        df_final.insert(0, 'No', range(1, len(df_final) + 1))

    print("\n" + "="*80)
    print("[INFO] PARSED DATA PREVIEW (Table Format)")
    print(df_final.head().to_markdown(index=False))
    print("="*80)

    output_format = ''
    while output_format not in ['1', '2']:
        try:
            output_format = input("출력 형식을 선택하세요:\n1. tsv\n2. html\n선택 (1 또는 2): ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n[WARNING] No user input. Defaulting to '1' (tsv).")
            output_format = '1'
    
    output_format_str = 'tsv' if output_format == '1' else 'html'

    try:
        save_confirm = input(f"미리보기가 올바른가요? {output_format_str.upper()} 파일로 저장하시겠습니까? (y/n): ").strip().lower()
    except (EOFError, KeyboardInterrupt):
        print("\n[WARNING] No user input. Assuming 'y'.")
        save_confirm = 'y'

    if save_confirm != 'y':
        print("[INFO] User cancelled. Exiting.")
        sys.exit(0)

    base_filename = os.path.splitext(os.path.basename(file_path))[0]
    
    if output_format_str == 'tsv':
        output_filename = f"{base_filename}_parsed.tsv"
        df_final.to_csv(output_filename, sep='\t', index=False, quoting=csv.QUOTE_MINIMAL)
        print(f"\n[SUCCESS] Data successfully saved to: {output_filename}")
    else:
        output_filename = f"{base_filename}_parsed.html"
        save_as_html(df_final, output_filename)
        print(f"\n[SUCCESS] Data successfully saved to: {output_filename}")

    print("[INFO] Program finished.")

if __name__ == "__main__":
    main()
