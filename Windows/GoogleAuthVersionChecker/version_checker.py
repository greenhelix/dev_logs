import os
import json
import hashlib
import requests
from bs4 import BeautifulSoup
from datetime import datetime
import sqlite3
import shutil
import tempfile
from http.cookiejar import CookieJar
from requests.cookies import create_cookie
import base64
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
import browser_cookie3
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager  # 드라이버 자동 설치를 위한 패키지
import shutil

CONFIG_FILE = "site_configs.json"
CACHE_DIR = "cache"
os.makedirs(CACHE_DIR, exist_ok=True)
DEBUG_FORCE_EXTRACT = True 
today_str = datetime.now().strftime("%Y%m%d")
output_filename = f"{today_str}.txt"
ORIGINAL_PROFILE_PATH = r"C:\Users\Kim\AppData\Local\Google\Chrome\User Data\Profile 4"
TEMP_PROFILE_DIR = tempfile.mkdtemp()
shutil.copytree(ORIGINAL_PROFILE_PATH, TEMP_PROFILE_DIR, dirs_exist_ok=True)
def get_hash(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()



def fetch_html_with_selenium(url):
    # Selenium을 사용하여 브라우저 열기
    options = Options()
    # options.add_argument("--headless")  # 브라우저를 띄우지 않으려면 이 줄 주석처리

    options = webdriver.ChromeOptions()
    options.add_argument(f"--user-data-dir={TEMP_PROFILE_DIR}")
    options.add_argument(r"profile-directory=Profile 4")
    options.add_argument("--disable-extensions")
    options.add_argument("--remote-debugging-port=9222")  # <-- 중요!
    # ChromeDriverManager를 사용하여 자동으로 chromedriver 다운로드 및 경로 설정
    service = Service(ChromeDriverManager().install())
    
    driver = webdriver.Chrome(service=service, options=options)
    
    # 로그인 페이지로 이동
    driver.get(url)
    input("로그인 완료 후 Enter를 누르세요...")

    # 수동으로 2단계 인증을 완료한 후, 로그인된 상태를 유지
    print("로그인 후 수동으로 2단계 인증을 완료하고 '확인' 버튼을 누른 뒤 20초 동안 대기하세요.")
    time.sleep(20)  # 로그인 후 충분한 시간을 주어야 합니다
    
    # 페이지를 가져와서 HTML로 변환
    html = driver.page_source
    
    with open("debug_fetched_page.html", "w", encoding="utf-8") as f:
        f.write(html)
    print("[DEBUG] 전체 HTML이 'debug_fetched_page.html'에 저장되었습니다.")

    # 쿠키를 가져와서 저장 (수동 로그인 상태로 이어짐)
    cookies = driver.get_cookies()
    driver.quit()  # 브라우저 종료
    
    return html, cookies

def extract_version_text(soup, version_selector, url):
    try:
        tags = soup.select(version_selector)
        for tag in tags:
            text = tag.get_text(strip=True)
            print(f"[DEBUG] version candidate: {text}")
            if "Last updated" in text:
                return text
    except Exception as e:
        print(f"[ERROR] Error extracting version text from {url}: {e}")
    return ""

def extract_content_by_id(soup, start_id, until_tag, max_releases=5):
    start_tag = soup.find(id=start_id)
    if not start_tag:
        print(f"[WARN] ID '{start_id}' not found in page")
        return "[ID NOT FOUND]"

    contents = []
    next_node = start_tag.find_next_sibling()
    count = 0
    while next_node and count < max_releases:
        if next_node.name == until_tag:
            break
        text = next_node.get_text(strip=True)
        if text:
            contents.append(text)
            count += 1
        next_node = next_node.find_next_sibling()
    return "\n".join(contents) if contents else "[NO CONTENT FOUND]"

def extract_target_selectors(soup, selectors, max_releases=5):
    texts = []
    count = 0
    for selector in selectors:
        try:
            tags = soup.select(selector)
            print(f"[DEBUG] {len(tags)} tags matched for selector '{selector}'")
            for tag in tags:
                text = tag.get_text(strip=True)
                if text:
                    texts.append(f"[{selector}] {text}")
                    count += 1
                if count >= max_releases:
                    break
        except Exception as e:
            print(f"[ERROR] Failed selector '{selector}': {e}")
    return "\n".join(texts) if texts else "[NO TARGET CONTENT]"

def load_site_configs():
    if not os.path.exists(CONFIG_FILE):
        print(f"[ERROR] Config file '{CONFIG_FILE}' not found.")
        return []
    try:
        with open(CONFIG_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"[ERROR] Failed to load JSON config: {e}")
        return []

def fetch_html(url):
    html, cookies = fetch_html_with_selenium(url)
    if html is None:
        return None, None
    
    # requests를 사용하여 HTML을 가져올 때, Selenium 쿠키를 사용하여 로그인 상태 유지
    session = requests.Session()
    for cookie in cookies:
        session.cookies.set(cookie['name'], cookie['value'], domain=cookie['domain'])
    
    return html, session

def main():
    site_configs = load_site_configs()
    if not site_configs:
        return

    with open(output_filename, "w", encoding="utf-8") as outfile:
        for config in site_configs:
            url = config.get("url")
            print(f"\n[PROCESSING] {url}")
            html, session = fetch_html(url)

            if html is None:
                outfile.write(f"{url}\n[FETCH ERROR]\n\n")
                continue

            try:
                soup = BeautifulSoup(html, "html.parser")
            except Exception as e:
                print(f"[ERROR] Failed to parse HTML for {url}: {e}")
                outfile.write(f"{url}\n[PARSE ERROR]\n\n")
                continue

            version_text = extract_version_text(soup, config.get("version_selector", ""), url)
            url_hash = get_hash(url)
            cache_file = os.path.join(CACHE_DIR, f"{url_hash}.txt")

            old_version = ""
            if os.path.exists(cache_file):
                with open(cache_file, "r", encoding="utf-8") as f:
                    old_version = f.read()


            print(f"[DEBUG] Version text: {version_text}")
            print(f"[DEBUG] Old version: {old_version}")

            if DEBUG_FORCE_EXTRACT or version_text != old_version:
                print(f"[INFO] Version changed: {url}")
                max_releases = config.get("max_releases", 5)

                if "target_by_id" in config:
                    start_id = config["target_by_id"].get("start_id")
                    until_tag = config["target_by_id"].get("include_until", "h2")
                    extracted = extract_content_by_id(soup, start_id, until_tag, max_releases)
                elif "target_selector" in config:
                    selectors = config.get("target_selector", [])
                    extracted = extract_target_selectors(soup, selectors, max_releases)
                else:
                    extracted = "[NO EXTRACTION CONFIG]"

                outfile.write(f"[CHANGED] {url}\nVersion: {version_text}\nExtracted:\n{extracted}\n\n")
                with open(cache_file, "w", encoding="utf-8") as f:
                    f.write(version_text)
            else:
                print(f"[INFO] No change detected for {url}")

if __name__ == "__main__":
    main()
