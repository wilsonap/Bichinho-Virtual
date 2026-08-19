# Redesign visual do Quintal

**Data:** 2026-08-18  
**Status:** implementado

## Objetivo

Criar um quintal 2D mais rico visualmente, no estilo Tamagotchi, sem alterar mecânicas, animações do pet, posicionamento ou sistema de brincadeiras.

## Escopo (apenas cenário)

- Céu, nuvens, sol/lua, cerca, gramado, caminho, árvore, balanço, lago, banco, flores, arbustos, borboletas.
- Horizonte mantido em `h * 0.58f` para o pet continuar no gramado.

## Fora de escopo

- Mecânicas de jogo, brincadeiras, animações do bichinho, posição do pet.

## Layout

| Zona | Conteúdo |
|------|----------|
| Esquerda | Árvore grande detalhada, balanço maior, arbusto, lago decorativo |
| Centro | Espaço livre + caminho de pedras |
| Direita | Banco de jardim, flores, arbustos |
| Fundo/céu | Cerca de madeira, nuvens animadas, sol/lua, borboletas |

## Arquivos

| Arquivo | Função |
|---------|--------|
| `BackyardScenery.kt` | Desenho completo do quintal (`drawBackyardScene` e helpers) |
| `HouseRoomsScenery.kt` | Removido desenho antigo; comentário aponta para o novo arquivo |
| `RoomSceneryRenderer.kt` | Continua chamando `drawBackyardScene` (mesmo package) |

## Elementos visuais

1. **Árvore** — tronco texturizado, copa em camadas, sombra
2. **Cerca de madeira** — ripas e postes ao longo do horizonte
3. **Caminho de pedras** — pedras irregulares no centro
4. **Jardim com flores** — agrupamentos coloridos à direita
5. **Arbustos** — esquerda e direita
6. **Banco de jardim** — madeira com sombra
7. **Balanço maior** — estrutura + assento sob a árvore
8. **Borboletas** — movimento suave via `phase`
9. **Nuvens animadas** — deslocamento com `phase`
10. **Lago decorativo** — oval com brilho/reflexo
11. **Gramado** — faixas com tonalidades diferentes + profundidade

## Compilação

`./gradlew :app:compileDebugKotlin` — OK (exit 0).
