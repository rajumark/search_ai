package com.photo.searchai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.photo.searchai.navigation.NavGraph
import com.photo.searchai.ui.theme.PhotoSearchAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoSearchAITheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}