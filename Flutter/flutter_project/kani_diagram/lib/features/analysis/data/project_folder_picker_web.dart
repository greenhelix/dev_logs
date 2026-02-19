// ignore_for_file: deprecated_member_use, avoid_web_libraries_in_flutter
import 'dart:async';
import 'dart:convert';
import 'dart:html' as html;
import 'dart:typed_data';

import '../domain/source_code_file.dart';
import 'project_folder_picker.dart';

class WebProjectFolderPicker implements ProjectFolderPicker {
  @override
  Future<List<SourceCodeFile>> pickCodeFilesFromFolder({
    Set<String> allowedExtensions = const {
      'dart',
      'yaml',
      'yml',
      'json',
      'kt',
      'java',
      'swift',
    },
  }) async {
    final input = html.FileUploadInputElement()..multiple = true;
    input.setAttribute('webkitdirectory', '');
    input.setAttribute('directory', '');

    input.click();
    await input.onChange.first;

    final pickedFiles = input.files;
    if (pickedFiles == null || pickedFiles.isEmpty) {
      return const [];
    }

    final sourceFiles = <SourceCodeFile>[];
    for (final file in pickedFiles) {
      final lowerName = file.name.toLowerCase();
      final isAllowed = allowedExtensions.any(
        (ext) => lowerName.endsWith('.${ext.toLowerCase()}'),
      );
      if (!isAllowed) {
        continue;
      }

      final bytes = await _readAsBytes(file);
      final content = utf8.decode(bytes, allowMalformed: true);

      String relativePath = file.name;
      try {
        final dynamicFile = file as dynamic;
        final webkitPath = dynamicFile.webkitRelativePath as String?;
        if (webkitPath != null && webkitPath.trim().isNotEmpty) {
          relativePath = webkitPath;
        }
      } catch (_) {
        // 브라우저 구현 차이로 webkitRelativePath가 없을 수 있음.
      }

      sourceFiles.add(
        SourceCodeFile(
          name: file.name,
          path: relativePath,
          content: content,
          size: file.size,
        ),
      );
    }

    return sourceFiles;
  }

  Future<Uint8List> _readAsBytes(html.File file) {
    final completer = Completer<Uint8List>();
    final reader = html.FileReader();

    reader.onError.listen((_) {
      if (!completer.isCompleted) {
        completer.complete(Uint8List(0));
      }
    });

    reader.onLoadEnd.listen((_) {
      if (completer.isCompleted) {
        return;
      }
      final result = reader.result;
      if (result is ByteBuffer) {
        completer.complete(Uint8List.view(result));
      } else if (result is Uint8List) {
        completer.complete(result);
      } else {
        completer.complete(Uint8List(0));
      }
    });

    reader.readAsArrayBuffer(file);
    return completer.future;
  }
}

ProjectFolderPicker createProjectFolderPickerImpl() => WebProjectFolderPicker();
