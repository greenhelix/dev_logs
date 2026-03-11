enum UploadTarget { firestore, redmine }

extension UploadTargetX on UploadTarget {
  String get label {
    switch (this) {
      case UploadTarget.firestore:
        return 'Firestore';
      case UploadTarget.redmine:
        return 'Redmine';
    }
  }

  String get actionLabel {
    switch (this) {
      case UploadTarget.firestore:
        return 'Firestore 업로드';
      case UploadTarget.redmine:
        return 'Redmine 업로드';
    }
  }
}
