# Evidencia y límites

## Resultados históricos del equipo original

| Indicador | Antes | Optimizado |
|---|---:|---:|
| Respuesta del panel, p95, 120 muestras por fase | 203,0 ms | 91,8 ms |
| Tiempo gráfico p95, mediana de tres rondas | 48 ms | 32 ms |
| Proporción mediana de cuadros tardíos | 29,29 % | 16,81 % |

La respuesta medida del panel se redujo un 54,8 %. No equivale a acelerar todo Android o Internet en esa proporción. El inicio de Xuper no mostró una mejora equivalente. No hubo crecimiento sostenido de memoria en la prueba de 20 aperturas/cierres.

Se verificaron controles, reproducción sobre las aplicaciones utilizadas, Bluetooth real, temporizador, suspensión y recuperación del auxiliar después de reiniciar en el firmware original. El usuario confirmó mejora de fluidez con el control físico. La prueba de 5 GHz no completó conexión; se conservó la red de 2,4 GHz. No se certificó el apagado del televisor por HDMI-CEC.

El panel inferior pasó de 240 a 201 píxeles en 1280×720, sin cambiar tamaños de iconos y botones. Las evidencias originales completas y capturas quedan en `privado`, fuera de Git.

## Consolidación

Las verificaciones nuevas son locales: copia con hashes, fuentes sin cambios, compilación independiente, certificado, APK y pruebas del asistente con ADB falso. No se reinstala el APK ni se ejecutan pruebas destructivas en el TV original.

Los resultados de esa fase se encuentran en `pruebas/resultados-consolidacion.json`.

## Adaptación Android TV 12

0.11.0/código 24 superó 27 comprobaciones en el AVD Android TV 12/API 31: HOME sobre tarea existente, foco con teclas nativas, ocho entradas y pie visibles a 720p, reloj gris y preferencias, volumen y silencio restaurados, ruta Bluetooth nativa, sustitución/cancelación/vencimiento de temporizador, suspensión explícita, despertar, reinicio real y recuperación autónoma del auxiliar. Evidencia: `pruebas/resultados-api31.json`.

El instalador superó 35 comprobaciones con ADB simulado, incluidos permisos de Android 12, prioridad del launcher del fabricante y recuperación de alarmas y permisos. El informe local está en `.local/verificacion-asistente.json`. También se comprobó restaurar el inicio nativo y volver a instalar en el emulador real.

Una autorización local no persistía por un fallo de escritura del almacén ADB de esta imagen. Se reparó con la API autorizada para retirar únicamente la clave local previamente aprobada y se volvió a autorizar en Android. No se utilizó root ni se retiró la clave del PC. El reinicio posterior pasó sin relanzar el auxiliar desde el PC.

Las aplicaciones externas se instalarán después por decisión del usuario. Su reproducción, Bluetooth físico/JBL, HDMI-CEC y control físico siguen pendientes aquí. Las métricas históricas de fluidez no se volvieron a medir ni se extrapolan a este emulador. El TV Box físico no fue modificado.

La comprobación final adicional de arranque completo y reinicio conservó America/Lima, HOME Z6 y conexión autónoma. Se registró en `pruebas/resultados-emulador-final.json`. El lanzador del AVD incluye zona explícita para evitar que esta imagen vuelva a GMT; consulta EMULADOR-TV12.md.
