# Step-by-step: Debug signing volta ao keystore padrão AGP

**Data:** 2026-08-17  
**Arquivo:** `app/build.gradle.kts`

Removido override de `$rootDir/debug.keystore`. Debug usa `~\.android\debug.keystore`.
