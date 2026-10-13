package com.example.meusexames.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meusexames.R
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import com.example.meusexames.ui.theme.MeusExamesTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val LIMITE_DESCRICAO_NO_CARD = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaExames(
    exames: List<Exame>,
    aoCadastrar: () -> Unit,
    aoEditar: (Exame) -> Unit,
    aoApagar: (Exame) -> Unit,
    aoCompartilhar: (Exame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatoData = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    // Só o id é guardado, para a confirmação continuar aberta depois de girar a tela.
    var idExameParaApagar by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.titulo_lista_exames)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = aoCadastrar) {
                Icon(
                    painter = painterResource(R.drawable.ic_adicionar),
                    contentDescription = stringResource(R.string.cadastrar_exame),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Espaço extra embaixo para o último card rolar acima do botão de cadastrar.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(exames, key = { it.id }) { exame ->
                ExameCard(
                    exame,
                    formatoData,
                    aoEditar = { aoEditar(exame) },
                    aoApagar = { idExameParaApagar = exame.id },
                    aoCompartilhar = { aoCompartilhar(exame) },
                )
            }
        }
    }

    exames.firstOrNull { it.id == idExameParaApagar }?.let { exame ->
        ConfirmacaoDeExclusao(
            exame = exame,
            aoConfirmar = {
                idExameParaApagar = null
                aoApagar(exame)
            },
            aoFechar = { idExameParaApagar = null },
        )
    }
}

@Composable
private fun ExameCard(
    exame: Exame,
    formatoData: DateTimeFormatter,
    aoEditar: () -> Unit,
    aoApagar: () -> Unit,
    aoCompartilhar: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exame.titulo, style = MaterialTheme.typography.titleMedium)
            DetalheDoExame(
                stringResource(R.string.data_exame_formatada, exame.dataExame.format(formatoData))
            )
            DetalheDoExame(
                stringResource(
                    R.string.houve_alteracao_formatada,
                    stringResource(if (exame.houveAlteracao) R.string.sim else R.string.nao),
                )
            )
            exame.descricao?.let { descricao ->
                DetalheDoExame(
                    stringResource(R.string.descricao_exame_formatada, truncar(descricao))
                )
            }
            // Em tela estreita os três botões não cabem lado a lado; o que sobra desce de linha.
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = aoEditar) {
                    Text(stringResource(R.string.editar))
                }
                FilledTonalButton(onClick = aoApagar) {
                    Text(stringResource(R.string.apagar))
                }
                FilledTonalButton(onClick = aoCompartilhar) {
                    Text(stringResource(R.string.compartilhar))
                }
            }
        }
    }
}

@Composable
private fun ConfirmacaoDeExclusao(exame: Exame, aoConfirmar: () -> Unit, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(stringResource(R.string.apagar))
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text(stringResource(R.string.cancelar))
            }
        },
        title = { Text(stringResource(R.string.titulo_apagar_exame)) },
        text = { Text(stringResource(R.string.mensagem_apagar_exame, exame.titulo)) },
    )
}

@Composable
private fun DetalheDoExame(texto: String) {
    Text(
        text = texto,
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun truncar(descricao: String): String {
    if (descricao.length <= LIMITE_DESCRICAO_NO_CARD) return descricao
    // Um emoji ocupa dois caracteres da string; o corte não pode ficar no meio dele.
    val cortaEmoji = descricao[LIMITE_DESCRICAO_NO_CARD - 1].isHighSurrogate()
    val fim = if (cortaEmoji) LIMITE_DESCRICAO_NO_CARD - 1 else LIMITE_DESCRICAO_NO_CARD
    return descricao.take(fim) + "..."
}

@Preview(showBackground = true)
@Composable
private fun ListaExamesPreview() {
    MeusExamesTheme {
        ListaExames(
            exames = listOf(
                exameDeExemplo(
                    id = 1,
                    titulo = "Hemograma",
                    descricao = "Exame de rotina anual",
                    houveAlteracao = true,
                ),
                exameDeExemplo(id = 2, titulo = "Eletro"),
            ),
            aoCadastrar = {},
            aoEditar = {},
            aoApagar = {},
            aoCompartilhar = {},
        )
    }
}

private fun exameDeExemplo(
    id: Long,
    titulo: String,
    descricao: String? = null,
    houveAlteracao: Boolean = false,
) = Exame(
    id = id,
    titulo = titulo,
    descricao = descricao,
    dataExame = LocalDate.of(2026, 9, 30),
    caminhoArquivo = "exames/exemplo.pdf",
    tipoArquivo = TipoArquivo.PDF,
    houveAlteracao = houveAlteracao,
)
