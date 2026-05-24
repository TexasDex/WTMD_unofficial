package com.texasdex.wtmdappthatdoesntsuck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.texasdex.wtmdappthatdoesntsuck.ui.screens.MainNavigation
import com.texasdex.wtmdappthatdoesntsuck.ui.theme.WTMDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WTMDTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainNavigation()
                }
            }
        }
    }
}
