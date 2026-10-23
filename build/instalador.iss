; Instalador de DentaVista para Windows (Inno Setup 6).
; @author EREJM (Everson Ricardo · Edilson Joel · Michael)
; Empaqueta la imagen de jpackage (DentaVista.exe + su propio Java) en un único
; DentaVista-<versión>-Setup.exe. No pide permisos de administrador: se instala para el
; usuario actual, crea accesos directos y queda en "Agregar o quitar programas".
;
; Uso:  iscc /DVersion=2.1.0 /DOrigen=..\dist\DentaVista /DSalida=..\dist build\instalador.iss

#ifndef Version
  #define Version "2.1.0"
#endif
#ifndef Origen
  #define Origen "..\dist\DentaVista"
#endif
#ifndef Salida
  #define Salida "..\dist"
#endif

[Setup]
AppId={{6E1B0A52-4C7F-4D1E-9A2B-DE47A0C3F5B1}
AppName=DentaVista
AppVersion={#Version}
AppVerName=DentaVista {#Version}
AppPublisher=EREJM · DentaVista
AppCopyright=© 2026 EREJM (Everson Ricardo, Edilson Joel, Michael)
AppPublisherURL=https://github.com/EdilsonJoel/DentaVista
AppSupportURL=https://github.com/EdilsonJoel/DentaVista/issues
DefaultDirName={localappdata}\Programs\DentaVista
DefaultGroupName=DentaVista
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir={#Salida}
OutputBaseFilename=DentaVista-{#Version}-Setup
SetupIconFile=icono.ico
UninstallDisplayIcon={app}\DentaVista.exe
UninstallDisplayName=DentaVista
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "escritorio"; Description: "Crear un acceso directo en el escritorio"; GroupDescription: "Accesos directos:"

[Files]
Source: "{#Origen}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\DentaVista"; Filename: "{app}\DentaVista.exe"
Name: "{group}\Desinstalar DentaVista"; Filename: "{uninstallexe}"
Name: "{userdesktop}\DentaVista"; Filename: "{app}\DentaVista.exe"; Tasks: escritorio

[Run]
Filename: "{app}\DentaVista.exe"; Description: "Abrir DentaVista"; Flags: nowait postinstall skipifsilent

; La base de datos (%LOCALAPPDATA%\DentaVista) no se borra al desinstalar: son datos clínicos.
