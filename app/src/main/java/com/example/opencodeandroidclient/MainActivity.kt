package com.example.opencodeandroidclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.opencodeandroidclient.ui.ChatScreen
import com.example.opencodeandroidclient.ui.QrScannerScreen
import com.example.opencodeandroidclient.ui.theme.OpenCodeTheme
import com.example.opencodeandroidclient.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenCodeTheme {
                val viewModel: ChatViewModel = viewModel()
                var showScanner by remember { mutableStateOf(false) }

                if (showScanner) {
                    QrScannerScreen(
                        onResult = { value ->
                            viewModel.onQrResult(value)
                            showScanner = false
                        },
                        onBack = { showScanner = false },
                    )
                } else {
                    ChatScreen(
                        viewModel = viewModel,
                        onScanClick = { showScanner = true },
                    )
                }
            }
        }
    }
}
