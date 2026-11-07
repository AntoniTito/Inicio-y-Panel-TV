# Verificación de Z6 Panel 0.16.2

Entrega del 6 de noviembre de 2026: **0.16.2/código 42**, un APK para Android 10 y 12. Corrige la desaparición breve del fondo al repetir Home o cambiar de pantalla.

## Causa y cambio

Android entrega nuevos intents HOME mediante una pausa y reanudación breves, incluso con el inicio ya visible. La implementación anterior retiraba y reciclaba el fondo en cada pausa y luego lo decodificaba otra vez. También podía retirar la imagen mientras se preparaba la transición a los ajustes.

Ahora `onPause` detiene observación, rotación y trabajos pendientes, conservando los píxeles que aún pueden verse. `onStop`, apagar la pantalla y destruir el inicio liberan el bitmap. Las respuestas antiguas se descartan y reciclan sin aplicarlas. Se conservan los límites de memoria y la reducción ante presión de memoria. No cambia resolución, HDMI, firmware, archivos de fotos ni preferencias.

Al regresar de una pantalla que ocultó Home se decodifica primero la copia local, antes del mantenimiento o la actualización de Internet. El primer dibujo espera a esa imagen mediante un listener de pre-dibujo, sin bloquear el hilo de interfaz, con salida de seguridad a los 1000 ms. La salida también se libera si no existe una imagen válida o al salir. La política de liberar la imagen oculta se mantiene.

Esta separación corresponde a la [visibilidad del ciclo de vida de Android](https://developer.android.com/guide/components/activities/activity-lifecycle): una actividad pausada puede seguir visible; una detenida está oculta.

## Evidencia

- TV original API 29 y emulador API 31: trece comprobaciones en cada uno. Nuevos intents HOME sin liberar ni volver a decodificar la imagen; Home del control mediante eventos nativos; Panel e inicio; ajustes de Android; retorno con el mismo fondo sin agotar la espera del primer dibujo; liberación al cubrir Home con Fondos; hashes y preferencias conservados. Informes en `pruebas/resultados-transiciones-0.16.2-api29.json` y `api31.json`.
- API 31: cuatro comprobaciones adicionales de retorno con Fondo original, Foto fija, Presentación y foto inexistente. Se importó una imagen sintética en el emulador y se eliminó al terminar, restaurando las preferencias originales. No se alteraron fotos personales. Informe en `pruebas/resultados-primer-cuadro-0.16.2-api31.json`.
- Comparación de captura física: seis intents HOME por ronda, con seis capturas antes y cinco después. Antes aparecía el color base en dos capturas; después, ninguna. Es una muestra de pantalla y no una medición de latencia del control ni de la salida HDMI. Capturas y datos originales permanecen privados.
- Asistente: 53 comprobaciones con ADB simulado, incluidas firma/hash, actualización, selección de recuperación y conservación de respaldos.
- TV original: 20 ciclos de Home, Panel e inicio y ajustes de Android. PSS de 33,789 KiB en el ciclo 10 y 31,004 KiB en el 20: variación de -2,785 KiB, dentro del margen de 8 MiB. No es una prueba prolongada de ausencia de fugas. Informe en `pruebas/resultados-memoria-transiciones-0.16.2-api29.json`.
- Confirmación del auxiliar conectado en ambos equipos después de actualizar. El emulador se detuvo al terminar; el TV quedó en Inicio.
- El usuario confirmó con el control físico que dejaron de parpadear Home y la entrada a ambos ajustes; después identificó el retorno como otro caso y confirmó que también quedó corregido con la carga local previa al primer dibujo.
- La comprobación final contra el respaldo anterior a actualizar verificó las tres imágenes sin cambios y todas las preferencias iguales. Se restauraron orden, visibilidad de aplicaciones y opacidad después de que el instalador reaplicara el perfil.
- No se repiten como evidencia nueva las pruebas extensas de reproducción, Bluetooth, suspensión y reinicio de entregas anteriores.

## Recuperación

Recuperación **0.16.1/código 43**, misma firma original. Su `classes.dex` coincide byte a byte con el APK histórico 0.16.1: `5B79838DE6AC2B0F7663F98A94398BBAF9D11A5A57DC486815A338F75FFCB962`.

Se verificaron construcción, firma y equivalencia del código; no se instaló la recuperación sobre el TV ni se desinstaló el panel. Tras utilizarla, una actualización necesitará código 44 o superior. El manifiesto conserva los APK históricos y selecciona la recuperación común para API 29/31.

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.16.2.apk | `931E3AE962EF68A466F2BEBDCCFF4BB7A9101555DCF7BE0CCF0BACA11E85A20B` |
| Z6-Panel-recuperacion-0.16.1-c43.apk | `02852351F8D2B0480FBA55A71036B3258C63617D946F7FD3D59EA6E5ECE699CC` |

Certificado original SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`. El portable excluye imágenes, claves, preferencias y respaldos personales.
