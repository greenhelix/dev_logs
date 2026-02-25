// Web-only implementation using dart:html.
import 'dart:html' as html;

// Opens the given URL in a new browser tab.
void openUrlNewTab(String url) {
  html.window.open(url, '_blank');
}
