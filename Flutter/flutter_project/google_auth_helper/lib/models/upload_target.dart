enum UploadTarget { firestore, redmine }

extension UploadTargetX on UploadTarget {
  String get label {
    switch (this) {
      case UploadTarget.firestore:
        return '파이어스토어';
      case UploadTarget.redmine:
        return '레드마인';
    }
  }

  String get actionLabel {
    switch (this) {
      case UploadTarget.firestore:
        return '파이어스토어 업로드';
      case UploadTarget.redmine:
        return '레드마인 업로드';
    }
  }
}
