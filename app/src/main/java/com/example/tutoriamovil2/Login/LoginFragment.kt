package com.example.tutoriamovil2.Login

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.Estudiante.Estudiante
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.Tutor.Tutor
import com.example.tutoriamovil2.databinding.FragmentLoginBinding
import com.example.tutoriamovil2.red.dto.UsuarioDto

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private var accionPendiente: (() -> Unit)? = null

    private val permisoRedLocal = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            accionPendiente?.invoke()
            accionPendiente = null
        } else {
            accionPendiente = null
            _binding?.let {
                it.tvError.text = "Necesitamos acceso a la red local para conectar con el servidor de la universidad. Si no aparece el aviso, activalo en Ajustes."
                it.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun conRedLocal(accion: () -> Unit) {
        // Android 17 exige este permiso para llegar al servidor local.
        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
                requireContext(), "android.permission.ACCESS_LOCAL_NETWORK",
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            binding.tvError.visibility = View.GONE
            accionPendiente = accion
            permisoRedLocal.launch("android.permission.ACCESS_LOCAL_NETWORK")
        } else {
            accion()
        }
    }

    private fun enviarLogin() {
        val vista = _binding ?: return
        vista.tvError.visibility = View.GONE
        viewModel.iniciarSesion(
            vista.etCorreo.text?.toString().orEmpty(),
            vista.etPassword.text?.toString().orEmpty(),
        )
    }

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
            conRedLocal { enviarLogin() }
        }

        binding.btnRegistro.setOnClickListener {
            conRedLocal { findNavController().navigate(R.id.action_login_a_registroCorreo) }
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

        // En el servidor el rol solo puede ser ESTUDIANTE o ADMIN. Ser tutor se guarda aparte:
        // es un estudiante cuya postulación fue aprobada, y eso viene en es_tutor_aprobado.
        val destino = if (usuario.esTutorAprobado) Tutor::class.java else Estudiante::class.java
        startActivity(Intent(requireContext(), destino))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
