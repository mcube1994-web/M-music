package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.M3RootApp
import com.example.ui.M3ViewModel
import com.example.ui.theme.M3MusicPlayerTheme

class MainActivity : ComponentActivity() {

    private val m3ViewModel: M3ViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val appearanceSkin by m3ViewModel.appearanceSkin.collectAsStateWithLifecycle()
            M3MusicPlayerTheme(skin = appearanceSkin) {
                M3RootApp(
                    viewModel = m3ViewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
