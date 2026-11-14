package com.example.meusexames.dal

import android.content.Context
import androidx.core.content.edit

class ConfiguracaoDal(context: Context) {

    private val preferencias =
        context.getSharedPreferences("com.example.meusexames.configuracoes", Context.MODE_PRIVATE)

    var bloqueioAtivo: Boolean
        get() = preferencias.getBoolean(CHAVE_BLOQUEIO_ATIVO, false)
        set(ativo) = preferencias.edit { putBoolean(CHAVE_BLOQUEIO_ATIVO, ativo) }

    private companion object {
        const val CHAVE_BLOQUEIO_ATIVO = "bloqueio_ativo"
    }
}
