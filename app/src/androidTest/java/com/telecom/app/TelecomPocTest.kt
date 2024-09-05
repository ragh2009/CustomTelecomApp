package com.telecom.app

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.telecom.app.presentation.ui.components.PocCallMainScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TelecomPocTest {

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        listOf(Manifest.permission.RECORD_AUDIO)
    }

    @get:Rule(order = 2)
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*permissionArray.toTypedArray())

    @Before
    fun setUp() {
        composeTestRule.setContent {
            PocCallMainScreen()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOngoingCall() {
        composeTestRule.onNodeWithText("Make fake call").performClick()
        composeTestRule.apply {

            // Wait till the call is connected
            waitUntilExactlyOneExists(hasText("Connected"), 5000)
            onNode(hasText("RR")).assertIsDisplayed()

            val onHold = "Pause or resume call"
            onNodeWithContentDescription(onHold).apply {
                assertIsEnabled()
                assert(isOff())
                performClick()
            }
            waitUntilExactlyOneExists(hasContentDescription(onHold) and isOn(), 5000)

            // Disconnect call and check
            onNodeWithContentDescription("Disconnect call").performClick()
            waitUntil {
                onAllNodesWithText("Call ended").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
