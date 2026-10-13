package com.example.meusexames.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meusexames.R
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import com.example.meusexames.ui.theme.MeusExamesTheme
import java.time.LocalDate
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListaExamesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val localidadeOriginal = Locale.getDefault(Locale.Category.FORMAT)

    @After
    fun restaurarLocalidade() {
        Locale.setDefault(Locale.Category.FORMAT, localidadeOriginal)
    }

    @Test
    fun exibeTituloDaTelaEBotaoDeCadastrar() {
        exibir(emptyList())

        composeRule.onNodeWithText(context.getString(R.string.titulo_lista_exames))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cadastrar_exame))
            .assertIsDisplayed()
    }

    @Test
    fun exibeUmCardComTituloEBotoesParaCadaExame() {
        exibir(
            listOf(
                novoExame(id = 1, titulo = "Hemograma"),
                novoExame(id = 2, titulo = "Eletro"),
            )
        )

        composeRule.onNodeWithText("Hemograma").assertIsDisplayed()
        composeRule.onNodeWithText("Eletro").assertIsDisplayed()
        composeRule.onAllNodesWithText(context.getString(R.string.editar)).assertCountEquals(2)
        composeRule.onAllNodesWithText(context.getString(R.string.apagar)).assertCountEquals(2)
        composeRule.onAllNodesWithText(context.getString(R.string.compartilhar))
            .assertCountEquals(2)
    }

    @Test
    fun exibeDataDoExameAbaixoDoTitulo() {
        // A data segue o formato do idioma do aparelho; em pt-BR ele é dia/mês/ano.
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("pt-BR"))
        val dataExame = LocalDate.of(2026, 10, 13)
        exibir(listOf(novoExame(id = 1, titulo = "Hemograma", dataExame = dataExame)))

        val titulo = composeRule.onNodeWithText("Hemograma").getUnclippedBoundsInRoot()
        val data = composeRule
            .onNodeWithText(context.getString(R.string.data_exame_formatada, "13/10/2026"))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue(data.top >= titulo.bottom)
    }

    @Test
    fun exibeSeHouveAlteracaoEmCadaExame() {
        exibir(
            listOf(
                novoExame(id = 1, titulo = "Hemograma", houveAlteracao = true),
                novoExame(id = 2, titulo = "Eletro"),
            )
        )

        val sim = composeRule.onNodeWithText(houveAlteracao(R.string.sim))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val nao = composeRule.onNodeWithText(houveAlteracao(R.string.nao))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        // O exame com alteração é o do primeiro card.
        assertTrue(sim.top < nao.top)
    }

    @Test
    fun exibeADescricaoInteiraQuandoElaCabeNoLimite() {
        // Vinte caracteres, o máximo que o card mostra sem cortar.
        exibir(
            listOf(novoExame(id = 1, titulo = "Hemograma", descricao = "Colesterol e glicose"))
        )

        composeRule.onNodeWithText(descricao("Colesterol e glicose")).assertIsDisplayed()
    }

    @Test
    fun cortaADescricaoQuePassaDoLimiteEPoeReticencias() {
        // Vinte e um caracteres: só o último fica de fora.
        exibir(
            listOf(novoExame(id = 1, titulo = "Hemograma", descricao = "Exame de rotina anual"))
        )

        composeRule.onNodeWithText(descricao("Exame de rotina anua...")).assertIsDisplayed()
    }

    @Test
    fun naoCortaADescricaoNoMeioDeUmEmoji() {
        // O emoji ocupa o vigésimo e o vigésimo primeiro caracteres.
        exibir(
            listOf(novoExame(id = 1, titulo = "Hemograma", descricao = "Exames de sangue ok😀"))
        )

        composeRule.onNodeWithText(descricao("Exames de sangue ok...")).assertIsDisplayed()
    }

    @Test
    fun exameSemDescricaoNaoExibeADescricao() {
        exibir(listOf(novoExame(id = 1, titulo = "Hemograma")))

        composeRule.onNodeWithText(descricao(""), substring = true).assertDoesNotExist()
    }

    @Test
    fun exibeCampoDePesquisaAcimaDaListaComOBotaoADireitaDele() {
        exibir(listOf(novoExame(id = 1, titulo = "Hemograma")))

        val campo = composeRule.onNodeWithText(context.getString(R.string.pesquisar_exame))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val botao = composeRule.onNodeWithContentDescription(context.getString(R.string.pesquisar))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val exame = composeRule.onNodeWithText("Hemograma").getUnclippedBoundsInRoot()
        assertTrue(botao.left >= campo.right)
        assertTrue(exame.top >= campo.bottom)
    }

    @Test
    fun botaoDePesquisarChamaAoPesquisarComOTermoDigitado() {
        val pesquisados = mutableListOf<String>()
        exibir(emptyList(), aoPesquisar = { pesquisados += it })

        composeRule.onNodeWithText(context.getString(R.string.pesquisar_exame))
            .performTextInput("hemograma")
        // Digitar não pesquisa: a pesquisa só sai quando o botão é tocado.
        assertTrue(pesquisados.isEmpty())
        composeRule.onNodeWithContentDescription(context.getString(R.string.pesquisar))
            .performClick()

        assertEquals(listOf("hemograma"), pesquisados)
    }

    @Test
    fun botaoDePesquisarComOCampoVazioChamaAoPesquisarSemTermo() {
        val pesquisados = mutableListOf<String>()
        exibir(emptyList(), aoPesquisar = { pesquisados += it })

        composeRule.onNodeWithContentDescription(context.getString(R.string.pesquisar))
            .performClick()

        assertEquals(listOf(""), pesquisados)
    }

    @Test
    fun teclaDePesquisaDoTecladoChamaAoPesquisarComOTermoDigitado() {
        val pesquisados = mutableListOf<String>()
        exibir(emptyList(), aoPesquisar = { pesquisados += it })

        // Com o termo digitado o campo deixa de mostrar o texto que o identificava.
        val campo = composeRule.onNode(hasSetTextAction())
        campo.performTextInput("eletro")
        campo.performImeAction()

        assertEquals(listOf("eletro"), pesquisados)
    }

    @Test
    fun botaoDeCadastrarChamaAoCadastrar() {
        var cadastros = 0
        exibir(emptyList(), aoCadastrar = { cadastros++ })

        composeRule.onNodeWithContentDescription(context.getString(R.string.cadastrar_exame))
            .performClick()

        assertEquals(1, cadastros)
    }

    @Test
    fun botaoEditarChamaAoEditarComOExameDoCard() {
        val hemograma = novoExame(id = 1, titulo = "Hemograma")
        val eletro = novoExame(id = 2, titulo = "Eletro")
        val editados = mutableListOf<Exame>()
        exibir(listOf(hemograma, eletro), aoEditar = { editados += it })

        composeRule.onAllNodesWithText(context.getString(R.string.editar))[1].performClick()

        assertEquals(listOf(eletro), editados)
    }

    @Test
    fun botaoApagarPedeConfirmacaoAntesDeApagar() {
        val apagados = mutableListOf<Exame>()
        exibir(listOf(novoExame(id = 1, titulo = "Hemograma")), aoApagar = { apagados += it })

        composeRule.onNodeWithText(context.getString(R.string.apagar)).performClick()

        composeRule.onNodeWithText(context.getString(R.string.titulo_apagar_exame))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.mensagem_apagar_exame, "Hemograma"))
            .assertIsDisplayed()
        assertTrue(apagados.isEmpty())
    }

    @Test
    fun confirmarAExclusaoChamaAoApagarComOExameDoCard() {
        val hemograma = novoExame(id = 1, titulo = "Hemograma")
        val eletro = novoExame(id = 2, titulo = "Eletro")
        val apagados = mutableListOf<Exame>()
        exibir(listOf(hemograma, eletro), aoApagar = { apagados += it })

        composeRule.onAllNodesWithText(context.getString(R.string.apagar))[1].performClick()
        botaoDaConfirmacao(R.string.apagar).performClick()

        assertEquals(listOf(eletro), apagados)
        composeRule.onNodeWithText(context.getString(R.string.titulo_apagar_exame))
            .assertDoesNotExist()
    }

    @Test
    fun cancelarAExclusaoNaoChamaAoApagar() {
        val apagados = mutableListOf<Exame>()
        exibir(listOf(novoExame(id = 1, titulo = "Hemograma")), aoApagar = { apagados += it })

        composeRule.onNodeWithText(context.getString(R.string.apagar)).performClick()
        botaoDaConfirmacao(R.string.cancelar).performClick()

        assertTrue(apagados.isEmpty())
        composeRule.onNodeWithText(context.getString(R.string.titulo_apagar_exame))
            .assertDoesNotExist()
    }

    @Test
    fun botaoCompartilharChamaAoCompartilharComOExameDoCard() {
        val hemograma = novoExame(id = 1, titulo = "Hemograma")
        val eletro = novoExame(id = 2, titulo = "Eletro")
        val compartilhados = mutableListOf<Exame>()
        exibir(listOf(hemograma, eletro), aoCompartilhar = { compartilhados += it })

        composeRule.onAllNodesWithText(context.getString(R.string.compartilhar))[1].performClick()

        assertEquals(listOf(eletro), compartilhados)
    }

    private fun exibir(
        exames: List<Exame>,
        aoPesquisar: (String) -> Unit = {},
        aoCadastrar: () -> Unit = {},
        aoEditar: (Exame) -> Unit = {},
        aoApagar: (Exame) -> Unit = {},
        aoCompartilhar: (Exame) -> Unit = {},
    ) {
        composeRule.setContent {
            MeusExamesTheme {
                ListaExames(
                    exames = exames,
                    aoPesquisar = aoPesquisar,
                    aoCadastrar = aoCadastrar,
                    aoEditar = aoEditar,
                    aoApagar = aoApagar,
                    aoCompartilhar = aoCompartilhar,
                )
            }
        }
    }

    // O botão de confirmar tem o mesmo texto do botão do card; o que os separa é o diálogo.
    private fun botaoDaConfirmacao(rotulo: Int) =
        composeRule.onNode(hasText(context.getString(rotulo)) and hasAnyAncestor(isDialog()))

    private fun novoExame(
        id: Long,
        titulo: String,
        dataExame: LocalDate = LocalDate.of(2026, 9, 30),
        descricao: String? = null,
        houveAlteracao: Boolean = false,
    ) = Exame(
        id = id,
        titulo = titulo,
        descricao = descricao,
        dataExame = dataExame,
        caminhoArquivo = "exames/hemograma.pdf",
        tipoArquivo = TipoArquivo.PDF,
        houveAlteracao = houveAlteracao,
    )

    private fun houveAlteracao(resposta: Int) =
        context.getString(R.string.houve_alteracao_formatada, context.getString(resposta))

    private fun descricao(exibida: String) =
        context.getString(R.string.descricao_exame_formatada, exibida)
}
