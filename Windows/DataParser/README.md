# Data Parser

## Setting

> 1. import pandas as pd
> ModuleNotFoundError: No module named 'pandas'
> pip install pandas
>
> 2. ImportError: Missing optional dependency 'tabulate'.  Use pip or conda to install tabulate.
> pip install tabulate



## Usage 

> 1. Fail 항목들을 그대로 복사해서 구글 스프레드 시트에 복붙을 한다. 
> 2. 구글 스프레드 시트에서 해당 실패항목만 있는 내용을 다운로드 > tsv 파일로 내보낸다. 
> 3. 내보낸 tsv 파일을 저장하고, Data_Parser.py 를 실행 시킨다. 
> 4. 프로그램을 실행시키면 파일 선택 화면이 뜨고 tsv 파일을 선택한다. 
> 5. 정상작동하면 예시화면과 파싱 상태를 보여주는 화면이 뜨고, tsv, html 선택 커맨드가 뜬다. 
> 6. 1, 2 번 선택을 통해 최종 파싱 정보를 받아온다. 
> 7. 파싱된 데이터를 복사해서 레드마인에 복 붙 하면 된다. 

#### 예시 

tsv 파일 형태 
```
armeabi-v7a CtsAppExitTestCases[instant]		
Test	Result	Details
android.app.cts.ActivityManagerAppExitInfoTest#testLmkdKill	fail	java.lang.AssertionError
armeabi-v7a CtsHdmiCecHostTestCases		
Test	Result	Details
android.hdmicec.cts.common.HdmiCecSystemStandbyTest#cect_HandleBroadcastStandby	fail	com.android.tradefed.device.DeviceNotAvailableException[DEVICE_UNAVAILABLE|520750|LOST_SYSTEM_UNDER_TEST]: Could not find device 92210000003S70000005
android.hdmicec.cts.playback.HdmiCecDeviceOsdNameTest#cect_11_2_11_1a_GiveOsdNameTest	fail	value of: trim()
android.hdmicec.cts.playback.HdmiCecDeviceOsdNameTest#cectGiveOsdNameTestInStandby	fail	expected: MagentaTV Stic
armeabi-v7a CtsHostsideNetworkTests[instant]		
Test	Result	Details
com.android.cts.net.HostsideRestrictBackgroundNetworkTests#testMeteredNetworkAccess_expeditedJob	fail	java.lang.AssertionError: on-device tests failed:
com.android.cts.net.HostsideRestrictBackgroundNetworkTests#testNonMeteredNetworkAccess_expeditedJob	fail	java.lang.AssertionError: on-device tests failed:
```

