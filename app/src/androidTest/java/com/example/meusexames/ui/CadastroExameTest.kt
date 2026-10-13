package com.example.meusexames.ui

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meusexames.R
import com.example.meusexames.bll.ArquivoSelecionado
import com.example.meusexames.bll.DadosExame
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import com.example.meusexames.ui.theme.MeusExamesTheme
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CadastroExameTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fusoOriginal = TimeZone.getDefault()
    private val localidadeOriginal = Locale.getDefault(Locale.Category.FORMAT)
    private val arquivo = Uri.parse("content://exames.teste/hemograma.pdf")
    private val exameGravado = Exame(
        id = 7,
        titulo = "Hemograma",
        descricao = "Exame de rotina",
        dataExame = LocalDate.of(2026, 9, 30),
        caminhoArquivo = "exames/gravado.jpg",
        tipoArquivo = TipoArquivo.IMAGEM,
        houveAlteracao = true,
        comentario = "Repetir em seis meses",
    )
    private val salvos = mutableListOf<DadosExame>()
    private val abertos = mutableListOf<Uri?>()
    private var cancelamentos = 0

    @After
    fun restaurarFusoELocalidade() {
        TimeZone.setDefault(fusoOriginal)
        Locale.setDefault(Locale.Category.FORMAT, localidadeOriginal)
    }

    @Test
    fun exibeOsCamposComNaoSelecionadoPorPadrao() {
        exibir()

        composeRule.onNodeWithText(texto(R.string.titulo_cadastro_exame)).assertIsDisplayed()
        val rotulos = listOf(
            R.string.titulo_exame,
            R.string.descricao_exame,
            R.string.data_exame,
            R.string.arquivo_exame,
            R.string.tipo_arquivo,
            R.string.selecionar_arquivo,
            R.string.houve_alteracao,
            R.string.sim,
            R.string.comentario,
            R.string.cancelar,
            R.string.salvar,
        )
        for (rotulo in rotulos) {
            composeRule.onNodeWithText(texto(rotulo)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText(texto(R.string.nao)).assertIsSelected()
        composeRule.onNodeWithText(texto(R.string.abrir_arquivo)).assertDoesNotExist()
    }

    @Test
    fun salvarEnviaOsDadosPreenchidos() {
        // Um fuso atrás do UTC denuncia a data convertida com o fuso do aparelho.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        exibir()

        composeRule.onNodeWithText(texto(R.string.titulo_exame)).performTextInput("Hemograma")
        composeRule.onNodeWithText(texto(R.string.descricao_exame))
            .performTextInput("Exame de rotina")
        composeRule.onNodeWithText(texto(R.string.data_exame)).performClick()
        composeRule.onNodeWithText("15", substring = true).performClick()
        composeRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        composeRule.onNodeWithText(texto(R.string.selecionar_arquivo))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("hemograma.pdf").assertExists()
        composeRule.onNodeWithText(texto(R.string.tipo_arquivo_pdf)).assertExists()
        composeRule.onNodeWithText(texto(R.string.sim)).performScrollTo().performClick()
        composeRule.onNodeWithText(texto(R.string.comentario))
            .performScrollTo()
            .performTextInput("Repetir em seis meses")
        composeRule.onNodeWithText(texto(R.string.salvar)).performScrollTo().performClick()

        val esperado = DadosExame(
            titulo = "Hemograma",
            descricao = "Exame de rotina",
            dataExame = LocalDate.now().withDayOfMonth(15),
            arquivo = arquivo,
            houveAlteracao = true,
            comentario = "Repetir em seis meses",
        )
        assertEquals(listOf(esperado), salvos)
    }

    @Test
    fun cancelarChamaAoCancelar() {
        exibir()

        composeRule.onNodeWithText(texto(R.string.cancelar)).performScrollTo().performClick()

        assertEquals(1, cancelamentos)
        assertTrue(salvos.isEmpty())
    }

    @Test
    fun enquantoSalvaOsBotoesFicamDesabilitados() {
        exibir(salvando = true)

        composeRule.onNodeWithText(texto(R.string.salvar)).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(texto(R.string.cancelar)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun naEdicaoExibeOsDadosDoExame() {
        // A data segue o formato do idioma do aparelho; em pt-BR ele é dia/mês/ano.
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("pt-BR"))
        exibir(exame = exameGravado)

        composeRule.onNodeWithText(texto(R.string.titulo_edicao_exame)).assertIsDisplayed()
        val valores = listOf(
            "Hemograma",
            "Exame de rotina",
            "30/09/2026",
            "gravado.jpg",
            texto(R.string.tipo_arquivo_imagem),
            "Repetir em seis meses",
        )
        for (valor in valores) {
            composeRule.onNodeWithText(valor).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText(texto(R.string.sim)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText(texto(R.string.abrir_arquivo))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun naEdicaoSalvarSemEscolherArquivoEnviaOsDadosAlterados() {
        exibir(exame = exameGravado)

        composeRule.onNodeWithText("Hemograma").performTextReplacement("Hemograma revisado")
        composeRule.onNodeWithText(texto(R.string.nao)).performScrollTo().performClick()
        composeRule.onNodeWithText(texto(R.string.salvar)).performScrollTo().performClick()

        val esperado = DadosExame(
            titulo = "Hemograma revisado",
            descricao = "Exame de rotina",
            dataExame = LocalDate.of(2026, 9, 30),
            arquivo = null,
            houveAlteracao = false,
            comentario = "Repetir em seis meses",
        )
        assertEquals(listOf(esperado), salvos)
    }

    @Test
    fun naEdicaoOArquivoEscolhidoSubstituiOGravadoNaTela() {
        exibir(exame = exameGravado)

        composeRule.onNodeWithText(texto(R.string.abrir_arquivo)).performScrollTo().performClick()
        composeRule.onNodeWithText(texto(R.string.selecionar_arquivo))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("hemograma.pdf").assertExists()
        composeRule.onNodeWithText(texto(R.string.tipo_arquivo_pdf)).assertExists()
        composeRule.onNodeWithText("gravado.jpg").assertDoesNotExist()
        composeRule.onNodeWithText(texto(R.string.abrir_arquivo)).performScrollTo().performClick()
        composeRule.onNodeWithText(texto(R.string.salvar)).performScrollTo().performClick()

        // Abrir vale para o arquivo que a tela mostra: primeiro o gravado, depois o escolhido.
        assertEquals(listOf(null, arquivo), abertos)
        assertEquals(arquivo, salvos.single().arquivo)
    }

    private fun exibir(salvando: Boolean = false, exame: Exame? = null) {
        val seletorQueEscolheOArquivo = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    dispatchResult(requestCode, arquivo)
                }
            }
        }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides seletorQueEscolheOArquivo,
            ) {
                MeusExamesTheme {
                    CadastroExame(
                        identificarArquivo = {
                            ArquivoSelecionado("hemograma.pdf", TipoArquivo.PDF)
                        },
                        aoSalvar = { salvos += it },
                        aoCancelar = { cancelamentos++ },
                        exame = exame,
                        aoAbrirArquivo = { abertos += it },
                        salvando = salvando,
                    )
                }
            }
        }
    }

    private fun texto(id: Int) = context.getString(id)
}
