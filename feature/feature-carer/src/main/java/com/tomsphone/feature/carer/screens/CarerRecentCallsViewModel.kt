package com.tomsphone.feature.carer.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val RECENT_CALLS_LIMIT = 500

/**
 * Read-only recent call history for carers (same Room `call_logs` as the user list).
 * One row per event; suitable for future remote sync from this table.
 */
@HiltViewModel
class CarerRecentCallsViewModel @Inject constructor(
    callLogRepository: CallLogRepository
) : ViewModel() {

    val recentCalls: StateFlow<List<CallLogEntry>> = callLogRepository
        .getRecentCalls(RECENT_CALLS_LIMIT)
        .map { list -> list.filter { it.phoneNumber.isNotBlank() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
