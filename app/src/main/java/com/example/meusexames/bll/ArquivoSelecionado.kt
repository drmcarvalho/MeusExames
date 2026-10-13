package com.example.meusexames.bll

import com.example.meusexames.dal.models.TipoArquivo

data class ArquivoSelecionado(
    val nome: String,
    // Nulo quando o arquivo não é de um tipo aceito como anexo.
    val tipo: TipoArquivo?,
)
