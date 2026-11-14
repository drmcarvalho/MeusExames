package com.example.meusexames.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.meusexames.bll.BloqueioBll

class ConfiguracoesActivity : ActivityProtegida() {

    private var disponivel by mutableStateOf(false)
    private var bloqueioAtivo by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bloqueioAtivo = BloqueioBll.estaAtivo(this)
        setConteudoProtegido {
            Configuracoes(
                bloqueioDisponivel = disponivel,
                bloqueioAtivo = bloqueioAtivo,
                aoMudarBloqueio = ::mudarBloqueio,
                aoVoltar = ::finish,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Na volta das configurações do celular o PIN pode ter sido criado ou removido.
        disponivel = BloqueioBll.podeBloquear(this)
    }

    override fun aoAtivarBloqueio() {
        bloqueioAtivo = true
    }

    private fun mudarBloqueio(ativar: Boolean) {
        if (ativar) {
            // O switch só liga em aoAtivarBloqueio: se o usuário cancelar, ele continua desligado.
            autenticar(MotivoAutenticacao.ATIVAR_BLOQUEIO)
        } else {
            BloqueioBll.salvarAtivo(this, false)
            bloqueioAtivo = false
        }
    }
}
