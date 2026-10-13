package com.example.meusexames.bll

import android.content.Context
import android.content.res.Resources
import android.database.SQLException
import android.net.Uri
import com.example.meusexames.R
import com.example.meusexames.dal.AppDatabase
import com.example.meusexames.dal.ArmazenamentoArquivos
import com.example.meusexames.dal.dao.ExameDao
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import java.io.File
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExameBll(
    private val exameDao: ExameDao,
    private val arquivos: ArmazenamentoArquivos,
    private val recursos: Resources,
) {

    constructor(context: Context) : this(
        AppDatabase.obterInstancia(context).exameDao(),
        ArmazenamentoArquivos(context),
        context.resources,
    )

    fun listar(): Flow<List<Exame>> = exameDao.listar()

    suspend fun buscarPorId(id: Long): Exame? = exameDao.buscarPorId(id)

    suspend fun identificarArquivo(arquivo: Uri): ArquivoSelecionado =
        ArquivoSelecionado(
            nome = arquivos.obterNome(arquivo),
            tipo = identificarTipo(arquivos.obterTipoMime(arquivo)),
        )

    /**
     * Localiza o arquivo anexado a [exame] para que outro app possa abri-lo ou recebê-lo. Para
     * esse app o arquivo tem o título do exame como nome, e não o nome interno da cópia.
     */
    suspend fun localizarArquivo(exame: Exame): ArquivoLocalizado {
        val uri = arquivos.obterUri(exame.caminhoArquivo, nomeDoArquivo(exame))
        // A extensão da cópia diz o tipo exato do arquivo. Quando ela falta ou não é do tipo
        // gravado no exame, vale o tipo genérico dele.
        val tipoMime = arquivos.obterTipoMime(uri)
            ?.takeIf { identificarTipo(it) == exame.tipoArquivo }
            ?: tipoMimeGenerico(exame.tipoArquivo)
        return ArquivoLocalizado(uri, tipoMime)
    }

    /** Localiza um [arquivo] escolhido na tela, ainda não gravado, para que outro app o abra. */
    suspend fun localizarArquivo(arquivo: Uri): ArquivoLocalizado =
        ArquivoLocalizado(arquivo, arquivos.obterTipoMime(arquivo))

    /** Monta o texto que acompanha [exame] ao compartilhá-lo: o título e a data dele. */
    fun textoParaCompartilhar(exame: Exame): String =
        recursos.getString(
            R.string.modelo_compartilhar_exame,
            exame.titulo,
            // A data sai no formato do idioma do aparelho, como na lista de exames.
            exame.dataExame.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
        )

    /**
     * Valida [dados] e grava o exame, copiando o arquivo anexado para dentro do app.
     * Devolve o que impediu o cadastro, ou null quando o exame foi gravado.
     */
    suspend fun cadastrar(dados: DadosExame): ErroCadastro? {
        val titulo = dados.titulo.trim()
        val descricao = dados.descricao.trim().ifEmpty { null }
        val comentario = dados.comentario.trim().ifEmpty { null }

        if (titulo.length !in TAMANHO_TITULO) return ErroCadastro.TITULO_INVALIDO
        if (descricao != null && descricao.length !in TAMANHO_DESCRICAO) {
            return ErroCadastro.DESCRICAO_INVALIDA
        }
        val dataExame = dados.dataExame ?: return ErroCadastro.DATA_OBRIGATORIA
        val arquivo = dados.arquivo ?: return ErroCadastro.ARQUIVO_OBRIGATORIO
        val tipoArquivo = identificarTipo(arquivos.obterTipoMime(arquivo))
            ?: return ErroCadastro.TIPO_ARQUIVO_INVALIDO
        if (comentario != null && comentario.length !in TAMANHO_COMENTARIO) {
            return ErroCadastro.COMENTARIO_INVALIDO
        }

        // A cópia e a gravação vão até o fim mesmo que a tela seja fechada no meio, para não
        // sobrar arquivo copiado sem exame.
        return withContext(NonCancellable) {
            val caminhoArquivo = try {
                arquivos.copiar(arquivo)
            } catch (e: IOException) {
                return@withContext ErroCadastro.FALHA_AO_SALVAR
            }
            try {
                exameDao.inserir(
                    Exame(
                        titulo = titulo,
                        descricao = descricao,
                        dataExame = dataExame,
                        caminhoArquivo = caminhoArquivo,
                        tipoArquivo = tipoArquivo,
                        houveAlteracao = dados.houveAlteracao,
                        comentario = comentario,
                    )
                )
                null
            } catch (e: SQLException) {
                arquivos.excluir(caminhoArquivo)
                ErroCadastro.FALHA_AO_SALVAR
            }
        }
    }

    /**
     * Valida [dados] e grava as alterações no exame de [id]. Quando outro arquivo foi escolhido,
     * ele é copiado para dentro do app e o que o exame tinha é apagado.
     * Devolve o que impediu a atualização, ou null quando o exame foi gravado.
     */
    suspend fun atualizar(id: Long, dados: DadosExame): ErroCadastro? {
        val exameAtual = exameDao.buscarPorId(id) ?: return ErroCadastro.EXAME_NAO_ENCONTRADO
        val titulo = dados.titulo.trim()
        val descricao = dados.descricao.trim().ifEmpty { null }
        val comentario = dados.comentario.trim().ifEmpty { null }

        // As regras são as de [cadastrar], menos a do arquivo: sem outro escolhido, o exame
        // continua com o que já tem.
        if (titulo.length !in TAMANHO_TITULO) return ErroCadastro.TITULO_INVALIDO
        if (descricao != null && descricao.length !in TAMANHO_DESCRICAO) {
            return ErroCadastro.DESCRICAO_INVALIDA
        }
        val dataExame = dados.dataExame ?: return ErroCadastro.DATA_OBRIGATORIA
        val novoArquivo = dados.arquivo
        val tipoNovoArquivo = novoArquivo?.let {
            identificarTipo(arquivos.obterTipoMime(it))
                ?: return ErroCadastro.TIPO_ARQUIVO_INVALIDO
        }
        if (comentario != null && comentario.length !in TAMANHO_COMENTARIO) {
            return ErroCadastro.COMENTARIO_INVALIDO
        }

        // Como no cadastro, nada aqui é interrompido se a tela for fechada no meio.
        return withContext(NonCancellable) {
            val caminhoNovoArquivo = try {
                novoArquivo?.let { arquivos.copiar(it) }
            } catch (e: IOException) {
                return@withContext ErroCadastro.FALHA_AO_SALVAR
            }
            val erro = try {
                val linhas = exameDao.atualizar(
                    exameAtual.copy(
                        titulo = titulo,
                        descricao = descricao,
                        dataExame = dataExame,
                        caminhoArquivo = caminhoNovoArquivo ?: exameAtual.caminhoArquivo,
                        tipoArquivo = tipoNovoArquivo ?: exameAtual.tipoArquivo,
                        houveAlteracao = dados.houveAlteracao,
                        comentario = comentario,
                    )
                )
                if (linhas > 0) null else ErroCadastro.EXAME_NAO_ENCONTRADO
            } catch (e: SQLException) {
                ErroCadastro.FALHA_AO_SALVAR
            }
            // Dos dois arquivos só fica o que o exame usa: o novo, ou o original quando a
            // gravação não deu certo.
            if (caminhoNovoArquivo != null) {
                val semUso = if (erro == null) exameAtual.caminhoArquivo else caminhoNovoArquivo
                arquivos.excluir(semUso)
            }
            erro
        }
    }

    /**
     * Apaga [exame] e o arquivo anexado a ele, para a cópia não ficar ocupando espaço no app.
     * Devolve false quando o exame não pôde ser apagado.
     */
    suspend fun excluir(exame: Exame): Boolean =
        // Como no cadastro, nada aqui é interrompido se a tela for fechada no meio.
        withContext(NonCancellable) {
            try {
                exameDao.excluir(exame)
            } catch (e: SQLException) {
                return@withContext false
            }
            // O arquivo só sai depois do exame: se a exclusão falhar, o exame continua com ele.
            arquivos.excluir(exame.caminhoArquivo)
            true
        }

    // O título do exame com a extensão da cópia. O que não pode aparecer em nome de arquivo vira
    // "_", como o Android faz ao gravar um arquivo.
    private fun nomeDoArquivo(exame: Exame): String {
        val titulo = exame.titulo.replace(INVALIDO_EM_NOME_DE_ARQUIVO, "_")
        val extensao = File(exame.caminhoArquivo).extension
        return if (extensao.isEmpty()) titulo else "$titulo.$extensao"
    }

    private fun identificarTipo(tipoMime: String?): TipoArquivo? = when {
        tipoMime == null -> null
        tipoMime.startsWith(PREFIXO_MIME_IMAGEM) -> TipoArquivo.IMAGEM
        else -> TIPO_POR_MIME[tipoMime]
    }

    private fun tipoMimeGenerico(tipo: TipoArquivo): String = when (tipo) {
        TipoArquivo.PDF -> TIPO_MIME_PDF
        TipoArquivo.IMAGEM -> TIPO_MIME_IMAGEM
        TipoArquivo.DOC -> TIPO_MIME_DOC
    }

    companion object {
        val TAMANHO_TITULO = 3..64
        val TAMANHO_DESCRICAO = 3..256
        val TAMANHO_COMENTARIO = 3..1024

        private const val PREFIXO_MIME_IMAGEM = "image/"
        private const val TIPO_MIME_IMAGEM = "$PREFIXO_MIME_IMAGEM*"
        private const val TIPO_MIME_PDF = "application/pdf"
        private const val TIPO_MIME_DOC = "application/msword"

        private val TIPO_POR_MIME = mapOf(
            TIPO_MIME_PDF to TipoArquivo.PDF,
            TIPO_MIME_DOC to TipoArquivo.DOC,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to
                TipoArquivo.DOC,
        )

        // No formato que o seletor de arquivos do Android usa para filtrar.
        val TIPOS_MIME_ACEITOS: List<String> = TIPO_POR_MIME.keys.toList() + TIPO_MIME_IMAGEM

        // Os caracteres que o Android recusa em nome de arquivo no armazenamento compartilhado,
        // onde o app que recebe o arquivo costuma gravá-lo.
        private val INVALIDO_EM_NOME_DE_ARQUIVO = Regex("""["*/:<>?\\|\p{Cntrl}]""")
    }
}
