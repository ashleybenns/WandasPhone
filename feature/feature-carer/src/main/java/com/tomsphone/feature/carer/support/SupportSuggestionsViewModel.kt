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

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    private val _threads = MutableStateFlow<List<SupportThread>>(emptyList())
    val threads: StateFlow<List<SupportThread>> = _threads.asStateFlow()

    private val _selectedThread = MutableStateFlow<SupportThread?>(null)
    val selectedThread: StateFlow<SupportThread?> = _selectedThread.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private val _announcements = MutableStateFlow<List<SupportAnnouncement>>(emptyList())
    val announcements: StateFlow<List<SupportAnnouncement>> = _announcements.asStateFlow()

    fun ensureDeviceId() {
        viewModelScope.launch {
            _deviceId.value = prefs.getOrCreateAnonymousId()
        }
    }

    fun loadThreads() {
        viewModelScope.launch {
            val did = _deviceId.value ?: prefs.getOrCreateAnonymousId()
            _deviceId.value = did
            apiClient.getThreads(did).onSuccess { _threads.value = it }
        }
    }

    fun loadThread(threadId: String) {
        viewModelScope.launch {
            val did = _deviceId.value ?: prefs.getOrCreateAnonymousId()
            apiClient.getThread(threadId, did).onSuccess { _selectedThread.value = it }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            val did = _deviceId.value ?: prefs.getOrCreateAnonymousId()
            val since = prefs.getLastVisitedAt()
            _unreadCount.value = apiClient.getPostsCountSince(since, did).getOrElse { 0 }
        }
    }

    fun markVisited() {
        viewModelScope.launch {
            prefs.setLastVisitedAt(System.currentTimeMillis())
            _unreadCount.value = 0
        }
    }

    fun submitNewThread(category: String, body: String) {
        viewModelScope.launch {
            _submitState.value = SubmitState.Sending
            val did = _deviceId.value ?: prefs.getOrCreateAnonymousId()
            val result = apiClient.postThread(did, category, body.trim())
            _submitState.value = when {
                result.isSuccess && result.getOrNull() != null -> SubmitState.Success(result.getOrNull())
                else -> SubmitState.Error
            }
        }
    }

    fun addReply(threadId: String, message: String) {
        viewModelScope.launch {
            val did = _deviceId.value ?: prefs.getOrCreateAnonymousId()
            apiClient.addReply(threadId, did, message.trim()).onSuccess {
                loadThread(threadId)
                loadThreads()
            }
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _announcements.value = apiClient.getAnnouncements().getOrElse { emptyList() }
        }
    }

    fun clearSubmitState() {
        _submitState.value = SubmitState.Idle
    }

    fun clearSelectedThread() {
        _selectedThread.value = null
    }

    sealed class SubmitState {
        data object Idle : SubmitState()
        data object Sending : SubmitState()
        data class Success(val threadId: String?) : SubmitState()
        data object Error : SubmitState()
    }
}
