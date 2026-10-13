package com.example.meusexames.bll

import android.net.Uri
import java.time.LocalDate

data class DadosExame(
    val titulo: String,
    val descricao: String,
    val dataExame: LocalDate?,
    // Arquivo escolhido na tela. Na edição, nulo mantém o arquivo que o exame já tem.
    val arquivo: Uri?,
    val houveAlteracao: Boolean,
    val comentario: String,
)
