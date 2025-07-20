# Diagram generator by java, kotlin

한눈에 프로그램이 어떻게 구성되어있는지 확인할 필요가 있다. 

----------------

다이어그램은 나만 보지만 내가 편하기 위해서  AI 한테 만들어달라고 했다. 
계속 수정을 해야할거 같지만 일단은 이런게 있으면 좋겠는데 1도 지원안하고 
1도 아무도 안만들기 때문에 개같아서 만들었다. 
공감한다면 사용해보고 공감 못한다면 그냥 쳐다도 보지마시길


## How To Modify [diagram_generator.py] & release this program

1. 파일을 연다.
2. 코드를 수정한다.
3. 아래의 명령어을 입력한다. (pyinstaller를 설치한 경우)
   1. 설치안했다면 설치한다. 
       > pip install pyinstaller

   > pyinstaller --onefile --windowed --name "BIST_Diagram_Maker" diagram_generator.pyw

4.  이 installer 을 돌리면 dist 폴더가 생성되고 거기안에 있는 exe를 배포용으로 사용하면 된다. 


### diagram_generator.pyw 
파일을 더블클릭하면 열수 있다. 

### dist / BIST_Diagram_Maker.exe
파일을 더블클릭하면 열수 있다. 
해당 파일을 실행파일로 python이 없는 환경에서도 사용이 가능하다.
배포용으로 좋은듯


#### exe로 배포 딱히 보안상의 정도가 심하진 않다. 그래도 코드를 쉽게 못보게 하니 어느정도는 보안책이라 생각한다. 오히려 환경설정의 거지같음을 뺄수 있어서 사용하는거 같기도 하다. 
- PyInstaller 디컴파일 과정
    일반적으로 디컴파일은 다음과 같은 2단계로 진행되며, 각 단계마다 자동화된 도구가 존재합니다.

    실행 파일 압축 해제 (Extracting)

    PyInstaller로 만들어진 .exe 파일은 내부에 필요한 파일들을 압축한 아카이브 형태입니다.

    pyinstxtractor와 같은 도구를 사용하면 이 .exe 파일을 쉽게 풀 수 있으며, 그 결과로 원본 소스 코드에 해당하는 .pyc (파이썬 바이트코드) 파일들을 얻을 수 있습니다.

    바이트코드 역컴파일 (Decompiling)

    추출된 .pyc 파일은 아직 사람이 읽을 수 있는 코드가 아닙니다.

    uncompyle6나 decompyle3 같은 디컴파일러를 사용하면 이 바이트코드를 다시 원래의 .py 파이썬 소스 코드로 변환할 수 있습니다.

    이러한 도구들이 공개되어 있고 사용법도 널리 알려져 있어, 기술 지식이 있는 사람이라면 누구나 시도해 볼 수 있습니다.