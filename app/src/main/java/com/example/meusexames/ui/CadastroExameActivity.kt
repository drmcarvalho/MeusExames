package com.example.meusexames.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.PluralsRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.meusexames.R
import com.example.meusexames.bll.DadosExame
import com.example.meusexames.bll.ErroCadastro
import com.example.meusexames.bll.ExameBll
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.ui.theme.MeusExamesTheme
import kotlinx.coroutines.launch

class CadastroExameActivity : ComponentActivity() {

    private val exameBll by lazy { ExameBll(this) }

    // Nulo quando a tela foi aberta para cadastrar um exame novo.
    private val idExame by lazy {
        if (intent.hasExtra(EXTRA_ID_EXAME)) intent.getLongExtra(EXTRA_ID_EXAME, 0) else null
    }
    private var exameEmEdicao by mutableStateOf<Exame?>(null)
    private var salvando by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val idExame = idExame
        if (idExame != null) {
            setTitle(R.string.titulo_edicao_exame)
            carregar(idExame)
        }
        setContent {
            MeusExamesTheme {
                if (idExame == null || exameEmEdicao != null) {
                    CadastroExame(
                        identificarArquivo = exameBll::identificarArquivo,
                        aoSalvar = ::salvar,
                        aoCancelar = ::finish,
                        exame = exameEmEdicao,
                        aoAbrirArquivo = ::abrirArquivo,
                        aceitarArquivo = ::aceitarArquivo,
                        salvando = salvando,
                    )
                }
            }
        }
    }

    private fun carregar(idExame: Long) {
        lifecycleScope.launch {
            val exame = exameBll.buscarPorId(idExame)
            if (exame == null) {
                exibirMensagem(getString(R.string.erro_exame_nao_encontrado))
                finish()
            } else {
                exameEmEdicao = exame
            }
        }
    }

    private fun salvar(dados: DadosExame) {
        if (salvando) return
        salvando = true
        lifecycleScope.launch {
            val idExame = idExame
            val erro = if (idExame == null) {
                exameBll.cadastrar(dados)
            } else {
                exameBll.atualizar(idExame, dados)
            }
            if (erro == null) {
                finish()
            } else {
                salvando = false
                exibirMensagem(mensagemDe(erro))
            }
        }
    }

    private suspend fun aceitarArquivo(arquivo: Uri): Boolean {
        if (!exameBll.excedeTamanhoMaximo(arquivo)) return true
        exibirMensagem(mensagemDe(ErroCadastro.ARQUIVO_MUITO_GRANDE))
        return false
    }

    private fun abrirArquivo(selecionado: Uri?) {
        val exame = exameEmEdicao ?: return
        lifecycleScope.launch {
            val arquivo = if (selecionado == null) {
                exameBll.localizarArquivo(exame)
            } else {
                exameBll.localizarArquivo(selecionado)
            }
            val abertura = Intent(Intent.ACTION_VIEW)
                .setDataAndType(arquivo.uri, arquivo.tipoMime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                startActivity(abertura)
            } catch (_: ActivityNotFoundException) {
                exibirMensagem(getString(R.string.erro_abrir_arquivo))
            }
        }
    }

    private fun exibirMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    private fun mensagemDe(erro: ErroCadastro): String = when (erro) {
        ErroCadastro.TITULO_INVALIDO ->
            mensagemDeTamanho(R.plurals.erro_titulo, ExameBll.TAMANHO_TITULO)
        ErroCadastro.DESCRICAO_INVALIDA ->
            mensagemDeTamanho(R.plurals.erro_descricao, ExameBll.TAMANHO_DESCRICAO)
        ErroCadastro.DATA_OBRIGATORIA -> getString(R.string.erro_data_exame)
        ErroCadastro.ARQUIVO_OBRIGATORIO -> getString(R.string.erro_arquivo)
        ErroCadastro.TIPO_ARQUIVO_INVALIDO -> getString(R.string.erro_tipo_arquivo)
        ErroCadastro.ARQUIVO_MUITO_GRANDE -> getString(
            R.string.erro_tamanho_arquivo,
            Formatter.formatShortFileSize(this, ExameBll.TAMANHO_MAXIMO_ARQUIVO),
        )
        ErroCadastro.COMENTARIO_INVALIDO ->
            mensagemDeTamanho(R.plurals.erro_comentario, ExameBll.TAMANHO_COMENTARIO)
        ErroCadastro.EXAME_NAO_ENCONTRADO -> getString(R.string.erro_exame_nao_encontrado)
        ErroCadastro.FALHA_AO_SALVAR -> getString(R.string.erro_salvar)
    }

    private fun mensagemDeTamanho(@PluralsRes mensagem: Int, tamanho: IntRange): String =
        resources.getQuantityString(mensagem, tamanho.last, tamanho.first, tamanho.last)

    companion object {
        private const val EXTRA_ID_EXAME = "id_exame"

        fun intentParaEditar(context: Context, idExame: Long): Intent =
            Intent(context, CadastroExameActivity::class.java).putExtra(EXTRA_ID_EXAME, idExame)
    }
}
