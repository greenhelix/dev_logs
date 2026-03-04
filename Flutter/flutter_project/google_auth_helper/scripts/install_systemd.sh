#!/usr/bin/env bash
set -euo pipefail

# Ubuntu 운영 환경 기준 기본 경로
APP_DIR=${APP_DIR:-/opt/google-auth-helper}
SERVICE_NAME=${SERVICE_NAME:-google-auth-helper}
SERVICE_FILE="${SERVICE_NAME}.service"
SYSTEMD_DIR=/etc/systemd/system

if [[ ! -d "${APP_DIR}" ]]; then
  echo "[오류] APP_DIR 경로를 찾을 수 없습니다: ${APP_DIR}"
  exit 1
fi

if [[ ! -f "${APP_DIR}/deploy/${SERVICE_FILE}" ]]; then
  echo "[오류] 서비스 파일을 찾을 수 없습니다: ${APP_DIR}/deploy/${SERVICE_FILE}"
  exit 1
fi

echo "[1/6] Python 가상환경 생성 확인"
if [[ ! -d "${APP_DIR}/.venv" ]]; then
  python3 -m venv "${APP_DIR}/.venv"
fi

echo "[2/6] 의존성 설치"
"${APP_DIR}/.venv/bin/pip" install -r "${APP_DIR}/requirements.txt"

echo "[3/6] .env 파일 확인"
if [[ ! -f "${APP_DIR}/.env" ]]; then
  cp "${APP_DIR}/.env.example" "${APP_DIR}/.env"
  echo "  -> .env 파일을 생성했습니다. 값 수정 후 재실행하세요."
fi

echo "[4/6] systemd 서비스 파일 설치"
sudo cp "${APP_DIR}/deploy/${SERVICE_FILE}" "${SYSTEMD_DIR}/${SERVICE_FILE}"

echo "[5/6] daemon reload 및 서비스 활성화"
sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}"

echo "[6/6] 서비스 시작"
sudo systemctl restart "${SERVICE_NAME}"
sudo systemctl --no-pager --full status "${SERVICE_NAME}"

echo "[완료] 서비스 설치가 끝났습니다."
