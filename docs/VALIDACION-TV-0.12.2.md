# Actualización del TV Box original

El 6 de noviembre de 2026 se actualizó el TV Box original, modelo TV GRATIS, Android 10/API 29 y pantalla 1280×720. Pasó de 0.10.4/código 22 a 0.12.1/código 28, y después a 0.12.2/código 30 para aclarar la recepción de fotos. Se usó actualización firmada sin desinstalar ni aplicar el perfil genérico sobre sus preferencias.

## Evidencia disponible

- El APK instalado inicialmente coincidía con la entrega 0.10.4 por SHA-256. Se guardaron APK, preferencias y estado del mismo dispositivo antes de ambas actualizaciones.
- Las preferencias existentes no cambiaron en ninguna actualización. HOME y Accesibilidad siguen usando Z6 Panel; YouTube FOR y Xuper continúan visibles con su orden anterior.
- En el TV físico se abrió Ajustes con sus ocho entradas y se llegó a Fondos. La capa del fondo mide 1280×720, sin los márgenes del contenido.
- El auxiliar volvió a responder PING después de actualizar, sin iniciarlo desde el PC. Su versión sigue siendo 0.11.0; es independiente de la versión del APK.
- El receptor 0.12.1 respondió HTTP 200 por la red local directa, sin reenvío ADB. El usuario confirmó desde su celular que la página abrió y la foto se recibió. Se comprobó después que la copia optimizada estaba guardada y que seguía presente tras actualizar a 0.12.2.
- El usuario señaló que permanecer ante el QR resultaba confuso. 0.12.2 añade confirmación visible, contador, Ver foto recibida y Terminar y abrir biblioteca. Recibir continúa sin aplicar automáticamente un fondo.
- La integración en el emulador API 31 pasó 18 comprobaciones: foco mediante OK nativo, botones dentro de 720p, recepción múltiple, errores, cierre del puerto, caducidad y vista previa. Las pruebas del instalador con ADB simulado pasaron 40 comprobaciones, sin contactar el TV.
- La firma es la original. La recuperación 0.12.1/código 31 tiene el mismo classes.dex que la entrega 0.12.1/código 28, con SHA-256 `9C71D3D580C20952DA31B9A8CB2F375EA1B1AACD34105BC020402B4F1BAA3D82`. Sirve para API 29 y 31; después exige código 32 o superior para actualizar.

## Alcance y pendientes

La confirmación del nuevo flujo 0.12.2 desde el celular y el control físico está pendiente de respuesta del usuario. No se aplicó una foto en su nombre. Tampoco se repitieron en esta sesión la medición de rendimiento, reproducción continua, salida JBL, USB, suspensión, HDMI-CEC ni recuperación tras un reinicio completo. Los resultados históricos de esas funciones siguen separados de esta actualización.

La conexión ADB se perdió una vez después de la primera recepción, aunque el TV seguía respondiendo en la red. El usuario reactivó Depuración USB y se recuperó el acceso. No se atribuye ese incidente a los fondos sin evidencia; no se cambiaron Wi-Fi, Bluetooth ni las opciones de depuración desde el proyecto.

Los respaldos, índices de fotos, capturas y sesiones QR están en privado/actualizaciones, excluidos de Git y del paquete portátil. El exportador de preferencias no incluye los archivos de imagen; el original enviado permanece en el celular.
