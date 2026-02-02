package com.tomsphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Missed Calls List Screen
 */
@HiltViewModel
class MissedCallsListViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // User name for personalization
    val userName: StateFlow<String> = settingsRepository.getUserName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "User"
        )
    
    // Missed calls list (unread only, most recent first)
    val missedCalls: StateFlow<List<CallLogEntry>> = callLogRepository.getMissedCalls(20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
