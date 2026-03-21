package com.tomsphone.feature.carer

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.feature.carer.screens.*

/**
 * Navigation routes for carer settings.
 */
object CarerRoutes {
    const val MAIN_MENU = "carer_menu"
    const val TOMS_PHONE_DESCRIPTION = "carer_toms_phone_description"
    const val USER_PROFILE = "carer_user_profile"
    const val PHOTO_CAPTURE = "carer_photo_capture"
    const val CONTACTS = "carer_contacts"
    const val CONTACTS_ASSISTANTS = "carer_contacts_assistants"
    const val CONTACTS_FRIENDS = "carer_contacts_friends"
    const val CONTACT_EDIT = "carer_contact_edit/{contactId}/{contactType}"
    const val CALL_HANDLING = "carer_call_handling"
    const val TOUCH_RESPONSE = "carer_touch_response"
    const val APPEARANCE = "carer_appearance"
    const val HOME_LAYOUT = "carer_home_layout"
    const val FEATURE_LEVEL = "carer_feature_level"
    const val ALWAYS_ON = "carer_always_on"
    const val FACTORY_RESET = "carer_factory_reset"
    const val SUPPORT_SUGGESTIONS = "carer_support_suggestions"
    const val SUPPORT_THREAD = "carer_support_thread/{threadId}"
    const val SUPPORT_NEW = "carer_support_new"
    const val RECENT_CALLS = "carer_recent_calls"

    fun supportThread(threadId: String) = "carer_support_thread/$threadId"

    fun contactEdit(contactId: Long, contactType: ContactType) = 
        "carer_contact_edit/$contactId/${contactType.name}"
}

/**
 * Carer settings navigation host.
 * 
 * All carer settings screens are nested here with proper back navigation.
 * Each screen reads the feature level directly from the ViewModel for reactivity.
 */
@Composable
fun CarerNavigation(
    onExitCarerSettings: () -> Unit,
    onExitApp: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = CarerRoutes.MAIN_MENU
    ) {
        // Main Menu
        composable(CarerRoutes.MAIN_MENU) {
            CarerMainMenuScreen(
                onNavigateToTomsPhoneDescription = { navController.navigate(CarerRoutes.TOMS_PHONE_DESCRIPTION) },
                onNavigateToUserProfile = { navController.navigate(CarerRoutes.USER_PROFILE) },
                onNavigateToAssistants = { navController.navigate(CarerRoutes.CONTACTS_ASSISTANTS) },
                onNavigateToFriends = { navController.navigate(CarerRoutes.CONTACTS_FRIENDS) },
                onNavigateToCallHandling = { navController.navigate(CarerRoutes.CALL_HANDLING) },
                onNavigateToTouchResponse = { navController.navigate(CarerRoutes.TOUCH_RESPONSE) },
                onNavigateToAppearance = { navController.navigate(CarerRoutes.APPEARANCE) },
                onNavigateToHomeLayout = { navController.navigate(CarerRoutes.HOME_LAYOUT) },
                onNavigateToFeatureLevel = { navController.navigate(CarerRoutes.FEATURE_LEVEL) },
                onNavigateToAlwaysOn = { navController.navigate(CarerRoutes.ALWAYS_ON) },
                onNavigateToFactoryReset = { navController.navigate(CarerRoutes.FACTORY_RESET) },
                onNavigateToSupportSuggestions = { navController.navigate(CarerRoutes.SUPPORT_SUGGESTIONS) },
                onNavigateToRecentCalls = { navController.navigate(CarerRoutes.RECENT_CALLS) },
                onExitApp = onExitApp,
                onBack = onExitCarerSettings
            )
        }
        
        // Tom's Phone Description
        composable(CarerRoutes.TOMS_PHONE_DESCRIPTION) {
            TomsPhoneDescriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // User Profile
        composable(CarerRoutes.USER_PROFILE) {
            UserProfileScreen(
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
        
        // Assistants list
        composable(CarerRoutes.CONTACTS_ASSISTANTS) {
            ContactsScreen(
                contactTypeFilter = ContactType.CARER,
                onNavigateToContactEdit = { contactId, contactType ->
                    navController.navigate(CarerRoutes.contactEdit(contactId, contactType))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Friends list
        composable(CarerRoutes.CONTACTS_FRIENDS) {
            ContactsScreen(
                contactTypeFilter = ContactType.GREY_LIST,
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
                onBack = { navController.popBackStack() }
            )
        }
        
        // Call Handling
        composable(CarerRoutes.CALL_HANDLING) {
            CallHandlingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Touch Response
        composable(CarerRoutes.TOUCH_RESPONSE) {
            TouchResponseScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Appearance
        composable(CarerRoutes.APPEARANCE) {
            AppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Home screen layout (7 assignable slots + Emergency)
        composable(CarerRoutes.HOME_LAYOUT) {
            HomeScreenLayoutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Recent calls (read-only, same DB as user list; for carer review / future sync)
        composable(CarerRoutes.RECENT_CALLS) {
            CarerRecentCallsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Feature Level
        composable(CarerRoutes.FEATURE_LEVEL) {
            FeatureLevelScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Always On Mode
        composable(CarerRoutes.ALWAYS_ON) {
            AlwaysOnScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Factory Reset
        composable(CarerRoutes.FACTORY_RESET) {
            FactoryResetScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Support & suggestions (inbox)
        composable(CarerRoutes.SUPPORT_SUGGESTIONS) {
            SupportSuggestionsScreen(
                onBack = { navController.popBackStack() },
                onThreadClick = { threadId -> navController.navigate(CarerRoutes.supportThread(threadId)) },
                onNewMessage = { navController.navigate(CarerRoutes.SUPPORT_NEW) }
            )
        }

        // Thread detail (conversation)
        composable(CarerRoutes.SUPPORT_THREAD) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
            SupportThreadDetailScreen(
                threadId = threadId,
                onBack = { navController.popBackStack() }
            )
        }

        // New message form
        composable(CarerRoutes.SUPPORT_NEW) {
            SupportNewMessageScreen(
                onBack = { navController.popBackStack() },
                onSent = { threadId ->
                    navController.popBackStack()
                    if (threadId != null) navController.navigate(CarerRoutes.supportThread(threadId))
                }
            )
        }
    }
}
