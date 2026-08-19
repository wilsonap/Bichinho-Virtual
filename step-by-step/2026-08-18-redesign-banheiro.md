# Redesign visual do Banheiro

**Data:** 2026-08-18  
**Status:** implementado

## Escopo

Apenas cenário Canvas. Sem mudanças em mecânicas, animações de banho, áudio ou posição do pet (`floorY = 70%`).

## Layout

| Zona | Conteúdo |
|------|----------|
| Esquerda | Banheira maior, chuveiro, saboneteira, planta |
| Centro | Livre para o pet; tapete antiderrapante no chão |
| Direita | Armário de higiene, pia, espelho, toalheiro, porta-escovas, prateleiras |
| Superior | Janela pequena de ventilação |

## Arquivos

- Novo: `BathroomScenery.kt`
- Removido desenho antigo de `HouseRoomsScenery.kt`
