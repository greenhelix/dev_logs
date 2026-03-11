#define MyAppName "Google Auth Helper"
#ifndef MyAppVersion
  #define MyAppVersion "v0.1.1"
#endif
#ifndef MyAppSourceDir
  #define MyAppSourceDir "."
#endif
#ifndef MyAppOutputDir
  #define MyAppOutputDir "."
#endif
#ifndef MyAppOutputBaseFilename
  #define MyAppOutputBaseFilename "gah-windows-v0.1.1-setup"
#endif

[Setup]
AppId={{A4A69C6B-4252-4D4A-8C82-92AFCB05F7F9}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher=greenhelix
DefaultDirName={autopf}\Google Auth Helper
DefaultGroupName=Google Auth Helper
OutputDir={#MyAppOutputDir}
OutputBaseFilename={#MyAppOutputBaseFilename}
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Files]
Source: "{#MyAppSourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Google Auth Helper"; Filename: "{app}\google_auth_helper.exe"
Name: "{group}\Uninstall Google Auth Helper"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\google_auth_helper.exe"; Description: "Launch Google Auth Helper"; Flags: nowait postinstall skipifsilent
