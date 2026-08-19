# Reposicionamento do balão de diálogo

**Data:** 2026-08-18  
**Status:** implementado

## Problema

O balão era forçado a uma faixa no **topo** do stage (~14,5%), cobrindo janelas e decorações.

## Solução

| Antes | Depois |
|-------|--------|
| Clamp no topo do cenário | Âncora na cabeça do pet |
| `petY - 42.dp` + banda superior | `petY - 28.dp - corpo - seta` |
| Topo livre para o balão | **15%** superior protegido (cenário) |
| Largura 182.dp | 168.dp / altura máx. 64.dp |

Textos, animações e lógica de falas **inalterados**.

## Arquivo

`HomeScreen.kt` — `PetLivingStage` (camada do balão)
