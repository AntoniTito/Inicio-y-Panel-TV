# Verificación de Z6 Panel 0.16.1

Entrega del 6 de noviembre de 2026: **0.16.1/código 40**, con el mismo APK para Android 10 y 12. Retira únicamente el botón Abrir galería del TV de Fondos del inicio. Conserva Mis fotos, recepción, MediaStore, paisajes, rotación y procesamiento de imágenes.

Instalado en el TV original API 29 y en el emulador API 31. Se respaldaron el APK anterior, las preferencias y los hashes de las imágenes existentes antes de actualizar el TV. Sus dos fotos permanentes y el paisaje activo conservaron sus hashes; las preferencias de fondos quedaron iguales. La galería nativa sigue habilitada y las imágenes permanecen visibles en Multimedia.

## Comprobaciones

- TV API 29: seis comprobaciones de ausencia del acceso, cinco controles diarios visibles, límites de pantalla a 720p, hashes, preferencias y recuperación del auxiliar.
- Emulador API 31: dos comprobaciones de retirada del acceso y controles completos. El AVD se detuvo después de verificarlo.
- Asistente: 52 comprobaciones con ADB simulado, incluidas integridad, códigos, selección de recuperación, respaldo de medios y reintento seguro.
- Compilación y firma original verificadas. La recuperación se construyó desde v0.16.0 cambiando solo el código a 41; su classes.dex coincide con la entrega histórica: `07C09D939268AB92A81D4BBBC8819E56B19AF43F0F0894563D4AB5CA9EEF7E99`.

Informes: [TV original](../pruebas/resultados-tv-original-0.16.1-api29.json), [interfaz API 31](../pruebas/resultados-interfaz-0.16.1-api31.json) y [asistente](../pruebas/resultados-asistente-0.16.1.json). La evidencia funcional y de rendimiento anterior sigue en [0.16.0](VERIFICACION-0.16.0.md); no se repitieron sus pruebas extensas para esta retirada de un botón.

## Resolución del fondo

El inicio efectivo del TV original sigue siendo **1280×720** y su paisaje guardado también es 1280×720. No se modificó HDMI ni se forzó otra resolución.

Conectarlo a una pantalla de 70 pulgadas o 4K no garantiza que Android cambie la resolución del inicio. Si mantiene 720p, el fondo se ampliará y puede verse menos definido, especialmente de cerca. El resultado depende también de la fotografía, la distancia y el reescalado del televisor. No se probó físicamente una pantalla de 70 pulgadas ni 4K.

Los paisajes de Internet siguen limitados a 1080p cuando el inicio y los recursos lo permiten; en este equipo están a 720p. Las fotos personales solo ofrecen 4K manual cuando el inicio es realmente 3840×2160 y se cumplen los límites de memoria existentes. Aumentar la pantalla o la selección de calidad no recupera detalle perdido en una copia pequeña.

## Entrega y recuperación

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.16.1.apk | `73FC2823565D1BD5996577A0B8A5B96F8EE356D0F63F7BFCFCE8F323756CE0D5` |
| Z6-Panel-recuperacion-0.16.0-c41.apk | `DF578BDCFC87BD76EC533566D78D11865AA04FE04C84C2D7F6470100F405C0CC` |

Certificado original SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`.

La recuperación conserva el comportamiento anterior con su botón de galería. No se instaló código 41 para esta comprobación; se verificaron firma, hash y equivalencia del código con v0.16.0. Se reconstruye con herramientas/recuperacion-0161.ps1. Después de usarla hará falta código 42 o superior para actualizar. Fotos y preferencias permanecen al actualizar sin desinstalar.

El portable incluye los dos APK y excluye claves, imágenes personales y respaldos. Las etiquetas anteriores se mantienen intactas; no hay publicación remota automática.
