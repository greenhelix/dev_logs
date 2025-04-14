Flutter에 사용되는 위젯의 범위이다. 


| 카테고리              | 설명                                                | 대표 위젯 예시                                |
|------------------------|---------------------------------------------------|------------------------------------------------|
| Structural Widgets      | 앱의 구조와 레이아웃을 정의함                            | `Scaffold`, `AppBar`, `MaterialApp`, `CupertinoApp` |
| Layout Widgets          | 자식 위젯의 위치, 크기, 정렬 등을 조절함                 | `Row`, `Column`, `Stack`, `Expanded`, `Container`, `Padding`, `Align` |
| Input Widgets           | 사용자로부터 입력을 받는 위젯들                         | `TextField`, `Checkbox`, `Radio`, `Switch`, `Slider` |
| Button Widgets          | 클릭, 탭과 같은 액션을 제공함                           | `ElevatedButton`, `TextButton`, `IconButton`, `FloatingActionButton` |
| Display Widgets         | 데이터를 화면에 출력하거나 단순 시각 요소를 제공         | `Text`, `Icon`, `Image`, `Card`, `Chip`, `Tooltip` |
| Animation & Motion      | 애니메이션 효과를 부여하는 위젯들                       | `AnimatedContainer`, `Hero`, `AnimatedOpacity`, `FadeTransition` |
| Interaction Widgets     | 제스처나 상태 변경 등을 감지하여 동작                    | `GestureDetector`, `Dismissible`, `Draggable`, `InkWell` |
| Async Widgets           | 비동기 데이터를 다루며 UI 상태를 업데이트                | `FutureBuilder`, `StreamBuilder` |
| Navigation & Routing    | 화면 전환과 라우팅 처리를 담당                          | `Navigator`, `PageView`, `BottomNavigationBar`, `Drawer` |
| Styling & Theming       | 앱 전체의 스타일링을 적용하거나 테마를 설정               | `Theme`, `MediaQuery`, `DefaultTextStyle`, `Directionality` |

✅ StatefulWidget 기반 – 실무에서 자주 사용하는 위젯

| 위젯 이름              | 설명                                                    |
|------------------------|---------------------------------------------------------|
| TextField              | 사용자 입력 받는 위젯 (텍스트 입력 상태 유지 필요)      |
| Checkbox / Switch / Radio | 사용자 선택 상태를 기억해야 할 때                   |
| Form / FormField       | 입력값 검증 및 상태 관리가 필요한 폼 UI 구성            |
| AnimatedContainer      | 애니메이션 포함된 레이아웃 변화 처리                   |
| BottomNavigationBar    | 선택된 탭 상태에 따라 UI가 달라질 때                   |
| PageView               | 여러 페이지를 넘기는 UI, 인덱스 상태 유지 필요          |
| Slider                 | 숫자나 비율 조절 등 슬라이드 UI                         |
| FutureBuilder          | 비동기 작업 상태에 따라 UI 변경                         |
| StreamBuilder          | 실시간 데이터 상태를 관찰하며 UI 구성                   |
| ExpansionTile          | 접고 펼치는 UI로, 열림 상태 유지 필요                   |
| Dismissible            | 리스트 스와이프 삭제 기능 (삭제 상태 반영 필요)         |


✅ StatelessWidget 기반 – 실무에서 자주 사용하는 위젯

| 위젯 이름              | 설명                                                   |
|------------------------|--------------------------------------------------------|
| Text                   | 텍스트 출력용 기본 위젯                               |
| Icon                   | Material 아이콘 출력                                   |
| Image, Image.asset     | 이미지 출력 (네트워크/에셋)                            |
| ElevatedButton         | 상태 없이 클릭만 처리되는 버튼                         |
| AppBar                 | 상단 앱바 구조 (타이틀, 액션 포함)                     |
| Scaffold               | 기본 레이아웃 구조 (AppBar, Body 등)                   |
| Container              | 배경, 패딩, 마진 등 다양한 스타일 처리용 박스         |
| Row / Column           | 위젯들을 가로/세로 방향으로 배치                       |
| Padding / SizedBox     | 여백이나 크기 조절을 위한 UI 구성                     |
| ListTile               | 리스트 형태 UI에서 자주 쓰이는 항목 단위 위젯         |
| Divider                | 항목 간 구분선                                         |


기본적으로는 StatelessWidget부터 시작 → 필요할 때만 Stateful로 전환
복잡한 상태는 Provider, Riverpod, Bloc 등의 상태 관리 도구 사용
성능과 유지보수를 위해 최대한 Stateless하게 구조화하는 게 좋음


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