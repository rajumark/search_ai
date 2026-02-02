package com.photo.searchai.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onAgreeClick: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = "Offline Image AI App",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            InfoItem(text = "Processes photos on device")
            InfoItem(text = "No internet required")
            InfoItem(text = "Full data privacy")

            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = onAgreeClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = "I Agree")
            }
        }
    }
}

@Composable
private fun InfoItem(text: String) {
    Text(
            text = "• $text",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = TextAlign.Start
    )
}
