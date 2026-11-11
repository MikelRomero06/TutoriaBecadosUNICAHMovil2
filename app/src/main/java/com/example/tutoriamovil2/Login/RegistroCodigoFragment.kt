package com.example.tutoriamovil2.Login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentRegistroCodigoBinding

class RegistroCodigoFragment : Fragment() {

    private var _binding: FragmentRegistroCodigoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistroViewModel by activityViewModels()

    private lateinit var digitos: List<EditText>
    private var contador: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegistroCodigoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        digitos = listOf(
            binding.digito1, binding.digito2, binding.digito3,
            binding.digito4, binding.digito5, binding.digito6,
        )
        prepararDigitos()

        binding.tvEnviadoA.text = "Lo enviamos a ${viewModel.correo}."

        binding.btnBack2.setOnClickListener { findNavController().navigateUp() }

        binding.btnVerificar.setOnClickListener {
            limpiarMensaje()
            viewModel.verificarCodigo(codigoIngresado())
        }

        binding.tvReenviar.setOnClickListener {
            if (binding.tvReenviar.isEnabled) {
                limpiarMensaje()
                viewModel.reenviarCodigo()
            }
        }

        binding.btnAbrirCorreo.setOnClickListener { abrirCorreo() }

        viewModel.codigoEstado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is PasoRegistro.Inicial -> Unit
                is PasoRegistro.Cargando -> mostrarCargando(true)
                is PasoRegistro.Error -> {
                    mostrarCargando(false)
                    mostrarMensaje(estado.mensaje)
                }
                is PasoRegistro.Reenviado -> {
                    mostrarCargando(false)
                    Toast.makeText(requireContext(), "Te enviamos un código nuevo.", Toast.LENGTH_SHORT).show()
                    iniciarContador()
                }
                is PasoRegistro.Ok -> {
                    mostrarCargando(false)
                    viewModel.limpiarCodigo()
                    findNavController().navigate(R.id.action_registroCodigo_a_completarPerfil)
                }
            }
        }

        iniciarContador()
    }

    private fun prepararDigitos() {
        digitos.forEachIndexed { indice, campo ->
            campo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if ((s?.length ?: 0) == 1 && indice < digitos.lastIndex) {
                        digitos[indice + 1].requestFocus()
                    }
                }
            })
            campo.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    campo.text.isEmpty() &&
                    indice > 0
                ) {
                    digitos[indice - 1].apply {
                        requestFocus()
                        setSelection(text.length)
                    }
                }
                false
            }
        }
    }

    private fun codigoIngresado(): String = digitos.joinToString("") { it.text?.toString()?.trim().orEmpty() }

    private fun iniciarContador() {
        contador?.cancel()
        val segundos = viewModel.reenviarEnSegundos
        if (segundos <= 0) {
            habilitarReenvio()
            return
        }
        binding.tvReenviar.isEnabled = false
        binding.tvReenviar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        contador = object : CountDownTimer(segundos * 1000L, 1000L) {
            override fun onTick(restante: Long) {
                val s = (restante / 1000).toInt()
                binding.tvReenviar.text = "Reenviar código en 0:%02d".format(s)
            }

            override fun onFinish() {
                habilitarReenvio()
            }
        }.start()
    }

    private fun habilitarReenvio() {
        binding.tvReenviar.isEnabled = true
        binding.tvReenviar.text = "Reenviar código"
        binding.tvReenviar.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
    }

    private fun mostrarMensaje(mensaje: String) {
        binding.tvIntentos.text = mensaje
        binding.tvIntentos.visibility = View.VISIBLE
    }

    private fun limpiarMensaje() {
        binding.tvIntentos.visibility = View.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnVerificar.isEnabled = !cargando
    }

    private fun abrirCorreo() {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_EMAIL) }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "No se encontró una app de correo.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        contador?.cancel()
        contador = null
        super.onDestroyView()
        _binding = null
    }
}
