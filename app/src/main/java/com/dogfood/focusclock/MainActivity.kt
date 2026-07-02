package com.dogfood.focusclock

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
