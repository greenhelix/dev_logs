# Exoplayer 라이브러리 적용 가이드 

libs 폴더 아래에 필요한 exo 관련 폴더를 생성한다.
.libs
├── media3_exoplayer_libs
└── media3_ui_libs

각 폴더에는 Android.bp와 AndroidManifest.xml 이 있어야 한다. 

aar, jar 파일을 넣어준다. 
dependency 의 pom을 맞춰 넣어준 것이지만, 불필요해서 다른 것을 지우게 되면
정상적인 작동이 안될 수 있다. 

빌드는 되더라도 기능이 정상 작동을 안하는 경우가 많았다. 



