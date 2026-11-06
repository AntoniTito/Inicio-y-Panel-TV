# Z6 Panel

Inicio y paneles superpuestos para TV Box, con reloj superior, Ajustes y las optimizaciones verificadas de **0.10.4, código 22**. Esta consolidación conserva el código y la firma; no cambia el firmware ni la versión instalada.

## Uso rápido en Windows

1. Descarga el repositorio privado y los dos APK de su entrega a `entregas/`. La copia local consolidada ya los incluye. Para descargar de un repositorio privado necesitas acceso a él. Este proyecto no se ha publicado automáticamente.
2. Si falta ADB: `powershell -File .\herramientas\preparar-herramientas.ps1 -SoloAdb`.
3. Activa depuración en el TV Box y autoriza el PC. Sustituye `IP:PUERTO` por su conexión real:

```powershell
powershell -File .\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica
```

El asistente identifica y respalda el destino, instala, aplica preferencias, configura permisos y verifica Z6 como HOME antes de deshabilitar el inicio anterior. Pedirá la autorización local de Android si está disponible. No cambia redes, emparejamientos, idioma, zona horaria ni resolución. El nombre del usuario se personaliza en el propio panel.

**Compatibilidad:** la instalación automática de este APK conservado está limitada a Android 10/API 29. En otras versiones se permite diagnosticar y se detiene la instalación antes de modificar el destino. La adaptación a Android reciente es una actualización separada; no se presenta como ya realizada.

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

`-Simular` y `-WhatIf` no ejecutan ADB. `restaurar` recupera el HOME y los ajustes del respaldo del mismo equipo, sin desinstalar ni borrar datos. `-RecuperarVersion` también instala la recuperación 0.10.3, código 23: futuras actualizaciones necesitan código 24 o superior. Se rechazan respaldos de otro dispositivo y downgrades.

Para aplicaciones externas, proporciona APK de procedencia verificada mediante `-CarpetaApks RUTA`, nombrados por paquete, por ejemplo `com.xuper.netxxus.apk`. Se valida el identificador con las build-tools antes de instalar. No se incluyen cuentas, licencias ni APK de terceros en Git; las aplicaciones ausentes aparecen en el informe.

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

El archivo `.local/Z6-Panel-0.10.4-portable.zip` es una entrega lista para trasladar: incluye código, asistentes y APK firmados, sin firma privada ni datos del equipo original. Se regenera con `herramientas/empaquetar.ps1`. No contiene el SDK; ADB se prepara con el comando indicado al principio.

## Comprobaciones

`powershell -File .\pruebas\verificar.ps1` ejecuta pruebas de instalación, repetición, recuperación e identidades con un ADB falso. Las pruebas físicas se realizan según [la lista de aceptación](docs/PRUEBAS-FISICAS.md).
