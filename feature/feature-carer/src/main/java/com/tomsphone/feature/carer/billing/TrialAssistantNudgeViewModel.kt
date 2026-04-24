package com.tomsphone.feature.carer.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.billing.EntitlementRepository
import com.tomsphone.core.billing.EntitlementSnapshot
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrialAssistantNudgeViewModel @Inject constructor(
    private val entitlementRepository: EntitlementRepository,
    private val tts: WandasTTS
) : ViewModel() {

    val snapshot = entitlementRepository.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EntitlementSnapshot(
            hasFullAccess = true,
            isTrialPeriod = false,
            trialEndsAtMillis = null,
            trialDaysRemainingInclusive = null,
            ownedProductId = null,
            isReviewBypass = false,
            isDebugBypass = false
        )
    )

    /** At most one spoken reminder per UTC day while in the last week of trial. */
    fun maybeSpeakAssistantTrialNudge() {
        viewModelScope.launch {
            val snap = entitlementRepository.snapshot.first()
            if (!snap.shouldNudgeAssistantsAboutTrial) return@launch
            if (!entitlementRepository.shouldPlayTrialAssistantNudge()) return@launch
            val days = snap.trialDaysRemainingInclusive ?: return@launch
            tts.speak(
                TTSScripts.trialEndingForAssistants(days),
                WandasTTS.Priority.HIGH
            )
            entitlementRepository.markTrialAssistantNudgePlayed()
        }
    }
}
