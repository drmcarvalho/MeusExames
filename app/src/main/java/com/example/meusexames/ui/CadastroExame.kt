package com.example.meusexames.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meusexames.R
import com.example.meusexames.bll.ArquivoSelecionado
import com.example.meusexames.bll.DadosExame
import com.example.meusexames.bll.ExameBll
import com.example.meusexames.dal.models.Exame
import com.example.meusexames.dal.models.TipoArquivo
import com.example.meusexames.ui.theme.MeusExamesTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TextoComFrases = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

/**
 * Formulário de exame. Sem [exame] ele cadastra um exame novo; com [exame], abre em modo de
 * edição, já preenchido com os dados dele.
 *
 * [aoAbrirArquivo] recebe o arquivo escolhido na tela, ou nulo para abrir o que [exame] já tem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroExame(
    identificarArquivo: suspend (Uri) -> ArquivoSelecionado,
    aoSalvar: (DadosExame) -> Unit,
    aoCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    exame: Exame? = null,
    aoAbrirArquivo: (Uri?) -> Unit = {},
    salvando: Boolean = false,
) {
    var titulo by rememberSaveable { mutableStateOf(exame?.titulo.orEmpty()) }
    var descricao by rememberSaveable { mutableStateOf(exame?.descricao.orEmpty()) }
    var dataExame by rememberSaveable { mutableStateOf(exame?.dataExame) }
    var arquivo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var houveAlteracao by rememberSaveable { mutableStateOf(exame?.houveAlteracao ?: false) }
    var comentario by rememberSaveable { mutableStateOf(exame?.comentario.orEmpty()) }
    var selecionandoData by rememberSaveable { mutableStateOf(false) }

    // Até outro arquivo ser escolhido, a tela mostra o que o exame em edição já tem.
    val arquivoExibido by produceState(
        initialValue = exame?.let {
            ArquivoSelecionado(it.caminhoArquivo.substringAfterLast('/'), it.tipoArquivo)
        },
        arquivo,
    ) {
        arquivo?.let { value = identificarArquivo(it) }
    }
    val seletorDeArquivo =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { escolhido ->
            if (escolhido != null) arquivo = escolhido
        }
    val formatoData = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    val tituloDaTela =
        if (exame == null) R.string.titulo_cadastro_exame else R.string.titulo_edicao_exame

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(tituloDaTela)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.titulo_exame)) },
                keyboardOptions = TextoComFrases,
                singleLine = true,
            )
            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.descricao_exame)) },
                keyboardOptions = TextoComFrases,
                minLines = 3,
            )
            OutlinedTextField(
                value = dataExame?.format(formatoData).orEmpty(),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    // O campo de texto consome o toque, então ele é observado antes, na
                    // passagem inicial, para abrir o seletor ao tocar em qualquer ponto.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            if (waitForUpOrCancellation(pass = PointerEventPass.Initial) != null) {
                                selecionandoData = true
                            }
                        }
                    },
                readOnly = true,
                label = { Text(stringResource(R.string.data_exame)) },
                trailingIcon = {
                    IconButton(onClick = { selecionandoData = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendario),
                            contentDescription = stringResource(R.string.selecionar_data),
                        )
                    }
                },
            )
            OutlinedTextField(
                value = arquivoExibido?.nome.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.arquivo_exame)) },
            )
            OutlinedTextField(
                value = arquivoExibido?.tipo?.let { stringResource(rotuloDe(it)) }.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.tipo_arquivo)) },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        seletorDeArquivo.launch(ExameBll.TIPOS_MIME_ACEITOS.toTypedArray())
                    },
                ) {
                    Text(stringResource(R.string.selecionar_arquivo))
                }
                if (exame != null) {
                    FilledTonalButton(onClick = { aoAbrirArquivo(arquivo) }) {
                        Text(stringResource(R.string.abrir_arquivo))
                    }
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.houve_alteracao),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(modifier = Modifier.selectableGroup()) {
                    OpcaoDeAlteracao(R.string.sim, selecionada = houveAlteracao) {
                        houveAlteracao = true
                    }
                    OpcaoDeAlteracao(R.string.nao, selecionada = !houveAlteracao) {
                        houveAlteracao = false
                    }
                }
            }
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.comentario)) },
                keyboardOptions = TextoComFrases,
                minLines = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = aoCancelar, enabled = !salvando) {
                    Text(stringResource(R.string.cancelar))
                }
                Button(
                    onClick = {
                        aoSalvar(
                            DadosExame(
                                titulo = titulo,
                                descricao = descricao,
                                dataExame = dataExame,
                                arquivo = arquivo,
                                houveAlteracao = houveAlteracao,
                                comentario = comentario,
                            )
                        )
                    },
                    enabled = !salvando,
                ) {
                    Text(stringResource(R.string.salvar))
                }
            }
        }
    }

    if (selecionandoData) {
        SeletorDeData(
            dataAtual = dataExame,
            aoConfirmar = { dataExame = it },
            aoFechar = { selecionandoData = false },
        )
    }
}

@Composable
private fun OpcaoDeAlteracao(
    @StringRes rotulo: Int,
    selecionada: Boolean,
    aoSelecionar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .selectable(selected = selecionada, role = Role.RadioButton, onClick = aoSelecionar)
            .padding(end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionada, onClick = null)
        Text(text = stringResource(rotulo), modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorDeData(
    dataAtual: LocalDate?,
    aoConfirmar: (LocalDate) -> Unit,
    aoFechar: () -> Unit,
) {
    // O seletor representa o dia escolhido como a meia-noite dele em UTC.
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = dataAtual?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
            ?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(
                onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        aoConfirmar(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    aoFechar()
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) {
                Text(stringResource(R.string.cancelar))
            }
        },
    ) {
        DatePicker(state = estado)
    }
}

@StringRes
private fun rotuloDe(tipo: TipoArquivo): Int = when (tipo) {
    TipoArquivo.PDF -> R.string.tipo_arquivo_pdf
    TipoArquivo.IMAGEM -> R.string.tipo_arquivo_imagem
    TipoArquivo.DOC -> R.string.tipo_arquivo_doc
}

@Preview(showBackground = true)
@Composable
private fun CadastroExamePreview() {
    MeusExamesTheme {
        CadastroExame(
            identificarArquivo = { ArquivoSelecionado("hemograma.pdf", TipoArquivo.PDF) },
            aoSalvar = {},
            aoCancelar = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EdicaoExamePreview() {
    MeusExamesTheme {
        CadastroExame(
            identificarArquivo = { ArquivoSelecionado("hemograma.pdf", TipoArquivo.PDF) },
            aoSalvar = {},
            aoCancelar = {},
            exame = Exame(
                id = 1,
                titulo = "Hemograma",
                descricao = "Exame de rotina",
                dataExame = LocalDate.of(2026, 9, 30),
                caminhoArquivo = "exames/exemplo.pdf",
                tipoArquivo = TipoArquivo.PDF,
                houveAlteracao = true,
                comentario = "Repetir em seis meses",
            ),
        )
    }
}
