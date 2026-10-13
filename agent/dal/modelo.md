# Modelo de dados

Camada: `com/example/meusexames/dal`

Vamos criar o modelo de dados com os seguintes campos.

Exame
- Titulo do exame 
  - obrigatório
  - máximo de 64 caracteres e mínimo de 3
- Descrição do exame em detalhes
  - opcional
  - máximo de 256 caracteres e mínimo de 3
- Data do exame
  - obrigatório
- Caminho do arquivo
  - obrigatório
- Extensão ou tipo do arquivo
  - obrigatório
- Houve alteração no exame
  - obrigatório
  - valor padrão False
  - False=Não, True=Sim
- Data de cadastro
  - obrigatório
  - valor padrão data atual
  - vai ser preenchido automaticamente
- Data de alteração
  - obrigatório
  - valor padrão data atual
  - vai ser preenchido automaticamente
- Comentário
  - Opcional
  - máximo de 1024 e mínimo de 3

Criar as demais classes e arquivos fonte com configuração do banco e CRUD. 

**PS**: _Não adicionar logica de negócio ou de aplicação nesse arquivo/classe que representa o
modelo._

Vamos usar o Room: https://developer.android.com/training/data-storage/room

Room documentação:
- [Guia do Room](https://developer.android.com/training/data-storage/room): página principal, com visão geral e configuração inicial.
- [Entidades](https://developer.android.com/training/data-storage/room/defining-data): como definir tabelas usando classes Kotlin.
- [DAOs](https://developer.android.com/training/data-storage/room/accessing-data): como criar consultas, inserções, atualizações e exclusões.
- [Migrações](https://developer.android.com/training/data-storage/room/migrating-db-versions): como mudar a estrutura do banco sem perder dados.
- [Versões e dependências](https://developer.android.com/jetpack/androidx/releases/room): versão atual da biblioteca e linhas para o Gradle.
- [Referência da API em Kotlin](https://developer.android.com/reference/kotlin/androidx/room/package-summary): lista de todas as classes e anotações do Room.
- [Codelab Room com Compose](https://developer.android.com/codelabs/basic-android-kotlin-compose-persisting-data-room): tutorial prático, passo a passo.

Arquitetura: `agent/arquitetura.md`