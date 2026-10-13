package com.example.meusexames.bll

import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meusexames.dal.AppDatabase
import com.example.meusexames.dal.ArmazenamentoArquivos
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExameBllTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pastaDeOrigem = File(context.cacheDir, "origem-dos-testes")
    private val localidadeOriginal = Locale.getDefault(Locale.Category.FORMAT)
    private lateinit var banco: AppDatabase
    private lateinit var bll: ExameBll

    @Before
    fun preparar() {
        pastaDeOrigem.mkdirs()
        banco = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bll = ExameBll(banco.exameDao(), ArmazenamentoArquivos(context), context.resources)
    }

    @After
    fun limpar() {
        runBlocking { gravados() }.forEach { copiaDe(it).delete() }
        banco.close()
        pastaDeOrigem.deleteRecursively()
        Locale.setDefault(Locale.Category.FORMAT, localidadeOriginal)
    }

    @Test
    fun cadastrarGravaOExameECopiaOArquivoParaOApp() = runBlocking {
        val erro = bll.cadastrar(
            DadosExame(
                titulo = "Hemograma completo",
                descricao = "Exame de rotina",
                dataExame = LocalDate.of(2026, 9, 30),
                arquivo = arquivoDeOrigem("hemograma.pdf", conteudo = "resultado do hemograma"),
                houveAlteracao = true,
                comentario = "Repetir em seis meses",
            )
        )

        assertNull(erro)
        val gravado = gravados().single()
        assertEquals("Hemograma completo", gravado.titulo)
        assertEquals("Exame de rotina", gravado.descricao)
        assertEquals(LocalDate.of(2026, 9, 30), gravado.dataExame)
        assertEquals(TipoArquivo.PDF, gravado.tipoArquivo)
        assertTrue(gravado.houveAlteracao)
        assertEquals("Repetir em seis meses", gravado.comentario)
        assertTrue(gravado.caminhoArquivo, gravado.caminhoArquivo.startsWith("exames/"))
        assertEquals("pdf", copiaDe(gravado).extension)
        assertEquals("resultado do hemograma", copiaDe(gravado).readText())
    }

    @Test
    fun cadastrarIgnoraEspacosNasPontasEOpcionalEmBranco() = runBlocking {
        val erro = bll.cadastrar(
            dados(titulo = "  Hemograma \n", descricao = "   ", comentario = "\n Repetir \t")
        )

        assertNull(erro)
        val gravado = gravados().single()
        assertEquals("Hemograma", gravado.titulo)
        assertNull(gravado.descricao)
        assertEquals("Repetir", gravado.comentario)
    }

    @Test
    fun tituloPrecisaTerEntre3E64Caracteres() = runBlocking {
        for (titulo in listOf("", "   ", "ab", "  ab  ", "a".repeat(65))) {
            val erro = bll.cadastrar(dados(titulo = titulo))
            assertEquals(titulo, ErroCadastro.TITULO_INVALIDO, erro)
        }
        assertTrue(gravados().isEmpty())

        for (titulo in listOf("abc", "  abc  ", "a".repeat(64), " " + "a".repeat(64) + " ")) {
            assertNull(titulo, bll.cadastrar(dados(titulo = titulo)))
        }
    }

    @Test
    fun descricaoPreenchidaPrecisaTerEntre3E256Caracteres() = runBlocking {
        for (descricao in listOf("ab", "  ab  ", "a".repeat(257))) {
            val erro = bll.cadastrar(dados(descricao = descricao))
            assertEquals(descricao, ErroCadastro.DESCRICAO_INVALIDA, erro)
        }
        assertTrue(gravados().isEmpty())

        for (descricao in listOf("", "abc", "a".repeat(256))) {
            assertNull(descricao, bll.cadastrar(dados(descricao = descricao)))
        }
    }

    @Test
    fun comentarioPreenchidoPrecisaTerEntre3E1024Caracteres() = runBlocking {
        for (comentario in listOf("ab", "  ab  ", "a".repeat(1025))) {
            val erro = bll.cadastrar(dados(comentario = comentario))
            assertEquals(comentario, ErroCadastro.COMENTARIO_INVALIDO, erro)
        }
        assertTrue(gravados().isEmpty())

        for (comentario in listOf("", "abc", "a".repeat(1024))) {
            assertNull(comentario, bll.cadastrar(dados(comentario = comentario)))
        }
    }

    @Test
    fun dataDoExameEObrigatoria() = runBlocking {
        val erro = bll.cadastrar(dados(dataExame = null))

        assertEquals(ErroCadastro.DATA_OBRIGATORIA, erro)
        assertTrue(gravados().isEmpty())
    }

    @Test
    fun arquivoEObrigatorio() = runBlocking {
        val erro = bll.cadastrar(dados(arquivo = null))

        assertEquals(ErroCadastro.ARQUIVO_OBRIGATORIO, erro)
        assertTrue(gravados().isEmpty())
    }

    @Test
    fun arquivoPrecisaSerDeUmTipoAceito() = runBlocking {
        val erro = bll.cadastrar(dados(arquivo = arquivoDeOrigem("notas.txt")))

        assertEquals(ErroCadastro.TIPO_ARQUIVO_INVALIDO, erro)
        assertTrue(gravados().isEmpty())
    }

    @Test
    fun arquivoQueNaoPodeSerLidoNaoGravaOExame() = runBlocking {
        val arquivoInexistente = Uri.fromFile(File(pastaDeOrigem, "sumiu.pdf"))

        val erro = bll.cadastrar(dados(arquivo = arquivoInexistente))

        assertEquals(ErroCadastro.FALHA_AO_SALVAR, erro)
        assertTrue(gravados().isEmpty())
    }

    @Test
    fun buscarPorIdDevolveOExameGravado() = runBlocking {
        val exame = cadastrado(dados())

        assertEquals(exame, bll.buscarPorId(exame.id))
        assertNull(bll.buscarPorId(exame.id + 1))
    }

    @Test
    fun atualizarGravaAsAlteracoesEMantemOArquivo() = runBlocking {
        val original = cadastrado(
            dados(arquivo = arquivoDeOrigem("hemograma.pdf", conteudo = "resultado do hemograma"))
        )

        val erro = bll.atualizar(
            original.id,
            DadosExame(
                titulo = "  Hemograma revisado ",
                descricao = "   ",
                dataExame = LocalDate.of(2026, 10, 1),
                arquivo = null,
                houveAlteracao = true,
                comentario = "Repetir em seis meses",
            ),
        )

        assertNull(erro)
        val atualizado = gravados().single()
        val esperado = original.copy(
            titulo = "Hemograma revisado",
            descricao = null,
            dataExame = LocalDate.of(2026, 10, 1),
            houveAlteracao = true,
            comentario = "Repetir em seis meses",
            dataAlteracao = atualizado.dataAlteracao,
        )
        assertEquals(esperado, atualizado)
        assertEquals("resultado do hemograma", copiaDe(atualizado).readText())
    }

    @Test
    fun atualizarComOutroArquivoGravaONovoEApagaOOriginal() = runBlocking {
        val original = cadastrado(dados(arquivo = arquivoDeOrigem("hemograma.pdf")))

        val erro = bll.atualizar(
            original.id,
            dados(arquivo = arquivoDeOrigem("raio-x.jpg", conteudo = "imagem do raio-x")),
        )

        assertNull(erro)
        val atualizado = gravados().single()
        assertEquals(original.id, atualizado.id)
        assertEquals(TipoArquivo.IMAGEM, atualizado.tipoArquivo)
        assertEquals("imagem do raio-x", copiaDe(atualizado).readText())
        assertFalse(copiaDe(original).exists())
    }

    @Test
    fun atualizarQueFalhaNaoMexeNoExameNemNosArquivos() = runBlocking {
        val original = cadastrado(dados())
        val arquivosAntes = arquivosDosExames()
        val falhas = listOf(
            dados(titulo = "ab") to ErroCadastro.TITULO_INVALIDO,
            dados(descricao = "ab") to ErroCadastro.DESCRICAO_INVALIDA,
            dados(dataExame = null) to ErroCadastro.DATA_OBRIGATORIA,
            dados(arquivo = arquivoDeOrigem("notas.txt")) to ErroCadastro.TIPO_ARQUIVO_INVALIDO,
            dados(comentario = "ab") to ErroCadastro.COMENTARIO_INVALIDO,
            dados(arquivo = Uri.fromFile(File(pastaDeOrigem, "sumiu.pdf"))) to
                ErroCadastro.FALHA_AO_SALVAR,
        )

        for ((alteracoes, erro) in falhas) {
            assertEquals(erro, bll.atualizar(original.id, alteracoes))
        }

        assertEquals(listOf(original), gravados())
        assertEquals(arquivosAntes, arquivosDosExames())
    }

    @Test
    fun atualizarExameQueNaoExisteDevolveErro() = runBlocking {
        val erro = bll.atualizar(id = 123, dados())

        assertEquals(ErroCadastro.EXAME_NAO_ENCONTRADO, erro)
        assertTrue(gravados().isEmpty())
    }

    @Test
    fun excluirApagaOExameEOArquivoDeleSemMexerNosOutros() = runBlocking {
        val mantido = cadastrado(dados(titulo = "Eletro"))
        assertNull(bll.cadastrar(dados(titulo = "Hemograma")))
        val apagado = gravados().single { it.id != mantido.id }

        assertTrue(bll.excluir(apagado))

        assertEquals(listOf(mantido), gravados())
        assertFalse(copiaDe(apagado).exists())
        assertTrue(copiaDe(mantido).exists())
    }

    @Test
    fun excluirQueFalhaNaoMexeNoExameNemNoArquivo() = runBlocking {
        val exame = cadastrado(dados())
        // O banco passa a recusar a exclusão, como numa falha de gravação.
        banco.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER recusa_exclusao BEFORE DELETE ON exames " +
                "BEGIN SELECT RAISE(ABORT, 'exclusão recusada'); END"
        )

        assertFalse(bll.excluir(exame))

        assertEquals(listOf(exame), gravados())
        assertTrue(copiaDe(exame).exists())
    }

    @Test
    fun localizarArquivoDoExameDevolveEnderecoLegivelEOTipoExato() = runBlocking {
        val exame = cadastrado(dados(arquivo = arquivoDeOrigem("laudo.docx", conteudo = "laudo")))

        val localizado = bll.localizarArquivo(exame)

        assertEquals("content", localizado.uri.scheme)
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            localizado.tipoMime,
        )
        val lido = context.contentResolver.openInputStream(localizado.uri)!!
            .use { it.readBytes().decodeToString() }
        assertEquals("laudo", lido)
    }

    @Test
    fun localizarArquivoSemExtensaoUsaOTipoGravadoNoExame() = runBlocking {
        val tiposMime = mapOf(
            TipoArquivo.PDF to "application/pdf",
            TipoArquivo.IMAGEM to "image/*",
            TipoArquivo.DOC to "application/msword",
        )
        for ((tipo, tipoMime) in tiposMime) {
            val exame = Exame(
                titulo = "Hemograma",
                dataExame = LocalDate.of(2026, 9, 30),
                caminhoArquivo = "exames/sem-extensao",
                tipoArquivo = tipo,
            )
            assertEquals(tipoMime, bll.localizarArquivo(exame).tipoMime)
        }
    }

    @Test
    fun localizarArquivoDoExameDaAoArquivoOTituloDoExameComoNome() = runBlocking {
        val exame = cadastrado(
            dados(titulo = "Eletrocardiograma de esforço", arquivo = arquivoDeOrigem("laudo.pdf"))
        )

        val nome = nomeParaOutrosApps(bll.localizarArquivo(exame))

        assertEquals("Eletrocardiograma de esforço.pdf", nome)
    }

    @Test
    fun nomeDoArquivoTrocaOQueNaoPodeAparecerEmNomeDeArquivo() = runBlocking {
        for (invalido in "\"*/:<>?\\|\n") {
            val exame = exameSemCopia("Glicemia ${invalido}jejum", "exames/copia.pdf")

            val nome = nomeParaOutrosApps(bll.localizarArquivo(exame))

            assertEquals("Glicemia _jejum.pdf", nome)
        }
    }

    @Test
    fun arquivoSemExtensaoFicaSoComOTituloDoExameComoNome() = runBlocking {
        val exame = exameSemCopia("Hemograma", "exames/sem-extensao")

        assertEquals("Hemograma", nomeParaOutrosApps(bll.localizarArquivo(exame)))
    }

    @Test
    fun localizarArquivoEscolhidoNaTelaDevolveOProprioEnderecoEOTipo() = runBlocking {
        val arquivo = arquivoDeOrigem("raio-x.png")

        assertEquals(ArquivoLocalizado(arquivo, "image/png"), bll.localizarArquivo(arquivo))
    }

    @Test
    fun textoParaCompartilharSegueOModeloComTituloEDataDoExame() {
        // A data segue o formato do idioma do aparelho; em pt-BR ele é dia/mês/ano.
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("pt-BR"))
        val exame = Exame(
            titulo = "Hemograma",
            dataExame = LocalDate.of(2026, 10, 13),
            caminhoArquivo = "exames/hemograma.pdf",
            tipoArquivo = TipoArquivo.PDF,
        )

        assertEquals(
            "Exame: Hemograma\nData do exame: 13/10/2026",
            bll.textoParaCompartilhar(exame),
        )
    }

    @Test
    fun identificarArquivoDevolveNomeETipo() = runBlocking {
        val tipos = mapOf(
            "hemograma.pdf" to TipoArquivo.PDF,
            "raio-x.JPG" to TipoArquivo.IMAGEM,
            "laudo.doc" to TipoArquivo.DOC,
            "laudo.docx" to TipoArquivo.DOC,
            "notas.txt" to null,
        )
        for ((nome, tipo) in tipos) {
            val identificado = bll.identificarArquivo(arquivoDeOrigem(nome))
            assertEquals(ArquivoSelecionado(nome, tipo), identificado)
        }
    }

    private suspend fun gravados(): List<Exame> = banco.exameDao().listar().first()

    private suspend fun cadastrado(novo: DadosExame): Exame {
        assertNull(bll.cadastrar(novo))
        return gravados().single()
    }

    private fun copiaDe(exame: Exame) = File(context.filesDir, exame.caminhoArquivo)

    // Um exame que não passou pelo cadastro: o arquivo dele não existe.
    private fun exameSemCopia(titulo: String, caminhoArquivo: String) = Exame(
        titulo = titulo,
        dataExame = LocalDate.of(2026, 9, 30),
        caminhoArquivo = caminhoArquivo,
        tipoArquivo = TipoArquivo.PDF,
    )

    // O nome que o arquivo tem para o app que recebe o endereço dele.
    private fun nomeParaOutrosApps(arquivo: ArquivoLocalizado): String =
        context.contentResolver
            .query(arquivo.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getString(0)
            }

    private fun arquivosDosExames(): Set<String> =
        File(context.filesDir, "exames").list().orEmpty().toSet()

    private fun arquivoDeOrigem(nome: String, conteudo: String = "conteúdo"): Uri {
        val arquivo = File(pastaDeOrigem, nome)
        arquivo.writeText(conteudo)
        return Uri.fromFile(arquivo)
    }

    private fun dados(
        titulo: String = "Hemograma",
        descricao: String = "",
        dataExame: LocalDate? = LocalDate.of(2026, 9, 30),
        arquivo: Uri? = arquivoDeOrigem("hemograma.pdf"),
        comentario: String = "",
    ) = DadosExame(
        titulo = titulo,
        descricao = descricao,
        dataExame = dataExame,
        arquivo = arquivo,
        houveAlteracao = false,
        comentario = comentario,
    )
}
