from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False)
    page = browser.new_page()

    page.goto("https://docs.partner.android.com/tv/test/android/gsi")
    page.fill("input[name='username']", "limwwbb@innopiatech.com")
    page.fill("input[name='password']", "k9511132")
    page.click("button[type='submit']")

    # 수동 인증 후 진행
    input("로그인 후 Enter를 누르세요...")

    # 로그인 후 페이지 내용 가져오기
    print(page.content())

    browser.close()