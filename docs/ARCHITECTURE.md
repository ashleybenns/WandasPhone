# WandasPhone Architecture

## Overview

WandasPhone is a **complete default phone replacement** for Android, using a multi-module Clean Architecture approach. It fully replaces the stock dialer with a simplified, audio-first calling experience.

Key architectural goals:
- **Default phone app**: Replaces Android's phone app entirely
- **Self-recovering**: Automatically returns to safe state after inactivity
- **Kiosk-locked**: User cannot exit to system UI (all levels)
- **Feature-leveled**: Interaction complexity scales with user capability
- **Audio-first**: TTS feedback for every action

---

## Default Phone App Architecture

WandasPhone registers as the system's default dialer, giving it full control over:
- Incoming call handling and UI
- Outgoing call initiation
- In-call screen and controls
- Call state management

### Required Components for Default Dialer

```
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID TELECOM SYSTEM                    │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ CallScreening   │  │  InCallService  │  │   DIALER Role   │
│    Service      │  │                 │  │                 │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ - Screen calls  │  │ - Manage calls  │  │ - Handle intents│
│ - Auto-answer   │  │ - Answer/reject │  │ - ACTION_DIAL   │
│ - Block unknown │  │ - Hold/mute     │  │ - ACTION_CALL   │
│                 │  │ - End calls     │  │ - Default app   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### WandasInCallService

The heart of call management. Implements `android.telecom.InCallService`:

```kotlin
class WandasInCallService : InCallService() {
    
    // Called when a new call is added (incoming or outgoing)
    override fun onCallAdded(call: Call) {
        // Bind call to WandasPhone UI
        // Start InCallActivity
        // Announce via TTS
    }
    
    // Called when call is removed
    override fun onCallRemoved(call: Call) {
        // Return to home screen
        // Announce via TTS
    }
    
    // Exposed call control methods
    fun answerCall(call: Call)
    fun rejectCall(call: Call)
    fun endCall(call: Call)
    fun toggleMute(call: Call)
    fun toggleSpeaker()
    fun setVolume(level: Int)
}
```

### Default Dialer Role Request

On first launch (or when needed), request the DIALER role:

```kotlin
class DialerRoleManager @Inject constructor(
    private val context: Context
) {
    private val roleManager = context.getSystemService(RoleManager::class.java)
    
    fun isDefaultDialer(): Boolean {
        return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }
    
    fun requestDefaultDialer(activity: Activity) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        activity.startActivityForResult(intent, REQUEST_CODE_DIALER)
    }
}
```

### Manifest Configuration

```xml
<manifest>
    <!-- Required for default dialer -->
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
    <uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
    
    <application>
        <!-- InCallService for call UI control -->
        <service
            android:name=".phone.WandasInCallService"
            android:permission="android.permission.BIND_INCALL_SERVICE"
            android:exported="true">
            <meta-data
                android:name="android.telecom.IN_CALL_SERVICE_UI"
                android:value="true" />
            <intent-filter>
                <action android:name="android.telecom.InCallService" />
            </intent-filter>
        </service>
        
        <!-- Call screening for auto-answer -->
        <service
            android:name=".phone.WandasCallScreeningService"
            android:permission="android.permission.BIND_SCREENING_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.telecom.CallScreeningService" />
            </intent-filter>
        </service>
        
        <!-- Handle dial intents -->
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.DIAL" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="tel" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.CALL" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="tel" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## Self-Recovering Safety Model

### Design Principle

> The phone must reliably return to home - one touch to contact a carer

Users' cognitive abilities fluctuate throughout the day. The phone must never become "stuck" in a state the user can't escape from.

### Safety Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    INACTIVITY MANAGER                        │
│  Monitors all screens, triggers timeout → Home              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      HOME SCREEN                             │
│  - Large clock                                               │
│  - Primary carer button (one tap to call)                   │
│  - TTS greeting on arrival                                  │
│  - Safe harbor - user can always reach help                 │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Call Ended    │  │   App Closed    │  │  Timeout Hit    │
│   → Go Home     │  │   → Go Home     │  │   → Go Home     │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### InactivityManager

```kotlin
class InactivityManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private var timeoutJob: Job? = null
    private val _shouldNavigateHome = MutableSharedFlow<Unit>()
    val shouldNavigateHome: SharedFlow<Unit> = _shouldNavigateHome
    
    // Call on any user interaction
    fun onUserActivity() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(settingsRepository.inactivityTimeout)
            _shouldNavigateHome.emit(Unit)
        }
    }
    
    // Pause during active calls
    fun pauseForCall() {
        timeoutJob?.cancel()
    }
    
    // Resume after call ends
    fun resumeAfterCall() {
        onUserActivity()  // Restart timeout
    }
}
```

### Universal Safety Elements

Every screen (except active call) includes:

| Element | Behavior | Implementation |
|---------|----------|----------------|
| **Carer Button** | One tap calls primary carer | Fixed position, all screens |
| **Inactivity Timeout** | Returns to home after N seconds | InactivityManager |
| **Emergency Long-Press** | 5s press anywhere → emergency call | Global gesture detector |
| **Back = Home** | Any navigation away → home | No deep back stacks |

### Active Call Exception

During an active call:
- Inactivity timeout is **paused** (user is engaged)
- Carer button is **hidden** (can't call while on call)
- End call button is **always visible and large**
- After call ends → **immediate return to home**

---

## Module Structure

```
WandasPhone/
├── app/                    # Application entry point
├── core/                   # Shared infrastructure
│   ├── core-ui/           # Compose theme and components
│   ├── core-tts/          # Text-to-speech engine
│   ├── core-config/       # Feature flags and settings
│   ├── core-data/         # Database and storage
│   ├── core-sync/         # Data sync abstraction (Phase 2: cloud)
│   └── core-telecom/      # Phone call management
└── feature/               # Feature modules
    ├── feature-home/      # Home screen
    ├── feature-contacts/  # Contact management
    ├── feature-phone/     # Calling UI and controls
    ├── feature-carer/     # Carer configuration
    └── feature-kiosk/     # Kiosk mode
```

---

## Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                          app                                 │
│  (MainActivity, Navigation, Hilt, InactivityManager)        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      feature modules                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │  home    │ │ contacts │ │  phone   │ │  carer   │  ...  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       core modules                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ core-ui  │ │ core-tts │ │  config  │ │ telecom  │  ...  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────┘
```

**Rules**:
- `app` depends on all `feature` modules
- `feature` modules depend on `core` modules only
- `feature` modules do NOT depend on each other
- `core` modules may depend on other `core` modules

---

## Cloud-Ready Data Architecture

### Design Principle

All data access uses **interface-based repositories** so that Phase 2 cloud integration requires only adding new implementations, not changing existing code.

```
┌─────────────────────────────────────────────────────────────┐
│                    Feature / ViewModel                       │
│           (Only knows about Repository interfaces)          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Interfaces                     │
│         ContactRepository, SettingsRepository, etc.         │
└─────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ LocalDataSource │  │ CloudDataSource │  │   SyncManager   │
│   (Room + DS)   │  │   (Phase 2)     │  │   (Phase 2)     │
│                 │  │                 │  │                 │
│ ✓ Implemented   │  │ ○ Future        │  │ ○ Future        │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Repository Interfaces

All repositories are defined as interfaces in `core-data`:

```kotlin
// core-data/repository/ContactRepository.kt
interface ContactRepository {
    fun getPrimaryContact(): Flow<Contact?>
    fun getContacts(limit: Int): Flow<List<Contact>>
    fun getContactByPhone(phone: String): Flow<Contact?>
    suspend fun addContact(contact: Contact): Result<Long>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun removeContact(id: Long): Result<Unit>
    suspend fun setPrimaryContact(id: Long): Result<Unit>
}

// core-data/repository/CallLogRepository.kt
interface CallLogRepository {
    fun getMissedCalls(limit: Int): Flow<List<CallLogEntry>>
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntry>>
    suspend fun logCall(entry: CallLogEntry): Result<Long>
    suspend fun markAsRead(id: Long): Result<Unit>
}

// core-data/repository/SettingsRepository.kt
interface SettingsRepository {
    val featureLevel: Flow<FeatureLevel>
    val carerSettings: Flow<CarerSettings>
    val inactivityTimeout: Flow<Duration>
    
    suspend fun setFeatureLevel(level: FeatureLevel): Result<Unit>
    suspend fun updateCarerSettings(settings: CarerSettings): Result<Unit>
    suspend fun setPin(hashedPin: String): Result<Unit>
    suspend fun verifyPin(hashedPin: String): Boolean
}

// core-data/repository/MessageRepository.kt
interface MessageRepository {
    fun getConversation(contactId: Long): Flow<List<Message>>
    fun getUnreadCount(): Flow<Int>
    suspend fun sendMessage(to: String, content: String): Result<Long>
    suspend fun markAsRead(id: Long): Result<Unit>
}
```

### Local Implementation (Phase 1)

```kotlin
// core-data/repository/impl/LocalContactRepository.kt
class LocalContactRepository @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    
    override fun getPrimaryContact(): Flow<Contact?> =
        contactDao.getPrimaryContact().map { it?.toContact() }
    
    override fun getContacts(limit: Int): Flow<List<Contact>> =
        contactDao.getContacts(limit).map { list -> list.map { it.toContact() } }
    
    override suspend fun addContact(contact: Contact): Result<Long> =
        runCatching { contactDao.insert(contact.toEntity()) }
    
    // ... etc
}
```

### Cloud Implementation (Phase 2 - Future)

```kotlin
// core-sync/repository/impl/CloudContactRepository.kt (PHASE 2)
class CloudContactRepository @Inject constructor(
    private val api: WandasCloudApi,
    private val localRepo: LocalContactRepository,  // Offline fallback
    private val syncManager: SyncManager
) : ContactRepository {
    
    override fun getContacts(limit: Int): Flow<List<Contact>> {
        // Prefer local, sync in background
        return localRepo.getContacts(limit)
            .onStart { syncManager.syncContacts() }
    }
    
    override suspend fun addContact(contact: Contact): Result<Long> {
        // Write to local first, then sync to cloud
        val localResult = localRepo.addContact(contact)
        if (localResult.isSuccess) {
            syncManager.queueSync(SyncOperation.ContactAdded(localResult.getOrThrow()))
        }
        return localResult
    }
}
```

### What Carers Can See/Control Remotely (Phase 2)

| Data | View | Modify | Notes |
|------|:----:|:------:|-------|
| **Missed Calls** | ✓ | - | Read-only log |
| **Call History** | ✓ | - | Read-only log |
| **Contacts** | ✓ | ✓ | Full management |
| **Feature Level** | ✓ | ✓ | Change remotely |
| **Settings** | ✓ | ✓ | All carer settings |
| **Battery Level** | ✓ | - | Current status |
| **Location** | ✓ | - | If enabled |
| **Photos** | ✓ | ✓ | Add/remove |
| **Quick Replies** | ✓ | ✓ | SMS presets |

### Sync Strategy (Phase 2)

```kotlin
// core-sync/SyncManager.kt (PHASE 2)
interface SyncManager {
    val syncState: StateFlow<SyncState>
    val lastSyncTime: StateFlow<Instant?>
    
    suspend fun syncAll()
    suspend fun syncContacts()
    suspend fun syncSettings()
    suspend fun syncCallLogs()
    
    fun queueSync(operation: SyncOperation)
}

enum class SyncState {
    IDLE,
    SYNCING,
    ERROR,
    OFFLINE
}

sealed class SyncOperation {
    data class ContactAdded(val id: Long) : SyncOperation()
    data class ContactUpdated(val id: Long) : SyncOperation()
    data class SettingsChanged(val key: String) : SyncOperation()
    // ... etc
}
```

### Offline-First Guarantee

Even with cloud enabled:
1. **Local data is authoritative** for user-facing operations
2. **Cloud sync happens in background** (never blocks UI)
3. **Offline mode works fully** (cloud is optional)
4. **Conflicts resolve** with "most recent wins" + carer notification

### Hilt Module Configuration

```kotlin
// Phase 1: Local only
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideContactRepository(
        contactDao: ContactDao
    ): ContactRepository = LocalContactRepository(contactDao)
}

// Phase 2: Cloud-enabled (replaces above)
@Module
@InstallIn(SingletonComponent::class)
object CloudDataModule {
    @Provides @Singleton
    fun provideContactRepository(
        localRepo: LocalContactRepository,
        api: WandasCloudApi,
        syncManager: SyncManager
    ): ContactRepository = CloudContactRepository(api, localRepo, syncManager)
}
```

### Data Models

All data models are defined in `core-data` and used by both local and cloud implementations:

```kotlin
// core-data/model/Contact.kt
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val priority: Int,
    val isPrimary: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

// core-data/model/CallLogEntry.kt
data class CallLogEntry(
    val id: Long,
    val contactId: Long?,
    val phoneNumber: String,
    val contactName: String?,
    val type: CallType,
    val timestamp: Instant,
    val duration: Duration,
    val isRead: Boolean
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}
```

---

## Core Modules

### core-ui

**Purpose**: Shared Compose theme, components, and accessibility utilities

**Contents**:
```
core-ui/
├── theme/
│   ├── WandasTheme.kt        # Main theme definition
│   ├── Color.kt              # Color palette (high contrast)
│   ├── Typography.kt         # Large, accessible text styles
│   └── Dimensions.kt         # Touch target sizes, spacing, inert borders
├── components/
│   ├── LargeButton.kt        # Primary action button (96dp+)
│   ├── ContactCard.kt        # Contact photo with name
│   ├── StatusBar.kt          # Clock, battery display
│   ├── CarerButton.kt        # Fixed-position carer access
│   ├── ProtectedButton.kt    # Multi-tap/hold-to-activate button
│   └── AudioFeedbackModifier.kt  # Tap-to-speak modifier
├── protection/
│   ├── InertBorder.kt        # Dead zone around screen edges
│   ├── TouchDebouncer.kt     # Prevents rapid accidental taps
│   ├── MultiTapDetector.kt   # Requires N taps to activate
│   ├── InCallTouchGuard.kt   # Disables most of screen during call
│   └── HardwareButtonBlocker.kt  # Disables volume/home/back
└── accessibility/
    ├── HighContrastPreview.kt
    └── TouchTargetValidator.kt
```

**Key APIs**:
```kotlin
@Composable
fun WandasTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
)

@Composable
fun LargeButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    onClickSpeak: String? = null  // TTS on click
)

@Composable
fun CarerButton(
    carerName: String,
    carerPhoto: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

fun Modifier.speakOnClick(
    tts: WandasTTS,
    message: String
): Modifier

fun Modifier.onLongPress(
    durationMs: Long,
    onLongPress: () -> Unit
): Modifier

// Touch protection
@Composable
fun InertBorderLayout(
    borderWidth: Dp = 28.dp,
    content: @Composable () -> Unit
)

@Composable
fun ProtectedButton(
    text: String,
    protection: ButtonProtection = ButtonProtection.DoubleTap,
    onClick: () -> Unit
)

enum class ButtonProtection {
    SingleTap,      // No protection (use sparingly)
    DoubleTap,      // Two taps within 500ms
    TripleTap,      // Three taps (for emergency)
    HoldToActivate, // 2 second hold
    TapAndConfirm   // Tap then confirm dialog
}

fun Modifier.multiTapRequired(
    taps: Int,
    onActivate: () -> Unit
): Modifier

fun Modifier.inertDuringCall(
    callState: CallState
): Modifier

fun Modifier.debounced(
    minIntervalMs: Long = 300
): Modifier
```

---

### core-tts

**Purpose**: Text-to-speech engine wrapper with queue management

**Contents**:
```
core-tts/
├── WandasTTS.kt              # Main TTS interface
├── TTSEngine.kt              # Android TTS wrapper
├── TTSQueue.kt               # Utterance queue management
├── TTSModule.kt              # Hilt module for DI
└── scripts/
    └── TTSScripts.kt         # Predefined TTS messages
```

**Key APIs**:
```kotlin
interface WandasTTS {
    fun speak(message: String, priority: Priority = NORMAL)
    fun speakNow(message: String)  // Interrupts current speech
    fun stop()
    fun setSpeed(speed: Speed)
    fun setVoice(voice: Voice)
    
    enum class Priority { LOW, NORMAL, HIGH, IMMEDIATE }
    enum class Speed { SLOW, NORMAL, FAST }
}

// Predefined scripts
object TTSScripts {
    fun greeting(name: String?): String
    fun calling(contactName: String): String
    fun callConnected(contactName: String): String
    fun callEnded(): String
    fun batteryStatus(percent: Int): String
    fun incomingCall(callerName: String?): String
    fun speakerOn(): String
    fun speakerOff(): String
    fun muted(): String
    fun unmuted(): String
    fun volumeLevel(level: Int): String
}
```

---

### core-config

**Purpose**: Feature level management and carer settings

**Contents**:
```
core-config/
├── FeatureLevel.kt           # Level enum (L1-L4)
├── Feature.kt                # Individual feature flags
├── FeatureConfig.kt          # Feature availability checker
├── CarerSettings.kt          # All carer-configurable settings
├── SettingsRepository.kt     # DataStore persistence
├── InactivityConfig.kt       # Timeout settings
└── ConfigModule.kt           # Hilt module
```

**Key APIs**:
```kotlin
enum class FeatureLevel(val level: Int) {
    MINIMAL(1),    // One-touch only
    BASIC(2),      // + Toggle controls
    STANDARD(3),   // + Two-touch navigation
    EXTENDED(4)    // + Text input
}

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

class FeatureConfig @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    val currentLevel: StateFlow<FeatureLevel>
    
    fun isEnabled(feature: Feature): Boolean
    fun maxContacts(): Int  // 1, 4, 12, or unlimited based on level
}

data class CarerSettings(
    // Feature level
    val featureLevel: FeatureLevel,
    
    // Security
    val carerPin: String,
    val settingsTapCount: Int,          // Taps to access settings (default 7)
    
    // Contacts
    val primaryContactId: Long,
    
    // Call handling
    val autoAnswerEnabled: Boolean,
    val autoAnswerRings: Int,
    val answerUnknownCalls: Boolean,
    val speakerVolume: Int,             // 1-10, preset for calls
    val missedCallNagIntervalMinutes: Int,
    
    // Touch configuration (consistent across app)
    val touchActivation: TouchActivation,  // ON_PRESS or ON_RELEASE
    val touchMinHoldMs: Int,               // 0-500, tremor filter
    val touchDebounceMs: Int,              // 100-1000, repeat prevention
    val endCallProtection: ButtonProtection,
    val inertBorderDp: Int,                // 24-40, dead zone width
    
    // Audio
    val ttsSpeed: TTSSpeed,
    val userName: String,               // "Wanda" - used in TTS
    
    // Safety
    val kioskMode: KioskMode,
    val inactivityTimeoutSeconds: Int,
    val emergencyTapCount: Int,         // Taps for emergency (default 3)
    val emergencyNumber: String         // 999, 911, etc.
)

enum class TouchActivation {
    ON_PRESS,   // Activates when finger touches (Wanda-type)
    ON_RELEASE  // Activates when finger lifts (traditional)
}

enum class ButtonProtection {
    SINGLE_TAP,     // No protection
    DOUBLE_TAP,     // Two taps within 500ms
    HOLD_TO_ACTIVATE, // Hold 2 seconds
    TAP_AND_CONFIRM   // Tap then confirm dialog
}
```

---

### core-telecom

**Purpose**: Phone call management and Telecom API integration

**Contents**:
```
core-telecom/
├── WandasInCallService.kt    # InCallService implementation
├── CallScreeningService.kt   # Auto-answer logic
├── CallManager.kt            # Call state management
├── CallState.kt              # Call state models
├── DialerRoleManager.kt      # Default dialer role
└── TelecomModule.kt          # Hilt module
```

**Key APIs**:
```kotlin
class CallManager @Inject constructor() {
    val currentCall: StateFlow<WandasCall?>
    val callState: StateFlow<CallState>
    
    fun placeCall(phoneNumber: String)
    fun answerCall()
    fun endCall()
    fun toggleMute()
    fun toggleSpeaker()
    fun setVolume(level: Int)
}

data class WandasCall(
    val id: String,
    val phoneNumber: String,
    val contact: Contact?,
    val state: CallState,
    val isMuted: Boolean,
    val isSpeakerOn: Boolean,
    val duration: Duration
)

enum class CallState {
    IDLE,
    DIALING,
    RINGING,
    ACTIVE,
    ON_HOLD,
    DISCONNECTED
}
```

---

### core-data

**Purpose**: Data models, repository interfaces, and local (Room) implementation

**Contents**:
```
core-data/
├── model/                        # Domain models (used everywhere)
│   ├── Contact.kt
│   ├── CallLogEntry.kt
│   ├── Message.kt
│   └── CarerSettings.kt
├── repository/                   # Repository interfaces
│   ├── ContactRepository.kt
│   ├── CallLogRepository.kt
│   ├── MessageRepository.kt
│   └── SettingsRepository.kt
├── local/                        # Local implementations
│   ├── database/
│   │   ├── WandasDatabase.kt
│   │   ├── ContactDao.kt
│   │   ├── CallLogDao.kt
│   │   └── MessageDao.kt
│   ├── entity/                   # Room entities (internal)
│   │   ├── ContactEntity.kt
│   │   ├── CallLogEntity.kt
│   │   └── MessageEntity.kt
│   ├── mapper/                   # Entity ↔ Model mappers
│   │   └── ContactMapper.kt
│   └── impl/                     # Local repository implementations
│       ├── LocalContactRepository.kt
│       ├── LocalCallLogRepository.kt
│       └── LocalSettingsRepository.kt
└── di/
    └── DataModule.kt             # Hilt bindings (Interface → Impl)
```

**Key Design**: Interfaces defined separately from implementations, enabling future cloud swap.

**Key APIs**:
```kotlin
// Domain model (used by features)
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val priority: Int,
    val isPrimary: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Repository interface (features depend on this)
interface ContactRepository {
    fun getPrimaryContact(): Flow<Contact?>
    fun getContacts(limit: Int): Flow<List<Contact>>
    fun getContactByPhone(phone: String): Flow<Contact?>
    suspend fun addContact(contact: Contact): Result<Long>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun removeContact(id: Long): Result<Unit>
    suspend fun setPrimaryContact(id: Long): Result<Unit>
}

// Room entity (internal to local implementation)
@Entity(tableName = "contacts")
internal data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val priority: Int,
    val isPrimary: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

### core-sync (Phase 2 - Future)

**Purpose**: Cloud synchronization and remote carer access

**Status**: Interface defined now, implementation in Phase 2

**Contents** (Phase 2):
```
core-sync/
├── api/
│   ├── WandasCloudApi.kt         # Retrofit API definition
│   └── ApiModels.kt              # API request/response models
├── sync/
│   ├── SyncManager.kt            # Sync orchestration
│   ├── SyncWorker.kt             # WorkManager for background sync
│   └── ConflictResolver.kt       # Handle sync conflicts
├── impl/                         # Cloud repository implementations
│   ├── CloudContactRepository.kt
│   ├── CloudSettingsRepository.kt
│   └── CloudCallLogRepository.kt
└── di/
    └── SyncModule.kt             # Hilt bindings (replaces local)
```

**Phase 1 Preparation**:
- All repositories use `Result<T>` return types (for error handling)
- All models include `createdAt`/`updatedAt` timestamps
- All repositories are interfaces (swappable implementations)
- DataModule binds interfaces → local implementations

**Phase 2 Implementation**:
- Add `core-sync` module
- Implement cloud API client
- Create cloud repository implementations
- Swap Hilt bindings to use cloud-backed repos
- Add sync status UI in carer screens

---

## Feature Modules

### feature-home

**Purpose**: Main home screen - the "safe harbor"

**Behavior by Level**:
| Level | Home Screen |
|-------|-------------|
| L1 | Clock + single large carer photo (tap anywhere to call) |
| L2 | Clock + grid of 4 contacts (tap to call) |
| L3 | Clock + navigation buttons (Contacts, Missed, Photos) |
| L4 | Clock + navigation + search |

**Key Components**:
```kotlin
@Composable
fun HomeScreen(
    onNavigateToContacts: () -> Unit,
    onCallPrimary: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tts: WandasTTS,
    private val featureConfig: FeatureConfig,
    private val contactRepository: ContactRepository,
    private val inactivityManager: InactivityManager
) : ViewModel() {
    val primaryContact: StateFlow<Contact?>
    val currentTime: StateFlow<LocalTime>
    val batteryLevel: StateFlow<Int>
    
    fun onScreenTap() {
        // L1: Call primary immediately
        // L2+: Navigate to contacts
    }
}
```

---

### feature-phone

**Purpose**: In-call UI and call controls

**Screens**:
- `CallingScreen`: Outgoing call (dialing)
- `InCallScreen`: Active call with controls
- `IncomingCallScreen`: (Rarely shown - auto-answer)

**Level-Dependent Controls**:
| Level | In-Call Controls |
|-------|------------------|
| L1 | End call button only |
| L2+ | End call + speaker + mute + volume |

```kotlin
@Composable
fun InCallScreen(
    call: WandasCall,
    featureLevel: FeatureLevel,
    onEndCall: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onVolumeChange: (Int) -> Unit
)
```

---

### feature-contacts

**Purpose**: Contact display and selection

**Level-Dependent Behavior**:
| Level | Contact UI |
|-------|------------|
| L1 | No separate screen - primary on home |
| L2 | 2x2 grid of 4 contacts on home |
| L3 | Scrollable list, up to 12 contacts |
| L4 | Full list with search |

```kotlin
@Composable
fun ContactListScreen(
    onContactClick: (Contact) -> Unit,
    viewModel: ContactListViewModel = hiltViewModel()
)
```

---

### feature-carer

**Purpose**: PIN-protected carer configuration

**Screens**:
- `PinEntryScreen`: PIN verification
- `CarerDashboardScreen`: Main settings hub
- `ContactManagementScreen`: Add/edit contacts
- `FeatureLevelScreen`: Select level 1-4
- `SafetySettingsScreen`: Timeouts, emergency config
- `TTSSettingsScreen`: Voice and speed
- `KioskSettingsScreen`: Lock mode options

**Access Method** (configurable):
- Triple-tap top corners
- Five-finger long press
- Specific gesture pattern

---

### feature-kiosk

**Purpose**: Kiosk mode and device lockdown (ALL levels)

**Components**:
- `WandasDeviceAdminReceiver`: Device admin for lock task
- `KioskManager`: Enable/disable kiosk mode
- `BootReceiver`: Auto-launch on boot
- `AppReturnMonitor`: Detect when external apps close

**Key Behavior**:
- Kiosk mode active at ALL levels (including L4)
- L4 apps are whitelisted, but return to WandasPhone on close
- No access to system settings, notification shade, or app drawer

---

## Navigation

### Navigation Graph

```kotlin
sealed class WandasDestination(val route: String) {
    object Home : WandasDestination("home")
    object Contacts : WandasDestination("contacts")
    object InCall : WandasDestination("incall")
    object Carer : WandasDestination("carer")
    object CarerPin : WandasDestination("carer/pin")
}

@Composable
fun WandasNavGraph(
    navController: NavHostController,
    inactivityManager: InactivityManager
) {
    // Listen for timeout
    LaunchedEffect(Unit) {
        inactivityManager.shouldNavigateHome.collect {
            navController.navigate(WandasDestination.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(...) }
        composable("contacts") { ContactListScreen(...) }
        composable("incall") { InCallScreen(...) }
        navigation(startDestination = "carer/pin", route = "carer") {
            composable("carer/pin") { PinEntryScreen(...) }
            composable("carer/dashboard") { CarerDashboardScreen(...) }
        }
    }
}
```

### Navigation Rules

1. **Flat hierarchy**: Maximum 2 levels deep (Home → Feature → Action)
2. **Always return home**: Back navigation goes to home, not previous screen
3. **No dead-ends**: Every screen has explicit way out (or timeout)
4. **Call takes priority**: Incoming/active call overrides all navigation

---

## Data Flow

### User Initiates Call (Level 1)

```
User taps anywhere on home screen
        │
        ▼
HomeViewModel.onScreenTap()
        │
        ▼
CallManager.placeCall(primaryContact.phoneNumber)
        │
        ├─── TTS: "Calling [Name]"
        │
        ▼
Android Telecom API → WandasInCallService.onCallAdded()
        │
        ▼
Navigate to InCallScreen
        │
        ▼
Call connects
        │
        ├─── TTS: "[Name] answered"
        │
        ▼
Call ends → WandasInCallService.onCallRemoved()
        │
        ├─── TTS: "Call ended"
        │
        ▼
Navigate to Home (automatic)
```

### Incoming Call Auto-Answer

```
Incoming call
        │
        ▼
WandasCallScreeningService.onScreenCall()
        │
        ▼
ContactRepository.findByPhoneNumber()
        │
        ├─ Known contact ──► Allow + Auto-answer after N rings
        │                            │
        │                            ▼
        │                    TTS: "[Name] is calling. Answering."
        │                            │
        │                            ▼
        │                    Navigate to InCallScreen
        │
        └─ Unknown ──► Check settings.answerUnknownCalls
                              │
                              ├─ true ──► Allow + Answer
                              └─ false ─► Reject silently
```

### Inactivity Timeout

```
User stops interacting
        │
        ▼
InactivityManager.onUserActivity() not called
        │
        ▼
Timeout (30s - 5min, configurable)
        │
        ▼
InactivityManager emits shouldNavigateHome
        │
        ▼
NavController.navigate("home") with popUpTo(0)
        │
        ▼
HomeScreen displayed
        │
        ├─── TTS: (optional) "[Name] is here to help. Tap to call."
        │
        ▼
InactivityManager.onUserActivity() - reset timer
```

---

## Dependency Injection

Using **Hilt** for dependency injection.

### Module Hierarchy

```kotlin
// core-tts
@Module
@InstallIn(SingletonComponent::class)
object TTSModule {
    @Provides @Singleton
    fun provideTTS(@ApplicationContext context: Context): WandasTTS
}

// core-config
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Provides @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository
    
    @Provides @Singleton
    fun provideFeatureConfig(
        settingsRepository: SettingsRepository
    ): FeatureConfig
    
    @Provides @Singleton
    fun provideInactivityManager(
        settingsRepository: SettingsRepository
    ): InactivityManager
}

// core-telecom
@Module
@InstallIn(SingletonComponent::class)
object TelecomModule {
    @Provides @Singleton
    fun provideCallManager(): CallManager
    
    @Provides @Singleton
    fun provideDialerRoleManager(
        @ApplicationContext context: Context
    ): DialerRoleManager
}

// core-data
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WandasDatabase
    
    @Provides
    fun provideContactRepository(db: WandasDatabase): ContactRepository
}
```

---

## Testing Strategy

### Unit Tests

| Module | Test Focus |
|--------|------------|
| core-config | Feature flag logic, level transitions, timeout calculation |
| core-tts | Queue management, script generation |
| core-data | Repository operations, DAO queries |
| core-telecom | Call state transitions, auto-answer logic |
| feature-* | ViewModel logic, state management |

### Integration Tests

- Feature level changes propagate correctly
- TTS triggered at correct times
- Inactivity timeout navigates home
- Call state transitions update UI

### UI Tests

- Touch targets meet minimum size (72dp+)
- Contrast ratios meet WCAG AAA (7:1)
- TTS messages match expected scripts
- Kiosk mode prevents system access
- Carer button visible on all screens

### Device Tests

- InCallService receives calls correctly
- Auto-answer works via CallScreeningService
- Default dialer role request works
- Boot receiver launches app
- Battery/charging detection

---

## Build Configuration

### Version Catalog (libs.versions.toml)

```toml
[versions]
kotlin = "1.9.20"
agp = "8.2.0"
compose-bom = "2024.01.00"
hilt = "2.48"
room = "2.6.1"
datastore = "1.0.0"
navigation = "2.7.6"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.1.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "1.9.20-1.0.14" }
```

### App Module build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wandasphone"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.wandasphone"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Feature modules
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-contacts"))
    implementation(project(":feature:feature-phone"))
    implementation(project(":feature:feature-carer"))
    implementation(project(":feature:feature-kiosk"))
    
    // Core modules
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-tts"))
    implementation(project(":core:core-config"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-telecom"))
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

---

## Security Considerations

### PIN Storage

- Carer PIN stored as hash (not plaintext)
- Using Android Keystore for encryption
- PIN attempt rate limiting (lockout after 5 failed attempts)
- Lockout duration increases with repeated failures

### Kiosk Mode

- Device admin required for lock task mode
- Active at ALL feature levels
- Cannot be bypassed without PIN or factory reset
- Boot receiver ensures kiosk persists after restart

### Contact Data

- All contact data stored locally only
- No cloud sync (privacy first)
- Photos stored in app-private storage
- No data leaves device

### Permissions

Required permissions:
- `CALL_PHONE` - Make outgoing calls
- `READ_PHONE_STATE` - Call state monitoring
- `ANSWER_PHONE_CALLS` - Auto-answer (API 26+)
- `MANAGE_OWN_CALLS` - Call management
- `RECEIVE_BOOT_COMPLETED` - Auto-launch
- `FOREGROUND_SERVICE` - In-call service

Optional permissions (Level 3+):
- `READ_SMS` - SMS reading
- `SEND_SMS` - Quick replies
- `READ_CONTACTS` - Initial contact import

Optional permissions (Level 4):
- `CAMERA` - Photo capture

---

## Future Considerations

### Phase 2: Carer Cloud Access

**Priority feature** for Phase 2 - remote carer management via web/mobile app.

#### What Carers Can Do Remotely

| Capability | Description |
|------------|-------------|
| **View missed calls** | See who called and when |
| **View call history** | Full incoming/outgoing log |
| **Manage contacts** | Add, edit, remove contacts with photos |
| **Change feature level** | Adjust L1-L4 remotely |
| **Update settings** | All carer settings configurable |
| **See battery level** | Current charge status |
| **View location** | If location sharing enabled |
| **Manage photos** | Add/remove gallery photos |
| **Set quick replies** | Configure SMS preset messages |
| **Receive alerts** | Missed calls, low battery, SOS triggered |

#### Technical Implementation

| Component | Technology |
|-----------|------------|
| Cloud backend | TBD (Firebase, custom API, etc.) |
| Authentication | Carer account with email/password |
| Phone ↔ Cloud | REST API + push notifications |
| Sync strategy | Offline-first, background sync |
| Web portal | Carer dashboard for settings |
| Mobile app | Optional carer companion app |

#### Prepared in Phase 1

- Repository interfaces (swap local → cloud)
- `Result<T>` return types for error handling
- Timestamps on all data models
- Hilt modules for easy implementation swap

---

### Planned Feature Modules

- `feature-sms`: SMS reading and quick replies (Level 3)
- `feature-calendar`: Calendar integration (Level 3)
- `feature-music`: Music player (Level 4)
- `feature-apps`: App launcher (Level 4)

### Potential Integrations

- Smart speaker integration (receive commands from Alexa/Google)
- Emergency service integration
- Hearing aid Bluetooth support
- Multiple carer support with different PINs/permissions

### Localization

- All TTS scripts externalized for translation
- RTL layout support
- Multiple voice options per language
- Region-specific emergency numbers (999/911/112)
