# Fondos del inicio

Abre Ajustes → Panel e inicio → Fondos del inicio. Los cuatro modos están **en una sola fila**. Las flechas muestran su sección y su vista previa; la marca **En uso** identifica el fondo aplicado. Mover el foco no aplica ni descarga imágenes.

Los fondos aparecen únicamente en Inicio. El video y el panel superpuesto conservan su comportamiento.

## Foto fija: gestionar sin salir

Al enfocar **Foto fija** aparecen tus fotos junto a la vista previa, seis por página. OK sobre el modo entra en ellas.

- Las flechas previsualizan las miniaturas. Moverse a los controles conserva la vista previa.
- **OK sobre una foto la usa**, sin salir de esta pantalla.
- Al volver a Foto fija se recupera la última vista previa de esta sesión; la foto aplicada se conserva también al reiniciar.
- **Eliminar foto** borra la foto enfocada, tras confirmar.
- **Seleccionar** permite marcar varias; **Eliminar (N)** pide confirmación.
- **Eliminar todas** borra todas las fotos personales de Z6, tras confirmar la cantidad.

El borrado es directo, **sin papelera**: se eliminan archivo y miniatura, se verifica su ausencia y se informa el tamaño liberado. Una miniatura pendiente no puede regenerarse después del borrado. Si falla una eliminación se indica en pantalla. Borrar el fondo activo recupera Fondo original; seleccionar otra miniatura solamente la previsualiza.

Se elimina la copia privada de Z6. Un original en el celular, USB o en otra carpeta del TV no se elimina. No es un servicio de borrado forense. Recuperar una versión del APK no devuelve las fotos que hayas eliminado.

**Mis fotos**, en el pie, conserva la galería ampliada de ocho miniaturas por página y sus opciones de información, orden y comparación de resoluciones. La gestión habitual ya está integrada en Foto fija.

## Presentación

Al enfocar **Presentación** se muestran las mismas fotos, con marcas de inclusión. OK sobre una miniatura incluye o excluye esa foto; las flechas la previsualizan.

- **Todas las fotos** incluye también futuras importaciones.
- Al desmarcar una, se pasa a **Fotos elegidas**: las importaciones posteriores no se añaden automáticamente.
- **Usar presentación (N)** guarda y aplica la selección. Las marcas pendientes no cambian el fondo antes de esa acción.
- Sobre **Cada N min**, izquierda/derecha eligen 5, 15, 30 o 60 minutos.
- **Ordenar** permite mover las participantes conservando las posiciones de las fotos ajenas. OK guarda el movimiento; Atrás lo cancela.

Si eliminas la última participante, se recupera Fondo original. Quitar una marca de la presentación no borra la foto del TV.

## Fondo original y Paisaje diario

**Fondo original** utiliza el color de Z6, sin fotos ni descargas. OK lo aplica.

**Paisaje diario** permite usar el paisaje guardado, actualizar cuando corresponde, consultar los créditos y **Elegir otro paisaje**.

Elegir otro paisaje abre opciones de Wikimedia dentro de la misma pantalla: hasta 30 opciones por consulta y seis miniaturas por página. Las flechas previsualizan una miniatura; **OK descarga, prepara y aplica ese paisaje**. Se descarga la versión útil para el equipo, hasta 1080p; no el archivo original de gran tamaño. La elección se conserva durante el día y el modo diario puede cambiarla al día siguiente cuando vuelvas a Inicio.

Atrás vuelve de los paisajes a los controles del modo. Salir cancela las solicitudes y elimina las miniaturas temporales. Las fotos personales no se envían a Internet. Si falla la descarga, se conserva el fondo anterior. El último paisaje válido permanece sin conexión. Actualizar no vuelve a descargar el paisaje de hoy si tiene resolución suficiente. Para archivos antiguos insuficientes aparece **Mejorar nitidez**, que recupera la misma fotografía con un encuadre útil.

La vista previa se identifica como **Miniatura**: la imagen del inicio se prepara al pulsar OK. El catálogo descarta archivos que no pueden llenar el encuadre con suficientes píxeles a partir de las versiones reducidas disponibles. En panorámicas se calcula también la altura necesaria antes de recortar, para evitar ampliar una franja pequeña y verla borrosa. La descarga se hace una vez al elegir; el fondo guardado se utiliza localmente.

Los créditos conservan autor, licencia y origen. Se utiliza la [colección de paisajes destacados](https://commons.wikimedia.org/wiki/Category:Featured_pictures_of_landscapes), con la [API de categorías](https://www.mediawiki.org/wiki/API:Categorymembers) e [información y miniaturas de imágenes](https://www.mediawiki.org/wiki/API:Imageinfo). No es una integración con Bing.

## Añadir imágenes

El pie mantiene **Desde el celular**, **Desde TV o USB**, **Mis fotos**, **Ver inicio** y **Volver**.

Desde el celular abre un QR temporal. Ambos equipos deben estar en la misma red local. La página permite enviar varias fotos; el TV muestra miniatura, resolución, tamaño y contador. **Usar esta foto** la aplica explícitamente; **Ver recibidas** abre la galería; **Seguir recibiendo** conserva el QR. Recibir nunca aplica automáticamente un fondo.

La recepción dura hasta 15 minutos y se cierra al salir, caducar o perder la red. Se usa HTTP local con una sesión aleatoria; no publiques el enlace ni abras puertos del router. El emulador se prueba desde el navegador del PC con reenvío ADB temporal; no incluye puente al celular.

Desde TV o USB utiliza el selector nativo cuando funciona, o una alternativa con carpetas, volúmenes, miniaturas y selección múltiple. El permiso se solicita al entrar. Si Android no expone un USB, copia las fotos a Imágenes o envíalas desde el celular. No se solicita acceso completo al almacenamiento ni root. La importación conserva los archivos válidos y explica los errores por archivo; no aplica fondos por sí sola.

## Resolución del fondo y almacenamiento

Resolución del fondo indica los píxeles que Z6 utiliza según el **área efectiva del inicio y los recursos disponibles**. No indica la resolución máxima del TV Box ni la resolución nativa del televisor. En el equipo original el inicio es 1280×720. No se cambia HDMI. [Referencia de Android](https://developer.android.com/reference/android/view/Display).

Automática usa hasta 1080p y limita a 720p en equipos con pocos recursos. 4K es manual: requiere un inicio de al menos 3840×2160, equipo sin poca RAM, heap de al menos 192 MB y 96 MB de margen disponible. Información separa inicio, límite utilizado y archivo guardado. Solo se ofrecen elecciones útiles. Elegir más resolución no recupera detalle perdido: el aviso **Puede verse borrosa** permite continuar o reenviar el original.

Las fotos personales se guardan en el almacenamiento privado del TV Box, en una sola copia JPEG optimizada. Verticales y cuadradas se muestran completas; horizontales se encuadran al centro. Orientación corregida antes de reducir, sin agrandar fotos pequeñas. Límites: entrada de 20 MB, 50 fotos y 100 MB de biblioteca. Copias de hasta 720p/1 MB, 1080p/2 MB o 4K/4 MB, JPEG de 85 a 75. JPG, PNG y WebP estáticos; HEIC/HEIF depende del decodificador del equipo.

Las fotos existentes conservan sus archivos. Para recuperar detalle perdido hace falta reenviar el original.

## Fluidez y recuperación

Foto fija y Presentación reutilizan la misma galería y sus vistas. Se mantiene una vista previa reducida, hasta 960×540, y una caché de miniaturas de 4 MB. Solo se cargan las miniaturas de la página visible. Las operaciones pesadas se serializan fuera del hilo de interfaz; las respuestas antiguas se descartan. La navegación entre modos reutiliza recursos mientras esta pantalla está abierta; al salir se liberan sus imágenes. Sin animaciones ni nuevos procesos periódicos.

Actualizar sin desinstalar conserva fotos y preferencias. El exportador de preferencias no incluye archivos de imagen: otro TV debe importar sus propias fotos. Fotos, sesiones y respaldos quedan fuera de Git y del paquete portable.

La recuperación de 0.15.0 utiliza el comportamiento 0.14.0 con código 37 y la firma original. Entiende las presentaciones con fotos elegidas. Después necesitarás código 38 o superior para actualizar. No desinstales para bajar una versión.

## Aplicaciones sin iconos visibles

Si una fila está vacía, **＋ Añadir aplicación** permite incorporar una app instalada u oculta con OK. Se conserva la edición con OK sostenido y Ajustes → Aplicaciones. Añadir un icono no instala aplicaciones; Play o Downloader se abren únicamente si están disponibles. No se incluyen APK ni cuentas de terceros.
