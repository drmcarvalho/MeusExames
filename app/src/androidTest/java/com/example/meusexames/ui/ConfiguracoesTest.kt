package com.example.meusexames.ui

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meusexames.R
import com.example.meusexames.ui.theme.MeusExamesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfiguracoesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val bloquearApp =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.bloquear_app)

    @Test
    fun opcaoDeBloqueioSomeQuandoOCelularNaoCumpreOsRequisitos() {
        exibir(bloqueioDisponivel = false)

        composeRule.onNodeWithText(bloquearApp).assertDoesNotExist()
    }

    @Test
    fun tocarNaOpcaoPedeParaLigarSemLigarOSwitch() {
        val pedidos = mutableListOf<Boolean>()

        exibir(bloqueioDisponivel = true, aoMudarBloqueio = { pedidos += it })
        composeRule.onNodeWithText(bloquearApp).performClick()

        assertEquals(listOf(true), pedidos)
        // Só a autenticação liga o switch; enquanto ela não confirma, ele fica desligado.
        composeRule.onNodeWithText(bloquearApp).assertIsOff()
    }

    private fun exibir(bloqueioDisponivel: Boolean, aoMudarBloqueio: (Boolean) -> Unit = {}) {
        composeRule.setContent {
            MeusExamesTheme {
                Configuracoes(
                    bloqueioDisponivel = bloqueioDisponivel,
                    bloqueioAtivo = false,
                    aoMudarBloqueio = aoMudarBloqueio,
                    aoVoltar = {},
                )
            }
        }
    }
}
