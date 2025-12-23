# WandasPhone Setup Guide

## Quick Start for Testing

### 1. Build and Install
```bash
cd /Users/ashleybenns/PhoneApp26
./gradlew installDebug
```

### 2. Grant Permissions
After installation, the app will request:
- Phone permissions (CALL_PHONE, READ_PHONE_STATE, etc.)
- **Grant all permissions**

### 3. Set as Default Dialer
1. Open Settings → Apps → Default apps → Phone app
2. Select "Wanda's Phone"

### 4. Add Test Contacts
1. Launch WandasPhone
2. Tap the clock 7 times rapidly
3. Create a PIN (any 4 digits, you'll use this to access carer settings)
4. Tap "Add Contact"
5. Enter:
   - Name: "Sarah" (or your test contact name)
   - Phone: "+1234567890" (or actual test number)
   - Mark as Primary if desired
6. Add a second contact for testing

### 5. Test Basic Calling
1. From home screen, tap a contact button
2. App should speak "Calling [Name]"
3. Call should be placed
4. In-call screen should appear
5. Tap "End Call" to hang up

## Kiosk Mode Setup (Production)

### Prerequisites
- A dedicated Android device (will be factory reset)
- Physical access to the device
- USB cable
- ADB installed on your computer

### Step-by-Step

#### 1. Factory Reset the Device
1. Go to Settings → System → Reset options → Erase all data (factory reset)
2. Confirm and wait for device to reset
3. **IMPORTANT**: During setup, skip the Google account sign-in
4. Complete basic setup (language, Wi-Fi, etc.)

#### 2. Enable Developer Options
1. Go to Settings → About phone
2. Tap "Build number" 7 times
3. Go back → System → Developer options
4. Enable "USB debugging"

#### 3. Connect via ADB
```bash
# Connect device via USB
adb devices

# If "unauthorized", check device for prompt and allow
```

#### 4. Install App
```bash
cd /Users/ashleybenns/PhoneApp26
./gradlew installDebug
```

#### 5. Set Device Owner
```bash
adb shell dpm set-device-owner com.wandasphone/.feature.kiosk.WandasDeviceAdminReceiver
```

Expected output:
```
Success: Device owner set to package com.wandasphone
Active admin set to component {com.wandasphone/com.wandasphone.feature.kiosk.WandasDeviceAdminReceiver}
```

#### 6. Grant Permissions
```bash
# Grant all phone permissions
adb shell pm grant com.wandasphone android.permission.CALL_PHONE
adb shell pm grant com.wandasphone android.permission.READ_PHONE_STATE
adb shell pm grant com.wandasphone android.permission.ANSWER_PHONE_CALLS
adb shell pm grant com.wandasphone android.permission.READ_CALL_LOG
adb shell pm grant com.wandasphone android.permission.WRITE_CALL_LOG
adb shell pm grant com.wandasphone android.permission.READ_CONTACTS
```

#### 7. Set as Default Dialer
```bash
adb shell cmd role add-role-holder android.app.role.DIALER com.wandasphone
```

#### 8. Reboot
```bash
adb reboot
```

After reboot:
- WandasPhone will auto-start
- Device will be in kiosk mode
- Home button, back button, recent apps will be disabled
- Only way to exit is via carer settings

## Removing Device Owner (for Uninstall)

### Option 1: Via Carer Settings (Preferred)
1. Access carer settings (7 taps on clock + PIN)
2. Scroll to bottom
3. Tap "Remove Device Owner"
4. Confirm
5. Go to Settings → Apps → WandasPhone → Uninstall

### Option 2: Via ADB
```bash
adb shell dpm remove-active-admin com.wandasphone/.feature.kiosk.WandasDeviceAdminReceiver
adb uninstall com.wandasphone
```

### Option 3: Factory Reset (Last Resort)
If above methods don't work, factory reset the device.

## Testing Checklist

### Basic Functionality
- [ ] App launches to home screen
- [ ] Clock updates every minute
- [ ] Contact buttons are visible and labeled
- [ ] Tapping contact initiates call
- [ ] TTS announces "Calling [Name]"
- [ ] In-call screen appears
- [ ] End call button works
- [ ] Returns to home after call

### Auto-Answer (if enabled)
- [ ] Incoming call rings
- [ ] TTS announces caller
- [ ] Call auto-answers after N rings
- [ ] Speaker is enabled
- [ ] Call quality is good

### Missed Call Reminders
- [ ] Miss a call from carer contact
- [ ] Attention sound plays
- [ ] TTS reminder plays: "Wanda, you missed a call from [Name], please call [Name]"
- [ ] Reminder repeats every N minutes
- [ ] Stops when user calls back

### Carer Access
- [ ] 7 taps on clock triggers PIN dialog
- [ ] First time: any 4-digit PIN is accepted and saved
- [ ] Subsequent: only correct PIN works
- [ ] Can view/edit contacts
- [ ] Can change feature level
- [ ] Can enable/disable auto-answer
- [ ] "Done" returns to home

### Kiosk Mode (if enabled)
- [ ] Home button does nothing
- [ ] Recent apps button does nothing
- [ ] Back button does nothing
- [ ] Can't pull down notification shade
- [ ] Can't access Settings
- [ ] Only WandasPhone is accessible
- [ ] Persists after reboot

## Troubleshooting

### "App is not default dialer"
- Go to Settings → Apps → Default apps → Phone app
- Select WandasPhone
- Or run: `adb shell cmd role add-role-holder android.app.role.DIALER com.wandasphone`

### "Missing permissions"
Grant manually or via ADB (see step 6 above)

### "Device owner not set"
- Ensure device was factory reset
- Ensure no Google account signed in
- Run the dpm command again

### TTS not working
- Go to Settings → Accessibility → Text-to-speech
- Ensure an engine is selected (Google TTS recommended)
- Test TTS output

### Calls not connecting
- Check SIM card is inserted and active
- Check phone plan allows calls
- Try calling from another phone to test incoming

### Can't exit kiosk mode
- Use carer settings to remove device owner first
- Or use ADB: `adb shell dpm remove-active-admin ...`
- Worst case: factory reset

## Contact

For issues or questions, refer to the main README or documentation in `/docs/`.

