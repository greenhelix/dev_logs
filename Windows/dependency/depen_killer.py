import os
import sys
import subprocess
import shutil
import tempfile
import xml.etree.ElementTree as ET

# 스크립트 정보
SCRIPT_NAME = "depen_killer.py"
SCRIPT_VERSION = "2.0.0" # 빌드 파일 선택 및 환경설정 가이드 기능 추가

def print_log(message):
    """실행 로그를 영어로 출력합니다."""
    print(f"[INFO] {message}")

def print_error(message):
    """오류 메시지를 영어로 출력합니다."""
    print(f"[ERROR] {message}", file=sys.stderr)

def print_setup_guide():
    """환경 변수 설정 가이드를 출력합니다."""
    print_error("Required command not found. Please ensure Python, JDK, and Maven are installed and in your system's PATH.")
    print("\n--- Environment Setup Guide ---")
    
    # macOS / Linux 가이드
    if sys.platform in ["darwin", "linux"]:
        print("\n# For macOS / Linux (using bash/zsh):")
        print("1. Install dependencies (e.g., using Homebrew on macOS or apt on Debian/Ubuntu):")
        print("   $ brew install python openjdk maven")
        print("   OR")
        print("   $ sudo apt-get update && sudo apt-get install -y python3 openjdk-11-jdk maven")
        print("\n2. Add to your shell profile (e.g., ~/.zshrc, ~/.bash_profile):")
        print('   export JAVA_HOME=$(/usr/libexec/java_home)')
        print('   export M2_HOME=/path/to/your/maven/installation')
        print('   export PATH=$PATH:$JAVA_HOME/bin:$M2_HOME/bin')
        print("\n3. Apply changes:")
        print("   $ source ~/.zshrc  # or your respective profile file")

    # Windows 가이드
    elif sys.platform == "win32":
        print("\n# For Windows:")
        print("1. Install Python, a JDK (e.g., Adoptium Temurin), and Apache Maven.")
        print("   - Python: from python.org or Microsoft Store.")
        print("   - JDK/Maven: Download and extract to a folder like C:\\tools\\")
        print("\n2. Edit System Environment Variables:")
        print("   - Open 'Edit the system environment variables' control panel.")
        print("   - Click 'Environment Variables...' button.")
        print("   - Under 'System variables', create/edit the following:")
        print("     - JAVA_HOME = C:\\path\\to\\your\\jdk")
        print("     - M2_HOME   = C:\\path\\to\\your\\maven")
        print("   - Find the 'Path' variable, click 'Edit...', and add these two new entries:")
        print("     - %JAVA_HOME%\\bin")
        print("     - %M2_HOME%\\bin")
        print("\n3. Open a new Command Prompt or PowerShell window to apply changes.")
    print("\n-------------------------------\n")

def check_prerequisites():
    """스크립트 실행에 필요한 프로그램(java, mvn)이 설치되어 있는지 확인합니다."""
    print_log("Checking for prerequisites (Java, Maven)...")
    if not all(shutil.which(cmd) for cmd in ["java", "mvn"]):
        print_setup_guide()
        return False
    print_log("Prerequisites are satisfied.")
    return True

def create_temp_pom(dependency_gav: str) -> str:
    """의존성을 다운로드하기 위한 임시 pom.xml 파일을 생성합니다."""
    try:
        group_id, artifact_id, version = dependency_gav.split(":")
    except ValueError:
        print_error(f"Invalid dependency format: '{dependency_gav}'. Use 'groupId:artifactId:version'.")
        return None

    temp_dir = tempfile.mkdtemp(prefix="depen_killer_")
    pom_path = os.path.join(temp_dir, "pom.xml")
    print_log(f"Creating a temporary pom.xml at: {pom_path}")

    pom_content = f"""
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>depen-killer-importer</artifactId>
    <version>1.0</version>
    <repositories>
        <repository>
            <id>google</id>
            <url>https://maven.google.com</url>
        </repository>
        <repository>
            <id>maven-central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </repository>
    </repositories>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>androidx.lifecycle</groupId>
                <artifactId>lifecycle-common</artifactId>
                <version>2.5.1</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>{group_id}</groupId>
            <artifactId>{artifact_id}</artifactId>
            <version>{version}</version>
            <type>aar</type>
        </dependency>
    </dependencies>
</project>
"""
    with open(pom_path, "w", encoding="utf-8") as f:
        f.write(pom_content)
    return pom_path

def download_dependencies(pom_path: str, output_dir: str):
    """Maven을 사용하여 모든 의존성을 다운로드합니다."""
    print_log(f"Downloading dependencies to '{output_dir}' folder...")
    command = ["mvn", "dependency:copy-dependencies", "-f", pom_path, f"-DoutputDirectory={os.path.abspath(output_dir)}", "-Dmdep.prependGroupId=true"]
    process = subprocess.run(command, capture_output=True, text=True, shell=(sys.platform == "win32"))
    if process.returncode != 0:
        print_error("Maven dependency download failed.")
        print_error("Maven output:\n" + process.stdout + process.stderr)
        return False
    print_log("Dependency download completed successfully.")
    return True

def generate_bp_file(source_dir: str, main_dependency_name: str):
    """Android.bp 파일을 생성합니다."""
    bp_path = os.path.join(source_dir, "Android.bp")
    print_log(f"Generating Android.bp file at: {bp_path}")
    
    bp_content = f"// Generated by {SCRIPT_NAME} v{SCRIPT_VERSION}\n// Do not edit this file manually.\n\n"
    module_names = []
    
    for filename in sorted(os.listdir(source_dir)):
        if not (filename.endswith(".aar") or filename.endswith(".jar")):
            continue
        
        module_name = filename.rsplit('.', 1)[0].replace('.', '_').replace('-', '_')
        module_names.append(module_name)
        
        if filename.endswith(".aar"):
            bp_content += f'android_prebuilt_aar {{\n    name: "{module_name}",\n    srcs: ["{filename}"],\n}}\n\n'
        elif filename.endswith(".jar"):
            bp_content += f'java_import {{\n    name: "{module_name}",\n    jars: ["{filename}"],\n}}\n\n'

    if module_names:
        aggregate_module_name = main_dependency_name + "_dependencies"
        static_libs_str = "\n".join([f'        "{name}",' for name in module_names])
        bp_content += f'android_library {{\n    name: "{aggregate_module_name}",\n    export_package_resources: true,\n    static_libs: [\n{static_libs_str}\n    ],\n}}\n'
    
    with open(bp_path, "w", encoding="utf-8") as f: f.write(bp_content)
    print_log(f"Android.bp file generated. You can depend on '{aggregate_module_name}' in your AOSP build.")

def generate_mk_file(source_dir: str, main_dependency_name: str):
    """Android.mk 파일을 생성합니다."""
    mk_path = os.path.join(source_dir, "Android.mk")
    print_log(f"Generating Android.mk file at: {mk_path}")
    
    mk_content = f"# Generated by {SCRIPT_NAME} v{SCRIPT_VERSION}\n# Do not edit this file manually.\n\n"
    module_names = []
    
    for filename in sorted(os.listdir(source_dir)):
        if not (filename.endswith(".aar") or filename.endswith(".jar")):
            continue
            
        module_name = filename.rsplit('.', 1)[0].replace('.', '_').replace('-', '_')
        module_names.append(module_name)
        
        mk_content += f"include $(CLEAR_VARS)\n"
        mk_content += f"LOCAL_MODULE := {module_name}\n"
        mk_content += f"LOCAL_SRC_FILES := {filename}\n"
        
        if filename.endswith(".aar"):
            mk_content += f"LOCAL_MODULE_CLASS := JAVA_LIBRARIES\n"
            mk_content += f"LOCAL_MODULE_SUFFIX := .aar\n"
        elif filename.endswith(".jar"):
            mk_content += f"LOCAL_MODULE_CLASS := JAVA_LIBRARIES\n"
            mk_content += f"LOCAL_MODULE_SUFFIX := .jar\n"
        
        mk_content += f"include $(BUILD_PREBUILT)\n\n"

    if module_names:
        aggregate_module_name = main_dependency_name + "_dependencies"
        static_libs_str = " \\\n    ".join(module_names)
        mk_content += f"include $(CLEAR_VARS)\n"
        mk_content += f"LOCAL_MODULE := {aggregate_module_name}\n"
        mk_content += f"LOCAL_STATIC_JAVA_LIBRARIES := {static_libs_str}\n"
        mk_content += f"LOCAL_SDK_VERSION := current\n"
        mk_content += f"include $(BUILD_STATIC_JAVA_LIBRARY)\n"
        
    with open(mk_path, "w", encoding="utf-8") as f: f.write(mk_content)
    print_log(f"Android.mk file generated. You can depend on '{aggregate_module_name}' in your AOSP build.")

def main():
    """메인 실행 함수"""
    print_log(f"Running {SCRIPT_NAME} v{SCRIPT_VERSION}")

    if not check_prerequisites():
        sys.exit(1)

    if len(sys.argv) != 4:
        print_error(f"Usage: python {SCRIPT_NAME} <groupId:artifactId:version> <output_directory> <bp|mk>")
        print_error(f"Example: python {SCRIPT_NAME} androidx.appcompat:appcompat:1.6.1 androidx_libs bp")
        sys.exit(1)

    # 의존성 죽일 라이브러리 대상 : dependency_gav
    # 그 라이브러리들 넣어줄 디렉토리 : output_dir
    # android.bp, android.mk 선택 : build_type
    dependency_gav, output_dir, build_type = sys.argv[1], sys.argv[2], sys.argv[3].lower()

    if build_type not in ['bp', 'mk']:
        print_error("Invalid build file type. Please choose 'bp' for Android.bp or 'mk' for Android.mk.")
        sys.exit(1)
    
    #  aar, jar, bp.mk 넣어둘 폴더 생성
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # 라이브러리르 중심으로 임시 pom 생성 (통신만 되게 비어있는상태라고 보면됨)ㄴ
    pom_path = create_temp_pom(dependency_gav)
    if not pom_path: sys.exit(1)
    
    temp_dir = os.path.dirname(pom_path)
    try:
        # mvn 명령어로 관련 라이브러리 데이터와 의존성 확립해서 받는다. 
        if not download_dependencies(pom_path, output_dir):
            sys.exit(1)

        parts = dependency_gav.split(':')
        main_dependency_name = "_".join(parts[0:2])
        
        # 다 받은 pom과 aar, jar들을 이제 정리한다.
        if build_type == 'bp':
            generate_bp_file(output_dir, main_dependency_name)
        elif build_type == 'mk':
            generate_mk_file(output_dir, main_dependency_name)

        print_log("All tasks completed successfully!")
    finally:
        shutil.rmtree(temp_dir)

if __name__ == "__main__":
    main()
