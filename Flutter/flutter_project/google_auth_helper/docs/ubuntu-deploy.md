# Ubuntu 배포 가이드

## 1) 서버 준비
```bash
sudo apt update
sudo apt install -y python3 python3-venv python3-pip
```

## 2) 코드 배치
```bash
sudo mkdir -p /opt/google-auth-helper
sudo chown -R $USER:$USER /opt/google-auth-helper
cd /opt/google-auth-helper
# 여기서 레포 클론 또는 코드 복사
```

## 3) 환경설정
```bash
cp .env.example .env
```

필수 확인:
- `CTS_TOOL_PATH`, `GTS_TOOL_PATH` 등 도구 경로
- `MONITOR_API_TOKEN` (Firebase 조회 전용 연동 시 권장)
- Jira/Redmine/Notion 토큰

## 4) systemd 서비스 설치
```bash
chmod +x scripts/install_systemd.sh
APP_DIR=/opt/google-auth-helper SERVICE_NAME=google-auth-helper ./scripts/install_systemd.sh
```

## 5) 운영 명령
```bash
sudo systemctl restart google-auth-helper
sudo systemctl status google-auth-helper
sudo journalctl -u google-auth-helper -f
```

## 6) 방화벽/접속
- 내부망 운영 권장
- 외부 노출 시 Nginx + HTTPS + IP 제한 권장

