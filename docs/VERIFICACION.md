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

Los resultados estructurados finales se encuentran en `pruebas/resultados-consolidacion.json`. La comprobación física de otro firmware y las pruebas de emuladores no se dan por realizadas.
