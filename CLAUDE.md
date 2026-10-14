# Notação de prompts em árvore

Os arquivos de prompt usam pontos para indicar níveis.
Cada ponto é um nível novo (sem contar o `.md` do final).
O pai tem o mesmo nome do filho, sem o último nível.

*O ponto só separa níveis. Num nível, as palavras usam hífen.*

Exemplo:
- `interface-principal.md` → raiz
- `interface-principal.btn-apagar.md` → filho de `interface-principal.md`
- `interface-principal.btn-apagar.bugfix-message.md` → filho de `interface-principal.btn-apagar.md`

## Antes de executar qualquer prompt
1. Tire o `.md` do final e separe o resto do nome pelos pontos.
2. Leia todos os ancestrais, da raiz até o arquivo atual, nessa ordem.
3. Se um ancestral não existir, pare e avise. Não invente contexto.
4. Se o filho contradizer o pai, vale o filho.