from html.parser import HTMLParser


class MyHTMLParser(HTMLParser):
    def __init__(self):
        HTMLParser.__init__(self)
        self.is_test = False

    def handle_starttag(self, tag, attrs):
        if tag == 'Test':  # <Test> 태그 시작
            self.is_test = True

    def handle_endtag(self, tag):
        if tag == 'Test':  # </Test> 태그 닫힘
            self.is_test = False

    def handle_starttag(self, tag, attrs):
        if tag == 'Test':  # <Test> 태그 시작
            self.is_test = True

    def handle_endtag(self, tag):
        if tag == 'Test':  # </Test> 태그 닫힘
            self.is_test = False

    def handle_data(self, data):
        if self.is_test:  # <Test>~</Test> 구간인 경우
            print(data)     # 데이터를 출력


with open('test_result.html') as html:
    parser = MyHTMLParser()
    parser.feed(html.read())
