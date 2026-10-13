Vamos ver se o método na classe dao `ExameDao` esta consistente:
```kotlin
@Query(value = "SELECT * FROM exames WHERE titulo LIKE :termo OR descricao LIKE :termo")
abstract fun pesquisar(termo: String): Flow<List<Exame>>
```

Siga a documentação do Room.