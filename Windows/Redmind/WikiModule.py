# -*- coding: utf-8 -*-
"""
temp.json의 모듈 정보를 읽어 Redmine Wiki에 자동 업로드(생성/업데이트)합니다.
- 제목: 각 항목의 name
- 본문: 설명, 코드 링크를 마크다운 형식으로 구성
- 부모 페이지: VTS_모듈_정보
- 버전 충돌(409) 자동 재시도
- 상세 로깅 기능 추가
"""

import os
import json
import time
import logging
import requests
from urllib.parse import quote

# ===== 로깅 설정 =====
# 로그 레벨: DEBUG (모든 로그), INFO, WARNING, ERROR, CRITICAL
# 포맷: [로그레벨] 시간 메시지
logging.basicConfig(
    level=logging.DEBUG, 
    format='[%(levelname)s] %(asctime)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

# ===== 고정 설정(필요 시 수정) =====
REDMINE_URL   = "https://redmine.innopiatech.com:17443"  # 예: https://redmine.example.com:443
API_KEY       = "8ffe1605a1904606036140a8a17eaf1b81d96f83"        # 반드시 실제 API 키로 바꾸세요
PROJECT_ID    = "xts"                                     # 프로젝트 식별자
PARENT_TITLE  = "VTS_모듈_정보"                               # 부모 위키 페이지 제목
INPUT_FILE    = "temp.json"                               # 업로드할 JSON 파일명
VERIFY_SSL    = False                                     # 사내 인증서 문제시 False로 변경
REQ_DELAY_SEC = 0.2                                       # 요청 간 지연(서버 부하 완화)


# ===== 본문 생성 =====
def build_wiki_body(item: dict) -> str:
    """
    JSON 객체를 Redmine Wiki 마크다운 형식으로 변환합니다.
    code_links가 리스트인 경우와 단일 문자열인 경우를 모두 처리합니다.
    """
    name = (item.get("name") or "").strip()
    desc = (item.get("description") or "").strip()
    links = item.get("code_links")

    lines = [
        f"# {name}", "",
        "* *설명*:", desc if desc else "[설명 미입력]", "",
    ]

    details = item.get("details")
    if details:
        lines.append("* *세부사항*:")
        
        purpose = (details.get("purpose") or "").strip()
        if purpose:
            lines.append(f"  * *목적*: {purpose}")

        components = details.get("components")
        mechanism = details.get("mechanism")
        if components:
            lines.append("  * *컴포넌트*:")
            # 'components'가 리스트인 경우
            if isinstance(components, list):
                for compo in components:
                    compo_name = (compo.get("name") or "이름").strip()
                    compo_desc = (compo.get("description") or "설명").strip()
                    lines.append(f"    * **{compo_name}**: {compo_desc}")
            # 'components'가 문자열인 경우
            elif isinstance(components, str):
                lines.append(f"    * {components.strip()}")
        elif mechanism:
            lines.append("  * *메커니즘*:")
            if isinstance(mechanism, str):
                lines.append(f"    * {mechanism.strip()}")
        lines.append("")

    if isinstance(links, list) and links:
        lines.append("* *코드 링크*:")
        for link_info in links:
            link_type = (link_info.get("type") or "링크").strip()
            link_url = (link_info.get("url") or "").strip()
            link_desc = (link_info.get("description") or "").strip()
            if link_url:
                lines.append(f'[{link_type}]({link_url})')
                # if link_desc:
                #     lines.append(f'*** {link_desc}')
    elif isinstance(links, str) and links.strip():
        lines.append(f'[{links}]({links})')
    else:
        lines.append("#### [링크 미입력]")

    return "\n".join(lines)


# ===== Redmine Wiki 버전 조회 =====
def get_wiki_version(title: str) -> int | None:
    url = f"{REDMINE_URL}/projects/{PROJECT_ID}/wiki/{quote(title)}.json"
    headers = {"Content-Type": "application/json", "X-Redmine-API-Key": API_KEY}
    
    logging.debug(f"버전 조회 요청: {title} (URL: {url})")
    try:
        r = requests.get(url, headers=headers, timeout=20, verify=VERIFY_SSL)
        logging.debug(f"버전 조회 응답: {title} (Status: {r.status_code})")
        
        if r.status_code == 200:
            version = r.json().get("wiki_page", {}).get("version")
            logging.info(f"기존 페이지 버전 확인: {title} (Version: {version})")
            return version
        elif r.status_code == 404:
            logging.info(f"새 페이지로 판단됨 (404 Not Found): {title}")
            return None
        else:
            logging.warning(f"버전 조회 API 오류 ({r.status_code}): {title} | 응답: {r.text}")
            return None
    except requests.exceptions.RequestException as e:
        logging.error(f"버전 조회 중 네트워크 오류: {title} | 오류: {e}")
        return None


# ===== Redmine Wiki 업서트(생성/갱신) =====
def upsert_wiki_page(title: str, text: str) -> bool:
    url = f"{REDMINE_URL}/projects/{PROJECT_ID}/wiki/{quote(title)}.json"
    headers = {"Content-Type": "application/json", "X-Redmine-API-Key": API_KEY}
    payload = {"wiki_page": {"text": text, "parent_title": PARENT_TITLE}}

    version = get_wiki_version(title)
    if version is not None:
        payload["wiki_page"]["version"] = version
        logging.debug(f"업데이트 페이로드 구성: {title} (Version: {version})")
    else:
        logging.debug(f"생성 페이로드 구성: {title}")

    try:
        r = requests.put(url, headers=headers, data=json.dumps(payload), timeout=30, verify=VERIFY_SSL)
        logging.debug(f"1차 업로드 시도 응답: {title} (Status: {r.status_code})")

        if r.status_code in (201, 204):
            return True

        if r.status_code == 409:
            logging.warning(f"버전 충돌 감지(409), 재시도: {title}")
            new_version = get_wiki_version(title)
            if new_version is None:
                logging.error(f"재시도 실패: 최신 버전 조회 중 페이지 삭제됨: {title}")
                return False
                
            payload["wiki_page"]["version"] = new_version
            logging.debug(f"재시도 페이로드 구성: {title} (New Version: {new_version})")
            
            r2 = requests.put(url, headers=headers, data=json.dumps(payload), timeout=30, verify=VERIFY_SSL)
            logging.debug(f"2차 업로드 시도 응답: {title} (Status: {r2.status_code})")
            return r2.status_code in (201, 204)

        logging.error(f"업로드 실패 ({r.status_code}): {title} | 응답: {r.text}")
        return False

    except requests.exceptions.RequestException as e:
        logging.error(f"업로드 중 네트워크 오류: {title} | 오류: {e}")
        return False


# ===== 입력 JSON 로드 =====
def load_items(path: str) -> list[dict]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


# ===== 메인 =====
def main():
    logging.info("===== Redmine Wiki 업로드 스크립트 시작 =====")
    
    if not API_KEY or "YOUR_API_KEY" in API_KEY:
        logging.critical("API_KEY가 설정되지 않았습니다. 스크립트를 종료합니다.")
        return

    if not os.path.exists(INPUT_FILE):
        logging.critical(f"입력 파일({INPUT_FILE})을 찾을 수 없습니다. 스크립트를 종료합니다.")
        return

    try:
        items = load_items(INPUT_FILE)
        logging.info(f"입력 파일 로드 성공: {INPUT_FILE}")
    except json.JSONDecodeError as e:
        logging.critical(f"JSON 파일 파싱에 실패했습니다: {INPUT_FILE} | 오류: {e}")
        return
    except Exception as e:
        logging.critical(f"파일 로드 중 예외 발생: {e}")
        return

    item_count = len(items)
    logging.info(f"총 {item_count}개 항목 업로드 시작 (프로젝트: {PROJECT_ID}, 부모 페이지: {PARENT_TITLE})")
    
    ok, fail = 0, 0
    for i, item in enumerate(items, 1):
        title = (item.get("name") or "").strip()
        logging.info(f"--- [{i}/{item_count}] 처리 시작: {title or '이름 없는 항목'} ---")

        if not title:
            logging.warning(f"[{i}/{item_count}] 건너뛰기: 항목에 'name' 필드가 없습니다.")
            fail += 1
            continue

        body = build_wiki_body(item)
        logging.debug(f"위키 본문 생성 완료: {title}")

        if upsert_wiki_page(title, body):
            logging.info(f"[{i}/{item_count}] 업로드 성공: {title}")
            ok += 1
        else:
            logging.error(f"[{i}/{item_count}] 업로드 최종 실패: {title}")
            fail += 1
        
        time.sleep(REQ_DELAY_SEC)

    logging.info("===== 모든 항목 처리 완료 =====")
    logging.info(f"최종 결과 - 성공: {ok}, 실패: {fail}")


if __name__ == "__main__":
    main()
