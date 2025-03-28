import requests
import json
import os

# Notion API 정보
NOTION_API_URL = "https://api.notion.com/v1/pages"
NOTION_API_KEY = "ntn_f39601858432QrJ46ODymErODtksDIZ7QceBtMnEdLN8Dv"  # 본인의 Notion API 키로 교체
DATABASE_ID = "1300ec0ba3f380478305ece95e79636f"  # 본인의 Notion 데이터베이스 ID로 교체
# https://www.notion.so/greenhelix/1300ec0ba3f380478305ece95e79636f?v=0ae495862d0e4d2989460d89eef7280f&pvs=4
def upload_log_to_notion(log_file):
    # Notion API 헤더
    headers = {
        "Authorization": f"Bearer {NOTION_API_KEY}",
        "Content-Type": "application/json",
        "Notion-Version": "2022-06-28"
    }

    # 로그 파일 읽기
    with open(log_file, 'r') as file:
        logs = file.readlines()

    # 로그를 Notion에 업로드
    for log in logs:
        # Notion 페이지에 추가할 데이터 구성
        data = {
            "parent": {"database_id": DATABASE_ID},
            "properties": {
                "Name": {
                    "title": [
                        {
                            "type": "text",
                            "text": {
                                "content": log.strip()  # 로그의 공백을 제거하여 삽입
                            }
                        }
                    ]
                }
            }
        }

        # Notion API에 요청
        response = requests.post(NOTION_API_URL, headers=headers, data=json.dumps(data))

        if response.status_code == 200:
            print(f"Successfully added log: {log.strip()}")
        else:
            print(f"Error: {response.status_code}, {response.text}")

# 메인 실행 부분
if __name__ == "__main__":
    log_folder = "log_folder"  # 로그 파일이 저장된 폴더
    log_files = [f for f in os.listdir(log_folder) if f.endswith('.txt')]  # 텍스트 파일 목록

    for log_file in log_files:
        upload_log_to_notion(os.path.join(log_folder, log_file))
