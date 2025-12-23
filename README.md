# WandasPhone (Phase 1 MVP)

A simplified Android phone app designed for seniors, people with disabilities, children, or anyone wanting a simple communication device.

**Core Principle**: "Give instructions, not choices" - provide directive guidance instead of complex menus.

## Features Implemented (Phase 1 MVP)

### ✅ Level 1 Interface (Minimal)
- Clock display
- 2 carer contact buttons (tap to call)
- Emergency button (3-tap protection)
- Auto-answer incoming calls
- Repeating TTS reminders for missed calls
- Kiosk mode (prevents accidental exit)

### ✅ Core Systems
- **4 High-Contrast Themes**:
  - High Contrast Light (default)
  - High Contrast Dark
  - Yellow on Black (best for aging eyes)
  - Soft Contrast
- **TTS Announcements**: All actions spoken
- **Feature Level System**: 4 levels (1-4) for progressive complexity
- **Carer Configuration**: PIN-protected settings
- **Repository Pattern**: Ready for Phase 2 cloud sync

### ✅ Accessibility
- WCAG AAA contrast ratios (7:1 minimum)
- Large touch targets (96dp+)
- Inert border (dead zone) prevents accidental touches
- Sans-serif fonts, 24sp+ for buttons
- TalkBack compatible

### ✅ Phone Capabilities
- InCallService for call handling
- CallScreeningService for auto-answer
- Call logging
- Contact management
- Speaker phone default

## Project Structure

```
PhoneApp26/
├── app/                          # Main app module
├── core/
│   ├── core-ui/                 # Theme, colors, components
│   ├── core-tts/                # Text-to-speech
│   ├── core-config/             # Feature levels, settings
│   ├── core-data/               # Room database, repositories
│   └── core-telecom/            # Call management
└── feature/
    ├── feature-home/            # Home screen
    ├── feature-phone/           # In-call UI
    ├── feature-carer/           # Carer settings
    └── feature-kiosk/           # Kiosk mode
```

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or later
- Android device running API 26+ (Android 8.0+)
- ADB installed and in PATH

### Building
```bash
./gradlew assembleDebug
```

### Installation
```bash
./gradlew installDebug
```

### Setting up as Default Dialer
1. Install the app
2. Go to Settings → Apps → Default apps → Phone app
3. Select "Wanda's Phone"

### Setting up Kiosk Mode (Device Owner)
**WARNING**: This will factory reset the device!

1. Factory reset the device (Settings → System → Reset)
2. During setup, **skip** Google account sign-in
3. Connect device via USB with ADB debugging enabled
4. Run:
   ```bash
   adb shell dpm set-device-owner com.wandasphone/.feature.kiosk.WandasDeviceAdminReceiver
   ```
5. Reboot device
6. WandasPhone will start automatically and enable kiosk mode

### Adding Contacts (via Carer Access)
1. Tap the clock 7 times rapidly
2. Enter PIN (first time: create any 4-digit PIN)
3. Add contacts with names and phone numbers
4. Set one as primary

## Phase 2 Roadmap (Not Yet Implemented)

- [ ] Level 2: In-call controls (speaker, mute, volume)
- [ ] Level 2: 4-contact grid on home
- [ ] Level 3: Contact list (up to 12)
- [ ] Level 3: Missed calls list
- [ ] Video calling (Jitsi/Twilio)

## Phase 3 Roadmap (Not Yet Implemented)

- [ ] Firebase backend
- [ ] Remote carer access via web portal
- [ ] Cloud sync (contacts, settings, call logs)

## Design Documentation

See `/docs/` folder for:
- `DESIGN_PRINCIPLES.md` - Core philosophy
- `ARCHITECTURE.md` - Technical details
- `FEATURE_LEVELS.md` - Level system

## License

[Choose appropriate license]

## Credits

Built for Wanda, who taught us that "instructions are easier to follow than choices."

