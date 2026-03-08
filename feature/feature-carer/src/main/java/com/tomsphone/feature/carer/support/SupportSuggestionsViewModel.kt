package com.tomsphone.feature.carer.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportSuggestionsViewModel @Inject constructor(
    private val apiClient: SupportApiClient,
    private val prefs: SupportSuggestionsPrefs
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private val _replies = MutableStateFlow<List<SupportReply>>(emptyList())
    val replies: StateFlow<List<SupportReply>> = _replies.asStateFlow()

    private val _announcements = MutableStateFlow<List<SupportAnnouncement>>(emptyList())
    val announcements: StateFlow<List<SupportAnnouncement>> = _announcements.asStateFlow()

    fun refreshUnreadCount() {
        viewModelScope.launch {
            val since = prefs.getLastVisitedAt()
            val result = apiClient.getPostsCountSince(since)
            _unreadCount.value = result.getOrElse { 0 }
        }
    }

    /** Load replies from support and announcements (for the Support screen). */
    fun loadRepliesAndAnnouncements() {
        viewModelScope.launch {
            _replies.value = apiClient.getReplies().getOrElse { emptyList() }
            _announcements.value = apiClient.getAnnouncements().getOrElse { emptyList() }
        }
    }

    fun markVisited() {
        viewModelScope.launch {
            prefs.setLastVisitedAt(System.currentTimeMillis())
            _unreadCount.value = 0
        }
    }

    fun submit(category: String, body: String) {
        viewModelScope.launch {
            _submitState.value = SubmitState.Sending
            val result = apiClient.post(category, body.trim(), null)
            _submitState.value = if (result.isSuccess) SubmitState.Success else SubmitState.Error
        }
    }

    fun clearSubmitState() {
        _submitState.value = SubmitState.Idle
    }

    sealed class SubmitState {
        data object Idle : SubmitState()
        data object Sending : SubmitState()
        data object Success : SubmitState()
        data object Error : SubmitState()
    }
}
