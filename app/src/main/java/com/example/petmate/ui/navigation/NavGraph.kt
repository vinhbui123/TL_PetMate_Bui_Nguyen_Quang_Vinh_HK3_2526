package com.example.petmate.ui.navigation

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.petmate.model.PetUser
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petmate.repository.OrganizationRepository
import com.example.petmate.ui.org.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    currentScreen: Screen,
    currentUser: User?,
    userRole: String?,
    userLatitude: Double?,
    userLongitude: Double?,
    blockedUserIds: List<Long>,
    totalUnreadCount: Int,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    coroutineScope: CoroutineScope,
    onNavigate: (Screen) -> Unit,
    onNavigateAndClear: (Screen) -> Unit,
    onPop: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    when (currentScreen) {
        is Screen.Home -> {
            MainScaffold(
                currentUser = currentUser,
                userRole = userRole,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                totalUnreadCount = totalUnreadCount,
                onLogout = onLogout,
                onNavigate = onNavigate,
                userLatitude = userLatitude,
                userLongitude = userLongitude,
                blockedUserIds = blockedUserIds,
                context = context
            )
        }
        is Screen.PostPet -> {
            BackHandler { onPop() }
            PostPetScreen(
                onBackClick = onPop,
                onPostSuccess = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                }
            )
        }
        is Screen.AdminDashboard -> {
            BackHandler { onPop() }
            AdminDashboardScreen(
                onNavigateToRescueApproval = { onNavigate(Screen.AdminRescueApproval) },
                onNavigateToPostApproval = { onNavigate(Screen.AdminPostApproval) },
                onNavigateToBroadcast = { onNavigate(Screen.AdminBroadcast) },
                onNavigateToUserManagement = { onNavigate(Screen.AdminUserManagement) },
                onNavigateToReports = { onNavigate(Screen.AdminReportManagement) },
                onNavigateToLogs = { onNavigate(Screen.AdminLogs) },
                onNavigateToStats = { onNavigate(Screen.AdminStats) },
                onBack = onPop
            )
        }
        is Screen.AdminLogs -> {
            BackHandler { onPop() }
            AdminLogsScreen(onBack = onPop)
        }
        is Screen.AdminStats -> {
            BackHandler { onPop() }
            AdminStatsScreen(onBack = onPop)
        }
        is Screen.AdminRescueApproval -> {
            BackHandler { onPop() }
            AdminRescueApprovalScreen(onBack = onPop)
        }
        is Screen.AdminPostApproval -> {
            BackHandler { onPop() }
            AdminPostApprovalScreen(onBack = onPop)
        }
        is Screen.AdminBroadcast -> {
            BackHandler { onPop() }
            AdminBroadcastScreen(onBack = onPop)
        }
        is Screen.AdminUserManagement -> {
            BackHandler { onPop() }
            AdminUserManagementScreen(onBack = onPop)
        }
        is Screen.Notification -> {
            BackHandler { onPop() }
            NotificationScreen(onBack = onPop)
        }
        is Screen.Profile -> {
            BackHandler { onPop() }
            ProfileScreen(
                onBackClick = onPop,
                onSaveSuccess = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                },
                onLogoutClick = {
                    onPop()
                    onLogout()
                },
                onViewFollowers = { uid, tab ->
                    onNavigate(Screen.FollowList(uid, tab))
                },
                onManageAdoptions = {
                    onNavigate(Screen.AdoptionManagement)
                },
                onViewSavedPets = {
                    onNavigate(Screen.SavedPets)
                },
                onOrgRegistrationClick = {
                    onNavigate(Screen.OrgRegistration)
                },
                onOrgProfileClick = {
                    coroutineScope.launch {
                        try {
                            val res = NetworkClient.orgApi.getMyOrg()
                            if (res.isSuccessful && res.body() != null) {
                                onNavigate(Screen.OrgProfile(res.body()!!))
                            } else {
                                onNavigate(Screen.OrgRegistration)
                            }
                        } catch (_: Exception) {
                            onNavigate(Screen.OrgRegistration)
                        }
                    }
                }
            )
        }
        is Screen.AdoptionManagement -> {
            BackHandler { onPop() }
            AdoptionManagementScreen(
                currentUserId = currentUser?.id,
                onBack = onPop
            )
        }
        is Screen.PostHistory -> {
            BackHandler { onPop() }
            PostHistoryScreen(
                onBack = onPop,
                onPetClick = { pet -> onNavigate(Screen.PetDetails(pet)) }
            )
        }
        is Screen.FollowList -> {
            BackHandler { onPop() }
            FollowListScreen(
                userId = currentScreen.userId,
                initialTab = currentScreen.initialTab,
                onBackClick = onPop,
                onUserClick = { user ->
                    onNavigate(Screen.SellerProfile(user))
                }
            )
        }
        is Screen.BlockedUsers -> {
            BackHandler { onPop() }
            BlockedUsersScreen(onBackClick = onPop)
        }
        is Screen.ChatConversation -> {
            BackHandler { onPop() }
            ChatScreen(
                roomId = currentScreen.chatRoom.id,
                currentUserId = currentUser?.id ?: 0L,
                otherUserName = currentScreen.chatRoom.otherUser.fullName,
                otherUserId = currentScreen.chatRoom.otherUser.id,
                onBack = onPop
            )
        }
        is Screen.SellerProfile -> {
            BackHandler { onPop() }
            val sellerAsPetUser = PetUser(
                id = currentScreen.user.id,
                fullName = currentScreen.user.fullName,
                email = currentScreen.user.email,
                avatarUrl = currentScreen.user.avatarUrl,
                role = currentScreen.user.role,
                phone = currentScreen.user.phone,
                address = currentScreen.user.address,
                latitude = currentScreen.user.latitude,
                longitude = currentScreen.user.longitude
            )
            SellerProfileScreen(
                sellerId = currentScreen.user.id,
                sellerInfo = sellerAsPetUser,
                isOrgProfile = currentScreen.isOrgProfile,
                onBack = onPop,
                onPetClick = { pet -> onNavigate(Screen.PetDetails(pet)) },
                userLatitude = userLatitude,
                userLongitude = userLongitude,
                blockedUserIds = blockedUserIds,
                onBlockStatusChanged = onRefresh,
                onViewFollowers = { uid, tab ->
                    onNavigate(Screen.FollowList(uid, tab))
                }
            )
        }
        is Screen.Chatbot -> {
            BackHandler { onPop() }
            ChatbotScreen(onBack = onPop)
        }
        is Screen.AdoptionForm -> {
            BackHandler { onPop() }
            AdoptionFormScreen(
                pet = currentScreen.pet,
                onBack = onPop,
                onSubmitSuccess = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                }
            )
        }
        is Screen.PetDetails -> {
            BackHandler { onPop() }
            PetDetailsScreen(
                initialPet = currentScreen.pet,
                onBackClick = onPop,
                onAdoptClick = {
                    if (currentUser == null) {
                        Toast.makeText(context, "Vui lòng đăng nhập để nhận nuôi!", Toast.LENGTH_SHORT).show()
                        onLogout()
                    } else {
                        onNavigate(Screen.AdoptionForm(currentScreen.pet))
                    }
                },
                onViewSellerProfile = { seller, isOrg -> onNavigate(Screen.SellerProfile(seller, isOrg)) },
                onChatClick = { seller ->
                    when {
                        currentUser == null -> {
                            Toast.makeText(context, "Vui lòng đăng nhập để gửi tin nhắn!", Toast.LENGTH_SHORT).show()
                            onLogout()
                        }
                        currentUser.id == seller.id -> {
                            Toast.makeText(context, "Bạn không thể tự chat với chính mình!", Toast.LENGTH_SHORT).show()
                        }
                        blockedUserIds.contains(seller.id) -> {
                            Toast.makeText(context, "Bạn không thể nhắn tin với người dùng này!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            coroutineScope.launch {
                                try {
                                    val payload = mapOf(
                                        "buyerId" to currentUser.id,
                                        "sellerId" to seller.id,
                                        "petId" to currentScreen.pet.id.toLong()
                                    )
                                    val room = NetworkClient.apiService.getOrCreateRoom(payload)
                                    onNavigate(Screen.ChatConversation(room))
                                } catch (e: Exception) {
                                    Log.e("NavGraph", "Lỗi khi tạo phiên chat", e)
                                    Toast.makeText(context, "Lỗi khi tạo phiên chat", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                onPetClick = { pet -> onNavigate(Screen.PetDetails(pet)) },
                userLatitude = userLatitude,
                userLongitude = userLongitude,
                currentUserId = currentUser?.id,
                onEditClick = { petToEdit ->
                    onNavigate(Screen.EditPet(petToEdit))
                }
            )
        }
        is Screen.EditPet -> {
            BackHandler { onPop() }
            EditPetScreen(
                pet = currentScreen.pet,
                onBackClick = onPop,
                onEditSuccess = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                }
            )
        }
        is Screen.AdminReportManagement -> {
            BackHandler { onPop() }
            AdminReportManagementScreen(
                onBack = onPop
            )
        }
        is Screen.SavedPets -> {
            BackHandler { onPop() }
            SavedPetsScreen(
                onBackClick = onPop,
                onPetClick = { pet -> onNavigate(Screen.PetDetails(pet)) },
                currentUser = currentUser,
                userLatitude = userLatitude,
                userLongitude = userLongitude
            )
        }
        is Screen.OrgRegistration -> {
            BackHandler { onPop() }
            OrgRegisterScreen(
                viewModel = rememberOrgViewModel(),
                onBack = onPop
            )
        }
        is Screen.OrgProfile -> {
            val org = currentScreen.org
            BackHandler { onPop() }
            OrgProfileScreen(
                initialOrg = org,
                viewModel = rememberOrgViewModel(),
                onBack = onPop,
                onManageMembers = { onNavigate(Screen.OrgMemberManagement(org.id!!)) },
                onEditClick = { updatedOrg -> onNavigate(Screen.EditOrgProfile(updatedOrg)) },
                onLeaveSuccess = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                },
                currentUserRole = userRole
            )
        }
        is Screen.OrgMemberManagement -> {
            BackHandler { onPop() }
            OrgMemberManagementScreen(
                orgId = currentScreen.orgId,
                viewModel = rememberOrgViewModel(),
                onBack = onPop,
                onDissolved = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                },
                onRefreshUser = onRefresh
            )
        }
        is Screen.EditOrgProfile -> {
            BackHandler { onPop() }
            EditOrgProfileScreen(
                org = currentScreen.org,
                viewModel = rememberOrgViewModel(),
                onBack = onPop,
                onSaveSuccess = {
                    onNavigateAndClear(Screen.Home)
                    onRefresh()
                }
            )
        }
        is Screen.OrgDashboard -> {
            BackHandler { onPop() }
            OrgDashboardScreen(
                onBackClick = onPop,
                onNavigateToAdoptions = { onNavigate(Screen.AdoptionManagement) },
                onNavigateToOrgProfile = {
                    coroutineScope.launch {
                        try {
                            val res = NetworkClient.orgApi.getMyOrg()
                            if (res.isSuccessful && res.body() != null) {
                                onNavigate(Screen.OrgProfile(res.body()!!))
                            } else {
                                Toast.makeText(context, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun rememberOrgViewModel(): OrganizationViewModel {
    val repo = OrganizationRepository(NetworkClient.orgApi)
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrganizationViewModel(repo) as T
            }
        }
    )
}
