# flutter_ui_study

Flutter UI Study Project

AnimatedAlignScreen

Implicit Animation 암시적 animation 
interpolation 보간: start-end 사이의 값들을 보간. 
https://beomseok95.tistory.com/320


Button
https://docs.flutter.dev/release/breaking-changes/buttons


## Data Table & Data Grid
For Using SfDataGrid 라는 것을 사용해보는 예제이다. 
**pubspec.yaml** 이 파일에 아래 내용을 추가해야한다.
```yaml
dependencies:
flutter:
sdk: flutter
syncfusion_flutter_datagrid: ^29.1.35
```
^29...이부분이 버전인데 버전 정보는 https://pub.dev/packages/syncfusion_flutter_datagrid/versions 여기서 확인가능
이렇게 입력하고 저장하고 아래의 명령어를 bash에서 시작
>> flutter pub get 
이 명령어를 해줘야 다운을 받는다. 뭔가 받고  
>> import 'package:syncfusion_flutter_datagrid/datagrid.dart';
코드로 돌아가보면 안정적이게 잘 import쓰니 잘된다.
https://pub.dev/documentation/syncfusion_flutter_datagrid/latest/datagrid/SfDataGrid-class.html
https://help.syncfusion.com/flutter/datagrid/getting-started
