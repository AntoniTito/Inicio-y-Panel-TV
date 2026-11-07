# Verificación de Z6 Panel 0.16.0

Entrega del 6 de noviembre de 2026: **0.16.0/código 38**, un APK para Android 10/API 29 y Android TV 12/API 31, firmado con la clave original. Instalado en el TV Box original y en el emulador. La rotación diaria comienza desactivada. Las etiquetas históricas se conservan y no se publicó el repositorio en un servicio remoto.

## Resultado y datos conservados

Paisaje diario ofrece hasta doce imágenes, seis por página, con catálogo y miniaturas persistentes durante la fecha local. Solo se descarga el paisaje elegido; con rotación activa se prepara también el siguiente mientras Inicio está visible. Se puede rotar cada 1, 2, 3 o 6 horas siguiendo el horario real, sin trabajo de fondos durante otras aplicaciones o suspensión. Conservar reutiliza el archivo y lo incorpora a Mis fotos sin caducidad.

Las fotos optimizadas y los paisajes descargados se publican en MediaStore, en álbumes separados bajo Pictures/Z6. Las miniaturas no se publican como fotografías. Una sola copia maestra por entrada; índices y cachés siguen privados. La limpieza retira los temporales anteriores conservando únicamente el fondo activo hasta disponer de un reemplazo. También elimina un respaldo migrado innecesario aunque pertenezca al mismo día. Personales y conservadas no se eliminan automáticamente.

El TV tenía **una foto personal al comenzar esta actualización**, no las tres de verificaciones históricas. Se respaldaron el APK 0.15.0, las preferencias inmediatas y los bytes de esa foto y del paisaje activo. La migración permaneció retenida hasta verificar el respaldo del PC. Después se comprobaron tamaño, hash e identificador de la foto compartida. Se conservó el fondo elegido; las pruebas retiraron solo sus imágenes sintéticas y el paisaje añadido expresamente para comprobar Conservar.

Se habilitó la galería AOSP ya instalada y previamente deshabilitada, conservando su estado original por dispositivo. La inspección visual mostró ambos álbumes con miniaturas. No se instalaron aplicaciones de terceros ni se modificaron HDMI, redes, emparejamientos, launcher o personalización ajena a esta función. Las elecciones posteriores del usuario no se restauran automáticamente.

## Evidencia por entorno

| Entorno | Resultado |
|---|---|
| Emulador API 31, interfaz | 23 comprobaciones de navegación, fila, vista previa, aplicación, selección, orden, borrado, páginas, caché sin Internet y recreación a 1080p. |
| Emulador API 31, medios | 23 comprobaciones de publicación, respaldo por bloques, copia pendiente interrumpida, recuperación por hash/URI/ID, borrado externo, doce candidatos, caché, conservación sin duplicados, rotación por horario real y renovación sin conexión. |
| Emulador API 31, respaldo migrado | Tres comprobaciones adicionales: mantener la elección con rotación desactivada, conservar el respaldo activo y eliminarlo al aplicar un reemplazo válido, incluso en la misma fecha. |
| Emulador API 31, controles | 27 comprobaciones: Home, foco, reloj, volumen/silencio, ruta Bluetooth nativa, temporizadores, suspensión y reinicio real; auxiliar recuperado sin arranque desde PC dentro de 180 segundos. |
| Emulador API 31, recuperación | Siete comprobaciones sobre el APK firmado código 39: instalación sin desinstalar, lectura y representación de fotos compartidas, integridad y limpieza de temporales sin perder permanentes. Un snapshot temporal devolvió el emulador a código 38 y se retiró al finalizar. |
| TV original API 29, 1280×720 | 22 comprobaciones: migración, hashes, opciones completas, galería nativa y eventos direccionales, doce paisajes, caché, recepción LAN, resolución útil, borrado externo y conservación de paisaje sin aplicarlo; actualización final y auxiliar comprobados. |
| Instalador, ADB simulado | 51 comprobaciones: identidad, hashes, permisos, HOME, restauración y recuperaciones por API, actualización antigua con respaldo previo, fallo de respaldo manteniendo la espera y reintento seguro. Habilitación y restauración del estado de la galería. Sin contactar hardware. |

Informes: [interfaz](../pruebas/resultados-fondos-0.16.0-api31.json), [medios](../pruebas/resultados-media-0.16.0-api31.json), [respaldo diario](../pruebas/resultados-respaldo-diario-0.16.0-api31.json), [controles](../pruebas/resultados-regresion-0.16.0-api31.json), [recuperación](../pruebas/resultados-recuperacion-0.16.0-api31.json), [TV original](../pruebas/resultados-tv-original-0.16.0-api29.json) y [asistente](../pruebas/resultados-asistente-0.16.0.json).

La fecha anterior y la publicación incompleta se simularon únicamente en el emulador AOSP, mediante operaciones limitadas protegidas por UID shell y DUMP. La interfaz de inyección rechaza el hardware original, comprobado expresamente. No se cambió el reloj físico. El primer intento de la regresión heredó una solicitud de Ver inicio de otra suite; se corrigió el aislamiento de la prueba y se completaron las 27 comprobaciones, sin cambiar el comportamiento de Home.

## Rendimiento y límites de la medición

Tres rondas de 30 eventos DPAD por modo a 720p, comparando con 0.15.0 en el mismo emulador. Se informa la mediana de los p95 de cada ronda. Incluye proceso y transporte ADB; **no mide pulsación física a cuadro visible ni velocidad real de Internet**.

| Modo | 0.15.0, ms | 0.16.0, ms | Límite, ms |
|---|---:|---:|---:|
| Original | 78,49 | 60,50 | 86,49 |
| Foto | 77,68 | 56,40 | 85,68 |
| Presentación | 74,15 | 57,71 | 82,15 |

Las tres comparaciones cumplen el margen del mayor entre 10 % y 8 ms. Se midió la interfaz y el almacenamiento compartido antes del último ajuste de limpieza del respaldo migrado, que no modificó su geometría ni navegación. Este ajuste tiene sus tres pruebas específicas sobre el APK final. No se extrapolan estos tiempos del emulador al TV.

En los ciclos 5, 10, 15 y 20 de apertura/cierre, el PSS fue 49.457, 47.293, 41.599 y 41.305 KB. El valor final quedó 5.988 KB por debajo del ciclo 10; no hubo crecimiento sostenido superior a 8 MB. Las rondas terminaron en Presentación y las imágenes temporales se eliminaron después.

La secuencia automatizada de apertura del catálogo en el TV registró 6.623 ms inicialmente y 4.090 ms al reabrir, incluyendo varios comandos ADB y consultas completas de estado. El dato verificable para la caché es que la reapertura no produjo consultas ni descargas nuevas y mantuvo los mismos candidatos. No representa la demora aislada del control remoto.

Datos: [referencia](../pruebas/referencia-fondos-0.15.0-para-0.16.0-api31.json) y [rendimiento y memoria](../pruebas/resultados-rendimiento-0.16.0.json).

## Integridad, recuperación y entrega

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.16.0.apk | `8B98E3CB7C8C35968122B66090AC3C891D476CB4A1E06140A5C4B38DBD9DD342` |
| Z6-Panel-recuperacion-0.15.0-c39.apk | `50323F1F1EFEDF58A1DFD60BDBF85736AAA71D78AF6EC37E981CC0CB428BBCFE` |

Certificado original SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`.

La recuperación conserva la interfaz y el comportamiento 0.15.0 y añade el adaptador para archivos antiguos/URI, el índice compartido y su limpieza compatible. No es el APK histórico código 36. Se reconstruye con herramientas/recuperacion-0160.cjs desde la base 0.16.0 y v0.15.0, usando la firma original. Tras instalar código 39 se necesitará **código 40 o superior** para actualizar. No se desinstaló ninguna app para recuperar datos ni se instaló la recuperación en el TV físico.

El portable incluye código, asistentes, guías y únicamente estos dos APK, con hashes; excluye SDK, claves, respaldos, fotos y sesiones. Los respaldos reales y capturas de la galería permanecen en privado/actualizaciones y no se distribuyen. El instalador conserva un registro por identidad para reanudar un respaldo fallido antes de liberar la migración.

Quedan separados como pendientes: confirmación manual con el control físico, 1080p/4K y USB físicos, reproducción prolongada YouTube/Xuper, JBL/CEC y reinicio completo del TV original no repetidos en esta entrega. La recepción física se comprobó desde el PC por LAN; no se presenta como una nueva prueba manual desde el celular. No se declara compatibilidad universal ni con API diferentes de 29/31.
