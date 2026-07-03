package com.dogfood.focusclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogfood.focusclock.ui.FocusScreen
import com.dogfood.focusclock.ui.FocusViewModel
import com.dogfood.focusclock.ui.SettingsStore
import com.dogfood.focusclock.ui.focusDataStore
import com.dogfood.focusclock.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 8.0 以上で通知チャネルを作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "focus_timer_channel"
            val channelName = "Focus Timer Notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, channelName, importance)
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        val settings = SettingsStore(applicationContext.focusDataStore)
        setContent {
            AppTheme {
                val vm: FocusViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            FocusViewModel(settings) as T
                    }
                )
                FocusScreen(vm)
            }
        }
    }
}
