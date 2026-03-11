# Google Auth Helper

Desktop Flutter application for XTS result parsing, Redmine upload, release monitoring, and automated Linux test execution.

## Current Scope
- `Results Upload`
  - Upload separate result and log zip files.
  - Build preview data from uploaded archives only.
  - Generate Firestore and Redmine upload previews from parsed data.
- `Release Watch`
  - Track release targets and show the latest watcher snapshot.
- `Environment`
  - Check Firebase Hosting, Firestore, ADB, and Redmine connectivity.
- `Auto Test`
  - Save tool profiles with tool root, results path, and logs path.
  - Discover ADB devices and run XTS through tradefed console startup.
- `Settings`
  - Configure Firebase, Redmine, and per-tool paths.

## Development
```powershell
flutter pub get
flutter analyze
flutter test
```

## Desktop Release
Windows packaging:
```powershell
pwsh -File scripts/build_windows_release.ps1 -Version v0.1.1
```

Ubuntu packaging:
```bash
bash scripts/build_ubuntu_release.sh v0.1.1
```

Desktop release flow:
- `flutter pub get`
- `flutter analyze`
- `flutter test`
- platform desktop build and packaging only

Desktop release scripts do not run `flutter build web` or `firebase deploy --only hosting`.

## Release Outputs
- `test_release/windows/v0.1.1/`
- `test_release/linux/v0.1.1/`

## Update Check
- Source: `https://github.com/greenhelix/GAH-Release-Repo/releases/latest`

## Publish Helpers
- `scripts/build_windows_release.ps1`
- `scripts/build_ubuntu_release.sh`
- `scripts/publish_release_repo.ps1`
- `scripts/publish_release_repo.sh`
