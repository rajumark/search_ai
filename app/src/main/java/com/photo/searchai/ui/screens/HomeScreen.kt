package com.photo.searchai.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Home") }) }) { paddingValues ->
        Text(text = "Welcome to Photo Search AI!", modifier = Modifier.padding(paddingValues))
    }
}
