# WandasPhone Design Principles

## Core Philosophy: "Give Instructions, Not Choices"

WandasPhone is built on a fundamental principle: **the phone tells the user what to do, not the other way around**. This inverts the typical smartphone paradigm where users navigate complex menus and make countless decisions.

### Why This Matters

Traditional smartphones overwhelm users with:
- Multiple ways to accomplish the same task
- Settings buried in nested menus
- Notifications demanding attention
- Visual clutter competing for focus

WandasPhone eliminates this cognitive burden by:
- Speaking clear, simple instructions via TTS
- Presenting one action per screen
- Hiding all configuration from the user
- Providing immediate audio feedback for every interaction

---

## Self-Recovering Design

### The Core Problem

User capability fluctuates throughout the day due to:
- Fatigue / drowsiness
- Medication effects
- Time of day
- Cognitive episodes
- Stress or confusion

**The phone must never become inoperable.** Even if a user gets confused mid-interaction, the phone automatically returns to a safe, simple state.

### The Home Screen Promise

The home screen is the "safe harbor." From here, the user can **always**:
1. See a large, clear clock
2. Tap once to reach their primary carer/contact
3. Hear a spoken greeting and instruction

This is where the phone returns to automatically.

### Recovery Mechanisms

| Mechanism | Behavior | Purpose |
|-----------|----------|---------|
| **Inactivity Timeout** | Any screen → Home after N seconds | User walked away or got confused |
| **Call End → Home** | After call ends, return to Home | No stranded post-call UI |
| **App Close → Home** | If any app closes/crashes, return to Home | No dead-ends |
| **Back = Home** | Any back navigation goes home | Simple mental model |

### Configuration by Carer

- Timeout duration: 30 seconds to 5 minutes
- Timeout disabled during active calls
- Optional TTS reminder when returning home

---

## Default Phone Replacement

WandasPhone is designed as a **complete replacement** for the Android phone app, not a companion app. This means:

- WandasPhone handles all incoming calls
- WandasPhone provides the in-call UI
- No stock phone app appears
- Full control over auto-answer behavior
- Consistent, simplified experience

### Why Full Replacement?

A companion app approach would mean:
- Stock phone UI appearing unexpectedly
- User confusion when different UI appears
- Kiosk mode broken by stock dialer
- Less reliable auto-answer

Full replacement ensures:
- Predictable, consistent experience
- Kiosk mode integrity
- Complete audio feedback control
- No UI surprises

---

## Audio-First Design

### Text-to-Speech (TTS) is Primary

Every screen, every interaction, every state change is accompanied by spoken audio:

| Event | TTS Response |
|-------|--------------|
| App opens | "Hello. Tap the screen to call [Name]." |
| Contact tapped | "Calling [Name]." |
| Call connected | "[Name] answered." |
| Call ended | "Call ended. Tap to call [Name]." |
| Low battery | "Battery is low. Please charge the phone." |
| Incoming call | "[Name] is calling. Answering now." |
| Returning home | (Optional) "[Name] is here to help." |

### Audio Design Rules

1. **Speak immediately** - No delays, no waiting for animations
2. **Use simple language** - Short sentences, common words
3. **Name people** - Always say who is calling/being called
4. **Confirm actions** - Every tap gets verbal confirmation
5. **No jargon** - Never say "swipe", "menu", "settings", or technical terms

### Silence is Intentional

The phone should be quiet when idle. Background music, notification sounds, and ambient audio are disabled. The only sounds are:
- Incoming call ringtone
- TTS spoken instructions
- Call audio

---

## Interaction Complexity Levels

WandasPhone uses a tiered system based on **interaction complexity**, not feature count. Each level adds more complex interaction patterns.

### Level 1: One-Touch

**Every tap causes an immediate action.** No navigation, no selection, no decisions.

- Tap screen → calls primary contact
- Incoming call → auto-answers
- That's it

**Best for**: Maximum simplicity needed, severe cognitive impairment

### Level 2: One-Touch + Toggles

Everything from Level 1, plus **binary controls** (on/off).

- Tap speaker button → toggle speaker
- Tap volume +/- → adjust volume
- Tap mute → toggle mute

**Best for**: Can handle on/off choices, need call controls

### Level 3: Two-Touch Navigation

Everything from Level 2, plus **simple sequences**.

- Tap "Contacts" → see list → tap contact
- Tap "Missed Calls" → see list → tap to call back

**Best for**: Can follow "go here, then do that" instructions

### Level 4: Text Input

Everything from Level 3, plus **typing** (still in kiosk).

- Compose SMS messages
- Search contacts

**Best for**: Transitioning to/from regular phone, need more capability

### Universal Safety (All Levels)

Regardless of level, these are always present:
- Inactivity timeout returns to home
- Carer button visible on every screen
- Emergency long-press (5 seconds)
- Kiosk mode active (even Level 4)

---

## Visual Design Principles

### Massive Touch Targets

Minimum touch target sizes:

| Element | Minimum Size | Preferred Size |
|---------|--------------|----------------|
| Primary action button | 72dp | 120dp+ |
| Contact photo | 96dp | 150dp+ |
| Secondary elements | 48dp | 72dp |
| In-call controls | 72dp | 96dp |

---

## Accidental Activation Prevention

### The Problem

Real users interact with phones in unexpected ways:
- Holding phone to forehead during calls
- Poking screen to emphasise points while talking
- Large fingers curling onto frameless screen edges
- Accidentally pressing hardware buttons
- Palm/cheek touches during calls

**The phone must not respond to accidental touches.**

### Hardware Button Lockout

All physical buttons are disabled except the power/standby button:

| Button | Status | Reason |
|--------|--------|--------|
| Volume Up/Down | **Disabled** | Carer sets volume; accidental press disrupts call |
| Home button | **Disabled** | Kiosk mode; would exit app |
| Back button | **Disabled** | Kiosk mode; confusing navigation |
| Recent apps | **Disabled** | Kiosk mode; would expose system |
| Power/Standby | **Active** | Needed to wake phone |

Volume is **carer-preset** and cannot be changed by user (L1) or only via deliberate on-screen toggle (L2+).

### Wide Inert Border

All screens have a **dead zone** around the edges where touches are ignored:

```
┌─────────────────────────────────────────┐
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
│░░                                     ░░│
│░░  ┌───────────────────────────────┐  ░░│
│░░  │                               │  ░░│
│░░  │      ACTIVE TOUCH AREA        │  ░░│  ← 24-32dp inert
│░░  │                               │  ░░│    border all sides
│░░  │                               │  ░░│
│░░  └───────────────────────────────┘  ░░│
│░░                                     ░░│
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
└─────────────────────────────────────────┘
  ░ = Inert border (touches ignored)
```

**Border width**: 24-32dp minimum (configurable by carer for larger hands)

This prevents:
- Fingers curling onto screen edges
- Palm touches from grip
- Accidental edge swipes

### In-Call Touch Protection

**Design principle**: During calls, screen is **completely inert by default** with explicit exceptions for specific buttons.

This prevents:
- Accidentally re-calling the same person
- Accidentally calling the other carer mid-call
- Any touch response from forehead/palm/pokes

```
┌─────────────────────────────────────────┐
│                                         │
│              Sarah                      │
│            Connected                    │  ← ENTIRE SCREEN IS INERT
│             02:34                       │    Visual info only
│                                         │    No touch response
│                                         │
│         [Level 2+ only:]                │
│     ┌─────┐ ┌─────┐ ┌─────┐             │
│     │SPKR │ │MUTE │ │ VOL │             │  ← Exception buttons
│     └─────┘ └─────┘ └─────┘             │    (Level 2+)
│                                         │
│     ┌───────────────────────────┐       │
│     │    END CALL (tap twice)   │       │  ← Exception button
│     └───────────────────────────┘       │    (all levels)
│                                         │
└─────────────────────────────────────────┘
```

**Active elements by level:**

| Level | Active In-Call Elements |
|-------|-------------------------|
| L1 | End Call (double-tap), Emergency (3-tap anywhere) |
| L2+ | + Speaker, Mute, Volume +/- (single tap each) |

### End Call Protection Options

Carer can choose protection level for End Call button:

| Option | Behavior | Best For |
|--------|----------|----------|
| **Single tap** | One tap ends call | Users who struggle with multi-tap |
| **Double tap** | Two taps within 500ms | Default; prevents accidental end |
| **Tap + confirm** | Tap, then confirm dialog | Maximum protection |
| **Hold 2 seconds** | Long press to end | Alternative to multi-tap |

### Proximity Sensor

When phone is near face/forehead:
- Screen turns **off completely** (black)
- All touches ignored
- Only re-activates when moved away

This is standard Android behavior but we ensure it's **always enabled** during calls.

### Configurable Touch Mechanism

Different users have fundamentally different touch patterns. The touch mechanism must be **carer-configurable** and **consistent across the entire app**.

#### Touch Pattern Examples

| User Type | Touch Pattern | Needs |
|-----------|---------------|-------|
| **Wanda-type** | Touches and keeps pressing harder, no "release" | Activate on touch-down |
| **Parkinson's/tremor** | Shaky, brief unintentional touches | Minimum hold duration |
| **Weak/frail** | Light, slow touches | Sensitive, no hold requirement |
| **Poking** | Rapid repeated pokes | Debounce, ignore repeats |

#### Touch Settings (Carer Configurable)

| Setting | Options | Default | Description |
|---------|---------|---------|-------------|
| **Activation mode** | `on-press` / `on-release` | `on-press` | When touch registers |
| **Minimum hold** | 0-500ms | 0ms | Ignore touches shorter than this |
| **Debounce interval** | 100-1000ms | 300ms | Ignore repeat touches within this |
| **Touch anywhere mode** | Yes/No | No (L1 only) | Entire screen is one button |

#### Activation Mode

**On-Press (Wanda-type)**:
- Button activates the **instant finger touches** screen
- No need to lift finger
- User can "press harder" - still only one activation
- Best for users who don't release cleanly

**On-Release**:
- Button activates when finger **lifts off** screen
- Allows "cancel" by dragging finger away before release
- Traditional smartphone behavior
- May confuse users who expect immediate response

#### Minimum Hold Duration (Tremor Filter)

For users with tremors or shaky hands:
- Touch must be held for N milliseconds before registering
- Very brief touches (tremor) are ignored
- Setting of 100-200ms filters most tremor touches
- Setting of 0ms = instant response (no filter)

```
Touch event timeline:
    
0ms        100ms       200ms       300ms
 │           │           │           │
 ▼           │           │           │
┌────────────┴───────────┴───────────┤  Touch held 300ms
│████████████████████████████████████│  → Registers at 100ms (if min=100)
└────────────────────────────────────┘

 ▼     ▲
┌──────┤                               Touch held 50ms (tremor)
│██████│                               → Ignored (if min=100)
└──────┘
```

#### Debounce Interval

Prevents the same button from being activated multiple times:
- After activation, ignore touches for N milliseconds
- Prevents "poking" from registering as multiple taps
- Does NOT affect intentional multi-tap (emergency button uses separate counter)

#### No Multi-Touch

WandasPhone is **single-touch only**:
- Only one touch point processed at a time
- If multiple fingers touch, only first is recognized
- Pinch, zoom, rotate gestures do not exist
- Simplifies mental model: one finger, one action

#### Consistency Requirement

**All interactive elements use the same touch settings.**

There is no per-button configuration. The carer sets touch behavior once, and it applies everywhere:
- Home screen buttons
- In-call end button
- Settings screens (carer mode)
- Photo gallery navigation
- Every tappable element

This ensures the user develops one consistent muscle memory for "how to tap."

### High Contrast

Default theme uses:
- **Dark background**: Reduces eye strain, saves battery on OLED
- **Bright foreground**: White or high-saturation colors
- **No gradients**: Solid colors only
- **No shadows**: Flat design for clarity

Contrast ratio must exceed **7:1** for all text (WCAG AAA).

### Typography

| Element | Size | Weight |
|---------|------|--------|
| Primary text (names, instructions) | 32sp+ | Bold |
| Clock display | 64sp+ | Bold |
| Secondary text (status) | 24sp+ | Regular |
| Tertiary text (carer screens only) | 16sp | Regular |

All text uses system font for familiarity. No decorative fonts.

### Single Action Per Screen

Each screen has **one primary action**. Examples:

| Screen | Primary Action | Level |
|--------|----------------|-------|
| Home (L1) | Tap anywhere to call carer | 1 |
| Home (L2) | Tap a contact to call | 2 |
| Contacts | Tap a name to call | 3 |
| In-call (L1) | Tap to end call | 1 |
| In-call (L2+) | End call + controls | 2+ |

No menus. No hamburger icons. No bottom navigation. No swipe gestures.

---

## Carer-Controlled Configuration

### User Never Configures

The phone user never sees:
- Settings screens
- Permission dialogs
- App stores
- System settings
- Notifications

All configuration is done by the **carer** (family member, caregiver, guardian) through a PIN-protected configuration screen.

### Carer Access

To access carer settings:
1. Perform access gesture (configurable: corner taps, 5-finger press, etc.)
2. Enter 4-6 digit PIN
3. Configure all settings
4. Exit returns to user mode

### What Carers Configure

| Category | Settings |
|----------|----------|
| **Feature Level** | Level 1-4 |
| **Contacts** | Add/remove, set photos, set primary |
| **Auto-Answer** | On/off, whitelist, ring count |
| **Safety** | Timeout duration, emergency number |
| **TTS** | Voice, speed |
| **Kiosk** | Access gesture, PIN |
| **Display** | Theme, brightness |

---

## Kiosk Mode

### Purpose

Prevents the user from:
- Exiting WandasPhone
- Accessing other apps (except L4 whitelisted)
- Changing system settings
- Uninstalling apps
- Accessing notifications

### Active at ALL Levels

Even Level 4 (with app launcher) runs in kiosk mode:
- Only carer-approved apps accessible
- Apps return to WandasPhone on close
- No system settings access
- No notification shade

### Escape Hatch

Carer can always exit kiosk mode via the PIN-protected settings. If PIN is forgotten, the device can be reset via Android recovery mode.

---

## Accessibility Considerations

### Vision

- High contrast themes (dark and light options)
- Large text throughout (32sp+ primary)
- No color-only indicators
- TTS describes all screen content
- Optional always-on display

### Hearing

- Visual call indicators (screen flash)
- Vibration patterns for different events
- Visual confirmation of all TTS content
- Adjustable volume with visual feedback

### Motor

- Massive touch targets (72dp minimum)
- No swipe gestures required
- No multi-touch required
- No precise tapping required
- Long-press reserved for emergency only

### Cognitive

- One action per screen
- Consistent layout across screens
- Same interaction pattern everywhere
- Audio reinforcement of visual content
- No timed interactions (except safety timeout)
- No complex navigation
- Self-recovering to home screen

---

## What WandasPhone is NOT

### Not a Medical Device

WandasPhone is a **communication and lifestyle tool**. It is not:
- A medical alert system (though it can call emergency contacts)
- A health monitoring device
- A medication reminder (though calendar can be used this way)
- A fall detection system

Marketing and documentation must stay within communication/lifestyle category.

### Not a "Senior Phone"

While many users will be seniors, the target market is broader:
- Anyone overwhelmed by smartphone complexity
- Children needing limited, safe phone access
- People with disabilities affecting phone use
- Minimalists preferring simplicity
- Emergency backup phones

Branding avoids ageist language. "Simple" and "safe" over "senior" or "elderly."

### Not a Full Smartphone Replacement

WandasPhone intentionally lacks:
- Web browser
- Email
- Social media
- App store access
- Most apps

This is a feature, not a limitation. For users who need a full smartphone, WandasPhone is not the right choice.

---

## Design Patterns

### The One-Tap Rule (Level 1)

At Level 1, every visible element should respond to a single tap with an immediate, meaningful action. If something requires two steps, it doesn't belong at Level 1.

### The Toggle Pattern (Level 2)

Toggle buttons clearly indicate their current state:
- Visual: Icon changes, color changes
- Audio: "Speaker on" / "Speaker off"
- No ambiguity about current state

### The List-Select Pattern (Level 3)

Two-touch interactions follow a consistent pattern:
1. Tap category → See list
2. Tap item → Perform action
3. Always: timeout returns home

### The Recovery Promise

Every screen, every level:
- Wait long enough → go home
- Carer button visible → one tap to help
- Call ends → go home
- App closes → go home

---

## Design Review Checklist

Before any feature ships, verify:

**Simplicity**
- [ ] Can it be explained in one short sentence?
- [ ] Does it match the interaction complexity of its level?
- [ ] Is the touch target at least 72dp?
- [ ] Does it work with one tap (L1) or appropriate pattern (L2-4)?

**Audio**
- [ ] Does it have TTS audio for all states?
- [ ] Does TTS avoid jargon and technical terms?
- [ ] Is TTS immediate (no delays)?

**Safety**
- [ ] Does it respect inactivity timeout?
- [ ] Is the carer button visible (except during call)?
- [ ] Does it return home appropriately?
- [ ] Does it work in kiosk mode?

**Accessibility**
- [ ] Is contrast ratio at least 7:1?
- [ ] Is text at least 24sp (32sp for primary)?
- [ ] Does it avoid color-only indicators?

**Carer Control**
- [ ] Is all configuration hidden from user?
- [ ] Is it hidden appropriately based on feature level?
- [ ] Can carer enable/disable this feature?

**First Impression**
- [ ] Would a confused user understand it immediately?
- [ ] Would a carer trust it with their loved one?
