# Ajuste de scroll do Hub de Minijogos

**Data:** 2026-08-21  
**Escopo:** layout/scroll apenas — sem alterar lógica, pontuação, recordes ou navegação dos minijogos.

## Problema

Com 5 minijogos, os cards altos e o espaçamento vertical faziam os últimos itens (Pescaria / Siga as Pegadas) ficarem cortados ou inacessíveis atrás da Bottom Navigation.

## Arquivo alterado

| Arquivo | Função |
|---------|--------|
| `app/src/main/java/com/example/ui/screens/MinigamesHubScreen.kt` | Tela do hub: TopAppBar + LazyColumn com cards dos minijogos |

## O que cada parte faz agora

- **Scaffold interno** com `contentWindowInsets = WindowInsets(0)`: evita insets duplicados; o `MainApp` já aplica `padding(innerPadding)` no `NavHost` por causa da Bottom Navigation.
- **TopAppBar** mais baixa (`expandedHeight = 48.dp`) e insets zerados: reduz o vão entre o título “Minijogos” e o card amarelo.
- **LazyColumn** com `contentPadding` (top 4.dp, bottom 28.dp) e `spacedBy(8.dp)`: lista realmente rolável até o último card, com folga acima da barra inferior.
- **MinigameCard** mais compacto (ícone 44.dp, padding 12×10, tipografia `titleSmall`): menos altura sem cortar ícone, nome, descrição, recorde e seta.

## Minijogos mantidos (ordem)

1. Jogo da Memória (`memory`)
2. Corrida do Bichinho (`runner`)
3. Captura de Objetos (`catch`)
4. Pescaria (`fishing`)
5. Siga as Pegadas (`footsteps`)

## Como validar

1. Abrir a aba **Jogos**.
2. Confirmar que a Bottom Navigation permanece fixa.
3. Rolar a lista até o fim.
4. Verificar que o card **Siga as Pegadas** aparece 100% (recorde + seta) acima da barra inferior, em telas pequenas e grandes.

## Compilação

`gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL.
