@echo off
echo ========================================
echo    VoiceNotes APK Builder
echo ========================================
echo.

REM Check if Android Studio is installed
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
if not exist "%ANDROID_HOME%" (
    echo ❌ Android SDK not found!
    echo Please install Android Studio first.
    echo Download from: https://developer.android.com/studio
    pause
    exit /b 1
)

REM Set up environment
set PATH=%ANDROID_HOME%\platform-tools;%PATH%
set PATH=%ANDROID_HOME%\build-tools\34.0.0;%PATH%

echo 🔧 Using Android SDK: %ANDROID_HOME%
echo.

REM Check if device is connected
echo 📱 Checking for connected devices...
adb devices
echo.

REM Try to build using Android Studio's Gradle
set GRADLE_PATH=%ANDROID_HOME%\..\..\.gradle\wrapper\dists
if exist "%GRADLE_PATH%" (
    echo 🔨 Building APK using system Gradle...
    "%ANDROID_HOME%\..\..\.gradle\wrapper\dists\gradle-8.2-bin\*\gradle-8.2\bin\gradle.bat" assembleDebug
) else (
    echo 🔨 Building APK using local Gradle...
    if exist "gradlew.bat" (
        call gradlew.bat assembleDebug
    ) else (
        echo ❌ No Gradle wrapper found!
        echo Please open project in Android Studio first.
        pause
        exit /b 1
    )
)

REM Check if build was successful
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ✅ Build successful!
    echo 📦 APK location: app\build\outputs\apk\debug\app-debug.apk
    echo 📏 APK size:
    dir "app\build\outputs\apk\debug\app-debug.apk" | find "app-debug.apk"
    echo.
    echo 📱 To install on connected device:
    echo    adb install app\build\outputs\apk\debug\app-debug.apk
    echo.
    set /p install="Install now? (y/n): "
    if /i "%install%"=="y" (
        echo 📲 Installing APK...
        adb install -r app\build\outputs\apk\debug\app-debug.apk
        if %errorlevel%==0 (
            echo ✅ Installation successful!
            echo 🚀 You can now test the app on your device.
        ) else (
            echo ❌ Installation failed!
            echo Make sure USB debugging is enabled.
        )
    )
) else (
    echo.
    echo ❌ Build failed!
    echo Check the output above for errors.
    echo.
    echo Common solutions:
    echo 1. Open project in Android Studio first
    echo 2. Let Android Studio download dependencies
    echo 3. Build once in Android Studio
    echo 4. Then try this script again
)

echo.
pause
