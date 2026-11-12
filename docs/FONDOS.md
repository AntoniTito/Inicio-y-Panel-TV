# Fondos del inicio

Inicio y Panel TV 0.20.2 conserva los cuatro modos en una fila: Fondo original, Foto fija, Presentación y Paisaje diario. Los fondos aparecen solo en Inicio; el video y el panel superpuesto conservan su comportamiento. Las flechas previsualizan y OK utiliza una elección. Para configurar otro TV, seguir [REPLICAR-ENTORNO.md](REPLICAR-ENTORNO.md); la entrega no incluye las fotos individuales ni el catálogo del original.

## Fotos y presentación

Foto fija muestra seis miniaturas junto a la vista previa: OK usa la foto sin salir. La vista previa se conserva al recorrer controles. Seleccionar permite marcar varias y Eliminar muestra una confirmación con la cantidad; Eliminar todas incluye las fotos personales y los paisajes conservados de Mis fotos.

Presentación admite Todas las fotos o Fotos elegidas, utiliza el orden de la biblioteca y cambia cada 5, 15, 30 o 60 minutos. Usar presentación guarda las marcas; mover el foco o marcar no aplica el cambio. Todas incluye futuras fotos personales y paisajes conservados, pero no paisajes temporales. Si se elimina la última participante, se recupera Fondo original.

Mis fotos conserva la galería ampliada de ocho miniaturas, orden, información y comparación de resoluciones. Las tareas habituales siguen integradas en Foto fija y Presentación.

## Los 12 paisajes de hoy

En Paisaje diario abre Los 12 de hoy. El catálogo tiene hasta doce fotografías de Wikimedia, incluida la identificada como Paisaje de hoy, elegida por el panel dentro de su colección. No equivale a la fotografía oficial del día de Wikimedia.

- Seis miniaturas por página. Las flechas previsualizan; OK descarga el paisaje elegido y lo usa. Una elección ya descargada se utiliza localmente.
- Conservar lo guarda en Mis fotos sin caducidad. Reutiliza el mismo archivo, sin volver a descargarlo ni crear otro original permanente. Si falta, lo prepara antes de confirmar.
- Los estados distinguen Disponible para descargar, Descargado · temporal, Conservado y Eliminada.
- Cambiar cada se ajusta con izquierda/derecha: Desactivado, 1, 2, 3 o 6 horas. Inicialmente está desactivado. Una selección manual reinicia el intervalo.
- La rotación sigue el tiempo real. Al volver de otra aplicación se elige la posición correspondiente, sin descargar los paisajes omitidos. Solo se prepara anticipadamente el siguiente mientras Inicio está visible.

El catálogo y sus miniaturas se conservan durante la fecha local del TV. Al renovarse, la selección avanza por la colección y evita repetir los del día anterior cuando hay suficientes candidatos. Si no hay doce adecuados, se muestra la cantidad disponible. Una renovación fallida conserva el catálogo anterior con su fecha y habilita Reintentar; no se presenta como actualizado. Los intentos automáticos están separados al menos una hora y solo se realizan al volver al panel.

Al comenzar otro día se eliminan los paisajes temporales anteriores, salvo el fondo activo, que sirve de respaldo sin Internet. Este se elimina al disponer de su reemplazo. La limpieza se realiza al volver a Inicio/Fondos o después de reiniciar; no despierta el TV a medianoche. Los conservados y las fotos personales permanecen hasta borrarlos.

Los créditos conservan autor, licencia y origen. Se utiliza la [colección de paisajes destacados](https://commons.wikimedia.org/wiki/Category:Featured_pictures_of_landscapes), con la [API de categorías](https://www.mediawiki.org/wiki/API:Categorymembers) y la [información de imágenes](https://www.mediawiki.org/wiki/API:Imageinfo). No es una integración con Bing. Las fotos personales no se envían a Wikimedia.

## Galería del TV y borrado

Las imágenes optimizadas se publican en MediaStore, en Pictures/Z6, separadas en Mis fotos, Conservadas y Paisajes del dia. Aparecen en las aplicaciones multimedia que consultan ese índice. Las miniaturas del catálogo permanecen privadas y no aparecen como fotos independientes. Un paisaje que todavía solo tiene miniatura no se ha descargado como fondo.

El acceso a la galería nativa se retiró de Fondos en 0.16.1. Mis fotos sigue integrado y las imágenes permanecen disponibles en Multimedia y en los visores del sistema. En el TV original se recupera reversiblemente la galería AOSP; si otro firmware carece de visor, Mis fotos sigue disponible. El instalador conserva por dispositivo el estado original de esa galería para restaurarlo. La apariencia y la papelera de aplicaciones externas dependen de cada visor.

Eliminar desdel panel borra directamente su imagen compartida y su miniatura, sin papelera, verifica la ausencia e informa el tamaño liberado. El mismo archivo deja de estar disponible en Multimedia. Un original que permanezca en el celular, USB u otra ubicación externa no se elimina. No se promete borrado forense del almacenamiento flash.

Una eliminación externa se detecta por eventos y al regresar al panel: se retiran las referencias de foto fija y presentación y no se vuelve a descargar automáticamente ese paisaje durante el catálogo actual. Si la otra app utiliza papelera, su funcionamiento se respeta. Recuperar un APK no devuelve fotos borradas.

## Añadir imágenes y resolución

Desde el celular abre un QR temporal para equipos de la misma red. La recepción dura hasta 15 minutos y se cierra al salir, caducar o perder la red. El TV muestra miniatura, resolución y tamaño. Usar esta foto aplica explícitamente; recibir nunca cambia el fondo por sí solo. El emulador se comprueba desde el PC mediante ADB, sin puente al celular.

Desde TV o USB utiliza el selector nativo o la alternativa con carpetas/volúmenes y selección múltiple. Se solicitan los permisos necesarios al entrar, sin acceso completo al almacenamiento ni root. Solo se copia al panel la versión optimizada. Si un USB no está expuesto por Android, copiar a Imágenes o enviar desde el celular.

Resolución del fondo representa el área efectiva de Inicio y los recursos disponibles: no la capacidad máxima del TV Box ni la resolución nativa del televisor. No se modifica HDMI. Automática usa hasta 1080p, limitada a 720p en equipos con pocos recursos. Las fotos personales pueden usar 4K manualmente con las protecciones existentes: Inicio efectivo 3840×2160, heap de 192 MB, equipo sin poca RAM y margen libre de 96 MB. Una foto pequeña no recupera detalle al elegir más resolución.

Fotos personales: entrada máxima 20 MB; hasta 50 fotos permanentes y 100 MB en total, incluidos paisajes conservados. Una copia JPEG optimizada por imagen, hasta 720p/1 MB, 1080p/2 MB o 4K/4 MB. Verticales y cuadradas completas, horizontales con recorte central. JPG, PNG y WebP estáticos; HEIC/HEIF depende del decodificador.

Paisajes de Internet: hasta 720p/1 MB o 1080p/2 MB, sin originales ni 4K. Se calcula la altura útil antes del recorte; se descartan derivados insuficientes para evitar ampliaciones borrosas. La vista previa se identifica como Miniatura y el fondo completo se prepara al utilizarlo.

## Fluidez, actualización y recuperación

Catálogo persistente de hasta doce; miniaturas en disco hasta 2 MB y caché decodificada hasta 4 MB. Solo una imagen de fondo activa y una vista previa reducida, hasta 960×540. Operaciones pesadas serializadas, solicitudes antiguas descartadas, una descarga simultánea y prioridad para la elección del usuario. Sin animaciones ni procesos de descarga durante otras apps o suspensión.

Se conserva como máximo el lote temporal del día y un fondo anterior de respaldo. Si falta espacio o memoria se detiene la preparación opcional y se conserva el fondo válido. La galería integrada reutiliza vistas y no decodifica originales al mover el foco.

La migración conserva IDs, orden y preferencias: publicación pendiente, verificación por tamaño/hash, registro de URI y retirada posterior del archivo privado. Se reanuda tras interrupción; un fallo conserva la copia original y se informa como publicación pendiente. El instalador usa una espera interna para respaldar los bytes antes de liberar la migración. Nunca copiar esa marca a perfiles de otros equipos.

Actualizar sin desinstalar conserva las fotos. Exportar preferencias no incluye imágenes y sus URI no sirven en otro TV. Fotos, sesiones, índices individuales y respaldos quedan fuera de Git y del portable.

La recuperación vigente es 0.20.1/código 63, con conservación del almacenamiento compartido y formatos de destinos. La receta reproducible es herramientas/recuperacion-0202.ps1; requiere Git, SDK/JDK y firma original, no son necesarios para instalar el APK ya firmado. Recupera la marca anterior Z6 Panel. No usar un APK histórico anterior a MediaStore sobre una biblioteca migrada. Tras instalar c63, una actualización necesita c64 o superior.
