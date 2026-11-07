# Verificación de Z6 Panel 0.16.3

Entrega del 7 de noviembre de 2026: **0.16.3/código 44**, mismo APK para Android 10/API 29 y Android 12/API 31. Reduce el trabajo del fondo al recibir nuevos intents Home. Conserva el primer dibujo con copia local y la liberación de imágenes cuando el inicio queda oculto.

## Cambios

- Reutiliza un fondo visible ya validado durante un máximo de 60 segundos, comprobado al entrar. Invalida por preferencias, eventos de MediaStore, cambio de día/zona, dimensiones o reducción de memoria. Al volver de una pantalla que ocultó Home se hace la validación completa.
- El observador permanece durante las pausas breves: mientras Home está pausado solo invalida, sin consultas ni descargas. Se retira al detener Home. No añade consultas ni tareas periódicas.
- Conserva el vencimiento pendiente de rotación y reprograma la preparación opcional del siguiente paisaje si una pausa breve la canceló.
- Evita consultar la memoria dinámica cuando Android ya clasifica el equipo como de poca RAM o su heap es inferior a 128 MB. Los umbrales y la calidad elegida se mantienen.

## Medición física

Tres rondas equivalentes, cada una con 12 intents HOME separados por 200 ms, en el TV original API 29. CPU acumulada del proceso de Z6 mediante `/proc/PID/stat` y `CLK_TCK`; incluye interfaz, Binder y trabajadores de la app. Se conservó el mismo fondo y no hubo descargas durante las rondas.

| Medida | 0.16.2 | 0.16.3 |
|---|---:|---:|
| CPU por ronda, ms | 1300 / 1410 / 1240 | 180 / 90 / 80 |
| Mediana de CPU, ms | 1300 | 90 |
| Reducción de la mediana | — | 93,08 % |

En la versión optimizada hubo 36 reanudaciones rápidas y el contador de mantenimiento quedó en uno durante las tres rondas. Estas cifras miden CPU del proceso durante esa secuencia, no latencia física del control ni rendimiento de todo Android. `gfxinfo` produjo cero cuadros en la mayoría de rondas, por lo que no hay un p95 gráfico comparable; sus valores sin cuadros se descartan. La duración de la secuencia incluye pausas deliberadas y transporte ADB.

Informe: `pruebas/resultados-rendimiento-home-0.16.3-api29.json`.

## Comprobaciones

- API 29 físico y API 31 emulado: trece comprobaciones de Home, Panel e inicio, ajustes de Android, retorno, liberación del bitmap oculto y conservación de fotos y preferencias, reutilizando la suite de transiciones con versión explícita.
- API 31: siete comprobaciones de reutilización, invalidación por preferencias, notificaciones de MediaStore durante una pausa, retorno y caducidad real de 60 segundos. La entrada vacía de prueba de MediaStore se eliminó. Cuatro comprobaciones adicionales de Fondo original, foto, presentación y foto inexistente; se eliminaron las imágenes sintéticas y se restauraron las preferencias.
- TV original: 20 ciclos de Home, Panel e inicio y ajustes de Android. PSS de 31768 KiB en el ciclo 10 y 33040 KiB en el 20; variación de 1272 KiB, dentro del margen de 8192 KiB. Es una prueba acotada, no de uso prolongado.
- Asistente: 54 comprobaciones con ADB simulado, incluida la recuperación nueva. Se preservaron las etiquetas anteriores.
- El arranque inmediatamente posterior a instalar agotó una vez la protección previa de primer dibujo de 1000 ms. Las secuencias y regresos comprobados no incrementaron ese contador. No se realizó una comparación controlada de arranque en frío.
- El usuario confirmó con el control físico que Home y la entrada y retorno de Panel e inicio y Ajustes de Android siguen fluidos y sin parpadeos.
- Un reinicio transitorio del servidor ADB del PC interrumpió una ejecución del emulador; se repitió esa suite con la conexión restablecida. No se alteró Depuración USB.
- Las pruebas extensas de reproducción, Bluetooth, suspensión y reinicio conservan su evidencia anterior; no se presentan como repetidas aquí.

## Entrega y recuperación

Se respaldaron APK instalado, preferencias y los bytes de las tres imágenes del TV antes de actualizar. Se instaló el APK firmado sin reaplicar el perfil de personalización. La comprobación final contra ese respaldo confirmó hashes de las tres imágenes y todas las preferencias iguales. El estado final y la memoria se registran en los informes de esta entrega.

Recuperación **0.16.2/código 45**, firma original, común a API 29/31. Su `classes.dex` coincide con 0.16.2: `BF613F37414D5D81D88A508515BE32C9526C678FE3AE92599277E26BD34D1665`. Se verificó su equivalencia; no se instaló la recuperación en el TV. Tras utilizarla, una actualización necesita código 46 o superior.

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.16.3.apk | `AF357D1A1A605680B8647346C4CCD10FA4F1911E9CE421D460B9FCB4331190A6` |
| Z6-Panel-recuperacion-0.16.2-c45.apk | `A44E4ECF2382D9684FCDA4F53C25E18D786CCAF2527F01A05798520634F0556E` |

Certificado SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`. El portable y Git excluyen fotos, claves, preferencias y respaldos personales.
