package com.thxios.storagetree.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thxios.storagetree.MainActivity
import com.thxios.storagetree.di.StorageModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(StorageModule::class)
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appLaunch_showsPermissionScreen() {
        composeRule.onNodeWithText("StorageTree").assertIsDisplayed()
        composeRule.onNodeWithText("권한 허용").assertIsDisplayed()
    }

    @Test
    fun appLaunch_permissionScreen_hasGrantButton() {
        composeRule.onNodeWithText("권한 허용").assertHasClickAction()
    }
}
