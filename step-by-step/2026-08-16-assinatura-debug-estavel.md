# Step-by-step: Assinatura debug estável (preservar dados)

**Data:** 2026-08-16  
**Objetivo:** Instalar builds debug por cima sem desinstalar o app (preservar Room e dados).

## Problema

`debugConfig` apontava para `${rootDir}/debug.keystore` (gitignored / inexistente), causando certificado instável entre builds → Android Studio desinstalava o app.

## Correção em `app/build.gradle.kts`

1. Removido o bloco `create("debugConfig") { ... }`.
2. Removida a linha `debug { signingConfig = signingConfigs.getByName("debugConfig") }`.
3. Release **não** foi alterado.
4. Debug passa a usar o keystore padrão do AGP: `%USERPROFILE%\.android\debug.keystore`.

## Confirmação (`:app:signingReport`)

| Campo | Valor |
|-------|--------|
| Variant | `debug` |
| Config | `debug` (padrão AGP) |
| Store | `C:\Users\Alessandro\.android\debug.keystore` |
| Alias | `AndroidDebugKey` |
| SHA1 | `93:E0:7C:A1:B4:E5:3D:C2:73:1B:1C:9A:90:8F:FD:4D:1F:8C:35:88` |

Release permanece apontando para `my-upload-key.jks` (inalterado).

## Observação

A build já instalada no aparelho pode ter outro certificado. Pode ser necessário **uma** desinstalação manual antes da primeira install com a assinatura estável. Depois disso, updates preservam dados.

`assembleDebug` validou a assinatura (`validateSigningDebug`), mas falhou depois por recursos `R.raw` ausentes em `GameAudioManager` — problema pré-existente, fora desta correção.
