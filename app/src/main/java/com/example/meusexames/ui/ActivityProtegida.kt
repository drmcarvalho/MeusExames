package com.example.meusexames.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.meusexames.R
import com.example.meusexames.bll.BloqueioBll
import com.example.meusexames.bll.Sessao
import com.example.meusexames.ui.theme.MeusExamesTheme

enum class MotivoAutenticacao { DESBLOQUEAR, ATIVAR_BLOQUEIO }

abstract class ActivityProtegida : FragmentActivity() {

    private lateinit var prompt: BiometricPrompt
    private var travado by mutableStateOf(false)
    private var promptAutomaticoPendente = false
    private var motivo = MotivoAutenticacao.DESBLOQUEAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(CHAVE_MOTIVO)?.let { motivo = MotivoAutenticacao.valueOf(it) }
        // Um só por activity: com mais de um, apenas o callback do último recebe o resultado.
        prompt = BiometricPrompt(this, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Sessao.desbloqueado = true
                when (motivo) {
                    MotivoAutenticacao.DESBLOQUEAR -> travado = false
                    MotivoAutenticacao.ATIVAR_BLOQUEIO -> {
                        BloqueioBll.salvarAtivo(this@ActivityProtegida, true)
                        aoAtivarBloqueio()
                    }
                }
            }
        })
        travado = BloqueioBll.precisaTravar(this)
        // Activity restaurada não abre o prompt sozinha: numa rotação o anterior continua na tela.
        promptAutomaticoPendente = travado && savedInstanceState == null
    }

    override fun onResume() {
        super.onResume()
        // O PIN pode ter sido removido com a tela bloqueada aberta; sem conferir de novo, o
        // prompt só daria erro e o app ficaria preso nela.
        travado = travado && BloqueioBll.precisaTravar(this)
        if (promptAutomaticoPendente) {
            promptAutomaticoPendente = false
            if (travado) autenticar(MotivoAutenticacao.DESBLOQUEAR)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Depois de uma rotação o resultado do prompt chega na activity nova.
        outState.putString(CHAVE_MOTIVO, motivo.name)
    }

    protected fun setConteudoProtegido(conteudo: @Composable () -> Unit) {
        setContent {
            MeusExamesTheme {
                if (travado) {
                    TelaBloqueada(aoDesbloquear = { autenticar(MotivoAutenticacao.DESBLOQUEAR) })
                } else {
                    conteudo()
                }
            }
        }
    }

    protected fun autenticar(motivo: MotivoAutenticacao) {
        this.motivo = motivo
        // Sem botão negativo: o PIN, padrão ou senha do celular é sempre aceito.
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.titulo_desbloquear))
                .setAllowedAuthenticators(BloqueioBll.tiposAceitos())
                .build()
        )
    }

    protected open fun aoAtivarBloqueio() {}

    private companion object {
        const val CHAVE_MOTIVO = "motivo_autenticacao"
    }
}
