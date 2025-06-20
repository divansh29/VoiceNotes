@echo off
color 0A
echo ========================================
echo    VoiceNotes - Setup and Test
echo ========================================
echo.

:MENU
echo Choose an option:
echo.
echo 1. 🔧 Setup Development Environment
echo 2. 🔨 Build APK
echo 3. 📱 Install on Device
echo 4. 🧪 Run Tests
echo 5. 🐛 Troubleshoot
echo 6. 📖 Open Testing Guide
echo 7. ❌ Exit
echo.
set /p choice="Enter your choice (1-7): "

if "%choice%"=="1" goto SETUP
if "%choice%"=="2" goto BUILD
if "%choice%"=="3" goto INSTALL
if "%choice%"=="4" goto TEST
if "%choice%"=="5" goto TROUBLESHOOT
if "%choice%"=="6" goto GUIDE
if "%choice%"=="7" goto EXIT
goto MENU

:SETUP
echo.
echo 🔧 Setting up development environment...
echo.
echo Step 1: Checking Android SDK...
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
if exist "%ANDROID_HOME%" (
    echo ✅ Android SDK found at: %ANDROID_HOME%
) else (
    echo ❌ Android SDK not found!
    echo Please install Android Studio from: https://developer.android.com/studio
    pause
    goto MENU
)

echo.
echo Step 2: Checking ADB...
adb version >nul 2>&1
if %errorlevel%==0 (
    echo ✅ ADB is working
) else (
    echo ❌ ADB not found in PATH
    echo Adding Android SDK to PATH...
    set PATH=%ANDROID_HOME%\platform-tools;%PATH%
)

echo.
echo Step 3: Checking connected devices...
adb devices
echo.
echo ✅ Setup complete! You can now build and install the app.
pause
goto MENU

:BUILD
echo.
echo 🔨 Building VoiceNotes APK...
echo.
call build-apk.bat
pause
goto MENU

:INSTALL
echo.
echo 📱 Installing VoiceNotes on device...
echo.
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ❌ APK not found! Please build the APK first.
    pause
    goto MENU
)

echo Checking for connected devices...
adb devices
echo.
echo Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel%==0 (
    echo ✅ Installation successful!
    echo 🚀 VoiceNotes is now installed on your device.
) else (
    echo ❌ Installation failed!
    echo Make sure:
    echo - USB debugging is enabled
    echo - Device is connected
    echo - Allow installation from unknown sources
)
pause
goto MENU

:TEST
echo.
echo 🧪 Running basic tests...
echo.
echo Test 1: Checking if app is installed...
adb shell pm list packages | find "com.voicenotes.app" >nul
if %errorlevel%==0 (
    echo ✅ App is installed
) else (
    echo ❌ App not found on device
    pause
    goto MENU
)

echo.
echo Test 2: Launching app...
adb shell am start -n com.voicenotes.app/.MainActivity
echo ✅ App launched (check your device)

echo.
echo Test 3: Checking permissions...
adb shell dumpsys package com.voicenotes.app | find "RECORD_AUDIO"
echo.

echo Manual tests to perform on device:
echo 1. 🎙️ Test recording (speak for 10+ seconds)
echo 2. 🔊 Test playback (should hear your voice)
echo 3. 📁 Test file upload
echo 4. ☁️ Test Google Drive integration
echo 5. 🔐 Test PIN protection
echo.
echo See TESTING_GUIDE.md for detailed instructions.
pause
goto MENU

:TROUBLESHOOT
echo.
echo 🐛 Troubleshooting VoiceNotes...
echo.
echo Common issues and solutions:
echo.
echo 1. Recording not working:
echo    - Check microphone permission
echo    - Try on physical device (not emulator)
echo    - Check device volume
echo.
echo 2. No sound during playback:
echo    - Check media volume (not ringtone)
echo    - Try with headphones
echo    - Ensure file was recorded properly
echo.
echo 3. Google Drive not working:
echo    - Check internet connection
echo    - Update Google Play Services
echo    - Clear app cache
echo.
echo 4. App crashes:
echo    - Check logs: adb logcat | grep VoiceNotes
echo    - Clear app data: adb shell pm clear com.voicenotes.app
echo.
echo 5. Installation fails:
echo    - Enable "Install from Unknown Sources"
echo    - Check storage space
echo    - Use: adb install -r app-debug.apk
echo.
pause
goto MENU

:GUIDE
echo.
echo 📖 Opening testing guide...
if exist "TESTING_GUIDE.md" (
    start TESTING_GUIDE.md
    echo ✅ Testing guide opened in your default editor
) else (
    echo ❌ TESTING_GUIDE.md not found in current directory
)
pause
goto MENU

:EXIT
echo.
echo 👋 Thanks for using VoiceNotes!
echo Happy testing! 🎉
pause
exit

