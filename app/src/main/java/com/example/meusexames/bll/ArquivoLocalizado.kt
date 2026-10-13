package com.example.meusexames.bll

import android.net.Uri

data class ArquivoLocalizado(
    // Endereço pelo qual outro app consegue ler o arquivo.
    val uri: Uri,
    // Nulo quando não foi possível descobrir o tipo do arquivo.
    val tipoMime: String?,
)
