package com.fixnow.app.presentation.designsystem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fixnow.app.databinding.FragmentDesignSystemBinding

/**
 * HU02: catálogo visual de la paleta, la tipografía y los componentes base.
 * Sirve para revisar de un vistazo (en modo claro y oscuro) cómo se ve todo antes de armar pantallas nuevas.
 */
class DesignSystemFragment : Fragment() {

    private var _binding: FragmentDesignSystemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesignSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
