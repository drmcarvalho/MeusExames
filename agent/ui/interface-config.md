# Interface de configurações

Tela de configurações do app. Ela reúne as opções que mudam o comportamento do MeusExames.

## Como abrir

- Na interface principal, adicionar um ícone de engrenagem no topo da tela pode ser no lado direito.
- O ícone fica sempre visível.
- Ao tocar no ícone, abre a `ConfiguracoesActivity`.

## Estrutura

- A `ConfiguracoesActivity` fica no pacote `ui`, como as outras telas.
- Registrar a activity no `AndroidManifest.xml` com `android:exported="false"`.
- No topo da tela: o título "Configurações" e um botão de voltar.
- As opções ficam numa lista, uma embaixo da outra. Usar os componentes do Material3 (`ListItem`, `Switch`).
- Cada opção tem a sua própria spec filha: `interface-config.<opcao>.md`.

## Opções

| Opção | Spec |
|---|---|
| Bloquear o app | `interface-config.bloqueio.md` |

## Textos

Para os textos siga o padrão do app alterando as strings para cada linguagem.

Seguir `agent/arquitetura.md`.
