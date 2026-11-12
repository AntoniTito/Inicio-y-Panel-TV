# Z6 Panel

Inicio y paneles superpuestos para TV Box. **0.20.1, código 60**, corrige el regreso a Mi control, añade gestión visible de rutas y distingue abrir apps de elegir una sección opcional. Conserva [destinos reutilizables, borradores y etiquetas editables](docs/DESTINOS-Y-BOTONES.md). Consulta la [verificación de la entrega](docs/VERIFICACION-0.20.1.md), incluido el estado de la instalación física.

La rama `main` contiene 0.20.1. Abrir o cerrar y abrir una app no requiere elegir un destino. Una sección probada se puede asignar a varios botones y al arranque/despertar. El nombre visible se edita sin cambiar la tecla detectada. El selector reconoce variantes instaladas según sus capacidades, sin una lista fija de nombres.

**Para replicar el entorno completo en otro TV:** entrega a la IA el [prompt único](docs/PROMPT-REPLICA.md) y la [guía de réplica](docs/REPLICAR-ENTORNO.md). Incluyen instalación, sustitución reversible del inicio, aspecto, apps, fondos, teclas, destinos reutilizables, arranque/despertar y recuperación. La misma entrega vigente sirve para Android 10/12; las autorizaciones, apps y control se comprueban en cada destino.

## Uso rápido en Windows

**Versiones:** `v0.10.4` / rama `android10` conserva el proyecto anterior para Android 10. `v0.11.0` conserva la adaptación anterior y `v0.12.0` conserva los fondos iniciales y `v0.12.1` corrige su cobertura completa. `v0.12.2` conserva la recepción confirmada. `v0.13.0` conserva la biblioteca visual. `v0.14.0` conserva el rediseño anterior. `v0.15.0` conserva la gestión integrada y elección de paisajes. `v0.16.0` conserva catálogo diario y galería compartida; `v0.16.1` retira el acceso a la galería nativa dentro de Fondos; `v0.16.2` corrige las transiciones del fondo; `v0.16.3` evita trabajo repetido al recibir Home; `v0.16.4` aclara la selección translúcida de las tarjetas; `v0.16.5` conserva la corrección del retorno desde Añadir app; `v0.17.0` conserva las filas circulares independientes del panel; `v0.18.0` añade la recuperación de TV configurable. `v0.18.1` corrige la reconexion del auxiliar sin reiniciar la depuracion. El instalador usa el APK de la rama seleccionada. Lee [como elegir y recuperar cada version](docs/VERSIONES.md); el historial Git no cambia por si solo el dispositivo.

1. Descarga el repositorio privado y los dos APK de su entrega (actual y recuperación común a API 29/31) a `entregas/`. La copia local consolidada ya los incluye. Para descargar de un repositorio privado necesitas acceso a él. Este proyecto no se ha publicado automáticamente.
2. Si falta ADB: `powershell -File .\herramientas\preparar-herramientas.ps1 -SoloAdb`.
3. Activa depuración en el TV Box y autoriza el PC. Sustituye `IP:PUERTO` por su conexión real:

```powershell
powershell -File .\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica -Simular
powershell -File .\z6.ps1 diagnosticar -Dispositivo IP:PUERTO
powershell -File .\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica -SoloPanel
```

El asistente identifica y respalda el destino, instala, aplica preferencias, configura permisos y verifica Z6 como HOME antes de deshabilitar el inicio anterior. Pedirá la autorización local de Android si está disponible. No cambia redes, emparejamientos, idioma, zona horaria ni resolución. El nombre del usuario se personaliza en el propio panel. `-SoloPanel` permite instalar las apps externas después; sin esa opción consulta las del perfil. Botones, destinos y arranque/despertar se completan según la guía, no se importan de otro TV.

**Compatibilidad:** el asistente admite Android 10/API 29 y Android TV 12/API 31. La base 0.10.4 fue comprobada en el TV físico Android 10; 0.12.0 se comprobó en el emulador API 31 y su biblioteca/recepción en API 29. La imagen stock API 29 presenta fallos nativos de superposición reproducidos también con 0.10.4; la comprobación física de la entrega actual se registra por separado y no se extrapola a otros modelos. Las otras API permiten diagnosticar, pero bloquean la instalación automática. Consulta la [matriz](docs/COMPATIBILIDAD.md).

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

`-Simular` y `-WhatIf` no ejecutan ADB. `restaurar` recupera el HOME y los ajustes del respaldo del mismo equipo, sin desinstalar ni borrar datos. `-RecuperarVersion` selecciona la recuperación 0.20.0/código 61 para API 29 y 31, con el arreglo de acceso a Mi control. Después se necesita código 62 o superior para actualizar. Se rechazan respaldos de otro dispositivo y downgrades.

Para aplicaciones externas, proporciona APK de procedencia verificada mediante `-CarpetaApks RUTA`, nombrados por paquete, por ejemplo `com.android.mgstv.apk` para la variante Xuper de referencia. Se valida el identificador con las build-tools antes de instalar; si falta otro paquete, adapta el perfil local como explica la guía. No se incluyen cuentas, licencias ni APK de terceros en Git. Usa `-SoloPanel` si las instalarás después.

## Contenido

| Carpeta | Función |
|---|---|
| `app` | Única base vigente: aplicación y auxiliar integrados. |
| `herramientas` | Compilación, dependencias y restauración. |
| `perfiles` | Preferencias transferibles y aplicaciones requeridas. |
| `entregas` | APK firmados locales y manifiesto de integridad. Los binarios se distribuyen aparte de Git. |
| `pruebas` | Pruebas con ADB simulado y suites de emulador, que exigen un destino explícito. |
| `docs` | Guías, limitaciones y resultados. |
| `privado` | Firma, respaldo de firma, auditorías, identidades y preferencias personales. Excluido de Git. |
| `.local` | SDK, compilaciones y resultados temporales. Excluido de Git. |

Lee [instalación y recuperación](docs/INSTALACION.md), [navegación del panel](docs/PANEL-SUPERPUESTO.md), [compilación](docs/COMPILACION.md), [compatibilidad](docs/COMPATIBILIDAD.md) y [consolidación](docs/CONSOLIDACION.md).

El archivo `.local/Z6-Panel-0.20.1-portable.zip` es una entrega lista para trasladar: incluye código, asistentes, instrucciones de réplica y APK firmados, sin firma privada ni datos del equipo original. Se regenera con `herramientas/empaquetar.ps1`. No contiene el SDK; ADB se prepara con el comando indicado al principio.

El emulador configurado y su recuperación se describen en [Android TV 12](docs/EMULADOR-TV12.md). Para otro emulador: `.\z6.ps1 instalar -Dispositivo emulator-5554 -SoloPanel`, sustituyendo el serial real.

Consulta [Aplicaciones y fondos](docs/FONDOS.md) para enviar fotos desde el celular, usar TV/USB y entender la resolución del fondo. El fondo original sigue siendo el predeterminado; las fotos y sus sesiones no se distribuyen.

## Comprobaciones

`powershell -File .\pruebas\verificar.ps1` ejecuta pruebas de instalación, repetición, recuperación e identidades con un ADB falso. Las pruebas físicas se realizan según [la lista de aceptación](docs/PRUEBAS-FISICAS.md).
