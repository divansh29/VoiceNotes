#!/bin/bash

echo "Building VoiceNotes Debug APK..."
echo

# Clean the project
echo "Cleaning project..."
./gradlew clean

# Build debug APK
echo "Building debug APK..."
./gradlew assembleDebug

# Check if build was successful
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo
    echo "✅ Build successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo
    echo "To install on connected device, run:"
    echo "adb install app/build/outputs/apk/debug/app-debug.apk"
else
    echo
    echo "❌ Build failed! Check the output above for errors."
fi
