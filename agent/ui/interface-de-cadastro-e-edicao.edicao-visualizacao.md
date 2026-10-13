Ao clicar no botão editar de um exame apresentado na tela de listagem de exames vamos efetuar uma 
busca no banco por esse exame e exibir na tela de cadastro no modo edição. Preencher todos os campos 
com o exame/model/entidade localizada pelo id. Permitir o usuário salvar a edição. 

Ao substituir o arquivo sobrescrever ou apagar o original e adicionar o novo arquivo selecionado 
pelo usuário.

Botão com a opção de abrir o arquivo conforme o tipo, imagem para galeria ou app de imagem, 
PDF com app de pdf e docs para app de docs, caso não houver um app padrão para o tipo de arquivo, 
vamos apenas exibir uma mensagem para o usuário.

Para contexto veja a spec de interface de cadastro principal `agent/ui/interface de cadastro e edicao.md`.

Botões de ação permanecem os mesmos, a diferença é que no salvar vai chamar o método update via BLL.

Seguir arquitetura: `agent/arquitetura.md`