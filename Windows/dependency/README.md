<!-- <img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/> -->

# readme.md 에 MAVEN이란 무엇인지 설명도 추가해줘

그리고 나는 android관련 라이브러리를 주로 사용하는데 그 와 관련해서 도움될 만한  기본, 기초 정보도 추가해줘
markdown 형식으로 작성해서 줘

네, 알겠습니다. 요청하신 `Maven`에 대한 설명과 Android 라이브러리 관련 기초 정보를 포함하여 전체 `README.md` 파일을 Markdown 형식으로 새롭게 작성해 드리겠습니다.

***

# DepenKiller: Maven 의존성 자동 해결 및 AOSP 빌드 파일 생성 도구

`depen_killer.py`는 Maven 저장소에 있는 라이브러리(AAR, JAR)와 그에 관련된 모든 **전이 의존성(Transitive Dependencies)**을 자동으로 다운로드하고, AOSP(Android Open Source Project) 빌드 시스템에서 즉시 사용할 수 있도록 `Android.bp` 또는 `Android.mk` 파일을 생성해주는 강력한 자동화 스크립트입니다.

## DepenKiller를 사용하는 이유?

AOSP 환경에서 외부 라이브러리를 사용하려면 해당 라이브러리뿐만 아니라, 그 라이브러리가 의존하는 다른 모든 라이브러리까지 수동으로 찾아 빌드 시스템에 추가해야 하는 번거로움이 있습니다. 이 스크립트는 그 모든 과정을 단 한 줄의 명령어로 해결합니다.

1. **전이 의존성 자동 분석**: 사용자가 최상위 라이브러리 하나(`예: androidx.media3:media3-exoplayer`)를 지정하면, Maven의 강력한 의존성 분석 기능을 사용하여 해당 라이브러리가 필요로 하는 모든 하위 라이브러리(`예: media3-decoder`, `guava` 등)를 재귀적으로 끝까지 추적합니다.
2. **모든 의존성 다운로드**: 분석된 전체 의존성 트리(Dependency Tree)에 포함된 모든 `.aar` 및 `.jar` 파일을 지정된 폴더로 다운로드합니다.
3. **빌드 파일 자동 생성**: 사용자의 선택에 따라 최신 `Android.bp`(Soong) 또는 레거시 `Android.mk`(Make) 형식의 빌드 파일을 생성합니다.
4. **통합 라이브러리 제공**: 모든 개별 모듈을 포함하는 편리한 통합 라이브러리 모듈을 함께 생성하여, 다른 빌드 파일에서는 이 통합 모듈 이름 하나만 추가하면 모든 의존성을 간편하게 사용할 수 있습니다.

> **핵심**: `mvnrepository.com`과 같은 웹사이트에서는 직접 의존성만 보이지만, 이 스크립트는 숨겨진 모든 전이 의존성까지 완벽하게 찾아내므로, 웹사이트에서 보이는 것보다 훨씬 많은 파일이 다운로드되는 것이 정상적인 동작입니다.

<br>

***

## 기본/기초 정보

### Maven이란 무엇인가?

**메이븐(Maven)**은 자바 프로젝트의 **빌드(Build) 자동화 및 의존성 관리 도구**입니다. 쉽게 말해, 프로젝트를 구축하는 데 필요한 모든 작업을 체계적으로 관리하고 자동화해주는 비서와 같습니다.[^2][^3]

* **빌드 자동화**: 소스 코드를 컴파일하고, 테스트를 실행하며, 최종 실행 가능한 파일(`JAR`, `WAR`, `AAR` 등)로 패키징하는 복잡한 과정을 명령어 하나로 처리합니다.[^10]
* **의존성 관리 (가장 중요한 기능)**: 내 프로젝트가 필요로 하는 외부 라이브러리(의존성)들을 `pom.xml`이라는 설정 파일에 명시만 해두면, Maven이 인터넷을 통해 자동으로 다운로드하고 관리해줍니다.[^1][^5]

이 스크립트는 바로 Maven의 강력한 의존성 관리 기능을 활용하여, AOSP 개발자가 수동으로 라이브러리를 찾아다니는 수고를 덜어줍니다.

### Android 라이브러리 사용자를 위한 팁

1. **AAR vs JAR**:
    * **JAR (Java Archive)**: 순수 자바 코드로만 구성된 라이브러리입니다.
    * **AAR (Android Archive)**: 자바 코드뿐만 아니라, 안드로이드 리소스(레이아웃 xml, 이미지, Manifest 파일 등)를 포함할 수 있는 안드로이드 전용 라이브러리 형식입니다. 대부분의 AndroidX 라이브러리는 AAR 형식입니다.
2. **라이브러리 검색**:
    * [**mvnrepository.com**](https://mvnrepository.com/): 가장 유명한 Maven 라이브러리 검색 사이트입니다.
    * [**Google's Maven Repository**](https://maven.google.com/web/index.html): 모든 `AndroidX` 및 Google Play 서비스 라이브러리는 이곳에서 직접 검색하고 확인할 수 있습니다.
3. **전이 의존성(Transitive Dependencies)의 중요성**:
    * AndroidX 라이브러리는 서로 복잡하게 얽혀있습니다. 예를 들어 `appcompat` 라이브러리 하나를 사용하더라도, 실제로는 `activity`, `fragment`, `lifecycle` 등 수많은 다른 라이브러리들이 연쇄적으로 필요하게 됩니다.
    * 이 모든 것을 수동으로 추적하는 것은 거의 불가능에 가깝기 때문에, `depen_killer.py`와 같은 자동화 도구를 사용하는 것이 필수적입니다.
<br>

***

## 사전 준비 사항

스크립트를 실행하기 전에, 사용 중인 운영체제(macOS, Windows, Linux)에 아래 프로그램들이 설치되어 있고, 터미널(명령 프롬프트)에서 바로 실행할 수 있도록 **환경 변수(PATH)**가 설정되어 있어야 합니다.

* **Python 3.x**
* **Java Development Kit (JDK) 8 이상**
* **Apache Maven**

만약 스크립트 실행 시 `Required command not found` 오류가 발생하면, 아래의 OS별 가이드를 참고하여 환경을 설정해주십시오.

<details>
<summary><b>환경 설정 가이드 보기 (macOS/Linux/Windows)</b></summary>

### macOS / Linux (bash/zsh 기준)
1.  **필수 프로그램 설치** (예: Homebrew 사용 시)
    ```bash
    brew install python openjdk maven
    ```
2.  **환경 변수 설정** (`~/.zshrc` 또는 `~/.bash_profile` 등 쉘 설정 파일에 추가)
    ```bash
    # JDK 경로를 자동으로 찾아 설정 (macOS 기준)
    export JAVA_HOME=$(/usr/libexec/java_home)
    # Maven 설치 경로 (Homebrew로 설치 시 보통 자동으로 설정됨)
    # export M2_HOME=/path/to/your/maven
    
    # PATH에 추가
    export PATH=$PATH:$JAVA_HOME/bin #:$M2_HOME/bin
    ```
3.  **설정 적용**
    ```bash
    source ~/.zshrc
    ```

### Windows
1.  **필수 프로그램 설치**:
    *   Python: `python.org` 또는 Microsoft Store에서 설치
    *   JDK, Maven: 다운로드 후 `C:\dev` 같은 경로에 압축 해제
2.  **시스템 환경 변수 편집**:
    *   `시스템 환경 변수 편집` 제어판 실행
    *   `환경 변수` 버튼 클릭
    *   `시스템 변수` 섹션에서 `새로 만들기` 클릭 후 변수 추가:
        *   `JAVA_HOME`: `C:\path\to\your\jdk`
        *   `M2_HOME`: `C:\path\to\your\maven`
    *   `시스템 변수` 목록에서 `Path` 변수를 찾아 `편집` 클릭 후 `새로 만들기`로 아래 두 경로 추가:
        *   `%JAVA_HOME%\bin`
        *   `%M2_HOME%\bin`
3.  **적용**: 열려있는 모든 명령 프롬프트나 PowerShell 창을 닫고 새로 열면 변경사항이 적용됩니다.

</details>
<br>

***

## 실행 가이드

모든 운영체제에서 사용법은 동일합니다. 터미널(macOS/Linux) 또는 명령 프롬프트/PowerShell(Windows)을 열고, `depen_killer.py` 파일이 있는 위치에서 아래 형식으로 명령어를 실행하세요.

**명령어 형식:**

```bash
python depen_killer.py <groupId:artifactId:version> <출력_폴더명> <bp|mk>
```

* **`<groupId:artifactId:version>`**: 다운로드할 메인 라이브러리의 Maven 좌표.
* **`<출력_폴더명>`**: 결과물이 저장될 폴더 이름.
* **`<bp|mk>`**: 생성할 빌드 파일의 종류.
    * `bp`: `Android.bp` 파일 생성 (최신 AOSP 환경 권장)
    * `mk`: `Android.mk` 파일 생성 (레거시 환경 지원)

***

### macOS / Linux 환경 실행 예시

```bash
# androidx.appcompat의 의존성을 다운로드하고 Android.bp 파일을 생성
python depen_killer.py androidx.appcompat:appcompat:1.6.1 androidx_libs bp

# media3-exoplayer의 의존성을 다운로드하고 Android.mk 파일을 생성
python depen_killer.py androidx.media3:media3-exoplayer:1.8.0 media3_libs mk
```


### Windows 환경 실행 예시

```cmd
# androidx.appcompat의 의존성을 다운로드하고 Android.bp 파일을 생성
python depen_killer.py androidx.appcompat:appcompat:1.6.1 androidx_libs bp

# media3-exoplayer의 의존성을 다운로드하고 Android.mk 파일을 생성
python depen_killer.py androidx.media3:media3-exoplayer:1.8.0 media3_libs mk
```

<span style="display:none">[^4][^6][^7][^8][^9]</span>

<div style="text-align: center">⁂</div>

[^1]: https://goddaehee.tistory.com/48

[^2]: https://junshock5.tistory.com/114

[^3]: https://unounou.tistory.com/58

[^4]: https://flowertaekk.tistory.com/22

[^5]: https://zero-zae.tistory.com/242

[^6]: https://mapleliberty.tistory.com/173

[^7]: https://sweeteuna.tistory.com/110

[^8]: https://www.elancer.co.kr/blog/detail/270

[^9]: https://twofootdog.github.io/Spring-Maven-개념-정리/

[^10]: https://mylittlenotepad.tistory.com/67


## 코드 설명 
- def print_log(message):
    - 로그 출력
- def print_error(message):
    - 로그 에러 출력
- def print_setup_guide():
    - 셋팅 가이드 생성 
- def check_prerequisites():
    - 사전 조건 확인
- def create_temp_pom(dependency_gav: str) -> str:
    - pom을 보고 
- def download_dependencies(pom_path: str, output_dir: str):
- def generate_bp_file(source_dir: str, main_dependency_name: str):
- def generate_mk_file(source_dir: str, main_dependency_name: str):
- def main():
