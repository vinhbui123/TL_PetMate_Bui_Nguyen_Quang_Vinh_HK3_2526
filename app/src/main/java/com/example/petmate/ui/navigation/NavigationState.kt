package com.example.petmate.ui.navigation

import com.example.petmate.model.Pet
import com.example.petmate.model.User
import com.example.petmate.model.ChatRoom

sealed interface Screen {
    data object Home : Screen
    data object PostPet : Screen
    data object AdminDashboard : Screen
    data object AdminRescueApproval : Screen
    data object Profile : Screen
    data class FollowList(val userId: Long, val initialTab: Int) : Screen
    data object BlockedUsers : Screen
    data object AdoptionManagement : Screen
    data object AdminPostApproval : Screen
    data object PostHistory : Screen
    data class ChatConversation(val chatRoom: ChatRoom) : Screen
    data class SellerProfile(val user: User) : Screen
    data object Chatbot : Screen
    data class AdoptionForm(val pet: Pet) : Screen
    data class PetDetails(val pet: Pet) : Screen
    data object AdminBroadcast : Screen
    data object Notification : Screen
    data object AdminUserManagement : Screen
    data class EditPet(val pet: Pet) : Screen
    data object AdminReportManagement : Screen
    data object SavedPets : Screen
}
