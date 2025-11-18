## 구조 설명 

사용한 아키텍쳐 : MVVM 패턴

com.innopia.bist.ver2
├── 📂 data
│   ├── 📂 model        // 데이터 클래스 (순수 데이터 객체)
│   │   ├── CardItem.java
│   │   └── HdmiTestData.java
│   │
│   └── 📂 repository   // 데이터 소스를 관리하는 저장소
│       ├── Test.java
│       ├── Test1Repository.java (기존 Test1)
│       └── HdmiRepository.java (기존 HdmiTest)
│
├── 📂 ui
│   ├── 📂 activity    // 액티비티
│   │   └── MainActivity.java
│   │
│   ├── 📂 fragment    // 프래그먼트
│   │   ├── MainFragment.java
│   │   └── test
│   │       ├── Test1Fragment.java
│   │       └── HdmiTestFragment.java
│   │
│   └── 📂 adapter     // 리사이클러뷰 어댑터
│       └── CardAdapter.java
│
├── 📂 viewmodel        // 뷰모델
│   ├── MainViewModel.java
│   └── Test1ViewModel.java
│
└── 📂 util             // 공용 유틸리티
├── AutoFitGridLayoutManager.java
├── OverscanHelper.java
├── TestResult.java
└── TestStatus.java