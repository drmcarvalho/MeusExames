Vamos criar um novo botão chamado `Compartilhar` no elemento exame.

Funcionalidade do botão:
Permitir compartilhar um exame seguindo o modelo abaixo:
```text
Exame: {titulo do exame}
Data do exame: {data do exame}
```
A lógica desse template/modelo pode ser montada na camada BLL.

Além dessa string vamos compartilhar o arquivo anexado ao exame também.

Ao clicar no botão permitir abrir aplicativos de mensagens para compartilhar o exame neles.

Referencia: `agent/ui/interface-principal.md`

Documentação:
https://developer.android.com/develop/ui/compose/sharing/send

Documentação adicional de apoio:
- [Visão geral: compartilhar texto e dados simples](https://developer.android.com/training/sharing)
- [Enviar texto para outros apps](https://developer.android.com/training/sharing/send)
- [Receber texto de outros apps](https://developer.android.com/training/sharing/receive)
- [Visão geral: compartilhar arquivos](https://developer.android.com/training/secure-file-sharing)
- [Configurar o compartilhamento de arquivos](https://developer.android.com/training/secure-file-sharing/setup-sharing)
- [Compartilhar um arquivo](https://developer.android.com/training/secure-file-sharing/share-file)
- [Pedir um arquivo de outro app](https://developer.android.com/training/secure-file-sharing/request-file)
- [Ler informações do arquivo](https://developer.android.com/training/secure-file-sharing/retrieve-info)
- [Classe FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)