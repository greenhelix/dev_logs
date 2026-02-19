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

## 📅 Development Log (중간 점검)
[2026-02-05] 기능 2: 뉴스 아카이브(News Archive) 개발 완료
📝 요약
두 번째 핵심 기능인 '뉴스 아카이브'를 완성함. Drift DB의 timestamp 정렬 기능을 활용하여 뉴스 피드(Feed) 형태의 UI를 구현하였으며, 목록 -> 상세 화면 흐름을 통해 이미지와 장문 텍스트를 효과적으로 보여주도록 함.

✅ 완료된 작업 (Accomplished)
1. 인프라 및 공통 (Infrastructure)
플랫폼 호환성: Windows/macOS/Web(Chrome) 빌드 환경 설정 완료 (Web은 Fake DB로 우회).

DB 스키마: NewsLogs 테이블 (Title, Content, Timestamp, ImageUrl) 정의 및 마이그레이션.

2. 기능 1: 인물 위키 (Person Wiki)
JSON 필드를 활용한 동적 속성(Wiki Data) 저장 시스템 구축.

목록/추가/상세 화면 UI 및 로직 구현 완료.

3. 기능 2: 뉴스 아카이브 (News Archive)
Repository: NewsRepository (최신순 정렬 조회 orderBy).

UI:

NewsListScreen: 타임라인 스타일의 카드 리스트 뷰.

NewsAddScreen: 제목, 내용, 이미지 URL 입력 및 유효성 검사.

NewsDetailScreen: 전체 내용 및 대형 이미지 보기 지원.

Routing: /news, /news/add, /news/detail 경로 연결 완료.

🗺️ 전체 진행 단계 (Roadmap Check)
총 11단계 중 10단계까지 완료했습니다!

단계	작업 내용	상태
Step 1	프로젝트 생성 및 의존성 설정 (pubspec.yaml)	✅ 완료
Step 2	앱 진입점(main.dart) 및 Riverpod 설정	✅ 완료
Step 3	전역 라우터(app.dart) 및 홈 화면 구성	✅ 완료
Step 4	로컬 DB 설계 (Drift Schema)	✅ 완료
Step 5	DB 코드 생성 (build_runner)	✅ 완료
Step 6	DB 연결 분리 (Web/Native) 및 Provider 설정	✅ 완료
Step 7	홈 화면 UI (대시보드) 연결	✅ 완료
Step 8	[기능 1] 인물 위키 개발 (Repo + UI)	✅ 완료
Step 9	[기능 1] 인물 상세/추가 기능 보강	✅ 완료
Step 10	[기능 2] 뉴스 아카이브 개발 (Repo + UI)	✅ 완료
Step 11	[기능 3] 지도 트래커 (Google Maps + Geolocation)	🚧 대기 중


## 📅 Development Log (중간 점검)
[2026-02-05] Step 11 수정 ->  DB 연결 개발 
Step 11을 지도 기능에서 Firebase 전환 및 배포로 변경하고, DB 선택 가이드부터 시작하겠습니다.

🧐 Firebase DB 선택: Realtime DB vs Firestore
작성하신 앱(인물 위키, 뉴스 아카이브, 지도 로그)의 특성을 고려할 때, 두 가지 선택지가 있습니다.

|특성	|Realtime Database (RTDB)|	Cloud Firestore (추천)|
|:--|:--|:--|
|데이터 구조| 거대한 JSON 트리 구조 (단순함) |문서(Document) & 컬렉션(Collection) 구조 (체계적)|
|쿼리(검색)| 매우 제한적 (필터링 1개만 가능) | 강력함 (복합 필터링, 정렬, 페이징 지원)|
|확장성|데이터가 많아지면 성능 저하 가능성	|자동 확장 (Scaling), 대용량 데이터에 적합|
|비용|대역폭(전송량) 기준 과금	|읽기/쓰기 횟수 기준 과금|
|적합성|단순 채팅, 실시간 상태값 동기화	|사용자 프로필, 뉴스 피드, 복잡한 데이터 관계|

🏆 추천: Cloud Firestore
이유:

데이터 관계성: 인물(Person)과 뉴스(News) 간의 관계나, 나중에 추가될 지도 데이터(MapLogs)는 구조화된 데이터입니다. Firestore의 컬렉션 구조가 이를 관리하기 훨씬 유리합니다.

검색 및 정렬: "최신 뉴스 순으로 보기", "나이가 20세 이상인 인물 찾기" 같은 쿼리를 하려면 Firestore가 필수적입니다. RTDB는 이런 게 어렵습니다.

데이터 분석(시각화): 추후 빅쿼리(BigQuery)와 연동하여 데이터 분석을 하거나 그래프를 그리기에도 Firestore 구조가 훨씬 용이합니다.

🚀 수정된 로드맵 (Roadmap v2)
Step 11: Firebase 프로젝트 생성 및 연동 (앱 설정)

> Flutter Project 폴더 내부에서 아래를 진행한다. 

> Firebase Console로 이동해서 프로젝트 생성 및 사용하려는 서비스를 먼저 생성한다. 
> cloud firestore의 경우 미리 만들어두면 된다. 

> Firebase CLI 를 OS맞춰서 다운로드 해준다. 

> macOS 기준 아래의 명령어 
> curl -sL https://firebase.tools | bash  
> 명령어를 쳐주면 다운로드를 하는데 password를 입력하라고 한다. pc의 비번을 입력하면 된다.

> firebase login 
> dart pub global activate flutterfire_cli
> flutterfire configure 

> 이 후 firebase에 미리 연동해두 프로젝트를 연결 및 로그인 하면 된다. 

이렇게 하면, lib/firebase_options.dart 파일이 해당 프로젝트에서 생성되어 있다. 
Step 12: Firestore 데이터 구조 설계 및 Repository 수정 (로컬 Drift -> Cloud Firestore 전환)

Step 13: Firebase Hosting 배포 (웹에서 접속 가능하게)

(보류) Step 14: 지도 기능 (추후 추가)

## 📅 Development Log
[2026-02-08 16:18] Firestore 데이터베이스 전환
📝 요약
로컬 DB(Drift) 대신 Google Cloud Firestore를 사용하도록 PersonRepository와 NewsRepository를 수정함. 이를 통해 앱 데이터가 클라우드에 저장되고, 웹/앱 어디서든 실시간 동기화됨.

🚀 진행 단계: Step 12 - Repository 교체
우리는 기존 Drift 코드를 지우지 않고, 새로운 Firestore용 Repository를 만들어서 연결만 바꿀 것입니다. (나중에 필요하면 로컬 모드로 돌아갈 수 있도록)

## 📅 Development Log
[2026-02-10 18:30] Web 배포를 위한 빌드 에러 해결
📝 요약
sqlite3 및 Drift 관련 Native 코드가 Web 빌드 과정에 포함되면서 발생한 Only JS interop members may be 'external' 컴파일 에러를 해결함.
Native(Mobile/Desktop)와 Web 환경의 DB 연결 코드를 분리하는 '조건부 임포트(Conditional Import)' 패턴을 적용하여, Web 빌드 시에는 sqlite3 관련 코드가 아예 제외되도록 조치함.

🚀 진행 단계: Step 13 - Firebase Hosting 배포 준비
성공적인 웹 배포를 위해, 플랫폼별(Native vs Web)로 다른 DB 연결 구현체를 사용하도록 구조를 재정비했습니다.

1. 문제 상황

flutter build web --release 실행 시 sqlite3 패키지 내부(FFI 관련)에서 대량의 에러 발생.

원인: Flutter Web 컴파일러(dart2js)는 FFI(C언어 연동)를 지원하지 않는데, connection_native.dart가 빌드 트리에 포함되어 있었음.

2. 해결 방법 (조건부 임포트 적용)

파일 분리: DB 연결 로직을 3개로 쪼개어 관리.

connection_stub.dart: 껍데기(Interface) 역할.

connection_native.dart: dart:io 환경용 (기존 sqlite3 코드 유지).

connection_web.dart: dart:html 환경용 (빈 껍데기 LazyDatabase 반환으로 에러 방지).

진입점 통일: app_database.dart에서 dart.library.io와 dart.library.html 조건을 걸어, 환경에 따라 올바른 파일만 import 하도록 설정.

직접 참조 제거: 프로젝트 전체에서 connection_native.dart나 package:drift/native.dart를 직접 import 하는 코드를 모두 찾아 제거하고, 위에서 만든 통일된 진입점을 사용하도록 수정.

3. 결과

flutter build web --release 정상 완료.

Firestore 연동은 그대로 유지되면서, Web 환경에서의 호환성 문제만 깔끔하게 해결됨.

##📅 Development Log
[2026-02-10 18:59] Firebase Hosting 배포 완료

📝 요약
Flutter Web 빌드 결과물을 Firebase Hosting에 성공적으로 배포하여, 외부 URL을 통해 앱에 접속할 수 있게 됨. firebase.json 설정 파일 구성을 완료하고, 프로덕션 빌드 및 배포 파이프라인을 구축함.

🚀 진행 단계: Step 13 - Web 배포 완료
로컬 개발 환경을 벗어나 실제 웹 서버(Firebase Hosting)에 앱을 게시했습니다.

1. 빌드 및 배포 프로세스 정립

빌드: flutter build web --release

최적화된 프로덕션용 정적 파일(HTML/JS/Assets)을 build/web 폴더에 생성.

앞서 해결한 '조건부 임포트' 덕분에 sqlite3 관련 에러 없이 빌드 성공.

배포: firebase deploy --only hosting

build/web 폴더의 내용을 Firebase 서버로 전송.

2. 설정 파일(firebase.json) 구성

기존 FlutterFire 설정(flutter 키)을 유지하면서, Hosting 배포를 위한 필수 설정(hosting 키)을 병합하여 적용함.

주요 설정:

public: "build/web": 배포할 폴더 지정.

rewrites: 모든 경로(**)를 /index.html로 리다이렉트하여 GoRouter(SPA)의 새로고침 404 문제 방지.

3. 결과 확인

제공된 Hosting URL로 접속 시 앱이 정상 구동됨.

Firestore 데이터 연동(Named DB) 및 라우팅 기능 동작 확인 완료.

## 📅 Development Log
[2026-02-10 21:17] News Feature 고도화 (Firestore 연동 & UX 개선)

📝 요약
기존 News 기능을 Person 기능과 동일한 아키텍처 패턴(순수 Dart 모델 + Manual Provider)으로 리팩토링하고, 태그(Tag) 기능과 직관적인 날짜 입력 UI를 추가함.

🚀 진행 단계: Step 5 - News Feature 고도화 (완료)

1. 데이터 모델 (NewsLog) 개선

Timestamp 처리: Firestore의 Timestamp 타입을 앱 내에서 DateTime으로 편하게 다룰 수 있도록 fromMap/toMap 메서드 내부에 변환 로직을 내재화함.

태그(Tags) 추가: 뉴스 분류를 위한 List<String> tags 필드 추가.

순수 Dart 객체: freezed 의존성을 제거하고, PersonModel과 동일하게 명시적인 생성자와 변환 메서드를 갖춘 구조로 통일.

2. Repository (NewsFirestoreRepository) 리팩토링

구조 통일: part 및 Code Generation을 제거하고, PersonFirestoreRepository와 동일한 일반 Provider 방식으로 전환하여 유지보수성을 높임.

Converter 활용: Firestore의 withConverter를 사용하여 데이터 입출력 시 모델 변환을 자동화함.

3. UI/UX 개선

추가 화면 통합: 별도의 NewsAddScreen 페이지를 제거하고, NewsListScreen 내부에서 다이얼로그(Dialog) 형태로 뉴스를 추가하도록 변경하여 작업 흐름을 끊지 않음.

태그 입력: # 태그를 쉽고 빠르게 입력할 수 있는 칩(Chip) 기반 UI 구현.

상세 화면: 날짜와 태그 정보를 시각적으로 강조한 NewsDetailScreen 구현.

4. 라우팅 (app.dart) 정리

불필요해진 news/add 라우트를 제거하고, 변경된 모델(NewsLog)에 맞춰 detail 화면 파라미터 전달 로직을 수정함.


# 📘 개발자 로그: 데이터 수집기 앱 - 이미지 업로드 기능 추가

**날짜:** 2026-02-19 16:20 KST
**상태:** ✅ 기능 업데이트 (Step 16)

## 1. 업데이트 요약
- **맵 트래커 (Map Tracker)**:
  - 웹 렌더링 문제를 Google Maps JS API 스크립트 추가와 널(Null) 타입 수정으로 해결했습니다.
  - 이제 웹 배포 시 지도와 위치 추적(REC) 기능이 정상 작동합니다.

- **인물 관리 (Person Feature)**:
  - 기존의 수동 URL 입력 방식을 **이미지 선택기(Image Picker)** 및 **Firebase Storage 업로드** 방식으로 전면 교체했습니다.
  - 모바일(Android/iOS) 및 웹(Web) 환경에서 갤러리 접근 및 파일 선택을 지원합니다.
  - 업로드 진행 시 로딩 인디케이터를 표시하여 사용자 경험을 개선했습니다.

## 2. 기술적 세부사항
- **추가된 라이브러리**: `image_picker`, `firebase_storage`
- **저장 경로**: 이미지는 `/person_images/{timestamp}_{filename}` 경로에 저장됩니다.
- **웹 호환성 처리**: `kIsWeb` 플래그를 사용하여 모바일은 `File` 객체로, 웹은 `readAsBytes` 방식으로 분기 처리하여 업로드를 구현했습니다.

## 3. 향후 계획
- Firebase Console에서 Storage 보안 규칙(Rules) 검토 및 적용.
- 실제 모바일 기기(Android/iOS)에서의 이미지 업로드 및 권한 요청 테스트.


# 📘 개발자 로그: 데이터 수집기 앱 - 이미지 업로드 기능 고도화

**날짜:** 2026-02-19 17:20 KST
**작성자:** Perplexity AI Assistant
**상태:** ✅ 기능 개선 및 안정화 (Step 17)

## 1. 업데이트 요약
- **권한 문제 해결**: 모바일(Android 13+/iOS) 환경에서 갤러리 접근 시 권한 팝업이 뜨지 않던 문제를 해결했습니다.
- **이미지 처리 로직 통합**: `CustomImagePicker` 위젯을 통해 'URL 직접 입력'과 '파일 업로드' 방식을 하나로 통합했습니다.
- **용량 최적화 적용**: 업로드 시 2MB 제한 및 자동 리사이징(Resize) 로직을 적용하여 서버 용량을 절약합니다.

## 2. 주요 변경 사항
### 🔧 권한 설정 (Permissions)
- **Android**: `AndroidManifest.xml`에 `READ_MEDIA_IMAGES` (Android 13 이상 필수) 및 `READ_EXTERNAL_STORAGE` 권한을 추가했습니다.
- **iOS**: `Info.plist`에 `NSPhotoLibraryUsageDescription` 키를 추가하여 앱 심사 기준을 충족했습니다.
- **코드**: `permission_handler` 패키지를 도입하여, 갤러리 접근 전 명시적으로 권한을 요청하고 거부 시 설정 화면으로 유도하도록 개선했습니다.

### 🖼️ 이미지 처리 (CustomImagePicker)
- **하이브리드 모드**:
  1. **URL 모드**: 웹 이미지 주소 복사-붙여넣기 시 `HEAD` 요청으로 파일 크기를 미리 검증합니다.
  2. **업로드 모드**: 갤러리 선택 시 `flutter_image_compress`를 통해 이미지를 압축한 후 Firebase Storage에 저장합니다.
- **UI 개선**: 이미지 선택, 로딩, 미리보기, 삭제 기능을 직관적인 원형 UI(CircleAvatar)로 통합했습니다.

## 3. 기술 스택
- **패키지**: `image_picker`, `firebase_storage`, `flutter_image_compress`, `permission_handler`
- **저장소**: Firebase Storage (Bucket: `asia-northeast3` / Path: `/uploads`)

## 4. 향후 계획 (Next Steps)
- 실제 기기에서 권한 팝업이 정상적으로 뜨는지 최종 확인. (특히 Android 13 이상 기기)
- News 및 Person 리스트 화면에서 썸네일 이미지가 깨지지 않고 잘 로딩되는지 확인.

# 📘 개발자 로그: 데이터 수집기 앱 - 데이터 입력 UX 개선

**날짜:** 2026-02-19 18:00 KST
**상태:** ✅ 입력 편의성 개선 완료 (Step 19)

## 1. 업데이트 요약
- **속성(Attribute) & 태그(Tag) 입력 방식 개선**: 기존의 원시적인 문자열 파싱(`key:value` 등) 방식을 제거하고, 직관적인 **전용 입력 위젯**을 도입했습니다.
- **키보드 접근성 강화**: PC와 모바일 환경 모두에서 `Enter` 또는 `Next` 키를 통해 연속적인 데이터 입력이 가능하도록 흐름을 최적화했습니다.

## 2. 주요 변경 사항 (Components)
### 🏷️ 태그 입력 (`TagInputWidget`)
- **기능**: 텍스트 입력 후 `Enter`를 누르면 즉시 하단에 `Chip` 형태로 태그가 추가됩니다.
- **삭제**: 추가된 태그의 `X` 버튼을 눌러 손쉽게 제거할 수 있습니다.
- **적용**: `NewsListScreen`의 뉴스 태그 입력란에 적용되었습니다.

### 🔑 속성 입력 (`AttributeInputWidget`)
- **구조**: `Key`와 `Value` 입력 필드를 분리하고 나란히 배치했습니다.
- **흐름**: [Key 입력] -> [Enter/Next] -> [Value 포커스 이동] -> [Value 입력] -> [Enter] -> [속성 추가 & 입력창 초기화 & Key 포커스 이동]의 순환 구조를 구현했습니다.
- **시각화**: 추가된 속성은 하단에 리스트 형태로 쌓이며, 개별 삭제가 가능합니다.
- **적용**: `PersonListScreen`의 인물 속성 입력란에 적용되었습니다.

## 3. 기술적 이점
- **데이터 무결성**: 사용자가 포맷(`:`, `,` 등)을 신경 쓸 필요가 없어 오타나 파싱 오류가 원천 차단됩니다.
- **코드 간소화**: 화면(Screen) 단의 복잡한 문자열 처리 로직이 제거되고, 모델 객체(`Map`, `List`)를 직접 조작하므로 코드가 훨씬 깔끔해졌습니다.

# 📘 개발자 로그: 데이터 수집기 앱 - UI/UX 통합 업데이트

**날짜:** 2026-02-19 18:20 KST
**작성자:** Perplexity AI Assistant
**상태:** ✅ 기능 통합 및 완료 (Step 20)

## 1. 업데이트 요약
- **UI 통합**: 이전에 개별적으로 개발된 `CustomImagePicker`, `TagInputWidget`, `AttributeInputWidget`을 `PersonListScreen`과 `NewsListScreen`에 완벽하게 통합했습니다.
- **버그 수정**: 기존 화면 코드에서 누락되거나 하드코딩 되어 있던 부분들을 모두 커스텀 위젯으로 교체하여 유지보수성과 사용성을 확보했습니다.

## 2. 화면별 변경 사항
### 👤 Person Feature (`PersonListScreen`)
- **프로필 사진**: `CustomImagePicker(isCircle: true)`를 적용하여 원형 프로필 사진 업로드/변경 UI를 구현했습니다.
- **속성 입력**: `AttributeInputWidget`을 적용하여 `Key:Value` 문자열 파싱 방식의 불편함을 해소하고, 직관적인 리스트 추가/삭제 방식을 도입했습니다.

### 📰 News Feature (`NewsListScreen`)
- **썸네일 이미지**: `CustomImagePicker(isCircle: false)`를 적용하여 뉴스 기사에 맞는 사각형 썸네일 업로드 UI를 구현했습니다.
- **태그 입력**: `TagInputWidget`을 적용하여 콤마(,) 구분 없이 `Enter/Tab` 키로 태그를 쉽게 추가하고 관리할 수 있도록 했습니다.

## 3. 남은 작업
- 실제 디바이스 빌드 및 배포 테스트.
- Firestore 데이터 구조가 변경된 모델(imageUrl 필드 등)과 잘 호환되는지 확인.
