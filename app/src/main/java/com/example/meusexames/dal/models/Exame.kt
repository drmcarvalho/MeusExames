package com.example.meusexames.dal.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "exames")
data class Exame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val descricao: String? = null,
    @ColumnInfo(name = "data_exame")
    val dataExame: LocalDate,
    @ColumnInfo(name = "caminho_arquivo")
    val caminhoArquivo: String,
    @ColumnInfo(name = "tipo_arquivo")
    val tipoArquivo: TipoArquivo,
    @ColumnInfo(name = "houve_alteracao")
    val houveAlteracao: Boolean = false,
    @ColumnInfo(name = "data_cadastro")
    val dataCadastro: Instant = Instant.now(),
    @ColumnInfo(name = "data_alteracao")
    val dataAlteracao: Instant = dataCadastro,
    val comentario: String? = null,
)
