# Flutter Projects. 



### 1. Data Collector 


### 2. Google Auth Helper
- SLM을 활용하여 모듈/테스트케이스 분석
- 결과에 따라 원인 분석
```
- cd google_auth_helper
- python3 -m venv .venv && source .venv/bin/activate
- pip install -r requirements.txt
- cp .env.example .env 후 값 설정(도구 경로, 토큰 등)
- 실행
- - 로컬 확인: python run_local.py --host 0.0.0.0 --port 8000 --no-browser
- - 또는: uvicorn app.main:app --host 0.0.0.0 --port 8000

브라우저 접속: http://<ubuntu-ip>:8000
운영 서비스로 띄우려면: docs/ubuntu-deploy.md + scripts/install_systemd.sh 사용
```

### 3. Kani Diagram
- 마인드맵, 다이어그램을 그리는 툴 
- sw(안드로이드, 플러터) 등의 프로젝트 코드를 넣으면 사용자가 보기 쉽게 다이어그램을 그린다

