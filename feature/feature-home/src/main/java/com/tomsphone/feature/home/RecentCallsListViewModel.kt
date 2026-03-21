package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.config.ListTextAlignment
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Max rows shown (each log row is separate; duplicates kept). */
private const val RECENT_CALLS_LIMIT = 500

/**
 * ViewModel for the assistant "Recent calls" screen.
 *
 * Shows incoming, outgoing, missed, and declined calls from the app call log,
 * newest first, with no merging of duplicate numbers.
 */
@HiltViewModel
class RecentCallsListViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val userName: StateFlow<String> = settingsRepository.getUserName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "User"
        )

    val recentCalls: StateFlow<List<CallLogEntry>> = callLogRepository.getRecentCalls(RECENT_CALLS_LIMIT)
        .map { calls -> calls.filter { it.phoneNumber.isNotBlank() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val listTextAlignment: StateFlow<ListTextAlignment> = settingsRepository.getSettings()
        .map { settings -> settings.listTextAlignment }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListTextAlignment.CENTER
        )

    val buttonActivation: StateFlow<ButtonActivationPreset> = settingsRepository.getSettings()
        .map { it.buttonActivation }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ButtonActivationPreset.ON_RELEASE
        )

    val touchDebounceMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.touchDebounceMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 150
        )

    val accumulatedTapThresholdMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapThresholdMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 500
        )

    val accumulatedTapTimeoutMs: StateFlow<Int> = settingsRepository.getSettings()
        .map { it.accumulatedTapTimeoutMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3000
        )
}
