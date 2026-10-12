package com.fixnow.app.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fixnow.app.presentation.navigation.FixNowNavHost
import com.fixnow.app.presentation.navigation.Routes
import com.fixnow.app.presentation.theme.FixNowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val abrirBandejaTecnica = mutableStateOf(false)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        abrirBandejaTecnica.value = intent.getBooleanExtra("openTechnicianInbox", false)

        setContent {

            FixNowTheme {

                val navController =
                    rememberNavController()

                LaunchedEffect(abrirBandejaTecnica.value) {
                    if (abrirBandejaTecnica.value && viewModel.startAtHome) {
                        navController.navigate(Routes.SOLICITUDES_TECNICO) {
                            launchSingleTop = true
                        }
                    }
                    abrirBandejaTecnica.value = false
                }

                val snackbarHostState =
                    remember {
                        SnackbarHostState()
                    }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState =
                                snackbarHostState
                        )
                    }
                ) { innerPadding ->

                    FixNowNavHost(
                        navController =
                            navController,

                        startAtHome =
                            viewModel.startAtHome,

                        activity =
                            this@MainActivity,

                        snackbarHostState =
                            snackbarHostState,

                        modifier =
                            Modifier.padding(
                                innerPadding
                            )
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("openTechnicianInbox", false)) {
            abrirBandejaTecnica.value = true
        }
    }
}