package com.example.meusexames.dal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.meusexames.dal.dao.ExameDao
import com.example.meusexames.dal.models.Exame

@Database(entities = [Exame::class], version = 1)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exameDao(): ExameDao

    companion object {
        private const val NOME_ARQUIVO = "meus_exames.db"

        @Volatile
        private var instancia: AppDatabase? = null

        fun obterInstancia(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOME_ARQUIVO,
                ).build().also { instancia = it }
            }
    }
}
