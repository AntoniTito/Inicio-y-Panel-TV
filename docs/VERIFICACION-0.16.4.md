# Verificación de Z6 Panel 0.16.4

Entrega del 7 de noviembre de 2026: **0.16.4/código 46**. El cambio de interfaz se limita a dos colores del foco de las tarjetas del inicio, incluida Añadir aplicación:

- Relleno seleccionado: de gris opaco `#FF3A3A3A` a oscuro translúcido `#48121A20` (aproximadamente 72 % de transparencia).
- Borde: de `#FFB8B8B8` a `#FFE5F0F3`, conservando los 2 dp y las esquinas existentes.

Geometría, iconos, texto, navegación, panel inferior, ajustes y tratamiento del fondo conservan su código. No se añadieron efectos, animaciones, tareas ni dependencias. La comparación de `app/` contra 0.16.3 solo modifica esos colores y la versión del manifiesto.

Instalado en el TV original API 29 conservando preferencias y hashes de las imágenes, con respaldo previo del APK instalado. Se revisó una captura con Xuper seleccionado: el paisaje se ve dentro de la tarjeta y el borde sigue el foco. El usuario confirmó que el resultado le gusta. Las capturas permanecen privadas.

Compilación y firma original verificadas. Asistente: 55 comprobaciones con ADB simulado. API 31 conserva las pruebas de la base 0.16.3; no se repitieron pruebas de emulador, rendimiento ni reproducción para este ajuste de colores.

Recuperación **0.16.3/código 47**, misma firma. Su `classes.dex` coincide con el APK histórico: `CBE68AA1D9B2980F509C9F7F609454A440C87F137F031CF2FF192BB8D59409FA`. Se verificó su equivalencia, sin instalarla en el TV. Tras utilizarla se requiere código 48 o superior para actualizar.

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.16.4.apk | `204B36608A88E130BF48FFBCA311AB3D8AC626E38E308F9E019BBFACC77A6BD8` |
| Z6-Panel-recuperacion-0.16.3-c47.apk | `97C0DE4E42BD14BEA2CFC2EA6BCB6CDDC0E276F0FC1F831C45BE0D2774C2BCAB` |

Certificado SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`. El portable incluye actualización y recuperación, excluyendo fotos, claves y respaldos personales.
