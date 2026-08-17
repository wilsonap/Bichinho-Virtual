# Step-by-step: Auditoria e restauração do áudio

**Data:** 2026-08-17

## Diagnóstico

Todos os **25** arquivos em `app/src/main/res/raw/*.wav` estão **corrompidos** (contêm `EF BF BD` = U+FFFD).

- Corrupção já presente no commit de introdução: `025f010` (*chore: add missing sound effect and music files*).
- Commit `7ffc834` reescreveu vários WAVs (tamanhos mudaram), ainda corrompidos.
- **Nenhuma versão válida existe no histórico Git** → restauração a partir do Git **impossível**.

## Causa provável

Pipeline/tool tratou WAV como texto UTF-8 e substituiu bytes inválidos por `EF BF BD` (ex.: sample rate `44 AC 00 00` → `44 EF BF BD 00 00`).

## Código

`GameAudioManager`, `MainActivity` (`onAppForeground`/`onAppBackground`), `MainApp` (`playBgm` por rota) e SFX no `PetViewModel` estão intactos. O silêncio vem dos assets inválidos (SoundPool/MediaPlayer falham ao decodificar).

## Proteção aplicada

Criado `.gitattributes`:

```
*.wav binary
*.mp3 binary
*.ogg binary
*.aac binary
*.m4a binary
```

## Próximo passo (obrigatório)

Substituir os 25 `.wav` por arquivos PCM válidos (ex.: 44.1 kHz / 16-bit), **sem** passar por edição de texto/UTF-8/Base64-as-text.
