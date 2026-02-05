# 📅 Development Log

## [2026-01-16] 초기 아키텍처 및 기술 스택 선정

### 📝 요약
Flutter 기반의 데이터 축적 앱(인물, 뉴스, 지도) 개발을 위한 기술 스택 및 프로젝트 구조를 확정함. 크로스 플랫폼 지원과 오프라인 데이터 동기화를 핵심 목표로 설정.

### 🚀 결정 사항 (Decisions)
1.  **Architecture**: `Riverpod` (Code Generation 방식 사용)
    *   선정 이유: 비동기 데이터 처리의 용이성과 컴파일 타임 안전성 확보.
2.  **Project Structure**: `Feature-first` (기능 중심) 구조 채택
    *   `lib/features/{feature_name}/data|presentation` 형태로 모듈화.
3.  **Local Database**: `Drift` (SQLite Abstraction)
    *   선정 이유: 지도 및 뉴스 데이터의 복잡한 쿼리(범위 검색, 정렬) 처리에 유리.
4.  **Backend**: `Firebase` (Firestore, Hosting, Storage)
    *   하이브리드 운영: 온라인 시 Cloud 동기화, 오프라인 시 로컬 DB 사용.

### 📂 TODO
- [ ] Flutter 프로젝트 생성 및 폴더 구조 세팅 (core, features, data)
- [ ] Riverpod 및 Drift 패키지 의존성 추가
- [ ] Firebase 프로젝트 생성 및 연동 (Web/Android/iOS)
- [ ] .gitignore 설정 파일 적용

### 파일구조
```
lib/
├── main.dart                  # 앱 진입점
├── app.dart                   # 앱 전역 설정 (Theme, Router)
├── core/                      # 모든 기능에서 공통으로 쓰는 유틸리티
│   ├── constants/             # 상수 (API Key 등)
│   ├── theme/                 # 앱 디자인 테마
│   └── utils/                 # 날짜 포맷터, 로거 등
├── data/                      # 전역 데이터 소스 (DB 설정 등)
│   ├── local/                 # Drift/Hive 설정 파일
│   └── remote/                # Firebase 설정 관련
├── features/                  # 기능별 모듈 (가장 중요)
│   ├── map/                   # [지도 기능]
│   │   ├── data/              # 지도의 Repository, Model, API
│   │   ├── domain/            # 지도의 비즈니스 로직 (Entity)
│   │   └── presentation/      # 지도 UI (Screen, Widget), Provider
│   ├── person_wiki/           # [인물 위키 기능]
│   │   ├── data/
│   │   └── presentation/
│   └── news/                  # [뉴스 기능]
│       ├── data/
│       └── presentation/
└── shared/                    # 여러 기능에서 공유하는 위젯 (예: 커스텀 버튼)
    └── widgets/
```


---------------

## [2026-01-16 16:55] 프로젝트 초기 설정 (Dependency Injection)
### 📝 요약
Feature-first 아키텍처 구현을 위한 필수 패키지들을 pubspec.yaml에 정의함. 상태 관리(Riverpod), 로컬 DB(Drift), 원격 DB(Firebase), 라우팅(GoRouter), 그리고 코드 생성기(Build Runner) 관련 의존성 추가.

## [2026-01-16 16:59] 앱 진입점(Main) 구현
### 📝 요약
Flutter 엔진과 Firebase를 초기화하고, Riverpod의 상태 관리 범위를 지정하는 ProviderScope를 설정함. 비동기 초기화 처리를 위해 main 함수를 async로 전환.

## [2026-01-16 17:11] 앱 전역 설정 및 라우터 구현
### 📝 요약
MaterialApp을 정의하고, 화면 이동(Navigation)을 관리할 GoRouter 설정을 준비함. 아직 개별 기능 화면(Feature Screen)이 없으므로 임시 홈 화면(Placeholder)을 연결하여 앱이 실행 가능한 상태로 만듦.

## [2026-01-16 17:36] 로컬 데이터베이스(Drift) 스키마 설계
### 📝 요약
drift 패키지를 사용하여 SQLite 데이터베이스 스키마를 정의함. 인물 정보(JSON 확장 필드 포함), 뉴스 아카이브, 지도 로그 테이블을 생성하고, 모바일/데스크톱 환경에서 DB 파일(db.sqlite)을 생성하는 연결 로직을 구현함.


## [2026-01-16 18:12] 데이터베이스 전역 Provider 설정
### 📝 요약
AppDatabase 인스턴스를 Riverpod Provider로 등록하여, 앱 어디서든 ref.read(databaseProvider)로 DB에 접근할 수 있도록 함. 추후 Firebase와의 동기화 로직을 위한 Repository 패턴의 기반을 마련함.

### 🚀 진행 상황 (Progress)
 Step 1: 프로젝트 의존성 설정 (pubspec.yaml)
 > `pubspec.yaml` 작성 후 터미널에서 `flutter pub get` 명령어를 실행하여 패키지를 다운로드하세요.

 Step 2: 앱 진입점 설정 (main.dart)
 > flutter 엔진 위젯바인딩 초기화 및 firebase초기화를 해준다. 
 > 앱을 실행하기전에 해줘야하는 단계를 여기서 진행하는것으로 보인다. 
 > 그리고 app.dart에 정의된 class DataCollector를 호출하여 앱을 실행시킨다.
 > 앱을 실행시킬때, riverpod으로 감싸서 상태관리를 잡아주고 실행시킨다.

 Step 3: 전역 앱 설정 (app.dart)
 > 라우터를 생성하고 라우터를 통해서 기능별 경로를 설정해준다. 
 > 이 라우터는 build에 따로 호출된다. routerConfig를 통해서 연결 해준다.

 Step 4: 로컬 DB 설정 - Drift (lib/data/local/app_database.dart)
 > 이 파일 하나에 테이블 정의와 데이터베이스 연결 설정을 모두 담았습니다.
 > JSON 지원: People 테이블의 attributes 필드는 JsonConverter를 통해 Map<String, dynamic> 형태로 자유롭게 저장됩니다. (스키마 변경 없이 필드 추가 가능)
 > 호환성: 안드로이드, iOS, macOS, Windows, Linux를 지원하는 NativeDatabase 방식을 사용합니다.

 Step 5: 데이터베이스 코드 생성 (build_runner)
 > 'part 'app_database.g.dart'
 > 한번 돌려준다. 이상없으면 .g. 파일이 생성된다. 
 > 'dart run build_runner build --delete-conflicting-ouputs'
 
```log
dart run build_runner build --delete-conflicting-out
puts
Building package executable... (1.8s)
Built build_runner:build_runner.
4s riverpod_generator on 3 inputs: 3 no-op; spent 2s sdk, 1s analyzing                      
0s freezed on 3 inputs: 3 no-op                                                             
4s json_serializable on 6 inputs: 3 skipped, 3 no-op; spent 3s analyzing                    
0s drift_dev on 24 inputs: 14 skipped, 7 output, 3 no-op                                    
0s source_gen:combining_builder on 12 inputs: 8 skipped, 1 output, 3 no-op                       
Built with build_runner in 8s; wrote 9 outputs. 
```

 Step 6: 기능 구현 - 인물 위키 (lib/features/person/...)
 > 


-------

## 💡 Provider와 Riverpod의 관계
1. "Riverpod" (리버팟)

**Riverpod은 Provider라는 단어의 철자 순서만 바꾼(Anagram) 이름입니다.**

만든 사람(Remi Rousselet)이 똑같습니다. 즉, **"Provider 패키지의 단점을 완전히 해결한 업그레이드 버전(2.0)"**이라고 보시면 됩니다.

2. 왜 이름이 Provider인가요?

Riverpod 안에서 데이터를 전해주는 "배급소" 역할을 하는 객체들을 통칭해서 Provider라고 부릅니다.

우리가 방금 작성한 Provider<AppDatabase>는 **"데이터베이스라는 물건을 배급해주는 배급소"**를 하나 만든 것입니다.

3. 작성하신 코드의 의미

```dart
// "데이터베이스 배급소(Provider)를 하나 만듭니다."
// 이 배급소는 'AppDatabase'라는 물건을 줍니다.
final databaseProvider = Provider<AppDatabase>((ref) {
  // 배급소가 처음 문을 열 때, 데이터베이스를 하나 생성합니다.
  return AppDatabase();
});
```

이제 앱 어디서든 ref.read(databaseProvider)라고 외치면, 이 배급소에서 만들어둔 데이터베이스 리모컨을 건네줍니다. 이것을 **의존성 주입(Dependency Injection)**이라고 합니다.


```
flutter clean
flutter pub get
dart run build_runner build --delete-conflicting-outputs
```
delete-conflicting-outputs: 기존에 잘못 생성된 파일(app_database.g.dart)을 강제로 지우고 다시 만듭니다.


## 📅 Development Log (중간 점검)
[2026-01-22] 1단계: 프로젝트 기본 구조 및 인물 위키 완성
✅ 완료된 작업 (Accomplished)
프로젝트 아키텍처 수립

Architecture: Feature-first (기능 단위 폴더 구조)

State Management: Riverpod (Provider)

Database: Drift (SQLite) - 플랫폼별(Web/Native) 연결 분리 구현 완료

Router: GoRouter (화면 이동 관리)

핵심 인프라 구축

lib/data/local/: Drift DB 스키마 정의 (Person, News, Map)

lib/data/local/connection/: Web과 Native 환경을 구분하는 DB 연결 로직 분리

기능 1: 인물 위키 (Person Wiki)

Repository: PersonRepository (DB CRUD 작업)

UI: PersonListScreen (인물 목록 조회, 추가, Null Safety 처리 완료)

Routing: 홈 화면 -> 인물 위키 화면 연결

🚧 현재 상태 (Status)
빌드 환경: macOS (성공 ✅), Chrome (설정 필요 ⚠️)

데이터: 인물 정보 저장 및 조회가 로컬 DB(SQLite)에서 정상 작동함.

🚀 다음 단계: 기능 2 - 뉴스 아카이브 (News Archive)
이제 두 번째 핵심 기능인 뉴스 저장소를 만듭니다. 이 기능의 핵심 도전 과제는 다음과 같습니다.

이미지 저장: 뉴스에는 사진이 들어갑니다. (DB에 직접 넣을지, 파일로 저장할지 결정 필요)

날짜 처리: DateTime을 다루고, 최신순으로 정렬해야 합니다.

📂 계획 (Plan)
NewsRepository 구현: 뉴스 데이터 저장/조회 로직

NewsListScreen 구현: 뉴스 목록 (타임라인 형태) UI

NewsAddScreen 구현: 제목, 내용, 사진 입력 화면

## 📅 Development Log
[2026-02-05] 기능 1: 인물 위키(Person Wiki) 개발 완료
📝 요약
첫 번째 핵심 모듈인 '인물 위키' 개발을 완료함. Drift DB의 JSON 필드를 활용한 유연한 속성 저장 구조를 UI까지 연결하였으며, 목록 -> 상세 화면으로 이어지는 네비게이션 흐름을 구축함.

✅ 완료된 작업 (Accomplished)
Repository Layer:

PersonRepository: 이름, 나이, 이미지 URL, 동적 속성(Map) 저장 로직 구현.

Presentation Layer (UI):

PersonListScreen: 인물 목록 조회 및 확장된 입력 다이얼로그(사진/속성 추가) 구현.

PersonDetailScreen: 선택한 인물의 상세 정보와 **Wiki Data(JSON 속성)**를 시각화하여 표시.

Navigation: GoRouter를 활용한 객체 전달(extra) 방식 적용.

Troubleshooting:

Drift 생성 클래스(Person vs PeopleData) 이름 불일치 해결.

Web/Native DB 연결 분리(connection_web.dart 등) 적용.

