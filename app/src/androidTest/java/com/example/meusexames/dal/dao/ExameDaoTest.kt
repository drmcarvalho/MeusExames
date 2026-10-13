package com.example.meusexames.dal.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meusexames.dal.AppDatabase
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExameDaoTest {

    private lateinit var banco: AppDatabase
    private lateinit var dao: ExameDao

    @Before
    fun abrirBanco() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        banco = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = banco.exameDao()
    }

    @After
    fun fecharBanco() {
        banco.close()
    }

    @Test
    fun inserirEBuscarPorId() = runBlocking {
        val exame = Exame(
            titulo = "Hemograma completo",
            descricao = "Exame de rotina",
            dataExame = LocalDate.of(2026, 9, 30),
            caminhoArquivo = "exames/hemograma.pdf",
            tipoArquivo = TipoArquivo.PDF,
            houveAlteracao = true,
            dataCadastro = Instant.parse("2026-10-01T12:00:00Z"),
            dataAlteracao = Instant.parse("2026-10-02T08:30:00Z"),
            comentario = "Repetir em seis meses",
        )

        val id = dao.inserir(exame)

        assertEquals(exame.copy(id = id), dao.buscarPorId(id))
    }

    @Test
    fun inserirPreencheValoresPadrao() = runBlocking {
        val antes = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val id = dao.inserir(novoExame())
        val depois = Instant.now()

        val gravado = dao.buscarPorId(id)!!
        assertNull(gravado.descricao)
        assertNull(gravado.comentario)
        assertFalse(gravado.houveAlteracao)
        assertFalse(gravado.dataCadastro.isBefore(antes))
        assertFalse(gravado.dataCadastro.isAfter(depois))
        assertEquals(gravado.dataCadastro, gravado.dataAlteracao)
    }

    @Test
    fun listarOrdenaPelaDataDoExameMaisRecente() = runBlocking {
        dao.inserir(novoExame(titulo = "Intermediário", dataExame = LocalDate.of(2026, 5, 10)))
        dao.inserir(novoExame(titulo = "Mais antigo", dataExame = LocalDate.of(2025, 12, 1)))
        dao.inserir(novoExame(titulo = "Mais recente", dataExame = LocalDate.of(2026, 9, 30)))

        val titulos = dao.listar().first().map { it.titulo }

        assertEquals(listOf("Mais recente", "Intermediário", "Mais antigo"), titulos)
    }

    @Test
    fun pesquisarEncontraOTermoNoTituloOuNaDescricao() = runBlocking {
        dao.inserir(novoExame(titulo = "Hemograma completo"))
        dao.inserir(novoExame(titulo = "Raio-X", descricao = "Pedido junto com o hemograma"))
        dao.inserir(novoExame(titulo = "Glicemia", descricao = "Em jejum"))
        dao.inserir(novoExame(titulo = "Ultrassom"))

        val titulos = dao.pesquisar("hemograma").first().map { it.titulo }

        assertEquals(setOf("Hemograma completo", "Raio-X"), titulos.toSet())
    }

    @Test
    fun pesquisarOrdenaPelaDataDoExameMaisRecente() = runBlocking {
        dao.inserir(novoExame(titulo = "TSH intermediário", dataExame = LocalDate.of(2026, 5, 10)))
        dao.inserir(novoExame(titulo = "TSH mais antigo", dataExame = LocalDate.of(2025, 12, 1)))
        dao.inserir(novoExame(titulo = "TSH mais recente", dataExame = LocalDate.of(2026, 9, 30)))

        val titulos = dao.pesquisar("tsh").first().map { it.titulo }

        assertEquals(listOf("TSH mais recente", "TSH intermediário", "TSH mais antigo"), titulos)
    }

    @Test
    fun atualizarGravaMudancasERenovaDataDeAlteracao() = runBlocking {
        val cadastro = Instant.parse("2020-01-01T00:00:00Z")
        val exame = novoExame().copy(dataCadastro = cadastro, dataAlteracao = cadastro)
        val id = dao.inserir(exame)

        val antes = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val linhas = dao.atualizar(
            exame.copy(id = id, titulo = "Hemograma revisado", houveAlteracao = true)
        )
        val depois = Instant.now()

        val atualizado = dao.buscarPorId(id)!!
        assertEquals(1, linhas)
        assertEquals("Hemograma revisado", atualizado.titulo)
        assertTrue(atualizado.houveAlteracao)
        assertEquals(cadastro, atualizado.dataCadastro)
        assertFalse(atualizado.dataAlteracao.isBefore(antes))
        assertFalse(atualizado.dataAlteracao.isAfter(depois))
    }

    @Test
    fun excluirRemoveOExame() = runBlocking {
        val id = dao.inserir(novoExame())
        val exame = dao.buscarPorId(id)!!

        assertEquals(1, dao.excluir(exame))
        assertNull(dao.buscarPorId(id))
    }

    private fun novoExame(
        titulo: String = "Hemograma",
        descricao: String? = null,
        dataExame: LocalDate = LocalDate.of(2026, 9, 30),
    ) = Exame(
        titulo = titulo,
        descricao = descricao,
        dataExame = dataExame,
        caminhoArquivo = "exames/hemograma.pdf",
        tipoArquivo = TipoArquivo.PDF,
    )
}
