package com.example.petmate.ui.navigation

import com.example.petmate.model.Pet
import com.example.petmate.model.User
import com.example.petmate.model.ChatRoom
import com.example.petmate.model.OrganizationProfileDto

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
    data class SellerProfile(val user: User, val isOrgProfile: Boolean = false) : Screen
    data object Chatbot : Screen
    data class AdoptionForm(val pet: Pet) : Screen
    data class PetDetails(val pet: Pet) : Screen
    data object AdminBroadcast : Screen
    data object Notification : Screen
    data object AdminUserManagement : Screen
    data class EditPet(val pet: Pet) : Screen
    data object AdminReportManagement : Screen
    data object AdminStats : Screen
    data object AdminLogs : Screen
    data object SavedPets : Screen
    data object OrgRegistration : Screen
    data class OrgProfile(val org: OrganizationProfileDto) : Screen
    data class OrgMemberManagement(val orgId: Long) : Screen
    data class EditOrgProfile(val org: OrganizationProfileDto) : Screen
    data object OrgDashboard : Screen
}
