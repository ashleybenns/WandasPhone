package com.tomsphone.feature.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tomsphone.core.ui.theme.ScaledDimensions
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import java.io.File

/**
 * Emergency confirm screen - shown after 3 taps on emergency button.
 * 
 * Design matches end call screens:
 * - Round button in CENTER of screen (different position from home screen buttons)
 * - 2 lines of instruction text above
 * - Test mode indicator at top
 * - Cancel option
 */
@Composable
fun EmergencyConfirmScreen(
    emergencyNumber: String,
    isTestMode: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val emergencyRed = Color(0xFFD32F2F)
    val buttonSize = 180.dp
    val textSize = ScaledDimensions.statusTextSize
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = emergencyRed
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScaledDimensions.edgePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section - test mode banner and cancel
            Column(
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Cancel button
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "← Cancel",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // Center section - instruction text and round button
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 2 lines of instruction text (like end call screens)
                Text(
                    text = "Press to confirm",
                    style = TextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = textSize * 1.2f
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$emergencyNumber call",
                    style = TextStyle(
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = textSize * 1.2f
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Round confirm button (centered, different position from home buttons)
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.size(buttonSize),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = emergencyRed
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Text(
                        text = if (isTestMode) "TEST" else "CALL",
                        fontSize = ScaledDimensions.buttonTextSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Bottom spacer for balance
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Emergency call screen - shown during an active emergency call.
 * 
 * Displays user's emergency info for attending EMTs:
 * - Scrollable content for extensive medical info
 * - Large user photo (for verification)
 * - Name, address
 * - Blood type, allergies, medications
 * - Medical conditions
 * - Warning about unknown calls being allowed
 * 
 * NO end call button - let EMT end the call
 */
@Composable
fun EmergencyCallScreen(
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
    onEndCall: () -> Unit
) {
    // Build full name
    val fullName = if (userSurname.isNotBlank()) "$userName $userSurname" else userName
    val emergencyRed = Color(0xFFD32F2F)
    val scrollState = rememberScrollState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(ScaledDimensions.edgePadding),
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
                        text = "⚠️ TEST MODE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Exit button - always visible (small, at top)
            TextButton(
                onClick = onEndCall,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "← Back to Home",
                    style = MaterialTheme.typography.bodyMedium
                )
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
                    text = if (isTestMode) "Test Call Active" else "999 CALL ACTIVE",
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
                        text = "No emergency info configured.\n\nCarer can add details in:\nSettings → User Profile",
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
