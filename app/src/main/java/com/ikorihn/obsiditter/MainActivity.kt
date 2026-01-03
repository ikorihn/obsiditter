package com.ikorihn.obsiditter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ikorihn.obsiditter.ui.ObsiditterApp
import com.ikorihn.obsiditter.ui.theme.ObsiditterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObsiditterTheme {
                ObsiditterApp()
            }
        }
    }
}
