# Interface de cadastro e edição de exames

Nesta tela vamos ter os campos para cadastro. Veja `agent/dal/modelo.md` para mais detalhes.
- Titulo do exame
  - componente de texto 
  - obrigatório
  - máximo de 64 caracteres e mínimo de 3
- Descrição do exame em detalhes
  - componente de texto em caixa 
  - opcional
  - máximo de 256 caracteres e mínimo de 3
- Data do exame
    - obrigatório
    - componente de data (apenas data)
- Caminho do arquivo
    - obrigatório
- Extensão ou tipo do arquivo
    - obrigatório
    - vai ser processado automaticamente conforme a extensão do arquivo selecionado como anexo, em DAO podemos criar o ENUM para PDF, imagem, DOC
- Houve alteração no exame
    - obrigatório
    - componente radio button, Sim, Não
    - valor padrão `False`
    - False=Não, True=Sim
- Comentário
    - opcional
    - componente de texto em caixa
    - máximo de 1024 e mínimo de 3
- Botão de seleção de arquivo para obter o arquivo e salvar a referência dele no banco via modelo

O fluxo de cadastrado vai ser processado inicialmente pela camada BLL, tanto na validação e no 
cadastro chamando a camada DAL. Exemplo: 
```
ui -> BLL (validação de dados) -> DAL
```

Botão salvar ação: processar todo o fluxo de cadastro e retornar mensagem de erro em formato Toast
após o cadastro fechar a atividade e voltar para a listagem de exames já atualizada com novo 
registro.

Botão cancelar ação: cancela o cadastro fecha a atividade e volta para a tela de listagem de exames.

Seguir o visual e estilo simples da tela de listagem.

Referências para seguir da documentação

- [Documentação do Jetpack Compose](https://developer.android.com/develop/ui/compose/documentation): página principal sobre criação de telas com Compose.
- [Componentes Material no Compose](https://developer.android.com/develop/ui/compose/components): botões, campos de texto, cards, diálogos e outros componentes.
- [Layouts no Compose](https://developer.android.com/develop/ui/compose/layouts): como organizar elementos na tela com Column, Row, Box e layouts próprios.
- [Layouts com Views e XML](https://developer.android.com/develop/ui/views/layout/declaring-layout): modelo antigo, útil para manter projetos mais velhos.
- [Componentes do Material Design 3](https://m3.material.io/components): guia visual de como cada componente deve parecer e funcionar.
- https://developer.android.com/design/ui/mobile/guides/components/material-overview

Seguir arquitetura: `agent/arquitetura.md`