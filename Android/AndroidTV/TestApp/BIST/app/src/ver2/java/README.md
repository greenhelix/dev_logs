# BIST ver2 프로젝트 구조

## 아키텍�처

**MVVM (Model-View-ViewModel) 패턴** 기반 Android TV 테스트 애플리케이션

## 프로젝트 구조

com.innopia.bist.ver2
├── 📂 data
│ ├── 📂 model // 데이터 모델 클래스
│ │ └── CardItem.java // 메인 화면 카드 아이템
│ │
│ └── 📂 repository // 데이터 레이어 (비즈니스 로직)
│ ├── Test.java // Repository 인터페이스
│ ├── Test1Repository.java
│ ├── Test2Repository.java
│ ├── Test3Repository.java
│ ├── CpuTestRepository.java
│ ├── MemoryTestRepository.java
│ ├── StorageTestRepository.java
│ ├── BluetoothTestRepository.java
│ ├── WifiTestRepository.java
│ ├── ProcessMonitorRepository.java
│ ├── RcuButtonTestRepository.java
│ ├── VideoTestRepository.java
│ └── TemperatureTestRepository.java
│
├── 📂 viewmodel // 뷰모델 레이어
│ ├── MainViewModel.java // 메인 화면 뷰모델
│ ├── Test1ViewModel.java
│ ├── Test2ViewModel.java
│ ├── Test3ViewModel.java
│ ├── CpuTestViewModel.java
│ ├── MemoryTestViewModel.java
│ ├── StorageTestViewModel.java
│ ├── BluetoothTestViewModel.java
│ ├── WifiTestViewModel.java
│ ├── ProcessMonitorViewModel.java
│ ├── RcuButtonTestViewModel.java
│ ├── VideoTestViewModel.java
│ └── TemperatureTestViewModel.java
│
├── 📂 ui // UI 레이어
│ ├── 📂 activity
│ │ └── MainActivity.java // 메인 액티비티
│ │
│ ├── 📂 fragment
│ │ ├── MainFragment.java // 메인 화면 프래그먼트
│ │ │__
│ │ └── 📂 test // 테스트 화면 프래그먼트
│ │ ├── Test1Fragment.java
│ │ ├── Test2Fragment.java
│ │ ├── Test3Fragment.java
│ │ ├── CpuTestFragment.java
│ │ ├── MemoryTestFragment.java
│ │ ├── StorageTestFragment.java
│ │ ├── BluetoothTestFragment.java
│ │ ├── WifiTestFragment.java
│ │ ├── ProcessMonitorFragment.java
│ │ ├── RcuButtonTestFragment.java
│ │ ├── VideoTestFragment.java
│ │ └── TemperatureTestFragment.java
│ │
│ └── 📂 adapter
│ └── CardAdapter.java // RecyclerView 어댑터
│
├── 📂 service // 백그라운드 서비스
│ └── OsdOverlayService.java // OSD 오버레이 서비스
│
└── 📂 util // 유틸리티 클래스
├── AutoFitGridLayoutManager.java
├── OverscanHelper.java
├── TestStatus.java // 테스트 상태 enum
├── TestResult.java // 테스트 결과 데이터 클래스
├── OsdManager.java // OSD 관리자
└── SecretCodeDetector.java // 시크릿 코드 감지기

---

## MVVM 데이터 흐름

[Fragment (View)]
↕ (관찰)
[ViewModel]
↕ (호출)
[Repository]
↕ (데이터 처리)
[System APIs / Hardware]

- **Fragment**: 사용자 입력 수신 → ViewModel 메서드 호출
- **ViewModel**: Repository 호출 → LiveData로 결과 발행
- **Repository**: 실제 테스트 실행 → 콜백으로 결과 반환


---

## 새로운 테스트 추가 가이드

새로운 테스트 항목을 추가하려면 다음 **3개의 파일**을 생성해야 합니다.

### 1️⃣ Repository 생성
**위치:** `data/repository/`  
**파일명:** `[테스트명]Repository.java`  
**역할:** 테스트 실행 로직 및 데이터 처리

### 2️⃣ ViewModel 생성
**위치:** `viewmodel/`  
**파일명:** `[테스트명]ViewModel.java`  
**역할:** UI와 Repository 연결, LiveData 관리

### 3️⃣ Fragment 생성
**위치:** `ui/fragment/test/`  
**파일명:** `[테스트명]Fragment.java`  
**역할:** 테스트 UI 화면

### 4️⃣ 레이아웃 파일 생성 (선택사항)
**위치:** `res/layout/`  
**파일명:** `fragment_[테스트명].xml`

### 5️⃣ MainFragment에 cardItem 추가
**위치:** `ui/fragment/`
**파일명:** `MainFragment.java`
- generateSampleData()에 CardItem 추가 -> Main 화면 에서 카드 생성 부분
- handleCardClick()에 case 추가 -> Fragment와 연결 부분
