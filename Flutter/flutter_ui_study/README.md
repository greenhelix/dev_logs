# flutter_ui_study

1. [테스트 환경 구성](#테스트 환경 구성)
2. [datatable](#data-table--data-grid)

[참고](#reference)


## 테스트 환경 구성

1. 기본 구현 템플릿

  일단 이 코드를 상단에 박아 버린다. 복사해서 넣기 
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

2. 필요한 dependency 추가하기

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
  
## Data Table & Data Grid

- 중요 하게 구성 해야 할 것들
  - DataGridCell 을 통해 DataGridRow 가 형성 된다. ([table4.dart.buildDataGridRow참고](lib/tables/table4.dart:131))
    - 여기서 중요한 것은  data 를 map 을 통해 한번 형변환 해주구, 다시 toList 를 해준다. 마지막 toList... 킹받네

## Reference

Implicit Animation 암시적 animation
interpolation 보간: start-end 사이의 값들을 보간.
https://beomseok95.tistory.com/320

Button
https://docs.flutter.dev/release/breaking-changes/buttons

