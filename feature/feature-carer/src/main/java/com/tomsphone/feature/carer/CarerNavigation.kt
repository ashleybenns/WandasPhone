package com.tomsphone.feature.carer

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tomsphone.core.data.model.ContactType
import com.tomsphone.core.ui.components.LocalSecondaryScreenIdleReset
import com.tomsphone.feature.carer.screens.*

/**
 * Single-level back within the carer [NavHost]. A second [NavController.popBackStack] while already
 * on [CarerRoutes.MAIN_MENU] would remove the start destination and leave an empty graph (white screen).
 */
private fun NavHostController.popCarerBack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}

/**
 * Navigation routes for carer settings.
 */
object CarerRoutes {
    const val MAIN_MENU = "carer_menu"
    const val ASSISTANT_PIN = "carer_assistant_pin"
    const val TOMS_PHONE_DESCRIPTION = "carer_toms_phone_description"
    const val USER_PROFILE = "carer_user_profile"
    const val PHOTO_CAPTURE = "carer_photo_capture"
    const val ASSISTANTS_HUB = "carer_assistants_hub"
    /** Unified contacts list (all people; home slots define assistants). */
    const val CONTACTS_LIST = "carer_contacts_list"
    const val CONTACT_EDIT =
        "carer_contact_edit/{contactId}/{contactType}/{homeSlotPendingIndex}?initialPhone={initialPhone}&parentBreadcrumb={parentBreadcrumb}"
    const val CALL_HANDLING = "carer_call_handling"
    const val TOUCH_RESPONSE = "carer_touch_response"
    const val APPEARANCE = "carer_appearance"
    const val HOME_LAYOUT = "carer_home_layout"
    const val ALWAYS_ON = "carer_always_on"
    const val FACTORY_RESET = "carer_factory_reset"
    const val DATA_TRANSFER = "carer_data_transfer"
    const val SUPPORT_SUGGESTIONS = "carer_support_suggestions"
    const val SUPPORT_THREAD = "carer_support_thread/{threadId}"
    const val SUPPORT_NEW = "carer_support_new"
    const val RECENT_CALLS = "carer_recent_calls"

    fun supportThread(threadId: String) = "carer_support_thread/$threadId"

    /** [homeSlotPendingIndex] 0–6 assigns that slot after a **new** contact is saved; -1 = normal flow. */
    fun contactEdit(
        contactId: Long,
        contactType: ContactType,
        homeSlotPendingIndex: Int = -1,
        initialPhone: String? = null,
        parentBreadcrumb: String? = null
    ): String {
        val path = "carer_contact_edit/$contactId/${contactType.name}/$homeSlotPendingIndex"
        // Always include query args so the route matches the registered pattern (defaults when empty).
        val ip = Uri.encode(initialPhone.orEmpty())
        val pb = Uri.encode(parentBreadcrumb.orEmpty())
        return "$path?initialPhone=$ip&parentBreadcrumb=$pb"
    }
}

/**
 * Carer settings navigation host.
 * 
 * All carer settings screens are nested here with proper back navigation.
 */
@Composable
fun CarerNavigation(
    onExitCarerSettings: () -> Unit,
    onExitApp: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val resetIdle = LocalSecondaryScreenIdleReset.current
    NavHost(
        navController = navController,
        startDestination = CarerRoutes.MAIN_MENU,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(resetIdle) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            resetIdle()
                        }
                    }
                }
            }
    ) {
        // Main Menu
        composable(CarerRoutes.MAIN_MENU) {
            CarerMainMenuScreen(
                onNavigateToTomsPhoneDescription = { navController.navigate(CarerRoutes.TOMS_PHONE_DESCRIPTION) },
                onNavigateToAssistantPin = { navController.navigate(CarerRoutes.ASSISTANT_PIN) },
                onNavigateToUserProfile = { navController.navigate(CarerRoutes.USER_PROFILE) },
                onNavigateToContactsHub = { navController.navigate(CarerRoutes.ASSISTANTS_HUB) },
                onNavigateToCallHandling = { navController.navigate(CarerRoutes.CALL_HANDLING) },
                onNavigateToTouchResponse = { navController.navigate(CarerRoutes.TOUCH_RESPONSE) },
                onNavigateToAppearance = { navController.navigate(CarerRoutes.APPEARANCE) },
                onNavigateToHomeLayout = { navController.navigate(CarerRoutes.HOME_LAYOUT) },
                onNavigateToAlwaysOn = { navController.navigate(CarerRoutes.ALWAYS_ON) },
                onNavigateToFactoryReset = { navController.navigate(CarerRoutes.FACTORY_RESET) },
                onNavigateToDataTransfer = { navController.navigate(CarerRoutes.DATA_TRANSFER) },
                onNavigateToSupportSuggestions = { navController.navigate(CarerRoutes.SUPPORT_SUGGESTIONS) },
                onExitApp = onExitApp,
                onBack = onExitCarerSettings
            )
        }

        composable(CarerRoutes.ASSISTANT_PIN) {
            AssistantPinScreen(onBack = { navController.popCarerBack() })
        }
        
        // Tom's Phone Description
        composable(CarerRoutes.TOMS_PHONE_DESCRIPTION) {
            TomsPhoneDescriptionScreen(
                onBack = { navController.popCarerBack() }
            )
        }

        // User Profile
        composable(CarerRoutes.USER_PROFILE) {
            UserProfileScreen(
                onNavigateToPhotoCapture = { navController.navigate(CarerRoutes.PHOTO_CAPTURE) },
                onBack = { navController.popCarerBack() }
            )
        }
        
        // Photo Capture
        composable(CarerRoutes.PHOTO_CAPTURE) {
            PhotoCaptureScreen(
                onPhotoCaptured = { photoUri ->
                    // Photo captured and saved via ViewModel in the screen
                    android.util.Log.d("CarerNav", "Photo captured: $photoUri, navigating back")
                    navController.popCarerBack()
                },
                onCancel = { navController.popCarerBack() }
            )
        }
        
        // Assistants: hub (contacts vs recent calls)
        composable(CarerRoutes.ASSISTANTS_HUB) {
            AssistantsSettingsHubScreen(
                onNavigateToAllContacts = { navController.navigate(CarerRoutes.CONTACTS_LIST) },
                onNavigateToRecentCalls = { navController.navigate(CarerRoutes.RECENT_CALLS) },
                onBack = { navController.popCarerBack() }
            )
        }

        composable(CarerRoutes.CONTACTS_LIST) {
            ContactsScreen(
                openedFromAssistantsHub = true,
                onNavigateToContactEdit = { contactId, contactType ->
                    navController.navigate(CarerRoutes.contactEdit(contactId, contactType))
                },
                onBack = { navController.popCarerBack() }
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
                },
                navArgument("initialPhone") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("parentBreadcrumb") {
                    type = NavType.StringType
                    defaultValue = ""
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
            val initialPhone = backStackEntry.arguments?.getString("initialPhone").orEmpty()
            val parentBreadcrumb = backStackEntry.arguments?.getString("parentBreadcrumb").orEmpty()
            ContactEditScreen(
                contactId = contactId,
                contactType = contactType,
                homeSlotPendingIndex = homeSlotPendingIndex,
                initialPhoneFromCallLog = initialPhone,
                parentBreadcrumbTitle = parentBreadcrumb,
                onBack = { navController.popCarerBack() }
            )
        }
        
        // Call Handling
        composable(CarerRoutes.CALL_HANDLING) {
            CallHandlingScreen(
                onBack = { navController.popCarerBack() }
            )
        }
        
        // Touch Response
        composable(CarerRoutes.TOUCH_RESPONSE) {
            TouchResponseScreen(
                onBack = { navController.popCarerBack() }
            )
        }
        
        // Appearance
        composable(CarerRoutes.APPEARANCE) {
            AppearanceScreen(
                onBack = { navController.popCarerBack() }
            )
        }

        // Home screen layout (7 assignable slots + Emergency)
        composable(CarerRoutes.HOME_LAYOUT) {
            HomeScreenLayoutScreen(
                onBack = { navController.popCarerBack() },
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
                onBack = { navController.popCarerBack() },
                onAddUnknownCallerToContacts = { phone ->
                    navController.navigate(
                        CarerRoutes.contactEdit(
                            contactId = 0L,
                            contactType = ContactType.GREY_LIST,
                            homeSlotPendingIndex = -1,
                            initialPhone = phone,
                            parentBreadcrumb = "Recent calls"
                        )
                    )
                }
            )
        }
        
        // Always On Mode
        composable(CarerRoutes.ALWAYS_ON) {
            AlwaysOnScreen(
                onBack = { navController.popCarerBack() }
            )
        }
        
        // App Reset (erase this app’s data only)
        composable(CarerRoutes.FACTORY_RESET) {
            FactoryResetScreen(
                onNavigateToDataTransfer = { navController.navigate(CarerRoutes.DATA_TRANSFER) },
                onBack = { navController.popCarerBack() }
            )
        }

        composable(CarerRoutes.DATA_TRANSFER) {
            DataTransferScreen(
                onBack = { navController.popCarerBack() }
            )
        }
        
        // Support & suggestions (inbox)
        composable(CarerRoutes.SUPPORT_SUGGESTIONS) {
            SupportSuggestionsScreen(
                onBack = { navController.popCarerBack() },
                onThreadClick = { threadId -> navController.navigate(CarerRoutes.supportThread(threadId)) },
                onNewMessage = { navController.navigate(CarerRoutes.SUPPORT_NEW) }
            )
        }

        // Thread detail (conversation)
        composable(CarerRoutes.SUPPORT_THREAD) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
            SupportThreadDetailScreen(
                threadId = threadId,
                onBack = { navController.popCarerBack() }
            )
        }

        // New message form
        composable(CarerRoutes.SUPPORT_NEW) {
            SupportNewMessageScreen(
                onBack = { navController.popCarerBack() },
                onSent = { threadId ->
                    navController.popCarerBack()
                    if (threadId != null) navController.navigate(CarerRoutes.supportThread(threadId))
                }
            )
        }
    }
}
