# Verificación de Z6 Panel 0.14.0

Entrega del 6 de noviembre de 2026: un APK **0.14.0/código 34** para Android 10/API 29 y Android TV 12/API 31, firmado con la clave original. Instalado en el TV original y en el emulador. Fotos y preferencias conservadas; no se publicaron archivos ni se movieron etiquetas anteriores.

## Qué cambió

Fondos del inicio usa vista previa y cuatro modos en una cuadrícula, con pie fijo a 720p. Foco y modo aplicado son independientes: las flechas no aplican ni descargan. OK utiliza una elección válida; una foto desde la galería se usa con OK y vuelve a Fondos. Presentación admite Todas/Elegidas y orden reversible que respeta posiciones de las fotos no participantes. Recepción muestra miniatura y aplicación explícita. La alternativa TV/USB permite miniaturas y selección múltiple con errores por archivo. Resolución del fondo distingue el inicio efectivo de las dimensiones guardadas y de las capacidades del televisor.

No se cambió la salida HDMI, el panel inferior, el reloj, sus iconos ni el comportamiento de aplicaciones externas. Las imágenes anteriores no se reconvirtieron.

## Evidencia

| Entorno | Comprobaciones |
|---|---|
| Emulador API 31 | 26 verificaciones de foco, aplicación, presentación parcial, orden, recepción, permisos denegados, importación parcial y recreación a 1080p/4K. |
| Emulador API 31, controles | 27 verificaciones de Home, foco sin reconstrucción, reloj, volumen, silencio, ajustes nativos Bluetooth, temporizador, suspensión y reinicio completo. El auxiliar se recuperó sin iniciarlo desde el PC, dentro de 180 segundos. |
| TV original API 29, inicio 1280×720 | 20 verificaciones: preferencias intactas, tres fotos con IDs/dimensiones/tamaños/orden preservados, controles completos a 720p, foco sin cambios, resolución explicada, recepción por LAN directa, miniatura, aplicación con OK, presentación seleccionada, cancelación de movimiento y borrado. |
| Emulador API 31, galería | 10 verificaciones adicionales: nueve fotos, páginas de ocho, Organizar, movimiento entre páginas, cancelación, OK sostenido real, vista completa y borrado total con confirmación. |
| Emulador API 31, Internet | 5 verificaciones: red realmente desconectada, fallo sin cambiar fondo ni caché, reintento disponible, uso del paisaje guardado sin otra descarga e inicio sin Internet. Wi-Fi restaurado al terminar. |
| Instalador con ADB simulado | 42 verificaciones de instalación, identidad, hashes, permisos, HOME, restauración y recuperación segura de selecciones parciales en API 29 y 31. No contacta el TV. |

El usuario confirmó con su control físico que la pantalla se ve completa, el foco es claro y responde con fluidez.

Informes: [Fondos API 31](../pruebas/resultados-fondos-0.14.0-api31.json), [regresión API 31](../pruebas/resultados-regresion-0.14.0-api31.json), [TV API 29](../pruebas/resultados-tv-original-0.14.0-api29.json) y [asistente](../pruebas/resultados-asistente-0.14.0.json), [galería](../pruebas/resultados-galeria-0.14.0-api31.json) y [paisaje sin conexión](../pruebas/resultados-paisaje-0.14.0-api31.json). Los respaldos, fotos y capturas del TV permanecen en privado/actualizaciones; las capturas sintéticas del emulador, en .local.

Se restauró la elección Paisaje diario y todas las preferencias registradas justo antes de la instalación. Las dos imágenes temporales de la prueba física se eliminaron. Cambios posteriores realizados por el usuario en la pantalla son elecciones nuevas y no se restauran automáticamente.

## Rendimiento

Tres rondas de 30 pulsaciones nativas por modo, mismo emulador a 1280×720, misma secuencia y PC sin el juego que interfería en la primera comparación. Los tiempos incluyen creación del comando y transporte ADB; **no son latencia aislada entre control y cuadro mostrado**.

| Modo | Mediana del p95 en 0.13.0 | En 0.14.0 | Límite, mayor margen entre 10 % y 8 ms |
|---|---:|---:|---:|
| Fondo original | 110,6 ms | 111,4 ms | 121,7 ms |
| Foto fija | 108,0 ms | 90,7 ms | 118,9 ms |
| Presentación | 116,2 ms | 88,0 ms | 127,8 ms |

Los tres cumplieron. La [referencia](../pruebas/referencia-fondos-0.13.0-para-0.14.0-api31.json) y [comparación sin carga](../pruebas/resultados-rendimiento-0.14.0-sin-carga.json) contienen las muestras. Para reinstalar la referencia en el emulador sin borrar datos se reconstruyó 0.13.0 cambiando únicamente el código del manifiesto a 34: su classes.dex es idéntico al APK original, SHA-256 `02363D774E678E64E05AA6A51DE9DBADA7BC499370158AC1F7F2AB4BC4DEC1AD`.

La [prueba de 20 ciclos](../pruebas/resultados-rendimiento-0.14.0.json) registró PSS de 52 660 KB en el ciclo 10, 52 767 en el 15 y 53 504 en el 20: crecimiento de **844 KB**, inferior a 8 MB tras calentamiento. Sus tiempos se tomaron con otra carga del PC y se conservan como evidencia secundaria; la tabla usa la repetición sin el juego.

La vista previa se decodifica al tamaño visible, como máximo 960×540, y se reutiliza al cambiar entre modos. Las solicitudes se agrupan durante navegación rápida. Mover el foco no lee archivos de créditos ni reconstruye la pantalla. Las cachés siguen limitadas a 4 MB; no se añaden servicios ni comprobaciones periódicas.

## Integridad y recuperación

- APK 0.14.0/código 34: `7C982CC6AE41538E68DF4480473BEDE594E65EB912BF216E97D7546D2A35E23C`.
- Recuperación 0.13.0/código 35: `D1E8A652BE6E2F75517D716FA1F062362F3E2F42B2D4AB82AA8865B6C707AF20`.
- Certificado SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`.

La recuperación usa el código archivado de v0.13.0 más una protección: una presentación parcial vuelve a Fondo original conservando los IDs para una actualización futura. El restaurador aplica la misma protección antes de recuperar preferencias antiguas. No se reinstaló la recuperación en el TV del usuario para esta entrega; se verificaron compilación/firma, procedencia y las dos rutas del restaurador con ADB simulado. No se declara identidad byte a byte con 0.13.0 porque la protección cambia el código. La recompilación final de 0.14.0 produjo exactamente el mismo SHA-256 que el APK instalado. Después de instalar código 35 se necesitará código 36 o superior. No desinstalar: se perderían los datos privados.

## Límites

La interfaz se recreó en el emulador a 1080p y 4K. El TV original solo permite validar físicamente un inicio 720p; otros modelos, 1080p/4K físicos, USB real y decodificadores HEIC requieren sus propias pruebas. No se deduce la resolución nativa del televisor del área de Android.

En esta entrega la recepción del hardware se verificó desde el PC por LAN, sin puente ni reenvío ADB. El escaneo desde un celular tiene evidencia histórica de 0.12.2; no se presenta como una prueba nueva de ese dispositivo. No se repitieron reproducción continua YouTube/Xuper, salida JBL, HDMI-CEC, reinicio físico ni todas las diferencias de firmware. El reinicio, suspensión y recuperación autónoma indicados arriba corresponden al emulador API 31.
