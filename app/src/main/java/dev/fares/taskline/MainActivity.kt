package dev.fares.taskline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = AppState(Store(applicationContext))
        setContent { TasklineApp(state) }
    }

    override fun onResume() {
        super.onResume()
        if (::state.isInitialized) state.refreshToday()
    }
}
