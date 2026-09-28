package com.fixnow.app.core.util

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun Fragment.showSnackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    view?.let { Snackbar.make(it, message, length).show() }
}

/**
 * Escucha un Flow solo mientras la pantalla está visible (STARTED).
 * Es la forma recomendada de consumir StateFlow / eventos desde un Fragment.
 */
fun <T> Fragment.collectWhenStarted(flow: Flow<T>, collector: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { collector(it) }
        }
    }
}
