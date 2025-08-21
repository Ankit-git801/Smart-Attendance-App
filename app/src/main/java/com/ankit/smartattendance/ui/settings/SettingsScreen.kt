package com.ankit.smartattendance.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ankit.smartattendance.viewmodel.AppViewModel

// CORRECTED: Add the OptIn annotation here
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appViewModel: AppViewModel) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Settings will be available soon.")
        }
    }
}
