import 'dart:typed_data';

Future<void> exportDiagramImage({
  required Uint8List pngBytes,
  required String fileName,
  required bool asJpg,
}) async {
  throw UnsupportedError('웹 환경에서만 이미지 내보내기를 지원합니다.');
}
