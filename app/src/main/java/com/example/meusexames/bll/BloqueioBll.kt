package com.example.meusexames.bll

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.example.meusexames.dal.ConfiguracaoDal

object BloqueioBll {

    // BIOMETRIC_STRONG com DEVICE_CREDENTIAL não funciona nas APIs 28 e 29.
    fun tiposAceitos(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

    // Conferido sempre na hora: o usuário pode criar ou remover o PIN com o app aberto.
    fun podeBloquear(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(tiposAceitos()) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun estaAtivo(context: Context): Boolean = ConfiguracaoDal(context).bloqueioAtivo

    fun salvarAtivo(context: Context, ativo: Boolean) {
        ConfiguracaoDal(context).bloqueioAtivo = ativo
    }

    fun precisaTravar(context: Context): Boolean =
        estaAtivo(context) && podeBloquear(context) && !Sessao.desbloqueado
}
