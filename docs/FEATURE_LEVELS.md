# WandasPhone Feature Levels

## Overview

WandasPhone uses a tiered feature system based on **interaction complexity**, allowing carers to match phone functionality to the user's cognitive and motor abilities. Each level adds more complex interaction patterns while maintaining core safety guarantees.

**Key Principle**: Levels are defined by *how* the user interacts, not just *what* features are available. This accommodates users whose abilities vary throughout the day.

---

## Contact Types (All Levels)

WandasPhone distinguishes between different relationship types:

| Type | Description | Call Out | Answer | Missed Nag | L1 | L2 | L3 | L4 |
|------|-------------|:--------:|:------:|:----------:|:--:|:--:|:--:|:--:|
| **Carer** | Primary caregivers (family/professional) | ✓ | ✓ | ✓ | 2 | 4 | 6 | ∞ |
| **Grey List** | Friends/family who know to contact carer | ✗ | ✓ | ✗ | 10 | 20 | ∞ | ∞ |
| **Unknown** | Not in any list | ✗ | ✗ | ✗ | - | - | - | - |

**Carer contacts**: Full privileges - user can call them, missed calls trigger nagging reminders
**Grey list contacts**: Answer only - user can receive but not initiate, no missed call pressure
**Unknown callers**: Silent rejection - phone doesn't ring, caller hears nothing or voicemail

---

## The Interaction Complexity Model

| Level | Interaction Pattern | Cognitive Load |
|-------|---------------------|----------------|
| **L1** | One-touch = immediate action | Minimal - no decisions |
| **L2** | + Toggle controls (on/off) | Low - binary choices |
| **L3** | + Two-touch navigation | Medium - sequence of actions |
| **L4** | + Text input | Higher - but still contained |

**All levels maintain**:
- Automatic timeout to home screen
- One-touch carer access from anywhere
- Kiosk mode (no system escape)
- Self-recovering behavior
- Missed call nagging for carer contacts

---

## Self-Recovering Safety Model

### The Core Problem

User capability fluctuates due to:
- Fatigue / drowsiness
- Medication effects
- Time of day
- Cognitive episodes
- Stress or confusion

**The phone must never become inoperable.** Even if a user gets confused mid-interaction, the phone will automatically return to a safe, simple state.

### Safety Mechanisms (All Levels)

| Mechanism | Behavior | Configurable |
|-----------|----------|--------------|
| **Inactivity Timeout** | Any screen → Home after X seconds | 30s - 5min |
| **Call End → Home** | After call ends, return to Home | Always on |
| **App Close → Home** | If any app closes/crashes, return to Home | Always on |
| **Carer Button** | Visible on every screen, one tap to call | Position, size |
| **Emergency Access** | Long-press (5s) anywhere → Emergency call | Duration |
| **End Call Button** | Always visible and large during calls | Always on |

### The Home Screen Promise

From the Home screen, the user can **always**:
1. See a large, clear clock
2. Tap once to reach their primary carer/contact
3. Hear a spoken greeting and instruction

This is the "safe harbor" the phone returns to automatically.

---

## Level 1: One-Touch (MVP)

**Target Users**: Maximum simplicity needed, cognitive impairment, emergency-only phone

**Real-world example**: Wanda - elderly user with two family carers and a grey list of friends who know to contact carers if needed.

### Contact Types

| Type | Max | Can Call Out | Can Answer | Missed Call Nag | Ringtone |
|------|-----|:------------:|:----------:|:---------------:|----------|
| **Carer** | 2 | ✓ (1 touch) | ✓ (1 touch) | ✓ (repeats) | "Wanda, that's your phone" |
| **Grey List** | ~10 | ✗ | ✓ (1 touch) | ✗ | Personalized ring |
| **Unknown** | - | ✗ | ✗ (silent) | ✗ | No ring |

### Home Screen Layout

```
┌─────────────────────────────────────────┐
│                                         │
│  "Tap a name to call them"              │  ← Simple instruction text
│                                         │
│  ┌─────────────────────────────────┐    │
│  │                                 │    │
│  │         [PHOTO]                 │    │  ← Carer 1 button
│  │          Sarah                  │    │     1 touch = call
│  │                                 │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │                                 │    │
│  │         [PHOTO]                 │    │  ← Carer 2 button
│  │          John                   │    │     1 touch = call
│  │                                 │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌───────────┐          ┌───────────┐   │
│  │ EMERGENCY │          │  [hidden] │   │  ← Emergency: 3 taps
│  │  3 taps   │          │  7 taps   │   │  ← Settings: 7 taps
│  └───────────┘          └───────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

### Interaction Model

| User Action | Result |
|-------------|--------|
| Tap carer button (1x) | Calls that carer immediately, speakerphone |
| Tap emergency (3x quickly) | Calls 999/911 |
| Tap settings area (7x quickly) | Opens carer PIN entry |
| Incoming carer call + 1 tap | Answers in speakerphone |
| Incoming grey list + 1 tap | Answers in speakerphone |
| Incoming unknown | Silent, no ring, rejected |

### Calling Out (Carer Only)

1. User taps carer button (single touch)
2. TTS immediately: "Calling Sarah"
3. Call placed in speakerphone mode
4. Call connects → TTS: "Sarah answered"
5. Call ends → TTS: "Call ended" → return to home

### Incoming Calls - Carer

1. Carer calls Wanda
2. **Ringtone plays with TTS**: "Wanda, that's your phone ringing" (classic ring)
3. Screen shows carer photo and name
4. User taps anywhere → answers in speakerphone (settable volume)
5. OR auto-answer after N rings (optional setting)

### Incoming Calls - Grey List

1. Grey list friend calls
2. **Personalized ringtone** (can be same ring, different TTS: "[Name] is calling")
3. Screen shows caller photo and name
4. User taps anywhere → answers in speakerphone
5. NO missed call nagging if not answered
6. NO call-back button (grey list cannot be called)

### Incoming Calls - Unknown

1. Unknown number calls
2. **Silent** - no ring, no screen change
3. Call rejected automatically
4. Optional: Voicemail message directs caller to contact carer

### Missed Call Nagging (Carer Only)

If user misses a call from a **carer**:

1. **Tannoy sound**: Classic "bing-bong" attention sound
2. **TTS message**: "Wanda, you missed a call from Sarah. Please call Sarah now."
3. Home screen shows visual indicator
4. **Repeats** every [settable] minutes (e.g., 5 min)
5. Continues until:
   - User calls carer back, OR
   - Carer calls again and user answers, OR
   - Carer dismisses remotely (Phase 2)

### TTS Scripts (User Name: Configurable)

| Event | Message |
|-------|---------|
| App launch | "Hello [UserName]. Tap a name to call them." |
| Carer button tap | "Calling [CarerName]." |
| Call connected | "[CarerName] answered." |
| Call ended | "Call ended." (return to home) |
| Carer incoming (ring) | "[UserName], that's your phone ringing." |
| Grey list incoming | "[FriendName] is calling." |
| Call answered | "Hello." (or silent) |
| Missed call (carer) | "[UserName], you missed a call from [CarerName]. Please call [CarerName] now." |
| Missed call repeat | Same message, every N minutes |
| Low battery | "Battery is low. Please charge the phone." |
| Charging | "The phone is charging." |

### In-Call Screen (Level 1)

**Design principle**: Screen is **completely blank/inert by default**, with only specific buttons active.

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│              Sarah                      │  ← Visual info only
│            Connected                    │    (name, time)
│             02:34                       │    NO TOUCH RESPONSE
│                                         │
│                                         │
│                                         │
│                                         │
│                                         │
│                                         │
│     ┌───────────────────────────┐       │
│     │                           │       │
│     │    END CALL (tap twice)   │       │  ← ONLY active button
│     │                           │       │
│     └───────────────────────────┘       │
│                                         │
└─────────────────────────────────────────┘

  Everything except END CALL = completely inert
  No contact photo button (would re-call)
  No carer buttons visible
  Touches anywhere else = ignored
```

**Why blank-by-default?**
- Prevents accidentally calling the same person again
- Prevents calling the other carer mid-call
- Pokes, forehead, palm touches do nothing
- Only deliberate END CALL tap registers

**Level 1 in-call - active elements:**

| Element | Active? | Protection |
|---------|:-------:|------------|
| End Call button | ✓ | Double-tap (configurable) |
| Emergency | ✓ | 3 taps anywhere |
| Everything else | ✗ | Completely inert |

**Physical protections:**
- **Proximity sensor**: Screen turns off when near face/forehead
- **Hardware buttons disabled**: Volume locked to carer preset
- **Wide inert border**: Edge touches ignored

### Carer Configuration

| Category | Setting | Options | Default |
|----------|---------|---------|---------|
| **User** | User name | Text | "Wanda" |
| **Contacts** | Carer 1 | Name, phone, photo | Required |
| | Carer 2 | Name, phone, photo | Optional |
| | Grey list | Up to 10 contacts | Optional |
| **Calls** | Speakerphone volume | 1-10 | 8 (loud) |
| | Auto-answer carers | Yes/No + ring count | No |
| | Missed call nag interval | 1-30 minutes | 5 min |
| **Touch** | Activation mode | On-press / On-release | On-press |
| | Minimum hold (tremor filter) | 0-500ms | 0ms |
| | Debounce interval | 100-1000ms | 300ms |
| | End call protection | Single/Double/Hold | Double tap |
| | Inert border width | 24-40dp | 28dp |
| **Safety** | Emergency number | 999/911/112 | Region |
| | Emergency tap count | 3-5 | 3 |
| | Settings tap count | 5-10 | 7 |
| | Inactivity timeout | 30s-5min | 2 min |
| **Audio** | Ringtone style | Classic/Modern | Classic |
| | TTS speed | Slow/Normal/Fast | Normal |

### What's NOT in Level 1

- Volume controls on screen (preset by carer)
- Speaker toggle (always speakerphone)
- Mute button
- Contact list navigation
- Calling grey list contacts
- Missed call list (just audio nag)
- Any text or typing

---

## Level 2: One-Touch + Toggles

**Target Users**: Can handle binary choices (on/off), need call controls, more contacts

### What's New in Level 2

| Feature | L1 | L2 |
|---------|----|----|
| Carer contacts | 2 | 4 |
| Grey list contacts | 10 | 20 |
| In-call speaker toggle | - | ✓ |
| In-call volume +/- | - | ✓ |
| In-call mute toggle | - | ✓ |

### Interaction Model

Everything from Level 1, plus **toggle controls** during calls.

| User Action | Result |
|-------------|--------|
| All Level 1 actions | Same behavior |
| Tap speaker button | Toggle speaker on/off |
| Tap volume up | Increase volume one step |
| Tap volume down | Decrease volume one step |
| Tap mute button | Toggle microphone mute |

### Home Screen (Level 2)

```
┌─────────────────────────────────────────┐
│                                         │
│  "Tap a name to call them"              │
│                                         │
│  ┌───────────────┐  ┌───────────────┐   │
│  │    [PHOTO]    │  │    [PHOTO]    │   │
│  │    Sarah      │  │    John       │   │  ← 2x2 grid
│  └───────────────┘  └───────────────┘   │    of carers
│                                         │
│  ┌───────────────┐  ┌───────────────┐   │
│  │    [PHOTO]    │  │    [PHOTO]    │   │
│  │    Emma       │  │    Mike       │   │
│  └───────────────┘  └───────────────┘   │
│                                         │
│  ┌───────────┐          ┌───────────┐   │
│  │ EMERGENCY │          │  [hidden] │   │
│  │  3 taps   │          │  7 taps   │   │
│  └───────────┘          └───────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

### In-Call Screen (Level 2)

**Same principle**: Screen is **blank/inert by default**, with specific control exceptions.

```
┌─────────────────────────────────────────┐
│                                         │
│              Sarah                      │  ← Visual info only
│            Connected                    │    NO TOUCH RESPONSE
│             02:34                       │
│                                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  │
│  │ SPEAKER │  │  MUTE   │  │  VOL +  │  │  ← ACTIVE (toggles)
│  │   ON    │  │  OFF    │  │         │  │    Single tap each
│  └─────────┘  └─────────┘  └─────────┘  │
│                                         │
│                           ┌─────────┐   │
│                           │  VOL -  │   │  ← ACTIVE
│                           └─────────┘   │
│                                         │
│     ┌───────────────────────────┐       │
│     │    END CALL (tap twice)   │       │  ← ACTIVE (protected)
│     └───────────────────────────┘       │
│                                         │
└─────────────────────────────────────────┘
```

**Level 2 in-call - active elements:**

| Element | Active? | Protection |
|---------|:-------:|------------|
| End Call button | ✓ | Double-tap |
| Speaker toggle | ✓ | Single tap |
| Mute toggle | ✓ | Single tap |
| Volume +/- | ✓ | Single tap |
| Emergency | ✓ | 3 taps anywhere |
| Everything else | ✗ | Inert |

Toggle buttons show clear **ON/OFF state** visually and via TTS.

### What's Still NOT Available

- Contact lists requiring scrolling
- Two-step navigation (tap category → tap item)
- Missed call list view
- Calling grey list contacts
- Any text or typing

### TTS Scripts (Additional)

| Event | Message |
|-------|---------|
| Speaker on | "Speaker on." |
| Speaker off | "Speaker off." |
| Mute on | "Muted." |
| Mute off | "Unmuted." |
| Volume change | "Volume [level]." |
| 4-carer grid shown | "Tap a photo to call." |

### Carer Configuration (Additional)

| Setting | Options | Default |
|---------|---------|---------|
| Contacts 1-4 | Name, phone, photo | Min 1 required |
| Show speaker toggle | Yes/No | Yes |
| Show volume controls | Yes/No | Yes |
| Show mute toggle | Yes/No | Yes |
| Default speaker mode | On/Off | Off |

---

## Level 3: Two-Touch Navigation

**Target Users**: Can follow simple sequences, understand "go here, then do that"

### Interaction Model

Everything from Level 2, plus **two-step navigation patterns**.

| User Action | Result |
|-------------|--------|
| All Level 2 actions | Same behavior |
| Tap "Contacts" | Shows contact list |
| Tap contact in list | Calls that contact |
| Tap "Missed Calls" | Shows missed call list |
| Tap missed call | Calls that person back |

### Additional Features

| Feature | Description |
|---------|-------------|
| **Up to 12 Contacts** | Scrollable contact list |
| **Missed Calls** | List of missed calls with callback |
| **Photo Gallery** | View photos (tap to advance) |
| **Simple SMS Reading** | TTS reads incoming messages |
| **SMS Quick Reply** | Tap preset reply to send |
| **Calendar Alerts** | Spoken reminders at scheduled times |

### Navigation Model

```
┌──────────────┐
│    HOME      │
│              │
│ [Clock]      │
│ [Carer Btn]  │◄────────────────────────┐
│              │     (timeout or tap)    │
│ [Contacts]   │─────┐                   │
│ [Missed]     │──┐  │                   │
│ [Photos]     │─┐│  │                   │
└──────────────┘ ││  │                   │
                 ││  ▼                   │
                 ││ ┌──────────────┐     │
                 ││ │  CONTACTS    │     │
                 ││ │              │     │
                 ││ │ [Contact 1]──│─────┼──► Call
                 ││ │ [Contact 2]  │     │
                 ││ │ [Contact 3]  │     │
                 ││ │   ...        │     │
                 ││ │              │     │
                 ││ │ [Back]───────│─────┘
                 ││ └──────────────┘
                 ││
                 │└► ┌──────────────┐
                 │   │ MISSED CALLS │
                 │   │              │
                 │   │ [Sarah 2:30] │────► Call back
                 │   │ [John 1:15]  │
                 │   │              │
                 │   │ [Back]───────│────► Home
                 │   └──────────────┘
                 │
                 └─► ┌──────────────┐
                     │   PHOTOS     │
                     │              │
                     │  [Photo 1]   │
                     │              │
                     │ Tap for next │
                     │ [Back]───────│────► Home
                     └──────────────┘
```

### TTS Scripts (Additional)

| Event | Message |
|-------|---------|
| Contacts list | "Your contacts. Tap a name to call." |
| Missed calls list | "You have [N] missed calls. Tap to call back." |
| SMS received | "Message from [Name]: [content]" |
| SMS reply sent | "Sent reply to [Name]." |
| Calendar reminder | "Reminder: [event]" |
| Photo gallery | "Tap to see the next photo." |
| Back button | "Back to home." |

### Carer Configuration (Additional)

| Setting | Options | Default |
|---------|---------|---------|
| Contacts 1-12 | Name, phone, photo | Min 1 required |
| Show missed calls | Yes/No | Yes |
| Show photo gallery | Yes/No | Yes |
| SMS reading | Yes/No | No |
| Quick reply messages | Up to 5 presets | "OK", "Call me" |
| Calendar source | None/Google | None |

---

## Level 4: Text Input

**Target Users**: Can type simple text, transitioning to/from regular phone, need more capability

### Interaction Model

Everything from Level 3, plus **text input** (still within kiosk).

| User Action | Result |
|-------------|--------|
| All Level 3 actions | Same behavior |
| Tap message compose | Opens keyboard for typing |
| Type and send | Sends SMS message |
| Search contacts | Type to filter contact list |

### Additional Features

| Feature | Description |
|---------|-------------|
| **Unlimited Contacts** | Full contact list with search |
| **SMS Compose** | Type and send messages |
| **Contact Search** | Type to filter contacts |
| **Simple App Launcher** | Access carer-approved apps |
| **Music Player** | Play/pause, next/previous |
| **Basic Settings** | Volume, brightness (simplified UI) |

### Still Kiosk-Locked

**Level 4 does NOT exit kiosk mode.** The user cannot:
- Access system settings
- Install/uninstall apps
- Access notification shade
- Leave WandasPhone

The app launcher only shows carer-approved apps, and those apps return to WandasPhone on close.

### SMS Compose Screen

```
┌─────────────────────────────────────────┐
│           MESSAGE TO SARAH              │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │                                   │  │
│  │  Type your message here...        │  │
│  │                                   │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌─────────┐            ┌───────────┐   │
│  │ CANCEL  │            │   SEND    │   │
│  └─────────┘            └───────────┘   │
│                                         │
│  ┌─────────────────────────────────────┐│
│  │  Q W E R T Y U I O P                ││
│  │   A S D F G H J K L                 ││
│  │    Z X C V B N M  ⌫                 ││
│  │  [123]  [SPACE]  [DONE]             ││
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘
```

### TTS Scripts (Additional)

| Event | Message |
|-------|---------|
| Compose opened | "Type your message. Tap send when done." |
| Message sent | "Message sent to [Name]." |
| Message cancelled | "Message cancelled." |
| App opened | "Opening [App name]." |
| Returning to Wanda | "Back to Wanda's Phone." |
| Music playing | "Now playing [song]." |
| Music paused | "Music paused." |

### Carer Configuration (Additional)

| Setting | Options | Default |
|---------|---------|---------|
| Contacts | Unlimited | - |
| SMS compose | Enabled/Disabled | Disabled |
| Allowed apps | Select from installed | None |
| App time limits | Per-app timeout | None |
| Music folder | Select folder | None |
| Search enabled | Yes/No | Yes |

---

## Feature Matrix by Level

| Feature | L1 | L2 | L3 | L4 |
|---------|:--:|:--:|:--:|:--:|
| **Carer Contacts** | 2 | 4 | 6 | ∞ |
| **Grey List Contacts** | 10 | 20 | ∞ | ∞ |
| **Interaction** | One-touch | +Toggles | +Two-touch | +Typing |
| | | | | |
| **Calling Out** | | | | |
| Call carer (1 touch) | ✓ | ✓ | ✓ | ✓ |
| Call grey list | - | - | ✓ | ✓ |
| | | | | |
| **Incoming Calls** | | | | |
| Answer carer | ✓ | ✓ | ✓ | ✓ |
| Answer grey list | ✓ | ✓ | ✓ | ✓ |
| Block unknown | ✓ | ✓ | ✓ | ✓ |
| | | | | |
| **In-Call Controls** | | | | |
| End call | ✓ | ✓ | ✓ | ✓ |
| Speaker toggle | - | ✓ | ✓ | ✓ |
| Volume controls | - | ✓ | ✓ | ✓ |
| Mute toggle | - | ✓ | ✓ | ✓ |
| | | | | |
| **Missed Calls** | | | | |
| Carer nag (repeating) | ✓ | ✓ | ✓ | ✓ |
| Missed call list | - | - | ✓ | ✓ |
| | | | | |
| **Navigation** | | | | |
| Contact list | - | - | ✓ | ✓ |
| Photo gallery | - | - | ✓ | ✓ |
| Contact search | - | - | - | ✓ |
| | | | | |
| **Messaging** | | | | |
| SMS reading (TTS) | - | - | ✓ | ✓ |
| Quick reply | - | - | ✓ | ✓ |
| SMS compose | - | - | - | ✓ |
| | | | | |
| **Other** | | | | |
| Calendar alerts | - | - | ✓ | ✓ |
| Music player | - | - | - | ✓ |
| App launcher | - | - | - | ✓ |
| Basic settings | - | - | - | ✓ |
| | | | | |
| **Safety (Always)** | | | | |
| Inactivity timeout | ✓ | ✓ | ✓ | ✓ |
| Emergency multi-tap | ✓ | ✓ | ✓ | ✓ |
| Kiosk mode | ✓ | ✓ | ✓ | ✓ |
| Missed carer nag | ✓ | ✓ | ✓ | ✓ |

---

## Emergency & Safety Features (All Levels)

Regardless of feature level, these are **always available**:

| Feature | Trigger | Behavior |
|---------|---------|----------|
| **Emergency Call** | 3 quick taps on emergency button | Calls 999/911/112 |
| **End Call** | Large button during calls | Ends current call |
| **Missed Carer Nag** | Missed call from carer | TTS reminder every N minutes |
| **Auto-Return Home** | Inactivity timeout | Returns to home screen |
| **Low Battery Alert** | Battery ≤10% | TTS warning, prompt to charge |
| **Speakerphone Default** | All calls | Auto-speakerphone at preset volume |

### Emergency Exceptions in Level 1

Even though Level 1 is "one-touch = call carer", these exceptions apply:

1. **End Call Button**: Always visible during calls (user must be able to hang up)
2. **Emergency Long-Press**: 5-second press calls emergency services
3. **Battery Warning**: Spoken alert with option to dismiss

---

## Level Transitions

### Upgrading (L1 → L2 → L3 → L4)

When carer increases feature level:
1. New UI elements appear immediately
2. TTS briefly introduces new capability
3. Existing data/settings preserved
4. Timeout behavior unchanged

### Downgrading (L4 → L3 → L2 → L1)

When carer decreases feature level:
1. Complex UI elements hidden immediately
2. No TTS announcement (avoid confusion)
3. Data preserved (messages, contacts still stored)
4. Automatic return to simpler home screen

### Automatic Level Adjustment (Future)

Potential future feature: Time-based level adjustment
- "Morning mode" at higher level
- "Evening mode" at lower level
- Based on historical usage patterns

---

## Implementation Notes

### Feature Flags

Each feature checks against current level:

```kotlin
enum class Feature(val requiredLevel: FeatureLevel) {
    // Level 1 - One touch
    PRIMARY_CONTACT(MINIMAL),
    AUTO_ANSWER(MINIMAL),
    CLOCK(MINIMAL),
    BATTERY_TTS(MINIMAL),
    
    // Level 2 - Toggles
    CONTACT_GRID_4(BASIC),
    SPEAKER_TOGGLE(BASIC),
    VOLUME_CONTROLS(BASIC),
    MUTE_TOGGLE(BASIC),
    
    // Level 3 - Two-touch
    CONTACT_LIST_12(STANDARD),
    MISSED_CALLS(STANDARD),
    PHOTO_GALLERY(STANDARD),
    SMS_READING(STANDARD),
    SMS_QUICK_REPLY(STANDARD),
    CALENDAR(STANDARD),
    
    // Level 4 - Text input
    CONTACT_UNLIMITED(EXTENDED),
    SMS_COMPOSE(EXTENDED),
    CONTACT_SEARCH(EXTENDED),
    APP_LAUNCHER(EXTENDED),
    MUSIC_PLAYER(EXTENDED)
}
```

### Timeout Implementation

```kotlin
class InactivityManager(
    private val timeoutSeconds: Int,
    private val onTimeout: () -> Unit
) {
    // Reset on any user interaction
    // Navigate to home on timeout
    // Exclude active call screen from timeout
}
```

### Safety-First UI

Every screen must include:
1. Clear path back to home (or auto-timeout)
2. Carer button visible (except during active call)
3. No dead-ends
4. Large, obvious touch targets
