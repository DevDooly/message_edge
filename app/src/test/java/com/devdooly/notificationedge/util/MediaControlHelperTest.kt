package com.devdooly.notificationedge.util

import android.content.Context
import android.media.session.PlaybackState
import android.util.Xml
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
@OptIn(ExperimentalCoroutinesApi::class)
class MediaControlHelperTest {
    private class Session(
        override var state: Int? = PlaybackState.STATE_PLAYING,
        override var actions: Long = PlaybackState.ACTION_PAUSE
    ) : MediaControlHelper.PlaybackSession {
        var pauseCount = 0
        override fun pause() { pauseCount++ }
    }

    @Test fun pauseIsConfirmedBeforeReturningAndNeverTogglesPlayback() = runTest {
        val session = Session(actions = PlaybackState.ACTION_PLAY_PAUSE)
        launch { delay(100); session.state = PlaybackState.STATE_PAUSED }
        assertEquals(MediaControlHelper.PauseResult.PAUSED, MediaControlHelper.pauseAndAwait(session))
        assertEquals(1, session.pauseCount)
        assertEquals(180, testScheduler.currentTime)
    }

    @Test fun alreadyPausedStoppedAndMissingStateAreNotTouched() = runTest {
        listOf(PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED, null).forEach { state ->
            val session = Session(state)
            assertEquals(MediaControlHelper.PauseResult.NOT_NEEDED, MediaControlHelper.pauseAndAwait(session))
            assertEquals(0, session.pauseCount)
        }
    }

    @Test fun unresponsivePlayerDoesNotBlockPanelIndefinitely() = runTest {
        val session = Session()
        assertEquals(MediaControlHelper.PauseResult.UNCONFIRMED, MediaControlHelper.pauseAndAwait(session))
        assertEquals(600, testScheduler.currentTime)
        assertEquals(1, session.pauseCount)
    }

    @Test fun unsupportedPauseDoesNotSendUnknownOrToggleCommands() = runTest {
        val session = Session(actions = PlaybackState.ACTION_PLAY)
        assertEquals(MediaControlHelper.PauseResult.UNCONFIRMED, MediaControlHelper.pauseAndAwait(session))
        assertEquals(0, session.pauseCount)
    }

    @Test fun cancellingWaitDoesNotReplayOrPauseAgain() = runTest {
        val session = Session()
        val task = launch { MediaControlHelper.pauseAndAwait(session) }
        testScheduler.runCurrent()
        task.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(task.isCancelled)
        assertEquals(1, session.pauseCount)
    }

    @Test fun detectsActivityPipDeclarationNotPackageNameOrOtherElements() {
        fun declares(elements: String): Boolean {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application>$elements</application></manifest>"))
            return MediaControlHelper.declaresPictureInPicture(parser) {
                parser.getAttributeValue("http://schemas.android.com/apk/res/android", "supportsPictureInPicture") == "true"
            }
        }
        assertTrue(declares("<activity android:supportsPictureInPicture=\"true\"/>"))
        assertFalse(declares("<activity android:supportsPictureInPicture=\"false\"/>"))
        assertFalse(declares("<service android:supportsPictureInPicture=\"true\"/>"))
        assertFalse(declares("<activity/>"))
    }

    @Test fun musicSelfAndUnknownAppsAreNotTargeted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(MediaControlHelper.isVideoApp(context, context.packageName))
        assertFalse(MediaControlHelper.isVideoApp(context, "example.fake.revanced.android.youtube"))
        assertFalse(MediaControlHelper.isVideoApp(context, "com.google.android.apps.youtube.music"))
        assertFalse(MediaControlHelper.isVideoApp(context, "example.missing"))
    }
}
