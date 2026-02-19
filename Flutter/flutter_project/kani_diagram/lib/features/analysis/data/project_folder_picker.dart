import '../domain/source_code_file.dart';

import 'project_folder_picker_stub.dart'
    if (dart.library.html) 'project_folder_picker_web.dart'
    as impl;

abstract class ProjectFolderPicker {
  Future<List<SourceCodeFile>> pickCodeFilesFromFolder({
    Set<String> allowedExtensions,
  });
}

ProjectFolderPicker createProjectFolderPicker() => impl.createProjectFolderPickerImpl();
