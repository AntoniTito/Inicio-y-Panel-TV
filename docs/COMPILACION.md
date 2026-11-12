# Compilar Inicio y Panel TV

Para instalar el entorno usa los APK firmados de [Releases](https://github.com/AntoniTito/Inicio-y-Panel-TV/releases/latest). Compilar es un flujo de desarrollo, no un requisito de instalación.

## Requisitos

Windows, JDK 21, Android Build Tools 35.0.0, plataforma Android 35 revisión 2 y Platform Tools 37.0.1. `herramientas/preparar-herramientas.ps1` descarga las herramientas Android desde Google y comprueba los hashes fijados. El SDK se guarda en `.local/sdk`, fuera de Git.

Configura `JAVA_HOME` al JDK 21 o proporciona `-Jdk RUTA\bin`. El código y los scripts resuelven sus rutas desde el proyecto, sin depender de carpetas fechadas o rutas personales.

## Compilación para desarrollo

El código propio está disponible bajo GPL-3.0-only. La clave oficial no se distribuye; puedes usar tu propia clave para compilar y estudiar una variante.

```powershell
New-Item -ItemType Directory -Force .local | Out-Null
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore .local\desarrollo.p12 -storetype PKCS12 -alias desarrollo -keyalg RSA -keysize 2048 -validity 3650
.\herramientas\compilar.ps1 -Keystore .local\desarrollo.p12 -Salida .local\build\Inicio-y-Panel-TV-desarrollo.apk
```

Introduce la contraseña de esa clave cuando se solicite. El compilador acepta también la variable temporal `Z6_STORE_PASSWORD`; no la escribas en un archivo versionado ni la compartas. Restaura sus variables de entorno al terminar. No genera otra firma automáticamente.

**Una firma propia no actualiza una instalación oficial firmada por el autor.** Utiliza un emulador o destino de desarrollo separado; no desinstales una app con datos para sortear la firma. Si publicas una variante, identifica su procedencia y firma y conserva las obligaciones de licencia y los avisos de terceros.

## Entregas oficiales

El propietario usa la clave original, conservada privadamente. Sin `-Keystore`, el compilador busca `privado/firma/original.p12`; su contraseña local DPAPI solo se reutiliza para esa clave. La clave y su respaldo nunca se publican ni se incorporan a GitHub Actions.

La fuente actual produce **0.20.2/c62**, paquete `local.z6panel`. La salida predeterminada es `.local/build/Inicio-y-Panel-TV-recompilado.apk`; no puede sobrescribir artefactos verificados de `entregas`. El compilador verifica el certificado y muestra el hash.

La recuperación **0.20.1/c63** se prepara con `herramientas/recuperacion-0202.ps1`, desde la etiqueta inmutable `v0.20.1` y la misma firma. Conserva los formatos de datos y la marca anterior Z6 Panel; tras instalarla se requiere c64 o superior. El cambio de marca no altera el protocolo del auxiliar.

Después de revisar manifiesto y artefactos, `herramientas/empaquetar.ps1` genera el portable con código, capturas, guías, licencia y dos APK. No incluye SDK, claves o datos individuales. Consulta [TERCEROS.md](TERCEROS.md), [VERSIONES.md](VERSIONES.md) y [CONTRIBUTING.md](../CONTRIBUTING.md).
