package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.notifications.StolenAlertNotifier
import com.example.ui.AppDestination
import com.example.ui.RasedApp
import com.example.ui.theme.RasedTheme

class MainActivity : ComponentActivity() {

    private var requestedDestination by mutableStateOf(AppDestination.SCANNER)
    private var destinationTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            RasedTheme {
                RasedApp(
                    initialDestination = requestedDestination,
                    externalDestinationTrigger = destinationTrigger
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getStringExtra(StolenAlertNotifier.EXTRA_OPEN_TAB) == StolenAlertNotifier.TAB_ALERTS) {
            requestedDestination = AppDestination.ALERTS
            destinationTrigger++
        }
    }
}
