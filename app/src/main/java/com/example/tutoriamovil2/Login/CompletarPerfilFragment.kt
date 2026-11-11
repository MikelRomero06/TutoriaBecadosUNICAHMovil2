package com.example.tutoriamovil2.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.Estudiante.Estudiante
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentCompletarPerfilBinding

class CompletarPerfilFragment : Fragment() {

    private var _binding: FragmentCompletarPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistroViewModel by activityViewModels()

    private var carreraId: Long = 0L
    private var fotoBytes: ByteArray? = null
    private var fotoNombre: String = "foto_perfil.jpg"
    private var fotoMime: String = "image/jpeg"

    private val elegirFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) cargarFoto(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCompletarPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack3.setOnClickListener { findNavController().navigateUp() }
        binding.btnAgregarFoto.setOnClickListener { elegirFoto.launch("image/*") }
        binding.btnCrearCuentaFinal.setOnClickListener { enviar() }

        observarCarreras()
        observarCompletar()
        viewModel.cargarCarreras()
    }

    private fun observarCarreras() {
        viewModel.carrerasEstado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is PasoRegistro.Ok -> llenarCarreras()
                is PasoRegistro.Error -> mostrarError(estado.mensaje)
                else -> Unit
            }
        }
    }

    private fun llenarCarreras() {
        val nombres = viewModel.carreras.map { it.nombre }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombres)
        binding.spinnerCarrera.setAdapter(adapter)
        binding.spinnerCarrera.setOnItemClickListener { _, _, posicion, _ ->
            carreraId = viewModel.carreras[posicion].id
        }
    }

    private fun observarCompletar() {
        viewModel.completarEstado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is PasoRegistro.Inicial -> Unit
                is PasoRegistro.Cargando -> mostrarCargando(true)
                is PasoRegistro.Error -> {
                    mostrarCargando(false)
                    mostrarError(estado.mensaje)
                }
                is PasoRegistro.Ok -> {
                    mostrarCargando(false)
                    viewModel.reiniciar()
                    entrar()
                }
                else -> Unit
            }
        }
    }

    private fun enviar() {
        binding.tvError.visibility = View.GONE
        viewModel.crearCuenta(
            nombre = binding.etNombre.text?.toString().orEmpty(),
            carreraId = carreraId,
            telefono = binding.etTelefono.text?.toString().orEmpty(),
            password = binding.etPassReg.text?.toString().orEmpty(),
            confirmar = binding.etConfirmPass.text?.toString().orEmpty(),
            fotoBytes = fotoBytes,
            fotoNombre = fotoNombre,
            fotoMime = fotoMime,
        )
    }

    private fun cargarFoto(uri: Uri) {
        val resolver = requireContext().contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
        if (bytes == null) {
            mostrarError("No se pudo leer la imagen. Probá con otra.")
            return
        }
        if (bytes.size > 5 * 1024 * 1024) {
            mostrarError("La imagen no puede pesar más de 5 MB.")
            return
        }
        fotoBytes = bytes
        fotoMime = mime
        fotoNombre = "foto_perfil." + when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        binding.ivFoto.imageTintList = null
        binding.ivFoto.setPadding(0, 0, 0, 0)
        binding.ivFoto.setImageURI(uri)
    }

    private fun entrar() {
        Toast.makeText(requireContext(), "¡Tu cuenta quedó lista!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), Estudiante::class.java))
        requireActivity().finish()
    }

    private fun mostrarError(mensaje: String) {
        binding.tvError.text = mensaje
        binding.tvError.visibility = View.VISIBLE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnCrearCuentaFinal.isEnabled = !cargando
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
