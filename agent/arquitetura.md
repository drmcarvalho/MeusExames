# Arquitetura

Use esse documento para entender a definição de arquitetura e de organização do aplicativo. 

O Agent deve seguir esse documento a risca e não tomar decisões nos quais vão influenciar na 
arquitetura e organização.

Pacote `com/example/meusexames/dal` contem toda a configuração com o banco de dados, classes DAO e 
modelo e demais arquivos kt para manipulação dos dados.

Pacote `com/example/meusexames/ui` possue as interfaces/activity ou componentes gráficos 
relacionados a qualquer tipo de interface.

Pacote `com/example/meusexames/bll` é responsável pelas regras de negócio ou regras de domínio, 
para separá-las da lógica de banco de dados e da lógica de interface e seus componentes gráficos, 
além de possibilitar o acesso aos dados da camada DAL.

A classe MainActivity `com/example/meusexames/MainActivity.kt` é a atividade principal no qual vai 
chamar a interface de listagem de exames, o caminho dela é `agent/interface principal.md`.

Para os demais padrões como strings, cores, multi linguagem, ou valores estáticos seguir o padrão 
do Android.