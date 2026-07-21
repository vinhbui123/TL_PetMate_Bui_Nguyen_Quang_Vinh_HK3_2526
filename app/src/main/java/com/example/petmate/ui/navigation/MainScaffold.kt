package com.example.petmate.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.petmate.model.User
import com.example.petmate.ui.AdoptionManagementScreen
import com.example.petmate.ui.ChatInboxScreen
import com.example.petmate.ui.PetMarketScreen
import com.example.petmate.ui.theme.PrimaryPeach

@Composable
fun MainScaffold(
    currentUser: User?,
    userRole: String?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    totalUnreadCount: Int,
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit,
    userLatitude: Double?,
    userLongitude: Double?,
    blockedUserIds: List<Long>,
    context: Context
) {
    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { onNavigate(Screen.Chatbot) },
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Trợ lý AI", modifier = Modifier.size(24.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = { 
                            if (currentUser == null) {
                                android.widget.Toast.makeText(context, "Vui lòng đăng nhập để đăng tin!", android.widget.Toast.LENGTH_SHORT).show()
                                onLogout()
                            } else {
                                onNavigate(Screen.PostPet)
                            }
                        },
                        containerColor = PrimaryPeach,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Đăng tin", modifier = Modifier.size(28.dp))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                if (userRole == "RESCUE_ORG") {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Kho thú cưng") },
                        label = { Text("Kho thú cưng") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Duyệt đơn") },
                        label = { Text("Duyệt đơn") }
                    )
                } else {
                    // Default is MEMBER
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Nhận nuôi") },
                        label = { Text("Nhận nuôi") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Mua bán") },
                        label = { Text("Mua bán") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { 
                            if (currentUser == null) {
                                Toast.makeText(context, "Vui lòng đăng nhập để xem đơn!", android.widget.Toast.LENGTH_SHORT).show()
                                onLogout()
                            } else {
                                onTabSelected(2)
                            }
                        },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "Đơn của tôi") },
                        label = { Text("Đơn của tôi") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { 
                            if (currentUser == null) {
                                Toast.makeText(context, "Vui lòng đăng nhập để xem tin nhắn!", android.widget.Toast.LENGTH_SHORT).show()
                                onLogout()
                            } else {
                                onTabSelected(3)
                            }
                        },
                        icon = { 
                            if (totalUnreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text(totalUnreadCount.toString()) }
                                    }
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = "Tin nhắn")
                                }
                            } else {
                                Icon(Icons.Default.Mail, contentDescription = "Tin nhắn")
                            }
                        },
                        label = { Text("Tin nhắn") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
                    if (userRole == "RESCUE_ORG") {
                        AdoptionManagementScreen(currentUserId = currentUser?.id)
                    } else {
                        com.example.petmate.ui.PetDiscoveryScreen(
                            currentUser = currentUser,
                            onPetClick = { pet -> onNavigate(Screen.PetDetails(pet)) },
                            onLogoutClick = onLogout,
                            onProfileClick = { onNavigate(Screen.Profile) },
                            onAdminDashboardClick = { onNavigate(Screen.AdminDashboard) },
                            onBlockedUsersClick = { onNavigate(Screen.BlockedUsers) },
                            onPostHistoryClick = { onNavigate(Screen.PostHistory) },
                            onNotificationsClick = { onNavigate(Screen.Notification) },
                            onNavigateToPostAd = { onNavigate(Screen.PostPet) },
                            userLatitude = userLatitude,
                            userLongitude = userLongitude,
                            blockedUserIds = blockedUserIds
                        )
                    }
                }
                1 -> {
                    if (userRole == "RESCUE_ORG") {
                       AdoptionManagementScreen(currentUserId = currentUser?.id)
                    } else {
                       PetMarketScreen(
                            onItemClick = { item ->
                                onNavigate(Screen.PetDetails(item))
                            },
                            onNavigateToPostAd = { onNavigate(Screen.PostPet) },
                            userLatitude = userLatitude,
                            userLongitude = userLongitude,
                            currentUser = currentUser,
                            blockedUserIds = blockedUserIds
                        )
                    }
                }
                2 -> {
                    if (userRole == "RESCUE_ORG") {
                        // Empty for RESCUE_ORG as they only have 2 tabs
                    } else {
                        AdoptionManagementScreen(currentUserId = currentUser?.id)
                    }
                }
                3 -> {
                    if (userRole != "RESCUE_ORG") {
                        ChatInboxScreen(
                            currentUserId = currentUser!!.id,
                            onRoomClick = { room ->
                                onNavigate(Screen.ChatConversation(room))
                            }
                        )
                    }
                }
            }
        }
    }
}
