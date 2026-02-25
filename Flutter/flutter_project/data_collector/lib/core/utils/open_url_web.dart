// Web-only implementation using dart:html.
// ignore: deprecated_member_use
import 'dart:html' as html;

// Opens the given URL in a new browser tab.
void openUrlNewTab(String url) {
  html.window.open(url, '_blank');
}
