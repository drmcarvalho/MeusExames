package com.example.meusexames.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.meusexames.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Configuracoes(
    bloqueioDisponivel: Boolean,
    bloqueioAtivo: Boolean,
    aoMudarBloqueio: (Boolean) -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_configuracoes)) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(
                            painter = painterResource(R.drawable.ic_voltar),
                            contentDescription = stringResource(R.string.voltar),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (bloqueioDisponivel) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.bloquear_app)) },
                    // A linha inteira é o controle: o leitor de tela anuncia texto e estado juntos.
                    modifier = Modifier.toggleable(
                        value = bloqueioAtivo,
                        role = Role.Switch,
                        onValueChange = aoMudarBloqueio,
                    ),
                    supportingContent = { Text(stringResource(R.string.bloquear_app_apoio)) },
                    trailingContent = { Switch(checked = bloqueioAtivo, onCheckedChange = null) },
                )
            }
        }
    }
}
