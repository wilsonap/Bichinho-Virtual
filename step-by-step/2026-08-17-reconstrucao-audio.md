# Step-by-step: Reconstrução completa do áudio

**Data:** 2026-08-17

## Auditoria

- 25/25 arquivos em `res/raw` estavam corrompidos (`EF BF BD`).
- Git não tinha versão válida → recriação obrigatória.
- Código (`GameAudioManager`, `MainActivity`, `MainApp`) **não alterado**.

## Geração

Ferramenta binária: `tools/GenerateGameAudio.cs` → `GenerateGameAudio.exe`  
Saída: PCM 16-bit LE, 44100 Hz, RIFF/WAVE válidos (sem UTF-8).

| Tipo | Canais | Arquivos |
|------|--------|----------|
| BGM | Stereo | home 52s, shop 48s, minigame 46s, celebration 8s, incubator 50s |
| SFX | Mono | 20 efeitos |

## Validação

25/25 OK: RIFF, WAVE, fmt PCM, data, 44100 Hz, 16-bit, sem `EF BF BD`.

## Proteção

`.gitattributes` com `*.wav binary` (e mp3/ogg/aac/m4a).
