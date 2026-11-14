# Configuração: bloquear o app

Opção da interface de configurações que protege o acesso ao app. Quando ela está ligada, o app pede a biometria (digital ou rosto) ou o PIN, padrão ou senha do celular antes de mostrar os exames.

## Decisões

- O app usa o bloqueio do próprio celular. Não existe senha própria do app.
- O prompt não tem botão negativo (`setNegativeButtonText`). O PIN, padrão ou senha do celular é sempre aceito (`DEVICE_CREDENTIAL`).
- A opção só aparece se o celular cumprir os requisitos mínimos. Se não cumprir, o app abre normal.
- O app trava só quando o processo começa do zero. Voltar de outra tela ou do segundo plano não trava de novo.

## Requisitos mínimos do celular

1. Android 9 (API 28) ou mais novo. É o `minSdk` do projeto.
2. Bloqueio de tela configurado: PIN, padrão ou senha. "Deslizar" e "Nenhum" não contam.

Leitor de digital e reconhecimento facial são opcionais. Se existirem e estiverem cadastrados, o prompt oferece. Se não, o prompt pede o PIN direto.

## Biblioteca

- Adicionar `androidx.biometric:biometric:1.1.0` (versão estável) no `gradle/libs.versions.toml` e no `app/build.gradle.kts`.
- O `BiometricPrompt` dessa versão precisa de uma `FragmentActivity`. Por isso as activities passam a herdar de `ActivityProtegida` (ver abaixo), que herda de `FragmentActivity`.
- O `setContent` do Compose continua funcionando, porque `FragmentActivity` herda de `ComponentActivity`.
- Adicionar também `androidx.fragment:fragment:1.9.1` (versão estável), de forma explícita. A `biometric` 1.1.0 traz a `fragment` 1.2.5, cuja `FragmentActivity` aceita só `requestCode` de até 16 bits. Os launchers do Compose (`rememberLauncherForActivityResult`) geram códigos maiores, e o app fecha com `IllegalArgumentException: Can only use lower 16 bits for requestCode` ao abrir o seletor de arquivos do cadastro. O limite saiu na `fragment` 1.3.0.

## Tipos de autenticação aceitos

A combinação muda conforme a versão do Android. `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` não funciona nas APIs 28 e 29.

```kotlin
fun tiposAceitos(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // API 30
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    else
        BIOMETRIC_WEAK or DEVICE_CREDENTIAL
```

## Arquivos por camada

### dal

`ConfiguracaoDal`
- Lê e grava as configurações do app em `SharedPreferences`.
- Nome do arquivo: `com.example.meusexames.configuracoes`, em modo privado.
- Chave `bloqueio_ativo` (Boolean). Valor padrão: `false`.

### bll

`Sessao` (object)
- `desbloqueado: Boolean = false`.
- Fica só em memória. Nunca salvar em disco nem no `savedInstanceState`. Assim, quando o processo termina, o app volta a travar.

`BloqueioBll`
- `tiposAceitos()`: código acima.
- `podeBloquear(context)`: retorna `canAuthenticate(tiposAceitos()) == BIOMETRIC_SUCCESS`. Qualquer outro retorno conta como "não pode".
- `estaAtivo(context)` e `salvarAtivo(context, ativo)`: usam o `ConfiguracaoDal`.
- `precisaTravar(context)`: `estaAtivo && podeBloquear && !Sessao.desbloqueado`.
- Nunca salvar o resultado de `podeBloquear`. Checar sempre na hora. O status muda: o usuário pode criar ou remover o PIN, e o sensor pode ficar indisponível por um tempo.

### ui

`ActivityProtegida` (classe abstrata, herda de `FragmentActivity`)
- No `onCreate`:
    - Cria um único `BiometricPrompt` para a activity. Não criar mais de um: só o callback do último recebe o resultado.
    - Calcula `travado = BloqueioBll.precisaTravar(this)`, como estado do Compose.
    - Se `travado` e `savedInstanceState == null`, marca para abrir o prompt automático no próximo `onResume`.
    - Se `savedInstanceState != null`, não abre o prompt automático. Numa rotação, o prompt anterior continua na tela sozinho. Se o processo foi recriado, o usuário vê a `TelaBloqueada` e toca em "Desbloquear".
- No `onResume`:
    - Se `travado`, recalcula com `BloqueioBll.precisaTravar(this)`. O usuário pode ter removido o PIN com a `TelaBloqueada` aberta: sem isso, o prompt só daria erro e o app ficaria preso nela.
    - Se houver prompt automático pendente, limpa a marca e, se ainda estiver `travado`, abre o prompt.
- `setConteudoProtegido(conteudo)`: substitui o `setContent`. Aplica o `MeusExamesTheme`. Se `travado`, mostra a `TelaBloqueada`. Se não, mostra o `conteudo`.
- `autenticar(motivo)`: abre o prompt. O `motivo` é `DESBLOQUEAR` ou `ATIVAR_BLOQUEIO`. Guardar o motivo no `savedInstanceState` para ele sobreviver à rotação.
- `aoAtivarBloqueio()`: método aberto, vazio na base. A `ConfiguracoesActivity` sobrescreve.
- `PromptInfo`:
    - título: "Desbloquear MeusExames";
    - `setAllowedAuthenticators(BloqueioBll.tiposAceitos())`;
    - sem `setNegativeButtonText`.
- Callback:
    - `onAuthenticationSucceeded`:
        - `DESBLOQUEAR`: `Sessao.desbloqueado = true` e `travado = false`.
        - `ATIVAR_BLOQUEIO`: `BloqueioBll.salvarAtivo(this, true)`, `Sessao.desbloqueado = true` e chama `aoAtivarBloqueio()`.
    - `onAuthenticationError` (inclui quando o usuário cancela): não muda nada. Se estava travado, continua na `TelaBloqueada`.
    - `onAuthenticationFailed` (digital ou rosto não reconhecido): não faz nada. O próprio prompt deixa tentar de novo.

`TelaBloqueada` (composable)
- Ícone de cadeado, o texto "MeusExames está bloqueado" e o botão "Desbloquear".
- O botão chama `autenticar(DESBLOQUEAR)`.
- Não mostra nenhum dado de exame.

`MainActivity` e `CadastroExameActivity`
- Passam a herdar de `ActivityProtegida`.
- Trocam `setContent` por `setConteudoProtegido`.
- As duas precisam da trava, porque o Android pode restaurar o app direto em qualquer uma delas.

`ConfiguracoesActivity`
- Herda de `ActivityProtegida`.
- Estado `disponivel`, atualizado no `onResume` com `BloqueioBll.podeBloquear(this)`. Assim, se o usuário sair, criar um PIN e voltar, a opção já aparece.
- Item "Bloquear o app" com um `Switch`. Texto de apoio: "Pede digital, rosto ou PIN ao abrir o app".
- O item só aparece quando `disponivel` for verdadeiro.
- Ligar: chama `autenticar(ATIVAR_BLOQUEIO)`. Só grava `true` se a autenticação der certo. Se o usuário cancelar, o switch volta para desligado.
- Desligar: grava `false` direto. Quem está nessa tela já passou pela trava.
- Sobrescreve `aoAtivarBloqueio()` para marcar o switch como ligado.

## Casos especiais

| Situação | Comportamento |
|---|---|
| Bloqueio ligado, mas o usuário tirou o PIN do celular | `podeBloquear` dá falso. O app abre normal. A opção some da tela de configurações. |
| Usuário tira o PIN com a `TelaBloqueada` aberta e volta ao app | O `onResume` vê que não precisa mais travar e mostra o conteúdo. |
| Backup do Google restaurado num celular novo | O `bloqueio_ativo` pode voltar como `true`. Vale a mesma regra: se o celular novo não cumprir os requisitos, o app abre normal. |
| Usuário cancela o prompt | Fica na `TelaBloqueada`. O botão "Desbloquear" abre o prompt de novo. |
| Usuário gira a tela com o prompt aberto | O mesmo prompt continua. Não abre um segundo. |
| Usuário vai para o cadastro e volta | Não pede de novo. |
| Usuário sai do app e abre de novo pouco depois | Não pede, se o Android ainda não encerrou o processo. |
| Android encerra o processo em segundo plano | Ao voltar, o app está travado. |

## Fora do escopo

- Senha própria do app.
- Travar de novo quando o app volta do segundo plano (por exemplo, após um tempo limite).
- Esconder o conteúdo na tela de apps recentes (`FLAG_SECURE`).
- Criptografar os dados com `CryptoObject`.
- Migrar para a `androidx.biometric` 1.4 (hoje em beta), que tem API própria para Compose.

## Textos

Todos os textos novos ficam em `strings.xml`, em português e inglês, conforme `traducao-strings-ingles.md`.

## Validação

O agente não roda o emulador. O usuário valida pelo Android Studio:

1. Emulador sem bloqueio de tela: a opção não aparece e o app abre normal.
2. Criar um PIN com o app aberto e voltar para as configurações: a opção aparece sem reiniciar o app.
3. Ligar a opção e cancelar o prompt: o switch fica desligado.
4. Ligar a opção e autenticar: o switch fica ligado.
5. Fechar o app pela tela de recentes e abrir de novo: o prompt aparece antes da lista.
6. Cancelar esse prompt: aparece a `TelaBloqueada`, sem nenhum exame visível.
7. Girar a tela com o prompt aberto: não aparece um segundo prompt.
8. Abrir o cadastro de um exame e voltar: não pede de novo.
9. Deixar o app em segundo plano e rodar `adb shell am kill com.example.meusexames`. Ao voltar, o app está travado.
10. Com a opção ligada, remover o PIN do celular: o app abre normal e a opção some.
11. Emulador com API 28 ou 29: ligar e desbloquear com PIN funciona.
12. Emulador com digital cadastrada (Extended Controls > Fingerprint): desbloquear com digital funciona.
13. No cadastro, tocar em "Selecionar arquivo": o seletor abre e o app não fecha (confere a `fragment` 1.9.1).

## Referências

- [Show a biometric authentication dialog](https://developer.android.com/identity/sign-in/biometric-auth): guia oficial. Tipos de autenticação, `canAuthenticate()`, `PromptInfo` e uso do `DEVICE_CREDENTIAL`.
- [BiometricManager](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricManager): retornos do `canAuthenticate()` e combinações que não funcionam antes da API 30.
- [BiometricPrompt](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt): construtor com `FragmentActivity`, prompt que continua após a rotação e aviso sobre criar várias instâncias.
- [Biometric release notes](https://developer.android.com/jetpack/androidx/releases/biometric): versões da biblioteca (1.1.0 estável, 1.4.0 em beta).
- [Fragment release notes](https://developer.android.com/jetpack/androidx/releases/fragment): versões da `fragment`; a 1.3.0 passou a usar o `ActivityResultRegistry` e tirou o limite de 16 bits do `requestCode`.
- [Save simple data with SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences): como gravar a configuração `bloqueio_ativo`.

Seguir `agent/arquitetura.md`.
