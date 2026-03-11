# Google Auth Helper

Desktop Flutter application for XTS result parsing, Redmine upload, release monitoring, and Ubuntu-based automated test execution.

## Current Scope
- Upload separate result and log zip files and preview parsed uploads.
- Track release changes from watcher sources.
- Check Firebase, ADB, and Redmine connectivity.
- Run tradefed-based auto tests on Ubuntu only.

## Development
```powershell
flutter pub get
flutter analyze
flutter test
```

## Desktop Release
Windows packaging:
```powershell
pwsh -File scripts/build_windows_release.ps1 -Version v0.1.2
```

Ubuntu packaging:
```bash
bash scripts/build_ubuntu_release.sh v0.1.2
```

Desktop release flow:
- `flutter pub get`
- `flutter analyze`
- `flutter test`
- desktop packaging only

## Release Outputs
- `test_release/windows/v0.1.2/`
- `test_release/linux/v0.1.2/`

Each release folder now keeps only:
- the installer package
- `INSTALL.txt`

## Latest Change
- `v0.1.2`: Windows auto test is disabled, platform icons stay visible on focus, and desktop release artifacts are simplified.

## Publish Helpers
- `scripts/build_windows_release.ps1`
- `scripts/build_ubuntu_release.sh`
- `scripts/publish_release_repo.ps1`
- `scripts/publish_release_repo.sh`
