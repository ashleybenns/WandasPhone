package com.tomsphone.feature.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.ui.components.SecondaryScreenIdleEffect
import com.tomsphone.core.ui.components.activationGesture
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.rememberEmergencyConfirmBackIconSize
import com.tomsphone.core.ui.theme.rememberEmergencyConfirmBackRowStyle
import com.tomsphone.core.ui.theme.rememberEmergencyConfirmDialDiameter
import com.tomsphone.core.ui.theme.rememberEmergencyConfirmDialTypography
import com.tomsphone.core.ui.theme.rememberEmergencyConfirmInstructionStyle
import com.tomsphone.core.ui.theme.rememberEndCallFixedActionTypography
import com.tomsphone.core.ui.theme.wandasColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val INACTIVITY_TIMEOUT_MS = 30_000L
private const val REQUIRED_TAPS = 3
private const val TAP_TIMEOUT_MS = 3000L

/**
 * ViewModel for EmergencyConfirmScreen - provides touch response settings
 */
@HiltViewModel
class EmergencyConfirmViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val buttonActivation: StateFlow<ButtonActivationPreset> = settingsRepository.getSettings()
        .map { it.buttonActivation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ButtonActivationPreset.ON_RELEASE)
    
    val touchDebounceMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.touchDebounceMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)
    
    val accumulatedTapThresholdMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapThresholdMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500)
    
    val accumulatedTapTimeoutMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapTimeoutMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)
    
    // Tap counting for 3-tap activation
    private val _tapCount = MutableStateFlow(0)
    val tapCount: StateFlow<Int> = _tapCount.asStateFlow()
    
    private var tapResetJob: Job? = null
    
    /**
     * Handle a tap on the emergency call button.
     * Returns true if the required taps have been reached.
     */
    fun onEmergencyTap(): Boolean {
        _tapCount.value++
        
        // Cancel previous reset job
        tapResetJob?.cancel()
        
        if (_tapCount.value >= REQUIRED_TAPS) {
            _tapCount.value = 0
            return true
        }
        
        // Reset tap count after timeout
        tapResetJob = viewModelScope.launch {
            delay(TAP_TIMEOUT_MS)
            _tapCount.value = 0
        }
        
        return false
    }
    
    /**
     * Reset tap count when leaving screen
     */
    fun resetTaps() {
        _tapCount.value = 0
        tapResetJob?.cancel()
    }
}

@HiltViewModel
class EmergencyCallScreenViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val buttonActivation: StateFlow<ButtonActivationPreset> =
        settingsRepository
            .getSettings()
            .map { it.buttonActivation }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                ButtonActivationPreset.ON_RELEASE,
            )

    val touchDebounceMs: StateFlow<Int> =
        settingsRepository
            .getSettings()
            .map { it.touchDebounceMs }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val accumulatedTapThresholdMs: StateFlow<Int> =
        settingsRepository
            .getSettings()
            .map { it.accumulatedTapThresholdMs }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500)

    val accumulatedTapTimeoutMs: StateFlow<Int> =
        settingsRepository
            .getSettings()
            .map { it.accumulatedTapTimeoutMs }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)
}

/**
 * Emergency confirm screen - requires 3 taps to make the call.
 * 
 * Design matches end call screens with styling like ListScreenLayout:
 * - Red background
 * - Back row, instruction line, and round dial use screen-fit typography (not carer appearance scale)
 * - "Press N times" instruction with countdown
 * - Large round button in center
 * - Test mode indicator
 * - Auto-cancels after 30 seconds of inactivity
 */
@Composable
fun EmergencyConfirmScreen(
    emergencyNumber: String,
    isTestMode: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EmergencyConfirmViewModel = hiltViewModel()
) {
    val emergencyRed = Color(0xFFD32F2F)
    val configuration = LocalConfiguration.current
    val buttonSize = rememberEmergencyConfirmDialDiameter()
    val dialPad = 20.dp
    val dialInnerW = (buttonSize - dialPad * 2).coerceAtLeast(64.dp)
    val dialInnerH = (buttonSize - dialPad * 2).coerceAtLeast(52.dp)
    val dialPrimaryText = if (isTestMode) "TEST" else emergencyNumber
    val dialTypography =
        rememberEmergencyConfirmDialTypography(dialPrimaryText, dialInnerW, dialInnerH)
    val instructionMaxWidth = (configuration.screenWidthDp.dp - 48.dp).coerceAtLeast(100.dp)
    val instructionBaseStyle =
        rememberEmergencyConfirmInstructionStyle(instructionMaxWidth, REQUIRED_TAPS)
    val backRowMaxWidth = (configuration.screenWidthDp.dp * 0.55f).coerceAtLeast(96.dp)
    val backTextStyle = rememberEmergencyConfirmBackRowStyle(backRowMaxWidth)
    val backIconSize = rememberEmergencyConfirmBackIconSize()

    // Touch response settings
    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()
    
    // Tap count for 3-tap activation
    val tapCount by viewModel.tapCount.collectAsState()
    
    // Reset taps on leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetTaps()
        }
    }
    
    // Handle tap and check if call should be made
    val handleTap: () -> Unit = {
        if (viewModel.onEmergencyTap()) {
            onConfirm()
        }
    }
    
    SecondaryScreenIdleEffect(timeoutMs = INACTIVITY_TIMEOUT_MS, onTimeout = onCancel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = emergencyRed
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = WandasDimensions.InertBorderWidth,
                    top = WandasDimensions.InertBorderWidth,
                    end = WandasDimensions.InertBorderWidth,
                    bottom = WandasDimensions.InertBorderBottom
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Test mode banner
            if (isTestMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFEB3B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠️ TEST MODE\nNo real call will be made",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Back button row - styled like ListScreenLayout
            val backInteractionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .indication(backInteractionSource, rememberRipple())
                    .activationGesture(
                        preset = buttonActivation,
                        debounceMs = touchDebounceMs,
                        accumulatedThresholdMs = accumulatedThresholdMs,
                        accumulatedTimeoutMs = accumulatedTimeoutMs,
                        onActivate = onCancel,
                        interactionSource = backInteractionSource
                    )
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(backIconSize),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back",
                    style = backTextStyle,
                    color = Color.White,
                )
            }
            
            // Center section - instruction text and round button
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Instruction text - shows tap progress
                val instructionText = if (tapCount > 0) {
                    "$tapCount / $REQUIRED_TAPS"
                } else {
                    "Press $REQUIRED_TAPS times"
                }
                
                Text(
                    text = instructionText,
                    style =
                        instructionBaseStyle.copy(
                            fontWeight = if (tapCount > 0) FontWeight.Bold else FontWeight.Medium,
                        ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Round emergency button (centered)
                // Uses activation gesture for consistent touch response
                val confirmInteractionSource = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .indication(confirmInteractionSource, rememberRipple())
                        .activationGesture(
                            preset = buttonActivation,
                            debounceMs = touchDebounceMs,
                            accumulatedThresholdMs = accumulatedThresholdMs,
                            accumulatedTimeoutMs = accumulatedTimeoutMs,
                            onActivate = handleTap,
                            interactionSource = confirmInteractionSource
                        ),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dialPrimaryText,
                                style = dialTypography.primaryLine,
                                color = emergencyRed,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                            Text(
                                text = "CALL",
                                style = dialTypography.callLine,
                                color = emergencyRed.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            
            // Bottom spacer for balance
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    }
}

/**
 * Emergency call screen — medical info for EMTs (scrollable).
 *
 * Exit control is pinned to the **top** (double-tap “Back”, same gesture sizing as end-call).
 */
@Composable
fun EmergencyCallScreen(
    emergencyDialDigits: String,
    userName: String,
    userSurname: String,
    userPhotoUri: String?,
    userAddress: String,
    userBloodType: String,
    userAllergies: String,
    userMedications: String,
    userMedicalConditions: String,
    userEmergencyNotes: String,
    emergencyContact1Name: String,
    emergencyContact1Phone: String,
    emergencyContact2Name: String,
    emergencyContact2Phone: String,
    isTestMode: Boolean,
    isCallActive: Boolean = true,
    onEndCall: () -> Unit,
    viewModel: EmergencyCallScreenViewModel = hiltViewModel(),
) {
    // Build full name
    val fullName = if (userSurname.isNotBlank()) "$userName $userSurname" else userName
    val emergencyRed = Color(0xFFD32F2F)
    val scrollState = rememberScrollState()

    val buttonActivation by viewModel.buttonActivation.collectAsState()
    val touchDebounceMs by viewModel.touchDebounceMs.collectAsState()
    val accumulatedThresholdMs by viewModel.accumulatedTapThresholdMs.collectAsState()
    val accumulatedTimeoutMs by viewModel.accumulatedTapTimeoutMs.collectAsState()

    var exitTapCount by remember { mutableIntStateOf(0) }
    var exitResetJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val exitConfirmPending = exitTapCount == 1
    val latestOnEndCall by rememberUpdatedState(onEndCall)

    DisposableEffect(Unit) {
        onDispose {
            exitResetJob?.cancel()
        }
    }

    val configuration = LocalConfiguration.current
    val screenW = configuration.screenWidthDp.dp
    val barHeight = (configuration.screenHeightDp * 0.13f).dp.coerceIn(56.dp, 132.dp)
    val innerTextWidth = (screenW - 48.dp).coerceAtLeast(120.dp)
    val exitTypography = rememberEndCallFixedActionTypography(innerTextWidth, barHeight)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.wandasColors.background,
        topBar = {
            val exitInteractionSource = remember { MutableInteractionSource() }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = WandasDimensions.SpacingLarge,
                            vertical = 8.dp,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(WandasDimensions.CornerRadiusLarge))
                            .indication(exitInteractionSource, rememberRipple())
                            .activationGesture(
                                preset = buttonActivation,
                                debounceMs = touchDebounceMs,
                                accumulatedThresholdMs = accumulatedThresholdMs,
                                accumulatedTimeoutMs = accumulatedTimeoutMs,
                                onActivate = {
                                    if (exitTapCount == 0) {
                                        exitTapCount = 1
                                        exitResetJob?.cancel()
                                        exitResetJob =
                                            scope.launch {
                                                delay(3000)
                                                exitTapCount = 0
                                            }
                                    } else {
                                        exitTapCount = 0
                                        exitResetJob?.cancel()
                                        latestOnEndCall()
                                    }
                                },
                                interactionSource = exitInteractionSource,
                            ),
                    shape = RoundedCornerShape(WandasDimensions.CornerRadiusLarge),
                    color = emergencyRed,
                    shadowElevation = WandasDimensions.ElevationMedium,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Back",
                                style = exitTypography.titleStyle,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                            Text(
                                text = if (exitConfirmPending) "Tap again" else "Tap twice",
                                style = exitTypography.subtitleStyle,
                                color =
                                    if (exitConfirmPending) {
                                        Color(0xFFFFEB3B)
                                    } else {
                                        Color.White.copy(alpha = 0.9f)
                                    },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(ScaledDimensions.edgePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Test mode banner
            if (isTestMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFEB3B),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "⚠️ TEST MODE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // IMPORTANT WARNING: Unknown calls allowed
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1976D2),  // Blue for info
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "📞 CALLS FROM ALL NUMBERS ALLOWED\nEmergency services may call back",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Call status - only show when call is active
            if (isCallActive) {
                Text(
                    text = if (isTestMode) "Test Call Active" else "${emergencyDialDigits} CALL ACTIVE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = emergencyRed,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // User photo - large for EMT visibility
            val context = LocalContext.current
            val photoFile = remember { File(context.filesDir, "emergency_photo.jpg") }
            val hasPhoto = photoFile.exists()
            
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.wandasColors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback to initial
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // User full name
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.wandasColors.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info sections (scrollable)
            if (userAddress.isNotBlank()) {
                InfoSection(label = "ADDRESS", value = userAddress)
            }
            
            if (userBloodType.isNotBlank()) {
                InfoSection(label = "BLOOD TYPE", value = userBloodType)
            }
            
            if (userAllergies.isNotBlank()) {
                InfoSection(
                    label = "⚠️ ALLERGIES",
                    value = userAllergies,
                    isWarning = true
                )
            }
            
            if (userMedications.isNotBlank()) {
                InfoSection(label = "MEDICATIONS", value = userMedications)
            }
            
            if (userMedicalConditions.isNotBlank()) {
                InfoSection(label = "MEDICAL CONDITIONS", value = userMedicalConditions)
            }
            
            if (userEmergencyNotes.isNotBlank()) {
                InfoSection(label = "NOTES", value = userEmergencyNotes)
            }
            
            // Emergency contacts
            if (emergencyContact1Name.isNotBlank() || emergencyContact2Name.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "EMERGENCY CONTACTS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.wandasColors.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                if (emergencyContact1Name.isNotBlank()) {
                    InfoSection(
                        label = emergencyContact1Name,
                        value = emergencyContact1Phone.ifBlank { "No phone" }
                    )
                }
                
                if (emergencyContact2Name.isNotBlank()) {
                    InfoSection(
                        label = emergencyContact2Name,
                        value = emergencyContact2Phone.ifBlank { "No phone" }
                    )
                }
            }
            
            // If no info configured
            if (userAddress.isBlank() && userBloodType.isBlank() && 
                userAllergies.isBlank() && userMedications.isBlank() &&
                userMedicalConditions.isBlank() && userEmergencyNotes.isBlank()) {
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.wandasColors.surface
                    )
                ) {
                    Text(
                        text = "No emergency info configured.\n\nAssistant can add details in:\nSettings → User Profile",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
            
            // Bottom padding for scroll
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoSection(
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    val emergencyRed = Color(0xFFD32F2F)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) 
                emergencyRed.copy(alpha = 0.1f) 
            else 
                MaterialTheme.wandasColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isWarning) emergencyRed else MaterialTheme.wandasColors.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.wandasColors.onSurface,
                fontWeight = if (isWarning) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
