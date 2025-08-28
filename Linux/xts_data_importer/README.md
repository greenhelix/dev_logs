# xts_data_importer
```
xts 데이터를 모은다.
```


## 아키텍처 설명
### 계층형 아키텍처 (Layered Architecture)

**계층형 아키텍처+클린 아키텍처(Clean Architecture)** 와 MVVM(Model-View-ViewModel) 패턴의 핵심 원칙들이 결합된 현대적인 Flutter 애플리케이션 구조를 사용했다. 

- 1. Presentation Layer (UI 계층)
	
	파일: home_page.dart, data_view_page.dart, add_data_dialog.dart

	역할: 사용자에게 보여지는 화면(UI)과 사용자 입력을 처리하는 것을 담당한다.

	특징:

	- ConsumerWidget 또는 ConsumerStatefulWidget을 사용하여 만들었다.

	- ref.watch()를 통해 Provider로부터 상태(데이터)를 "구독"하고, 상태가 변경되면 자동으로 UI를 다시 그린다.

	- ref.read()를 통해 Provider의 함수를 "호출"하여 사용자 액션(버튼 클릭 등)을 처리한다.

	- 이 계층은 비즈니스 로직을 전혀 모릅니다. 단지 데이터를 표시하고, 이벤트가 발생하면 상위 계층에 알리는 역할만 수행한다.

- 2. Application / Domain Layer (비즈니스 로직 계층)
	
	파일: providers.dart, csv_import_service.dart

	역할: 앱의 핵심 비즈니스 로직과 상태 관리를 담당한다.

	특징:

	- providers.dart: Riverpod의 Provider들을 정의하는 중앙 허브이다. 어떤 데이터가 있고, 그 데이터를 어떻게 가져오고, 어떻게 가공하는지에 대한 모든 '규칙'이 여기에 정의된다. UI 계층과 데이터 계층을 연결하는 다리 역할을 한다.

	- csv_import_service.dart: CSV 파일을 파싱하고 DB에 저장하는 것과 같은 순수한 비즈니스 로직을 수행하는 클래스이다. 이 클래스는 Flutter UI에 대해 전혀 알지 못하므로, 매우 독립적이고 테스트하기 쉽다.

- 3. Data Layer (데이터 계층)

	파일: database.dart, database_repository.dart

	역할: 데이터의 출처(Source)를 관리하고, 데이터에 접근하는 방법을 추상화한다.

	특징:

	- database.dart: Drift를 사용하여 데이터베이스의 스키마를 정의하고 실제 DB와의 연결을 담당하는 저수준(low-level) 코드이다.

	- database_repository.dart: **레포지토리 패턴(Repository Pattern)**을 구현한 부분이다. 이 클래스는 데이터 소스(지금은 Drift DB)에 대한 접근을 캡슐화하고, 상위 계층에는 "어떻게" 데이터를 가져오는지는 숨긴 채 "무엇을" 할 수 있는지(예: getAllResults(), getCategories())에 대한 간단한 API만 제공한다.

-------

1. 관심사의 분리 (Separation of Concerns, SoC)

	이전 코드에서는 UI, 상태 관리, 데이터 로직이 하나의 위젯에 뒤섞여 있었지만, 새 아키텍처에서는 각 계층이 명확하게 분리되어 하나의 책임만 가진다. 이로 인해 코드를 이해하고, 수정하고, 디버깅하기가 훨씬 쉬워진다.

2. 의존성 주입 (Dependency Injection, DI)
Riverpod의 역할: Riverpod는 단순한 상태 관리를 넘어, 강력한 의존성 주입 프레임워크 역할을 한다.

	예시: CsvImportService는 DatabaseRepository가 필요하고, DatabaseRepository는 AppDatabase가 필요합니다. 이전 코드에서는 각 클래스가 필요한 다른 클래스를 직접 생성(final db = AppDatabase())했다. 하지만 새 구조에서는 ref.watch(...) 또는 ref.read(...)를 통해 Riverpod가 필요한 의존성을 "주입"해 준다.

	장점: 클래스 간의 결합도(Coupling)가 낮아져 각 부분을 독립적으로 개발하고 테스트할 수 있다.

3. 단방향 데이터 흐름 (Unidirectional Data Flow)

	상태 변화가 항상 한 방향으로 흐른다.

	UI 이벤트 발생 (버튼 클릭) → 비즈니스 로직 호출 (ref.read) → 데이터 상태 변경 (DB 업데이트) → 변경된 상태가 UI로 전파 (ref.watch) → UI 업데이트

	이러한 예측 가능한 데이터 흐름은 setState를 여기저기서 호출하는 것보다 상태를 추적하고 관리하기 훨씬 쉽게 만든다.


## DB
### 데이터의 형태

- Test Result Data

  - csv 파일 파싱을 한다.
  - 테이블 헤더(컬럼) 구성을 기반으로 파싱한다.
  ```csv
  Test_Date,Module,Test,result,Detail,description,FW_ver,Test_Tool_ver,Security_Patch,SDK_ver,ABI
  ```
  - 데이터 테이블 기본 헤더구성에 맞춰 데이터를 모은다.
  |test_date|module|test_name|result|detail|description|fw_version|test_tool_ver|security_patch|sdk_version|abi|
  |:--------|:-----|:--------|:-----|:-----|:----------|:---------|:------------|:-------------|:----------|:--|






### 사용한 도구


$FLUTTER_DIR = "D:\Android\Flutter" 
New-Item -ItemType Directory -Force -Path $FLUTTER_DIR | Out-Null