package com.tomsphone.feature.carer

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    const val ASSISTANTS_HUB = "carer_assistants_hub"
    /** Unified contacts list (all people; home slots define assistants). */
    const val CONTACTS_LIST = "carer_contacts_list"
    const val CONTACT_EDIT = "carer_contact_edit/{contactId}/{contactType}/{homeSlotPendingIndex}"
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

    /** [homeSlotPendingIndex] 0–6 assigns that slot after a **new** contact is saved; -1 = normal flow. */
    fun contactEdit(contactId: Long, contactType: ContactType, homeSlotPendingIndex: Int = -1) =
        "carer_contact_edit/$contactId/${contactType.name}/$homeSlotPendingIndex"
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
                onNavigateToContactsHub = { navController.navigate(CarerRoutes.ASSISTANTS_HUB) },
                onNavigateToCallHandling = { navController.navigate(CarerRoutes.CALL_HANDLING) },
                onNavigateToTouchResponse = { navController.navigate(CarerRoutes.TOUCH_RESPONSE) },
                onNavigateToAppearance = { navController.navigate(CarerRoutes.APPEARANCE) },
                onNavigateToHomeLayout = { navController.navigate(CarerRoutes.HOME_LAYOUT) },
                onNavigateToFeatureLevel = { navController.navigate(CarerRoutes.FEATURE_LEVEL) },
                onNavigateToAlwaysOn = { navController.navigate(CarerRoutes.ALWAYS_ON) },
                onNavigateToFactoryReset = { navController.navigate(CarerRoutes.FACTORY_RESET) },
                onNavigateToSupportSuggestions = { navController.navigate(CarerRoutes.SUPPORT_SUGGESTIONS) },
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
        
        // Assistants: hub (contacts vs recent calls)
        composable(CarerRoutes.ASSISTANTS_HUB) {
            AssistantsSettingsHubScreen(
                onNavigateToAllContacts = { navController.navigate(CarerRoutes.CONTACTS_LIST) },
                onNavigateToRecentCalls = { navController.navigate(CarerRoutes.RECENT_CALLS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(CarerRoutes.CONTACTS_LIST) {
            ContactsScreen(
                openedFromAssistantsHub = true,
                onNavigateToContactEdit = { contactId, contactType ->
                    navController.navigate(CarerRoutes.contactEdit(contactId, contactType))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Contact Edit
        composable(
            route = CarerRoutes.CONTACT_EDIT,
            arguments = listOf(
                navArgument("contactId") { type = NavType.LongType },
                navArgument("contactType") { type = NavType.StringType },
                navArgument("homeSlotPendingIndex") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
            val contactTypeName = backStackEntry.arguments?.getString("contactType") ?: "GREY_LIST"
            val contactType = try {
                ContactType.valueOf(contactTypeName)
            } catch (e: Exception) {
                ContactType.GREY_LIST
            }
            val homeSlotPendingIndex = backStackEntry.arguments?.getInt("homeSlotPendingIndex") ?: -1
            ContactEditScreen(
                contactId = contactId,
                contactType = contactType,
                homeSlotPendingIndex = homeSlotPendingIndex,
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
                onBack = { navController.popBackStack() },
                onNavigateToContactAfterSlot = { id, type ->
                    navController.navigate(CarerRoutes.contactEdit(id, type, homeSlotPendingIndex = -1))
                },
                onNavigateToNewContactForSlot = { slotIndex ->
                    navController.navigate(
                        CarerRoutes.contactEdit(0L, ContactType.GREY_LIST, homeSlotPendingIndex = slotIndex)
                    )
                }
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
