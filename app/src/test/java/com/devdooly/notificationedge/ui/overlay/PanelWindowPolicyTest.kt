package com.devdooly.notificationedge.ui.overlay

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams
import androidx.core.view.WindowCompat
import com.devdooly.notificationedge.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 29, 30, 34])
@Suppress("DEPRECATION")
class PanelWindowPolicyTest {
    private lateinit var controller: ActivityController<Activity>

    @Before fun setUp() {
        controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_NotificationEdge_TranslucentPanel)
        controller.create().start().resume()
    }

    @After fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test fun panelThemeIsTransparentFloatingAndDoesNotDimTheBackground() {
        val attributes = controller.get().obtainStyledAttributes(intArrayOf(
            android.R.attr.windowIsFloating,
            android.R.attr.windowIsTranslucent,
            android.R.attr.backgroundDimEnabled,
            android.R.attr.windowDrawsSystemBarBackgrounds
        ))
        try {
            assertTrue(attributes.getBoolean(0, false))
            assertTrue(attributes.getBoolean(1, false))
            assertFalse(attributes.getBoolean(2, true))
            assertFalse(attributes.getBoolean(3, true))
        } finally {
            attributes.recycle()
        }
    }

    @Test fun panelUsesFullAvailableAreaWithoutTakingSystemBarColorsOrLosingFocus() {
        val window = controller.get().window
        window.addFlags(LayoutParams.FLAG_SECURE or LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            LayoutParams.FLAG_TRANSLUCENT_STATUS or LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        val originalAppearance = insetsController.isAppearanceLightStatusBars

        configureTransparentPanelWindow(window)

        assertEquals(LayoutParams.MATCH_PARENT, window.attributes.width)
        assertEquals(LayoutParams.MATCH_PARENT, window.attributes.height)
        val clearedFlags = LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            LayoutParams.FLAG_TRANSLUCENT_STATUS or LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_DIM_BEHIND
        assertEquals(0, window.attributes.flags and clearedFlags)
        assertTrue(window.attributes.flags and LayoutParams.FLAG_SECURE != 0)
        assertEquals(LayoutParams.SOFT_INPUT_ADJUST_RESIZE, window.attributes.softInputMode)
        assertEquals(originalAppearance, insetsController.isAppearanceLightStatusBars)
        assertEquals(Color.TRANSPARENT, window.statusBarColor)
        assertEquals(Color.TRANSPARENT, window.navigationBarColor)
        if (Build.VERSION.SDK_INT >= 29) {
            assertFalse(window.isStatusBarContrastEnforced)
            assertFalse(window.isNavigationBarContrastEnforced)
        }
    }

    @Test
    @Config(sdk = [30, 34])
    fun modernWindowAvoidsVisibleSystemBarsAndCutoutsWithoutFittingImeTwice() {
        val window = controller.get().window
        configureTransparentPanelWindow(window)
        assertEquals(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            window.attributes.fitInsetsTypes
        )
        assertEquals(WindowInsets.Side.all(), window.attributes.fitInsetsSides)
        assertFalse(window.attributes.isFitInsetsIgnoringVisibility)
        assertEquals(0, window.attributes.fitInsetsTypes and WindowInsets.Type.ime())
    }
}
