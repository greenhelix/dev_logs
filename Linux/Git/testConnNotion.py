from notion_client import Client

# API 키와 데이터베이스 ID 설정
notion = Client(auth="ntn_i39601858431jJrjUAPsSWrsKRZYobUAtonHIXRSlgA58d")
database_id = "1300ec0ba3f380478305ece95e79636f"

# 데이터베이스 조회
response = notion.databases.query(database_id)
print(response)
