package com.tomsphone.feature.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.config.ButtonActivationPreset
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.components.ListScreenLayout
import com.tomsphone.core.ui.theme.PastelColors

/**
 * Add a blocked (unknown) caller as an answer-only contact or Assistant inside Wanda's Phone only
 * — does not open the device contacts app.
 */
@Composable
fun AddBlockedCallerScreen(
    phoneNumber: String,
    suggestedDisplayName: String?,
    onBack: () -> Unit,
    onAdded: () -> Unit,
    buttonActivation: ButtonActivationPreset,
    touchDebounceMs: Int,
    accumulatedThresholdMs: Int,
    accumulatedTimeoutMs: Int,
    viewModel: AddBlockedCallerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var displayName by remember(phoneNumber, suggestedDisplayName) {
        mutableStateOf(suggestedDisplayName.orEmpty())
    }
    var saving by remember { mutableStateOf(false) }

    ListScreenLayout(
        backgroundColor = PastelColors.lightYellow,
        title = "Add to Wanda's Phone",
        emptyMessage = "",
        isEmpty = false,
        onBack = onBack,
        activationPreset = buttonActivation,
        debounceMs = touchDebounceMs,
        accumulatedThresholdMs = accumulatedThresholdMs,
        accumulatedTimeoutMs = accumulatedTimeoutMs
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "This number was blocked as unknown. Add it as an answer-only contact or Assistant so future calls can get through (if reject-unknown is on, they must be in the app).",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Number: $phoneNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name in app") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    viewModel.addContact(phoneNumber, displayName, ContactType.GREY_LIST) { result ->
                        saving = false
                        result.fold(
                            onSuccess = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onAdded()
                            },
                            onFailure = { e ->
                                Toast.makeText(
                                    context,
                                    e.message ?: "Could not add contact",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text("Add as answer-only contact")
            }
            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    viewModel.addContact(phoneNumber, displayName, ContactType.CARER) { result ->
                        saving = false
                        result.fold(
                            onSuccess = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onAdded()
                            },
                            onFailure = { e ->
                                Toast.makeText(
                                    context,
                                    e.message ?: "Could not add contact",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text("Add as Assistant")
            }
        }
    }
}
