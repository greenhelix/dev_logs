# flutter_ui_study

Flutter UI Study Project
Implicit Animation 암시적 animation 
interpolation 보간: start-end 사이의 값들을 보간. 
https://beomseok95.tistory.com/320

Button
https://docs.flutter.dev/release/breaking-changes/buttons

## 테스트 환경 구성
일단 이 코드를 상단에 박아 버린다. 
```dart
import 'package:flutter/material.dart';

void main() {
  runApp(MaterialApp(
      title: '뭐라뭐라쓰고싶은제목',
      theme: ThemeData(
        primarySwatch: Colors.green,  //색상 바꿔도 됨
      ),
      home: const AAA())); //여기에 구현한 함수를 추가 해서 띄우기
}

class AAA extends StatefulWidget {
  const  AAA({Key? key}) : super(key: key);

  @override
  _AAAState createState() => _AAAState();
}

class _AAAState extends State<AAA> 
{
  // blah blah
}
```
이런 식으로 _AAAState 이하 부터 구현된 코드를 가져 다가 테스트 해 보거나 직접 만들 어서 넣어 보는게 좋다. 


## Data Table & Data Grid
For Using SfDataGrid 라는 것을 사용해 보는 예제 이다. 
**pubspec.yaml** 이 파일에 아래 내용을 추가 해야 한다.
```yaml
dependencies:
flutter:
sdk: flutter
syncfusion_flutter_datagrid: ^29.1.35
```
^29...이 부분이 버전 인데 버전 정보는 https://pub.dev/packages/syncfusion_flutter_datagrid/versions 여기서 확인 가능
이렇게 입력 하고 저장 하고 아래의 명령어 bash 에서 시작
>> flutter pub get 
이 명령을 해줘야 다운을 받는다. 뭔가 받고  
>> import 'package:syncfusion_flutter_datagrid/datagrid.dart';
코드로 돌아가 보면 안정적 이게 잘 import 쓰니 잘된다.
https://pub.dev/documentation/syncfusion_flutter_datagrid/latest/datagrid/SfDataGrid-class.html
https://help.syncfusion.com/flutter/datagrid/getting-started

**test-description 을 포함 하는 테이블 생성해 봄**
status 를 drop down button 으로 구현 하고 description 부분은 alert 창으로 띄어서 자세히 볼수 있게 했다. 

### 중요 하게 구성 해야 할 것들
- DataGridCell 을 통해 DataGridRow 가 형성 된다. ([table4.dart.buildDataGridRow참고](lib/tables/table4.dart:131))
  - 여기서 중요한 것은  data 를 map 을 통해 한번 형변환 해주구, 다시 toList 를 해준다. 마지막 toList... 킹받네
- 


## State
Flutter에서 State는 **화면(UI)**의 상태를 관리하는 객체입니다. 
즉, 화면에 그려지는 내용을 바꾸고 싶을 때 상태(state)를 바꾸고,
이를 반영해 다시 build()를 호출해 UI를 새로 그리게 돼요.

- initState()
  - 한번만 호출됨.
  - API 호출, 초기 데이터 로딩, AnimationController 설정 등 초기화 작업에 사용돼요.

- build()
  - 위젯을 화면에 그리는 역할을 합니다.
  - setState() 호출 시마다 build()가 다시 실행돼 UI가 새로 그려집니다.

### StatefulWidget 
생명주기
createState()	위젯이 처음 생성될 때 상태 객체를 생성
initState()	위젯이 위젯 트리에 삽입될 때 한 번만 호출됨
didChangeDependencies()	위젯의 종속 객체가 변경됐을 때 호출됨
build()	UI를 그릴 때마다 호출
setState()	상태를 변경하고 build()를 다시 실행
deactivate()	위젯이 트리에서 제거될 때 호출
dispose()	위젯이 완전히 사라지기 전에 리소스를 정리할 때 사용 (예: 컨트롤러 해제 등)

| Android Activity 생명주기 | Flutter StatefulWidget 생명주기 | 설명                                     |
|---------------------------|----------------------------------|------------------------------------------|
| `onCreate()`              | `initState()`                    | 초기 설정, 데이터 로딩 등 1회 초기화 작업 |
| `onStart()` / `onResume()`| `build()`                        | UI를 그릴 때마다 호출됨                  |
| `onPause()` / `onStop()`  | `deactivate()`                   | 위젯이 트리에서 제거될 때 호출           |
| `onDestroy()`             | `dispose()`                      | 리소스를 해제할 때 호출 (ex. controller) |
| `setContentView()`        | `build()`                        | UI를 그리는 함수                         |

