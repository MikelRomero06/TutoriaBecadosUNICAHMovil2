package com.example.tutoriamovil2.Login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.Estudiante.Estudiante
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentLoginBinding
import com.example.tutoriamovil2.red.dto.UsuarioDto

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnIniciarSesion.setOnClickListener {
            binding.tvError.visibility = View.GONE
            viewModel.iniciarSesion(
                binding.etCorreo.text?.toString().orEmpty(),
                binding.etPassword.text?.toString().orEmpty(),
            )
        }

        binding.btnRegistro.setOnClickListener {
            findNavController().navigate(R.id.action_login_a_registroCorreo)
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is EstadoLogin.Inicial -> Unit
                is EstadoLogin.Cargando -> mostrarCargando(true)
                is EstadoLogin.Error -> {
                    mostrarCargando(false)
                    binding.tvError.text = estado.mensaje
                    binding.tvError.visibility = View.VISIBLE
                }
                is EstadoLogin.Autenticado -> {
                    mostrarCargando(false)
                    entrar(estado.usuario)
                }
            }
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnIniciarSesion.isEnabled = !cargando
    }

    private fun entrar(usuario: UsuarioDto) {
        Toast.makeText(
            requireContext(),
            "Sesión iniciada como ${usuario.name} · ${usuario.rol}",
            Toast.LENGTH_SHORT,
        ).show()

        startActivity(Intent(requireContext(), Estudiante::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
