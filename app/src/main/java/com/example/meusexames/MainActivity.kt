package com.example.meusexames

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.meusexames.bll.ExameBll
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.ui.CadastroExameActivity
import com.example.meusexames.ui.ListaExames
import com.example.meusexames.ui.theme.MeusExamesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val exameBll by lazy { ExameBll(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeusExamesTheme {
                // O termo da última pesquisa feita. Antes dela a lista mostra todos os exames.
                var termoPesquisado by rememberSaveable { mutableStateOf<String?>(null) }
                val fluxoExames = remember(termoPesquisado) {
                    termoPesquisado?.let(exameBll::pesquisar) ?: exameBll.listar()
                }
                val exames by fluxoExames.collectAsState(initial = emptyList())
                ListaExames(
                    exames = exames,
                    aoPesquisar = { termoPesquisado = it },
                    aoCadastrar = {
                        startActivity(Intent(this, CadastroExameActivity::class.java))
                    },
                    aoEditar = { exame ->
                        startActivity(CadastroExameActivity.intentParaEditar(this, exame.id))
                    },
                    aoApagar = ::apagar,
                    aoCompartilhar = ::compartilhar,
                )
            }
        }
    }

    private fun apagar(exame: Exame) {
        lifecycleScope.launch {
            if (!exameBll.excluir(exame)) {
                Toast.makeText(this@MainActivity, R.string.erro_apagar, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun compartilhar(exame: Exame) {
        lifecycleScope.launch {
            val arquivo = exameBll.localizarArquivo(exame)
            val envio = Intent(Intent.ACTION_SEND)
                .setType(arquivo.tipoMime)
                .putExtra(Intent.EXTRA_TEXT, exameBll.textoParaCompartilhar(exame))
                .putExtra(Intent.EXTRA_STREAM, arquivo.uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            envio.clipData = ClipData.newRawUri(null, arquivo.uri)
            startActivity(Intent.createChooser(envio, null))
        }
    }
}
