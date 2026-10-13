package com.example.meusexames.dal

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Conversores {

    @TypeConverter
    fun deLocalDate(data: LocalDate): String = data.toString()

    @TypeConverter
    fun paraLocalDate(texto: String): LocalDate = LocalDate.parse(texto)

    @TypeConverter
    fun deInstant(instante: Instant): Long = instante.toEpochMilli()

    @TypeConverter
    fun paraInstant(milissegundos: Long): Instant = Instant.ofEpochMilli(milissegundos)
}
