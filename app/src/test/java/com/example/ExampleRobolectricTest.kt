package com.example

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SearchCacheStore
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("m³ music player", appName)
  }

  @Test
  fun mainActivity_launchesWithoutCrash_andNavigatesAll5Tabs() {
    composeTestRule.onNodeWithTag("quick_launch_music").assertExists()
    composeTestRule.onNodeWithTag("quick_launch_movies").assertExists()
    composeTestRule.onNodeWithTag("quick_launch_download").assertExists()
    composeTestRule.onNodeWithTag("home_songs_search_bar").assertExists()

    composeTestRule.onNodeWithTag("nav_tab_video_player").performClick()
    composeTestRule.onNodeWithTag("video_url_input").assertExists()

    composeTestRule.onNodeWithTag("nav_tab_download").performClick()
    composeTestRule.onNodeWithTag("download_search_input").assertExists()

    composeTestRule.onNodeWithTag("nav_tab_search").performClick()
    composeTestRule.onNodeWithTag("unified_search_input").assertExists()
    composeTestRule.onNodeWithText("iTunes", substring = true).assertDoesNotExist()

    composeTestRule.onNodeWithTag("nav_tab_home").performClick()
    composeTestRule.onNodeWithTag("home_songs_search_bar").assertExists()
  }

  @Test
  fun homeScreen_opensAppearanceAndVisualizersStudio_andSwitchesSkinAndVisualizer() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_appearance_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("skin_card_apple_music_glass").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("appearance_vis_vizz_spherical_shader").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("vis_palette_acid_psychedelic").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("appearance_done_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_visualizer_card"))
    composeTestRule.onNodeWithTag("home_visualizer_card").assertExists()

    composeTestRule.onNodeWithTag("home_vizz_live_pill").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vizz_fm_live_webview_stage", useUnmergedTree = true).assertExists()
  }

  @Test
  fun homeScreen_opensTenBandEqualizerDialog_andInteractsWithSliders() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_eq_button"))
    composeTestRule.onNodeWithTag("home_eq_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ten_band_eq_card").assertExists()
    composeTestRule.onNodeWithTag("eq_master_switch").assertExists()

    for (bandIdx in 0 until 10) {
      composeTestRule.onNodeWithTag("eq_band_slider_$bandIdx")
        .performScrollTo()
        .assertExists()
    }

    composeTestRule.onNodeWithTag("eq_master_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("eq_reset_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("eq_dialog_done_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ten_band_eq_card").assertDoesNotExist()
  }

  @Test
  fun homeScreen_andDolbySheet_toggleAndroidSpatializerEffect() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_spatializer_card"))

    val homeSwitch = composeTestRule.onNodeWithTag("home_spatializer_switch", useUnmergedTree = true)
    homeSwitch.assertIsOn()
    homeSwitch.performClick()
    composeTestRule.waitForIdle()
    homeSwitch.assertIsOff()

    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_dolby_button"))
    composeTestRule.onNodeWithTag("home_dolby_button").performClick()
    composeTestRule.waitForIdle()

    val sheetSwitch = composeTestRule.onNodeWithTag("spatializer_settings_switch")
    sheetSwitch.assertExists()
    sheetSwitch.assertIsOff()
    sheetSwitch.performClick()
    composeTestRule.waitForIdle()
    sheetSwitch.assertIsOn()

    composeTestRule.onNodeWithTag("spatializer_system_settings_btn").assertExists()
    composeTestRule.onNodeWithTag("dolby_sheet_close").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_spatializer_card"))
    composeTestRule.onNodeWithTag("home_spatializer_switch", useUnmergedTree = true).assertIsOn()
  }

  @Test
  fun videoPlayer_supportsRotationAndFullscreenOverlay() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_tab_video_player").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("video_lazy_column")
      .performScrollToNode(hasTestTag("video_card_vid_tears_of_steel_hls"))
    composeTestRule.onNodeWithTag("video_card_vid_tears_of_steel_hls").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("video_lazy_column")
      .performScrollToNode(hasTestTag("video_fullscreen_btn"))
    composeTestRule.onNodeWithTag("video_rotate_btn").assertExists()
    composeTestRule.onNodeWithTag("video_fullscreen_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(
      ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
      composeTestRule.activity.requestedOrientation
    )
    composeTestRule.onNodeWithTag("video_fullscreen_overlay").assertExists()
    composeTestRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()
    composeTestRule.onNodeWithTag("video_fullscreen_resize_btn").assertExists()
    composeTestRule.onNodeWithTag("video_fullscreen_rotate_btn").assertExists()
    composeTestRule.onNodeWithTag("video_exit_fullscreen_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(
      ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
      composeTestRule.activity.requestedOrientation
    )
    composeTestRule.onNodeWithTag("video_fullscreen_overlay").assertDoesNotExist()
    composeTestRule.onNodeWithTag("nav_tab_home").assertExists()
  }

  @Test
  fun miniPlayer_opensUpNextQueueSheet_andTogglesShuffleRepeatAndClear() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_tab_video_player").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("video_lazy_column")
      .performScrollToNode(hasTestTag("video_card_vid_tears_of_steel_hls"))
    composeTestRule.onNodeWithTag("video_card_vid_tears_of_steel_hls").performClick()
    composeTestRule.waitForIdle()

    // MiniPlayer should now be visible with Queue button
    composeTestRule.onNodeWithTag("mini_player_queue_btn").assertExists().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("up_next_queue_sheet").assertExists()
    composeTestRule.onNodeWithTag("queue_now_playing_card").assertExists()
    composeTestRule.onNodeWithTag("queue_shuffle_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("queue_repeat_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("queue_clear_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("queue_sheet_close").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("up_next_queue_sheet").assertDoesNotExist()

    // Expand Studio Player via track info area and verify Shuffle / Repeat / Queue buttons
    composeTestRule.onNodeWithTag("mini_player_info", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_shuffle_btn").performScrollTo().assertExists()
    composeTestRule.onNodeWithTag("studio_repeat_btn").performScrollTo().assertExists()
    composeTestRule.onNodeWithTag("studio_queue_btn").performScrollTo().assertExists()
  }

  @Test
  fun searchCacheStore_persistsAndExpiresQueryResults() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cache = SearchCacheStore(context)
    cache.clear()

    val sampleTrack = MediaTrack(
      id = "cache_1",
      title = "Kesariya",
      artist = "Arijit Singh",
      album = "Brahmastra",
      language = MusicLanguage.HINDI,
      backend = StreamingBackend.JIOSAAVN,
      streamUrl = "https://example.com/kesariya.mp4",
      artworkUrl = "https://example.com/art.jpg",
      durationMs = 240_000L
    )

    val now = 1_700_000_000_000L
    cache.saveResults("  Arijit Singh ", listOf(sampleTrack), nowMs = now)

    val hit = cache.getCachedResults("arijit singh", nowMs = now + 60_000L)
    assertNotNull(hit)
    assertEquals(1, hit?.size)
    assertEquals("Kesariya", hit?.firstOrNull()?.title)

    // After 6h TTL expires, a fresh store instance returns null
    val freshCache = SearchCacheStore(context)
    val expired = freshCache.getCachedResults("arijit singh", nowMs = now + 7 * 60 * 60 * 1000L)
    assertNull(expired)
  }
}

