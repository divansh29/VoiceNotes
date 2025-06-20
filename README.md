# VoiceNotes - AI-Powered Voice Recording App

A modern Android app built with Kotlin that records voice memos and automatically generates titles, summaries, and key points using AI processing.

## Features

- 🎤 **High-Quality Audio Recording** - Record voice memos with clear audio quality
- 🤖 **AI-Powered Processing** - Automatic title generation, summarization, and key point extraction
- 📱 **Modern UI** - Clean, intuitive interface built with Jetpack Compose
- 💾 **Local Storage** - All recordings stored locally with Room database
- ▶️ **Playback Controls** - Play, pause, and manage your recordings
- 🔒 **Privacy-First** - No cloud storage, all data stays on your device

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Audio**: MediaRecorder & MediaPlayer
- **AI Processing**: Mock implementation (easily replaceable with real AI services)
- **Minimum SDK**: Android 21 (Android 5.0)
- **Target SDK**: Android 34

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 8 or higher
- Android SDK with API level 21+

### 1. Clone and Open Project
```bash
# If using git
git clone <repository-url>
cd voicenotes

# Open in Android Studio
# File -> Open -> Select the voicenotes folder
```

### 2. Sync Project
- Android Studio will automatically detect the Gradle project
- Click "Sync Now" when prompted
- Wait for Gradle sync to complete

### 3. Build the Project
```bash
# Using Android Studio
# Build -> Make Project (Ctrl+F9)

# Or using command line
./gradlew build
```

## Demo Instructions

### Option 1: Android Emulator (Recommended for Testing)

1. **Create an Emulator**:
   - Tools -> AVD Manager
   - Create Virtual Device
   - Choose a device (e.g., Pixel 6)
   - Select API level 30+ (recommended)
   - Finish setup

2. **Run the App**:
   - Select your emulator from the device dropdown
   - Click the green "Run" button or press Shift+F10
   - Grant microphone permission when prompted

3. **Test Features**:
   - Tap the microphone button to start recording
   - Speak for 10-30 seconds
   - Tap stop to end recording
   - Watch as the app processes and generates title/summary
   - Tap play to listen to your recording

### Option 2: Physical Device

1. **Enable Developer Options**:
   - Go to Settings -> About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings -> Developer Options
   - Enable "USB Debugging"

2. **Connect Device**:
   - Connect your Android device via USB
   - Allow USB debugging when prompted
   - Device should appear in Android Studio

3. **Install and Run**:
   - Select your device from the dropdown
   - Click "Run" to install and launch the app

### Option 3: Generate Debug APK

1. **Build APK**:
   ```bash
   # Using Android Studio
   # Build -> Build Bundle(s) / APK(s) -> Build APK(s)
   
   # Or using command line
   ./gradlew assembleDebug
   ```

2. **Locate APK**:
   - APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

3. **Install APK**:
   ```bash
   # Using ADB
   adb install app/build/outputs/apk/debug/app-debug.apk
   
   # Or transfer to device and install manually
   # (Enable "Install from Unknown Sources" in device settings)
   ```

## App Usage

1. **First Launch**:
   - Grant microphone permission when prompted
   - The main screen will show the recording interface

2. **Recording**:
   - Tap the blue microphone button to start recording
   - The button will turn red and animate while recording
   - Tap the stop button to end recording

3. **AI Processing**:
   - After stopping, the app will show "Processing..."
   - AI will generate a title, summary, and key points
   - This takes a few seconds

4. **Managing Recordings**:
   - All recordings appear in a list below the recording button
   - Tap "Play" to listen to a recording
   - Tap the delete icon to remove a recording
   - View generated summaries and key points for each recording

## Architecture Overview

```
app/
├── src/main/java/com/voicenotes/app/
│   ├── data/                 # Room database, entities, DAOs
│   ├── repository/           # Data repository layer
│   ├── audio/               # Audio recording and playback
│   ├── ai/                  # AI processing service
│   ├── viewmodel/           # ViewModels for UI state
│   ├── ui/
│   │   ├── components/      # Reusable UI components
│   │   ├── screens/         # Main app screens
│   │   └── theme/           # Material Design theme
│   └── MainActivity.kt      # Main activity
└── src/main/res/            # Resources (layouts, strings, etc.)
```

## Customization

### Adding Real AI Integration

The current implementation uses mock AI processing. To integrate real AI services:

1. **Replace AIService.kt** with actual API calls:
   - OpenAI Whisper for speech-to-text
   - OpenAI GPT for summarization
   - Google Speech-to-Text API
   - Azure Cognitive Services

2. **Add API dependencies** in `build.gradle.kts`:
   ```kotlin
   implementation("com.squareup.retrofit2:retrofit:2.9.0")
   implementation("com.squareup.retrofit2:converter-gson:2.9.0")
   ```

3. **Update permissions** if needed for internet access

### Styling Customization

- Modify colors in `app/src/main/res/values/colors.xml`
- Update theme in `app/src/main/java/com/voicenotes/app/ui/theme/`
- Customize UI components in `app/src/main/java/com/voicenotes/app/ui/components/`

## Troubleshooting

### Common Issues

1. **Build Errors**:
   - Ensure you have the latest Android Studio
   - Check that all dependencies are properly synced
   - Clean and rebuild: Build -> Clean Project, then Build -> Rebuild Project

2. **Permission Issues**:
   - Make sure microphone permission is granted
   - Check that the device has a working microphone

3. **Audio Recording Issues**:
   - Test on a physical device (emulator audio can be unreliable)
   - Ensure no other apps are using the microphone

4. **APK Installation Issues**:
   - Enable "Install from Unknown Sources" in device settings
   - Use `adb install -r app-debug.apk` to replace existing installation

## Release Preparation

For production release:

1. **Update version** in `app/build.gradle.kts`
2. **Add signing configuration** for release builds
3. **Enable ProGuard/R8** for code obfuscation
4. **Test thoroughly** on multiple devices and Android versions
5. **Generate signed APK** or App Bundle for Play Store

## License

This project is created for demonstration purposes. Feel free to use and modify as needed.
