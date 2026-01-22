package com.tomsphone.feature.carer

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsphone.core.config.FeatureLevel
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.feature.carer.screens.*

/**
 * Navigation routes for carer settings.
 */
object CarerRoutes {
    const val MAIN_MENU = "carer_menu"
    const val USER_PROFILE = "carer_user_profile"
    const val PHOTO_CAPTURE = "carer_photo_capture"
    const val CONTACTS = "carer_contacts"
    const val CONTACT_EDIT = "carer_contact_edit/{contactId}/{contactType}"
    const val CALL_HANDLING = "carer_call_handling"
    const val APPEARANCE = "carer_appearance"
    const val FEATURE_LEVEL = "carer_feature_level"
    const val ALWAYS_ON = "carer_always_on"
    const val FACTORY_RESET = "carer_factory_reset"
    
    fun contactEdit(contactId: Long, contactType: ContactType) = 
        "carer_contact_edit/$contactId/${contactType.name}"
}

/**
 * Carer settings navigation host.
 * 
 * All carer settings screens are nested here with proper back navigation.
 */
@Composable
fun CarerNavigation(
    onExitCarerSettings: () -> Unit,
    featureLevel: FeatureLevel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = CarerRoutes.MAIN_MENU
    ) {
        // Main Menu
        composable(CarerRoutes.MAIN_MENU) {
            CarerMainMenuScreen(
                featureLevel = featureLevel,
                onNavigateToUserProfile = { navController.navigate(CarerRoutes.USER_PROFILE) },
                onNavigateToContacts = { navController.navigate(CarerRoutes.CONTACTS) },
                onNavigateToCallHandling = { navController.navigate(CarerRoutes.CALL_HANDLING) },
                onNavigateToAppearance = { navController.navigate(CarerRoutes.APPEARANCE) },
                onNavigateToFeatureLevel = { navController.navigate(CarerRoutes.FEATURE_LEVEL) },
                onNavigateToAlwaysOn = { navController.navigate(CarerRoutes.ALWAYS_ON) },
                onNavigateToFactoryReset = { navController.navigate(CarerRoutes.FACTORY_RESET) },
                onBack = onExitCarerSettings
            )
        }
        
        // User Profile
        composable(CarerRoutes.USER_PROFILE) {
            UserProfileScreen(
                featureLevel = featureLevel,
                onNavigateToPhotoCapture = { navController.navigate(CarerRoutes.PHOTO_CAPTURE) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Photo Capture
        composable(CarerRoutes.PHOTO_CAPTURE) {
            PhotoCaptureScreen(
                onPhotoCaptured = { photoUri ->
                    // Photo captured and saved via ViewModel in the screen
                    android.util.Log.d("CarerNav", "Photo captured: $photoUri, navigating back")
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
        
        // Contacts List
        composable(CarerRoutes.CONTACTS) {
            ContactsScreen(
                featureLevel = featureLevel,
                onNavigateToContactEdit = { contactId, contactType -> 
                    navController.navigate(CarerRoutes.contactEdit(contactId, contactType))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Contact Edit
        composable(CarerRoutes.CONTACT_EDIT) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")?.toLongOrNull() ?: 0L
            val contactTypeName = backStackEntry.arguments?.getString("contactType") ?: "CARER"
            val contactType = try { 
                ContactType.valueOf(contactTypeName) 
            } catch (e: Exception) { 
                ContactType.CARER 
            }
            ContactEditScreen(
                contactId = contactId,
                contactType = contactType,
                featureLevel = featureLevel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Call Handling
        composable(CarerRoutes.CALL_HANDLING) {
            CallHandlingScreen(
                featureLevel = featureLevel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Appearance
        composable(CarerRoutes.APPEARANCE) {
            AppearanceScreen(
                featureLevel = featureLevel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Feature Level
        composable(CarerRoutes.FEATURE_LEVEL) {
            FeatureLevelScreen(
                featureLevel = featureLevel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Always On Mode
        composable(CarerRoutes.ALWAYS_ON) {
            AlwaysOnScreen(
                featureLevel = featureLevel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Factory Reset
        composable(CarerRoutes.FACTORY_RESET) {
            FactoryResetScreen(
                featureLevel = featureLevel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
