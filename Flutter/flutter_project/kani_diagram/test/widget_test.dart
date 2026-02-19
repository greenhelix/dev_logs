import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kani_diagram/app.dart';

void main() {
  testWidgets('앱 기본 화면 렌더링', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: KaniDiagramApp()));

    expect(find.text('코드 다이어그램 분석기'), findsOneWidget);
    expect(find.text('프로젝트 폴더 업로드'), findsOneWidget);
  });
}
