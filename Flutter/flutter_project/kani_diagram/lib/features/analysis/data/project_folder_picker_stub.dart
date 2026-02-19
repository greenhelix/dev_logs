import '../domain/source_code_file.dart';
import 'project_folder_picker.dart';

class UnsupportedProjectFolderPicker implements ProjectFolderPicker {
  @override
  Future<List<SourceCodeFile>> pickCodeFilesFromFolder({
    Set<String> allowedExtensions = const {},
  }) async {
    throw UnsupportedError('이 플랫폼에서는 폴더 업로드를 지원하지 않습니다.');
  }
}

ProjectFolderPicker createProjectFolderPickerImpl() => UnsupportedProjectFolderPicker();
