package com.fixnow.app.presentation.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.fixnow.app.R
import com.fixnow.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity de la app: solo aloja el NavHost. Toda la UI son Fragments. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController

        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(if (viewModel.startAtHome) R.id.homeFragment else R.id.loginFragment)
        navController.graph = graph
    }
}
