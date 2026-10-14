package com.sanvalero.boutiquestyle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sanvalero.boutiquestyle.ui.navigation.BoutiqueNavigation
import com.sanvalero.boutiquestyle.ui.theme.BoutiqueTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoutiqueTheme {
                BoutiqueNavigation()
            }
        }
    }
}
