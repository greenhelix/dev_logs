# BIST 
Built In Self Test

MVVM (Model View ViewModel) Architecture 

## 코드 설명 
- src 폴더 구성
View (TestFragment, MainActivity)
Model (Test, Test[aTest, bTest, cTest, ...])
ViewModel (MainViewModel, BaseTestViewModel, *TestViewModel)
Util (LogRepository, TestType, Status)

> - **ViewFragment - ViewModel -  Model 의 관계**
> 
> ViewModel은 UI의 생명주기(Life Cycle)와 분리되어 데이터를 보존하는 중요한 역할을 합니다.
> 
> ViewModelProvider는 이러한 ViewModel 인스턴스를 생성하고 관리해주는 핵심 클래스입니다.


## Test 추가 방법 
MVVM 패턴을 따르기 때문에 특정 규칙만 지키면 간단히 기능을 추가할 수 있다. 

UI는 처리되거나 추가해야하는 것이 있다면 따로 layout폴더에 fragment를 생성하고 작성해야 한다.
- fragment_a_test.xml

A라는 Test를 추가할때 생성해야하는 파일 
- ATest.java 
- ATestViewModel.java
- ATestFragment.java


ATest.java 파일은 핵심적 기능을 넣는 곳이다.
구현하고자 하는 기능을 함수로 구현한뒤 runManualTest 안에서 실행을 시키면된다. 

나머지 ATestViewModel 과 ATestFragment는 LiveData를 추가해야하는경우가 아니라면 똑같은 양식을 그대로 사용하고 
ATest에 링크되는 부분을 변경해주면 된다.

## Wifi SSID 정보 가져오기 
- 권한 처리를 허용을 안해주면 SSID 정보를 가져오지 못한다. 
- WifiManager.getConnectionInfo().getSSID() 메서드는 안드로이드 8.1 (API 27)부터 사용자의 위치 정보 보호를 위해 ACCESS_FINE_LOCATION 권한이 필요하도록 변경되었습니다.