// ignore_for_file: deprecated_member_use, avoid_web_libraries_in_flutter
import 'dart:html' as html;
import 'dart:typed_data';

import 'package:image/image.dart' as img;

Future<void> exportDiagramImage({
  required Uint8List pngBytes,
  required String fileName,
  required bool asJpg,
}) async {
  Uint8List bytes = pngBytes;
  var mimeType = 'image/png';
  var exportName = '$fileName.png';

  if (asJpg) {
    final decoded = img.decodeImage(pngBytes);
    if (decoded != null) {
      bytes = Uint8List.fromList(img.encodeJpg(decoded, quality: 92));
      mimeType = 'image/jpeg';
      exportName = '$fileName.jpg';
    }
  }

  final blob = html.Blob([bytes], mimeType);
  final url = html.Url.createObjectUrlFromBlob(blob);
  html.AnchorElement(href: url)
    ..setAttribute('download', exportName)
    ..click();
  html.Url.revokeObjectUrl(url);
}
