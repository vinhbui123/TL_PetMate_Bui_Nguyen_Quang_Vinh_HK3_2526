package com.example.petmate.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.petmate.model.Pet
import com.example.petmate.model.PetUser
import com.example.petmate.ui.theme.PetMateTheme
import org.junit.Rule
import org.junit.Test

class PetDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val samplePet = Pet(
        id = 1,
        name = "Bé Mèo Nga",
        breed = "Mèo Nga",
        age = "1.5 năm",
        weight = "2.5 kg",
        sex = "Cái",
        about = "Bé mèo rất ngoan.",
        imageUrl = null,
        price = "Miễn phí",
        imageRes = com.example.petmate.R.drawable.beagle_dog,
        user = PetUser(id = 1, fullName = "Nguyễn Văn A", address = "Hà Nội")
    )

    @Test
    fun petDetails_displaysCorrectInformation() {
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {}
                )
            }
        }

        // Verify name is displayed
        composeTestRule.onNodeWithText("Bé Mèo Nga").assertIsDisplayed()
        
        // Verify price is displayed (Miễn phí)
        composeTestRule.onNodeWithText("Miễn phí").assertIsDisplayed()
        
        // Verify characteristics section
        composeTestRule.onNodeWithText("Đặc điểm thú cưng").assertIsDisplayed()
        composeTestRule.onNodeWithText("Giống").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mèo Nga").assertIsDisplayed()
        
        // Verify description
        composeTestRule.onNodeWithText("Mô tả chi tiết").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bé mèo rất ngoan.").assertIsDisplayed()
        
        // Verify seller info
        composeTestRule.onNodeWithText("Nguyễn Văn A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hà Nội").assertIsDisplayed()
    }

    @Test
    fun petDetails_freePet_showsAdoptButton() {
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {}
                )
            }
        }

        // Verify "Nhận nuôi" button is displayed for free pet
        composeTestRule.onNodeWithText("Nhận nuôi").assertIsDisplayed()
        // Verify "Chat" button is also present
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
    }

    @Test
    fun petDetails_paidPet_showsCallButton() {
        val paidPet = samplePet.copy(price = "1.000.000 đ")
        
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = paidPet,
                    onBackClick = {}
                )
            }
        }

        // Verify "Gọi ngay" button is displayed for paid pet instead of "Nhận nuôi"
        composeTestRule.onNodeWithText("Gọi ngay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nhận nuôi").assertDoesNotExist()
    }

    @Test
    fun petDetails_backClick_triggersCallback() {
        var backClicked = false
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = { backClicked = true }
                )
            }
        }

        // Perform click on back button by its content description
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        assert(backClicked)
    }

    @Test
    fun petDetails_reportClick_showsDialog() {
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {}
                )
            }
        }

        // Click on report icon
        composeTestRule.onNodeWithContentDescription("Báo cáo").performClick()
        
        // Verify report dialog is shown
        composeTestRule.onNodeWithText("Báo cáo tin đăng").assertIsDisplayed()
    }

    @Test
    fun petDetails_adoptClick_triggersCallback() {
        var adoptClicked = false
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {},
                    onAdoptClick = { adoptClicked = true }
                )
            }
        }

        // Click on "Nhận nuôi" button
        composeTestRule.onNodeWithText("Nhận nuôi").performClick()
        
        assert(adoptClicked)
    }

    @Test
    fun petDetails_chatClick_triggersCallback() {
        var chatUser: com.example.petmate.model.User? = null
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {},
                    onChatClick = { chatUser = it }
                )
            }
        }

        // Click on "Chat" button
        composeTestRule.onNodeWithText("Chat").performClick()
        
        assert(chatUser != null)
        assert(chatUser?.id == samplePet.user?.id)
    }

    @Test
    fun petDetails_sellerProfileClick_triggersCallback() {
        var profileUser: com.example.petmate.model.User? = null
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {},
                    onViewSellerProfile = { profileUser = it }
                )
            }
        }

        // Click on "Xem trang" button
        composeTestRule.onNodeWithText("Xem trang").performClick()
        
        assert(profileUser != null)
        assert(profileUser?.id == samplePet.user?.id)
    }

    @Test
    fun petDetails_ownPet_showsDeleteButton() {
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {},
                    currentUserId = samplePet.user?.id
                )
            }
        }

        // Verify "Xoá bài" button is shown for own pet
        composeTestRule.onNodeWithText("Xoá bài").assertIsDisplayed()
        // Verify "Xem trang" is NOT shown for own pet
        composeTestRule.onNodeWithText("Xem trang").assertDoesNotExist()
    }

    @Test
    fun petDetails_imageClick_showsFullScreenDialog() {
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = samplePet,
                    onBackClick = {}
                )
            }
        }

        // Click on the pet image (identified by content description)
        composeTestRule.onNodeWithContentDescription(samplePet.name).performClick()
        
        // Verify close button in full screen dialog is shown
        composeTestRule.onNodeWithContentDescription("Đóng").assertIsDisplayed()
        
        // Click close to dismiss
        composeTestRule.onNodeWithContentDescription("Đóng").performClick()
        
        // Verify dialog is gone
        composeTestRule.onNodeWithContentDescription("Đóng").assertDoesNotExist()
    }

    @Test
    fun petDetails_showsDistance_whenCoordinatesProvided() {
        // Pet location
        val petWithLocation = samplePet.copy(latitude = 10.0, longitude = 10.0)
        
        composeTestRule.setContent {
            PetMateTheme {
                PetDetailsScreen(
                    pet = petWithLocation,
                    onBackClick = {},
                    userLatitude = 10.001, // ~110m away
                    userLongitude = 10.0
                )
            }
        }

        // Verify distance text is displayed
        composeTestRule.onNode(hasText("Cách", substring = true)).assertIsDisplayed()
    }
}
