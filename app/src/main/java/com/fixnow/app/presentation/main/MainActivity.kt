package com.fixnow.app.presentation.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fixnow.app.presentation.navigation.FixNowNavHost
import com.fixnow.app.presentation.theme.FixNowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            FixNowTheme {

                val navController =
                    rememberNavController()

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
}