No botão deletar da classe `com/example/meusexames/ui/ListaExames.kt`:
```kotlin
FilledTonalButton(onClick = { /* TODO: apagar exame */ }) {
    Text(stringResource(R.string.apagar))
}
```

Vamos implementar a ação exclusão delegando uma nova ação para a camada BLL para excluir um exame. 

Atividade principal `MainActivity`:
```kotlin
ListaExames(
    exames = exames,
    aoCadastrar = {
        startActivity(Intent(this, CadastroExameActivity::class.java))
    },
    aoEditar = { exame ->
        startActivity(CadastroExameActivity.intentParaEditar(this, exame.id))
    },
    aoApagar
)
```

Vamos solicitar uma confirmação para garantir se o usuário realmente quer apagar para evitar 
exclusão acidental.

Ao apagar um exame, vamos apagar o arquivo movido em data do exame cadastrado para não ficar 
acumulando lixo de arquivos no armazenamento.