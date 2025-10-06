@echo off
echo Building Hotspot Manager APK...
echo.
echo Make sure you have Android SDK installed and ANDROID_HOME environment variable set.
echo.

cd /d "%~dp0"

echo Running Gradle build...
gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ APK built successfully!
    echo Location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To install on your phone:
    echo 1. Copy app-debug.apk to your phone
    echo 2. Enable "Install from unknown sources" in phone settings
    echo 3. Tap the APK file to install
) else (
    echo.
    echo ✗ Build failed. Make sure Android SDK is properly installed.
)

pause