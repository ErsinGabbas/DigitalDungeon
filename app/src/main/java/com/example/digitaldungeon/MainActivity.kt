package com.example.digitaldungeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.example.digitaldungeon.ui.navigation.AppNavigation
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            DigitalDungeonTheme {
                AppNavigation()
            }
        }
    }
}

