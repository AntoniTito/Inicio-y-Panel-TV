# Verificación 0.18.1 — recuperación del control

11 de noviembre de 2026. Versión 0.18.1/código 54, misma firma y APK común a API 29/31. Recuperación 0.18.0/código 55; después de instalarla se necesita código 56 o superior.

## Fallo y corrección

En el TV original, Netflix estaba asignado pero el auxiliar había terminado. La captura conservaba el estado «OK» aunque Binder estaba desconectado. El usuario había reactivado Depuración USB; los intentos anteriores podían haberse agotado y no existía un observador de ese cambio.

- Se invalida la disponibilidad al morir Binder y se descartan respuestas antiguas.
- La recuperación responde a cambios de depuración, red, pantalla y retorno a Inicio. Los intentos son limitados; no hay consultas periódicas en reposo.
- Se retiró el apagado/encendido de ADB de la recuperación. Reiniciar adbd puede matar los procesos que se pretende recuperar. Solo se conserva la finalización de una restauración pendiente de versiones antiguas.
- Una respuesta BUSY permite hasta tres reintentos espaciados de la asignación; una respuesta fallida no se conserva como configuración aplicada.

La asignación, el recorrido, las preferencias y las fotos no requieren migración.

## TV físico API 29

Se respaldaron privadamente preferencias, configuración y hashes de las dos fotos.

1. El APK inició el auxiliar autorizado sin una orden de arranque desde el PC.
2. Se terminó únicamente el proceso auxiliar, comprobando antes su nombre y UID shell. El estado dejó de anunciar «OK».
3. Se recuperó con un PID nuevo en **8429 ms**, sin arrancarlo desde el PC ni cambiar el valor de Depuración USB. La escucha del control volvió a estar activa.
4. Un primer intento del usuario mostró YouTube oscuro con audio y no dejó una invocación del botón en Z6. Se capturó después únicamente la tecla de función: el botón Netflix entregó F12/código 88. La causa específica de aquella pantalla de YouTube no quedó determinada.
5. La repetición abrió Xuper y completó el recorrido. El usuario confirmó imagen en movimiento y sonido.
6. Se abrió Inicio de Z6 y se repitió el caso original: el usuario confirmó que Netflix también funciona desde allí. Se comprobó proceso nuevo de Xuper y America TV Peru HD como destino.
7. Los identificadores, tamaños y SHA-256 de las imágenes permanecieron iguales.

Evidencia: `resultados-recuperacion-auxiliar-0.18.1-api29.json` y `resultados-tecla-netflix-0.18.1-api29.json`, en `pruebas/`.

## Reposo y alcance

Con el lector activo sobre un dispositivo y el canal reproduciéndose, la muestra de 12 segundos registró **0 ms de CPU** en el auxiliar y Z6, con la resolución de los contadores de `/proc`. No hubo nuevas consultas de navegación ni reenlaces. No equivale a consumo cero permanente ni mide la CPU de Xuper o del sistema completo. Ver `resultados-reposo-control-0.18.1-api29.json`.

El asistente superó **69 comprobaciones simuladas**, incluidas instalación, recuperación, conservación de datos y distinción entre versiones funcionales y códigos de APK. La compilación y firma se comprobaron. La recuperación conserva el DEX de la etiqueta v0.18.0.

No se repitieron en esta corrección las pruebas completas de arranque, navegación ni el ensayo de recuperación en hardware API 31. La evidencia anterior se conserva en VERIFICACION-0.18.0.md; no se presenta como una prueba nueva del APK 0.18.1. La autorización local y disponibilidad de ADB siguen dependiendo del firmware. Apagar únicamente el televisor no garantiza un evento de despertar del TV Box.

Los scripts `recuperacion-auxiliar-0181.cjs` y `reposo-control-0181.cjs` requieren un serial explícito. El primero termina realmente el auxiliar para comprobar su recuperación; no reinicia adbd ni cambia Depuración USB. Los respaldos y la captura de la tecla permanecen fuera de Git.
