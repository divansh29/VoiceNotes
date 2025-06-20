@echo off
echo ========================================
echo    VoiceNotes - Build Verification
echo ========================================
echo.

echo 🔍 Checking project structure...
if not exist "app\build.gradle.kts" (
    echo ❌ build.gradle.kts not found!
    echo Make sure you're in the project root directory.
    pause
    exit /b 1
)
echo ✅ Project structure looks good

echo.
echo 🔍 Checking key source files...
set FILES_OK=1

if not exist "app\src\main\java\com\voicenotes\app\MainActivity.kt" (
    echo ❌ MainActivity.kt missing
    set FILES_OK=0
)

if not exist "app\src\main\java\com\voicenotes\app\viewmodel\VoiceNotesViewModel.kt" (
    echo ❌ VoiceNotesViewModel.kt missing
    set FILES_OK=0
)

if not exist "app\src\main\java\com\voicenotes\app\audio\AudioRecorder.kt" (
    echo ❌ AudioRecorder.kt missing
    set FILES_OK=0
)

if not exist "app\src\main\java\com\voicenotes\app\cloud\GoogleDriveService.kt" (
    echo ❌ GoogleDriveService.kt missing
    set FILES_OK=0
)

if %FILES_OK%==1 (
    echo ✅ All key source files present
) else (
    echo ❌ Some source files are missing
    pause
    exit /b 1
)

echo.
echo 🔍 Checking Android SDK...
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
if exist "%ANDROID_HOME%" (
    echo ✅ Android SDK found: %ANDROID_HOME%
    set PATH=%ANDROID_HOME%\platform-tools;%PATH%
) else (
    echo ❌ Android SDK not found!
    echo Please install Android Studio first.
    pause
    exit /b 1
)

echo.
echo 🔍 Checking for connected devices...
adb devices
echo.

echo 🔨 Ready to build! Choose an option:
echo.
echo 1. Build APK now
echo 2. Just exit (I'll build manually)
echo.
set /p choice="Enter choice (1-2): "

if "%choice%"=="1" (
    echo.
    echo 🔨 Building APK...
    call build-apk.bat
) else (
    echo.
    echo 👍 You can build manually using:
    echo    - Android Studio: Build → Build APK
    echo    - Command line: gradlew assembleDebug
    echo    - Or run: build-apk.bat
)

echo.
echo ✅ Build verification complete!
pause
