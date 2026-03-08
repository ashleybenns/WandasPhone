#!/usr/bin/env bash
# Build and install PhoneApp26 on all attached devices.
set -e
cd "$(dirname "$0")"
echo "Building latest debug APK..."
./gradlew assembleDebug --no-daemon -q
APK=app/build/outputs/apk/debug/app-debug.apk
DEVICES=$(adb devices | grep -w device | awk '{print $1}')
COUNT=$(echo "$DEVICES" | grep -c . || true)
if [ "$COUNT" -eq 0 ]; then
  echo "No devices attached. Connect devices with USB debugging enabled and run again."
  exit 1
fi
echo "Installing on $COUNT device(s)..."
for device in $DEVICES; do
  echo "  -> $device"
  adb -s "$device" install -r "$APK"
done
echo "Done."
