- vscode 에서 수정화면에서 한글입력 안되는 현상 

    [관련 링크](https://gist.github.com/philoskim/a79440bd51ae40f04a4d7cafa472caf1?permalink_comment_id=4764911)

    sudo dpkg -i code_1.40.2-1574694120_amd64.deb
    sudo apt -f install

# Tools

#### CTS setup

- CTS tool 다운로드 : [다운로드 링크](https://source.android.com/docs/compatibility/cts/downloads)

- unzip 구분해서 하기 

    추가적으로 tool을 설치하거나 daily 설치시 복잡해지고 구분이 안됨 
    다른 사람이 같은 pc를 봤을 경우 망가트릴 확률이 높아짐
    unzip android-cts-12_r15-linux_x86-arm.zip -d android-cts-12_r15-linux_x86-arm

>  CTS start 
> **그냥 실행되면 아래와 같은 에러 로그 발생**

```
./cts-tradefed 
Unable to find aapt in path.
```

- English to allow CTS 

    ubuntu 설치 시 english 로 설정하니 따로 신경안써도 될듯

- GNU C Library (glibc) 2.17 or higher

    ```bash
    innopia@innopia:~$ ldd --version
    ldd (Ubuntu GLIBC 2.35-0ubuntu3.9) 2.35
    Copyright (C) 2022 Free Software Foundation, Inc.
    This is free software; see the source for copying conditions.  There is NO
    warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    Written by Roland McGrath and Ulrich Drepper.
    ```

- Install the FFmpeg package version 5.1.3 or higher
    ```bash
    innopia@innopia:~$ sudo apt install ffmpeg
    innopia@innopia:~$ ffmpeg -version
    ffmpeg version 4.4.2-0ubuntu0.22.04.1 Copyright (c) 2000-2021 the FFmpeg developers
    built with gcc 11 (Ubuntu 11.2.0-19ubuntu1)
    configuration: --prefix=/usr --extra-version=0ubuntu0.22.04.1 --toolchain=hardened --libdir=/usr/lib/x86_64-linux-gnu --incdir=/usr/include/x86_64-linux-gnu --arch=amd64 --enable-gpl --disable-stripping --enable-gnutls --enable-ladspa --enable-libaom --enable-libass --enable-libbluray --enable-libbs2b --enable-libcaca --enable-libcdio --enable-libcodec2 --enable-libdav1d --enable-libflite --enable-libfontconfig --enable-libfreetype --enable-libfribidi --enable-libgme --enable-libgsm --enable-libjack --enable-libmp3lame --enable-libmysofa --enable-libopenjpeg --enable-libopenmpt --enable-libopus --enable-libpulse --enable-librabbitmq --enable-librubberband --enable-libshine --enable-libsnappy --enable-libsoxr --enable-libspeex --enable-libsrt --enable-libssh --enable-libtheora --enable-libtwolame --enable-libvidstab --enable-libvorbis --enable-libvpx --enable-libwebp --enable-libx265 --enable-libxml2 --enable-libxvid --enable-libzimg --enable-libzmq --enable-libzvbi --enable-lv2 --enable-omx --enable-openal --enable-opencl --enable-opengl --enable-sdl2 --enable-pocketsphinx --enable-librsvg --enable-libmfx --enable-libdc1394 --enable-libdrm --enable-libiec61883 --enable-chromaprint --enable-frei0r --enable-libx264 --enable-shared
    libavutil      56. 70.100 / 56. 70.100
    libavcodec     58.134.100 / 58.134.100
    libavformat    58. 76.100 / 58. 76.100
    libavdevice    58. 13.100 / 58. 13.100
    libavfilter     7.110.100 /  7.110.100
    libswscale      5.  9.100 /  5.  9.100
    libswresample   3.  9.100 /  3.  9.100
    libpostproc    55.  9.100 / 55.  9.100
    ```  
    gcc 가 걸림....

- Install the most recent versions of Android Debug Bridge (adb) and Android Asset Packaging Tool (AAPT2) and add the location of those tools to the system path of your machine

    ### 따로 각각 설치해도 무관하지만 환경이 계속 변화는 인증 환경 및 경험에 의하면 거지같아진다. 관리하는 툴을 베이스로 설치를 해두면 원할하게 변경이 가능하다. 즉 너의 선택. 지옥으로 가고 싶으면 맘대로 설치

    0. Android 관련 파일 위치를 잡는다. 1번이 나은듯
        ex1) $HOME/Android
        ex2) $HOME/xts/Android

    1. cmdline-tools 다운로드

        cmdline-tools를 설치하면, sdkmanager를 사용할 수 있다. 
        그냥 apt install 같은걸로 하면 개꼬여서 정확하게 관리(manage)가 개똥이된다. 
        무조건 android 세팅은 이 가이드 대로 진행하는게 상책이다. 

        - mv ~/Android
        - https://developer.android.com/studio#command-line-tools-only 여기가서 최신버전 번호가져와서
        - wget https://dl.google.com/android/repository/commandlinetools-linux-{여기에 최신버전입력}_latest.zip
        - 다운받은 cmdline-tools를 latest에 이동시킨다.
        - mkdir ~/Android/cmdline-tools/latest/
        - mv cmdline-tools/* cmdline-tools/latest/

    2. .bashrc에 PATH, ANDROID_HOME 설정 1

        ```bash
        export ANDROID_HOME=$HOME/Android
        export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
        ```

        ```bash
        if [[ ":$PATH:" != *":$ANDROID_HOME/cmdline-tools/latest/bin:"* ]]; then
            export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
        fi

        if [[ ":$PATH:" != *":$ANDROID_HOME/platform-tools:"* ]]; then
            export PATH=$ANDROID_HOME/platform-tools:$PATH
        fi
        ```
        
        ```bash
        innopia@innopia:~$ source ~/.bashrc
        innopia@innopia:~$ sdkmanager --list
        ```
        **JDK 가 안맞는다고 할 수도 있다. 요구하는 버전을 설치해준다.
        ```bash
        sudo apt install openjdk-17-jdk -y
        ```
        sdkmanager 가 잘 뜨면 이제 최신 버전 좀 확인하고 sdkmanager를 통해 나머지 툴을 다 설치해야 한다. 그래야 관리가 됨.

    3. platform-tools, build-tools 다운로드
        
        ```bash
        sdkmanager "platform-tools" "build-tools;36.0.0" "platforms;android-36"
        ```
        엄청 많은 안내사항이 떴던걸로 기억한다. 그냥 y눌러준다.

    4. .bashrc에 PATH 설정 2
        ```bash
        export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/{버전(36.0.0)}:$PATH
        ```    
    5. check 

        ```bash 
        sudo apt update 
        which adb
        which aapt2
        which sdkmanager
        ```
        ```bash
        innopia@innopia:~/Android$ sdkmanager --list 
        [=======================================] 100% Computing updates...             
        Installed packages:
        Path                 | Version | Description                | Location            
        -------              | ------- | -------                    | -------             
        build-tools;36.0.0   | 36.0.0  | Android SDK Build-Tools 36 | build-tools/36.0.0  
        platform-tools       | 35.0.2  | Android SDK Platform-Tools | platform-tools      
        platforms;android-36 | 1       | Android SDK Platform 36    | platforms/android-36

        Available Packages:
        Path                                                                            | Version           | Description                                                           
        -------                                                                         | -------           | -------                                                               
        add-ons;addon-google_apis-google-15                                             | 3                 | Google APIs                                                           
        add-ons;addon-google_apis-google-16                                             | 4                 | Google APIs                                                           
        add-ons;addon-google_apis-google-17                                             | 4                 | Google APIs                                                           
        add-ons;addon-google_apis-google-18                                             | 4                 | Google APIs                                                           
        add-ons;addon-google_apis-google-19                                             | 20                | Google APIs                                                           
        add-ons;addon-google_apis-google-21                                             | 1                 | Google APIs                                                           
        add-ons;addon-google_apis-google-22                                             | 1                 | Google APIs                                                           
        add-ons;addon-google_apis-google-23                                             | 1                 | Google APIs                                                           
        add-ons;addon-google_apis-google-24                                             | 1                 | Google APIs                                                           
        build-tools;19.1.0                                                              | 19.1.0            | Android SDK Build-Tools 19.1                                          
        build-tools;20.0.0                                                              | 20.0.0            | Android SDK Build-Tool
        ```

#### sudo apt install android-sdk 하면 
/usr/lib/android-sdk 경로에 자동으로 설치됨
sdkmanager ? 이딴걸로 감지 안됨.

>  CTS start 

```bash
innopia@innopia:~/xts/cts/android-cts-12_r15-linux_x86-arm/android-cts/tools$ ./cts-tradefed 
Wrong java version. 1.8, 9, or 11 is required.
sudo update-alternatives --config java
There are 2 choices for the alternative java (providing /usr/bin/java).

  Selection    Path                                         Priority   Status
------------------------------------------------------------
* 0            /usr/lib/jvm/java-17-openjdk-amd64/bin/java   1711      auto mode
  1            /usr/lib/jvm/java-11-openjdk-amd64/bin/java   1111      manual mode
  2            /usr/lib/jvm/java-17-openjdk-amd64/bin/java   1711      manual mode

Press <enter> to keep the current choice[*], or type selection number: 1
```

java 버전을 11로 변경하면 Sdkmanager가 안됨
```bash
sdkmanager --list 
This tool requires JDK 17 or later. Your version was detected as 11.0.26.
To override this check, set SKIP_JDK_VERSION_CHECK.
```

> **일단 실행은 됨**

- Install the proper version of Java Development Kit (JDK)

    For Android 11 or higher, install JDK 11.
    ```bash
    innopia@innopia:~/xts/cts/android-cts-12_r15-linux_x86-arm/android-cts/tools$ ./cts-tradefed 
    Wrong java version. 1.8, 9, or 11 is required.
    sudo update-alternatives --config java
    There are 2 choices for the alternative java (providing /usr/bin/java).

    Selection    Path                                         Priority   Status
    ------------------------------------------------------------
    * 0            /usr/lib/jvm/java-17-openjdk-amd64/bin/java   1711      auto mode
    1            /usr/lib/jvm/java-11-openjdk-amd64/bin/java   1111      manual mode
    2            /usr/lib/jvm/java-17-openjdk-amd64/bin/java   1711      manual mode

    Press <enter> to keep the current choice[*], or type selection number: 1
    ```

- To ensure Python is installed, type python3. The Python version and date should appear indicating python is properly installed.

[python versions](https://devguide.python.org/versions/#versions)

우분투 22.04에는 python3.12 버전이 repo에 없다. 
빠르게 끝나기도 하고 꽤 걸리기도하는데 암튼 다 받아한다.
```bash
sudo add-apt-repository ppa:deadsnakes/ppa
sudo apt update
sudo apt install python3.12 -y
sudo apt install python3.12-venv -y 
sudo apt install python3.12-distutils -y #더이상 지원안함
sudo apt install curl
url -sS https://bootstrap.pypa.io/get-pip.py | python3.12
python3.12 -m pip install setuptools # distutils 대체툴
python3.12 -m pip install --user pipx
python3.12 -m pipx ensurepath
source ~/.bashrc
pipx --version
pipx install virtualenv
```

[python deadsnakes git](https://github.com/deadsnakes)

- (optional) For Android 13 or higher, install virtualenv.
The virtualenv tool is required for multidevice tests.







