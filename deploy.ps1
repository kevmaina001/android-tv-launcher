# Builds the release APK, installs it on the TV and makes it the home screen.
#
# Run:  powershell -ExecutionPolicy Bypass -File .\deploy.ps1 -Tv 192.168.1.50
#
# The TV needs network debugging on (see README). adb is taken from PATH unless -Adb is given,
# and the JDK from JAVA_HOME.
param(
    [Parameter(Mandatory = $true)] [string] $Tv,
    [string] $Adb = 'adb'
)

$ErrorActionPreference = 'Stop'
$pkg = 'com.kev.launcher'
if ($Tv -notmatch ':') { $Tv = "${Tv}:5555" }

& "$PSScriptRoot\gradlew.bat" -p $PSScriptRoot assembleRelease -q
if ($LASTEXITCODE -ne 0) { throw "Build failed" }

& $Adb connect $Tv | Out-Null
& $Adb -s $Tv install -r "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk"
# Sideloaded apps aren't optimised by the Play Store; AOT-compile so the launcher opens instantly.
& $Adb -s $Tv shell cmd package compile -m speed -f $pkg
# Continue watching reads other apps' rows from the TV provider. The app also asks on first run,
# but some TVs hide that prompt, so grant it here too (harmless if already granted).
& $Adb -s $Tv shell pm grant $pkg android.permission.READ_TV_LISTINGS
& $Adb -s $Tv shell cmd package set-home-activity "$pkg/.MainActivity"
& $Adb -s $Tv shell input keyevent KEYCODE_HOME
