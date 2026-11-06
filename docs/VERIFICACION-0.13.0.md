# Verificación de Z6 Panel 0.13.0

Entrega del 6 de noviembre de 2026: APK 0.13.0/código 32 para API 29 y 31, firmado con la clave original. No se modificaron las etiquetas anteriores ni se publicó el repositorio.

## Resultados

| Entorno | Evidencia |
|---|---|
| Emulador Android TV 12/API 31 | 36 comprobaciones de biblioteca, optimización, orientación EXIF, límites por foto, límite de 50 imágenes, borrado, comparación de calidad y reducción por memoria. Resoluciones efectivas 720p, 1080p y 4K. |
| Emulador API 31, revisión visual | 4 comprobaciones de envío múltiple desde navegador, almacenamiento y píxeles de los fondos vertical/horizontal. Capturas con imágenes sintéticas revisadas en .local. |
| Emulador API 31, controles | 27 comprobaciones de Home, foco, reloj, volumen, silencio, destino Bluetooth, temporizador, suspensión y reinicio completo. El auxiliar se recuperó sin iniciarlo desde el PC, dentro de 180 segundos. |
| Instalador con ADB simulado | 40 comprobaciones de instalación, repetición, integridad, identidad, recuperación y conservación de configuraciones. No contacta el TV. |
| TV original Android 10/API 29, 1280×720 | 17 comprobaciones automáticas de preferencias, tres fotos existentes, guardado adaptado, receptor por LAN real, miniaturas, comparación y borrado de dos imágenes temporales. El usuario confirmó además miniaturas reconocibles y navegación fluida con el control físico. |

Los informes están en pruebas/resultados-*-0.13.0*.json. Las fotos, sesiones, capturas del TV y respaldos permanecen fuera de Git, en privado/actualizaciones. Las imágenes temporales se eliminaron y se restauraron la selección, calidad y preferencias del TV. Las tres fotos anteriores conservan identificadores, dimensiones y tamaños; no se reconvirtieron.

## Rendimiento

Se comparó 0.12.2 con 0.13.0 en el mismo emulador API 31 y la misma secuencia: tres rondas de 30 acciones por modo. La medición incluye el transporte ADB; no representa la latencia aislada entre pulsación y cuadro mostrado.

| Modo | Mediana del p95 antes | Después | Límite aceptado |
|---|---:|---:|---:|
| Actual | 969,6 ms | 971,9 ms | 1066,6 ms |
| Una foto | 975,9 ms | 967,8 ms | 1073,5 ms |
| Presentación | 973,9 ms | 972,8 ms | 1071,3 ms |

Los tres modos cumplieron el mayor margen entre 10 % y 8 ms. Después del calentamiento, el PSS cambió −364 KB entre los ciclos 10 y 20, dentro del límite de crecimiento de 8 MB. La caché de miniaturas permanece limitada a 4 MB. La presión de memoria redujo la imagen mostrada a 720p conservando el archivo 4K.

## Conservación y recuperación

- APK vigente SHA-256: `8B983DDEE56CB6C112353C42F5F389AE278227CB7387F93A0C0819038966D05D`.
- Recuperación 0.12.2/código 33 SHA-256: `570B7718818B4B9A92DE328FEA96A62031AAA07E708BD1145FC941934B1249CB`.
- Su classes.dex coincide con 0.12.2/código 30: `D2C02FAA74DA241234735C2A4F60F869EA10237BD2356E7421605BCB514002E4`.
- Certificado original SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`.

La recuperación funciona para ambas API admitidas, sin desinstalar. Después de instalar código 33 se necesitará código 34 o superior para actualizar. El respaldo previo del TV incluye APK, preferencias, identidad y estado de configuración; el exportador de preferencias no incluye los archivos de imagen. Una recuperación no recupera fotos que el usuario haya eliminado expresamente.

## Límites de la evidencia

1080p y 4K se probaron en el emulador; falta validarlos en hardware físico de esas resoluciones. El TV original solo ofrece un inicio efectivo 1280×720. Reproducir video 4K no garantiza una interfaz de inicio 4K, y Z6 no modifica la salida HDMI.

No se repitieron en el TV físico el reinicio completo, la reproducción continua de YouTube/Xuper, el audio JBL, USB ni HDMI-CEC durante esta entrega. Sus resultados anteriores siguen siendo históricos. Los formatos y permisos USB/HEIC dependen del firmware. No se declara compatibilidad universal.

Las pruebas de integración requieren Node y Playwright/Chromium preparados localmente; las imágenes sintéticas se generan en .local y no necesitan fotos del usuario. El paquete de instalación no necesita Node ni Java: utiliza los APK firmados.
