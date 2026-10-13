package com.example.meusexames.dal.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.meusexames.dal.models.Exame
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ExameDao {

    @Insert
    abstract suspend fun inserir(exame: Exame): Long

    @Query("SELECT * FROM exames ORDER BY data_exame DESC, id DESC")
    abstract fun listar(): Flow<List<Exame>>

    @Query(
        """
        SELECT * FROM exames
        WHERE titulo LIKE '%' || :termo || '%' OR descricao LIKE '%' || :termo || '%'
        ORDER BY data_exame DESC, id DESC
        """
    )
    abstract fun pesquisar(termo: String): Flow<List<Exame>>

    @Query("SELECT * FROM exames WHERE id = :id")
    abstract suspend fun buscarPorId(id: Long): Exame?

    suspend fun atualizar(exame: Exame): Int =
        atualizarRegistro(exame.copy(dataAlteracao = Instant.now()))

    @Update
    protected abstract suspend fun atualizarRegistro(exame: Exame): Int

    @Delete
    abstract suspend fun excluir(exame: Exame): Int
}
