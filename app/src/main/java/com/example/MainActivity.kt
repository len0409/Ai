package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.repo.AuraRepository
import com.example.ui.components.AuraScreen
import com.example.ui.model.AuraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(this)
        val repository = AuraRepository(database)

        val viewModel = AuraViewModel(
            repository = repository,
            filesDir = filesDir,
            context = this
        )

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuraScreen(viewModel = viewModel)
            }
        }
    }
}