# Z6 Panel

Inicio y paneles superpuestos para TV Box, con reloj superior, Ajustes y las optimizaciones de 0.10.4. La versión vigente **0.11.0, código 24**, añade adaptación comprobada en el emulador Android TV 12 y conserva la firma original. El TV Box físico permanece en 0.10.4.

## Uso rápido en Windows

**Versiones:** `v0.10.4` / rama `android10` conserva el proyecto anterior para Android 10. `v0.11.0` / rama `main` contiene la adaptacion comprobada en Android TV 12. El instalador usa el APK de la rama seleccionada. Lee [como elegir y recuperar cada version](docs/VERSIONES.md); el historial Git no cambia por si solo el dispositivo.

1. Descarga el repositorio privado y los dos APK de su entrega a `entregas/`. La copia local consolidada ya los incluye. Para descargar de un repositorio privado necesitas acceso a él. Este proyecto no se ha publicado automáticamente.
2. Si falta ADB: `powershell -File .\herramientas\preparar-herramientas.ps1 -SoloAdb`.
3. Activa depuración en el TV Box y autoriza el PC. Sustituye `IP:PUERTO` por su conexión real:

```powershell
powershell -File .\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica
```

El asistente identifica y respalda el destino, instala, aplica preferencias, configura permisos y verifica Z6 como HOME antes de deshabilitar el inicio anterior. Pedirá la autorización local de Android si está disponible. No cambia redes, emparejamientos, idioma, zona horaria ni resolución. El nombre del usuario se personaliza en el propio panel.

**Compatibilidad:** el asistente admite Android 10/API 29 y Android TV 12/API 31. La base 0.10.4 fue comprobada en el TV físico Android 10; 0.11.0 se comprobó en el emulador API 31. Su regresión física en Android 10 queda pendiente. Las otras API permiten diagnosticar, pero bloquean la instalación automática. Consulta la [matriz](docs/COMPATIBILIDAD.md).

## Comandos

```powershell
.\z6.ps1 instalar -Dispositivo IP:PUERTO -Simular
.\z6.ps1 diagnosticar -Dispositivo IP:PUERTO
.\z6.ps1 configurar -Dispositivo IP:PUERTO
.\z6.ps1 verificar -Dispositivo IP:PUERTO
.\z6.ps1 exportar -Dispositivo IP:PUERTO
.\z6.ps1 restaurar -Dispositivo IP:PUERTO
.\z6.ps1 compilar
```

`-Simular` y `-WhatIf` no ejecutan ADB. `restaurar` recupera el HOME y los ajustes del respaldo del mismo equipo, sin desinstalar ni borrar datos. En API 29, `-RecuperarVersion` también instala la recuperación 0.10.4, código 25; después se necesita código 26 o superior para actualizar. Esta recuperación antigua se bloquea en API 31, donde se restaura el inicio nativo. Se rechazan respaldos de otro dispositivo y downgrades.

Para aplicaciones externas, proporciona APK de procedencia verificada mediante `-CarpetaApks RUTA`, nombrados por paquete, por ejemplo `com.xuper.netxxus.apk`. Se valida el identificador con las build-tools antes de instalar. No se incluyen cuentas, licencias ni APK de terceros en Git. Usa `-SoloPanel` si las instalarás después, como en el emulador actual.

## Contenido

| Carpeta | Función |
|---|---|
| `app` | Única base vigente: aplicación y auxiliar integrados. |
| `herramientas` | Compilación, dependencias y restauración. |
| `perfiles` | Preferencias transferibles y aplicaciones requeridas. |
| `entregas` | APK firmados locales y manifiesto de integridad. Los binarios se distribuyen aparte de Git. |
| `pruebas` | Pruebas locales con ADB simulado, sin tocar el TV. |
| `docs` | Guías, limitaciones y resultados. |
| `privado` | Firma, respaldo de firma, auditorías, identidades y preferencias personales. Excluido de Git. |
| `.local` | SDK, compilaciones y resultados temporales. Excluido de Git. |

Lee [instalación y recuperación](docs/INSTALACION.md), [compilación](docs/COMPILACION.md), [compatibilidad](docs/COMPATIBILIDAD.md) y [consolidación](docs/CONSOLIDACION.md).

El archivo `.local/Z6-Panel-0.11.0-portable.zip` es una entrega lista para trasladar: incluye código, asistentes y APK firmados, sin firma privada ni datos del equipo original. Se regenera con `herramientas/empaquetar.ps1`. No contiene el SDK; ADB se prepara con el comando indicado al principio.

El emulador configurado y su recuperación se describen en [Android TV 12](docs/EMULADOR-TV12.md). Para otro emulador: `.\z6.ps1 instalar -Dispositivo emulator-5554 -SoloPanel`, sustituyendo el serial real.

## Comprobaciones

`powershell -File .\pruebas\verificar.ps1` ejecuta pruebas de instalación, repetición, recuperación e identidades con un ADB falso. Las pruebas físicas se realizan según [la lista de aceptación](docs/PRUEBAS-FISICAS.md).
