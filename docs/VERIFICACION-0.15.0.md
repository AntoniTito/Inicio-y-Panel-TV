# Verificación de Z6 Panel 0.15.0

Entrega del 6 de noviembre de 2026: **0.15.0/código 36**, un APK firmado con la clave original para Android 10/API 29 y Android TV 12/API 31. Instalado en el TV Box original y en el emulador API 31. Se conservaron las tres fotos personales del TV, sus preferencias y las etiquetas anteriores de Git.

## Resultado y correcciones

Los cuatro modos de Fondos aparecen en una fila. Foto fija y Presentación comparten la galería integrada, con seis fotos por página junto a la vista previa. Las flechas previsualizan sin aplicar; OK utiliza la foto. Las marcas de presentación se guardan al pulsar Usar presentación. La vista previa se conserva al recorrer controles y volver entre modos. La galería ampliada sigue disponible desde Mis fotos.

Eliminar foto, Eliminar seleccionadas y Eliminar todas solicitan confirmación y borran directamente los archivos privados y miniaturas, sin papelera. Se comprueba su ausencia y se informa el espacio liberado. Las cargas pendientes no pueden regenerar una miniatura borrada. El borrado afecta a las copias de Z6, no a los originales del teléfono o USB; no se promete borrado forense del almacenamiento flash.

Elegir otro paisaje permite recorrer miniaturas y descargar la elección al pulsar OK. Se corrigieron una carrera de limpieza de temporales y la selección de panorámicas sin suficientes píxeles para el recorte. Para calcular la descarga se considera la altura necesaria y se utilizan tamaños reducidos estándar de Wikimedia. Se descartan originales o proporciones que no pueden cubrir el inicio con esos tamaños. Una miniatura no se utiliza como fondo completo. Los errores conservan el fondo anterior y permiten reintentar.

En el TV original, la misma panorámica guardada a 484×272 se recuperó a **1280×720**, con archivo de **159 KB**. Una panorámica extrema exigía una miniatura nominal de 5000 píxeles, pero su URL entregaba 3840: no alcanzaba la altura para el encuadre. El catálogo final excluye esa elección incompatible. Tres paisajes diferentes se aplicaron en el emulador con el APK final, en 2,45–4,19 segundos. Son tiempos de descarga y preparación, no una promesa de velocidad de Internet. El usuario confirmó en el TV: **«Sí, ahora se aplica»**.

No se modificaron HDMI, red física, panel inferior, reloj, iconos ni rutas de audio. Las fotos personales anteriores no se recodificaron. Los fondos guardados se usan localmente; la navegación no introduce descargas ni comprobaciones periódicas nuevas.

## Evidencia y alcance

| Entorno | Resultado |
|---|---|
| Emulador API 31, Fondos | 23 comprobaciones: fila, foco, aplicación, vista previa persistente, selección, páginas, borrado sin restos de prueba, presentación parcial, orden, vacío, recreación a 1080p, limpieza al cerrar paisajes y fallo sin conexión conservando el fondo. |
| Emulador API 31, controles | 27 comprobaciones: Home, foco, secciones, volumen/silencio, ruta nativa Bluetooth, temporizador, suspensión y reinicio real. Recuperación autónoma del auxiliar dentro de 180 segundos. |
| TV original API 29, inicio 1280×720 | 20 comprobaciones: actualización conservando preferencias y tres fotos, recepción por LAN, uso y foco de foto recibida, gestión integrada, borrado de archivos y miniaturas sintéticos, presentación de elegidas y recuperación del fondo original al borrar la última participante. |
| Nitidez | Dos comprobaciones específicas en emulador y tres en el TV. Inicio y archivo preparados a 1280×720; las tres fotos personales se conservaron. |
| Otros paisajes, APK final | Tres elecciones diferentes aplicadas en el emulador; aplicación confirmada además por el usuario en el TV original. |
| Instalador, ADB simulado | 43 comprobaciones: identidad, integridad, instalación, permisos, HOME, restauración y recuperación 0.14.0/código 37 para ambas API, manteniendo selecciones parciales. Sin contactar al TV. |

Informes: [Fondos API 31](../pruebas/resultados-fondos-0.15.0-api31.json), [controles API 31](../pruebas/resultados-regresion-0.15.0-api31.json), [TV API 29](../pruebas/resultados-tv-original-0.15.0-api29.json), [nitidez API 29](../pruebas/resultados-nitidez-0.15.0-api29.json), [nitidez API 31](../pruebas/resultados-nitidez-0.15.0-api31.json), [otros paisajes](../pruebas/resultados-otros-paisajes-0.15.0-api31.json) y [asistente](../pruebas/resultados-asistente-0.15.0.json).

Las pruebas de borrado usaron únicamente fotos sintéticas identificadas. Se restauraron las preferencias al terminar las pruebas automatizadas, antes de entregar el control al usuario. Sus elecciones posteriores de paisajes son cambios intencionales y se conservaron. Respaldos, fotos y registros individuales quedan en privado/; los temporales de desarrollo, en .local/.

## Rendimiento

Tres rondas de 30 eventos DPAD por modo, en API 31 a 720p y sin el juego abierto en el PC. Se compara la mediana de los p95 de cada ronda. La métrica incluye el transporte ADB; **no mide el tiempo entre la pulsación física y el cuadro visible**.

| Modo | 0.14.0, ms | 0.15.0, ms | Límite, ms | Estado |
|---|---:|---:|---:|---|
| Original | 91,16 | 90,05 | 100,27 | Dentro del margen |
| Foto | 87,10 | 90,58 | 95,81 | Dentro del margen |
| Presentación | 120,55 | 99,33 | 132,61 | Dentro del margen |

El margen es el mayor entre 10 % y 8 ms. La medición corresponde a la interfaz final reutilizada; las correcciones posteriores de selección y descarga de paisajes no modificaron esa geometría. No se atribuye esta medición del emulador al TV físico.

En 20 aperturas/cierres con el paisaje diario guardado, el PSS fue 53.020, 45.912, 49.141 y 49.089 KB en los ciclos 5, 10, 15 y 20. El crecimiento entre el mínimo tras calentamiento y el final fue **3.177 KB**, dentro del máximo de 8 MB. Este registro no prueba 20 ciclos separados de presentación. Una repetición adicional se interrumpió por desconexión/reinicio del emulador; no se cuenta como resultado aprobado ni sustituye el registro completo anterior.

Datos: [referencia](../pruebas/referencia-fondos-0.14.0-para-0.15.0-api31.json), [navegación](../pruebas/resultados-rendimiento-0.15.0-sin-carga.json) y [memoria](../pruebas/resultados-memoria-0.15.0.json).

## Integridad y recuperación

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.15.0.apk | `D014EDF2FA1464AC3B74D165C71213A3470E45A498B4EE0B82F6357317621662` |
| Z6-Panel-recuperacion-0.14.0-c37.apk | `20A1C67C9928DDB044CCEEA80BECFD9661B78A52B096462B80BE6CCFEFBB8DD2` |

Certificado original SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`.

La recuperación se compiló desde v0.14.0 cambiando el código a 37. Su classes.dex coincide con el de la entrega 0.14.0: `69AD419733C56ECF4383A1B834DA5DAE1A8A022E23FDB7673255B5BD6F8BE58C`. Se verificaron firma e integridad; el asistente selecciona ese mismo APK para API 29 y 31. Entiende presentaciones parciales. No se instaló la recuperación sobre los equipos durante estas pruebas: la ruta de restauración se verificó con ADB simulado. Tras usarla, una actualización deberá tener **código 38 o superior**. No desinstalar para restaurar.

El paquete portable incluye código, guías y únicamente los dos APK vigentes, con sus hashes; excluye SDK, firma privada, respaldos y fotos. No se publicó el proyecto en un servicio remoto.

## Pendientes físicos

No se repitieron para esta entrega 1080p/4K físicos, USB físico, reproducción prolongada en YouTube/Xuper, JBL/HDMI-CEC ni reinicio completo del TV físico. El reinicio y recuperación autónoma se probaron en el emulador API 31. El control físico confirmó la aplicación de otro paisaje; no equivale a una medición instrumental de fluidez en hardware. No se declara compatibilidad universal ni se extrapola el emulador a otros firmwares.
