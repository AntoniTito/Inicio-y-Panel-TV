# Fondos del inicio

Abre **Ajustes → Panel e inicio → Fondos del inicio**, o el acceso de Personalización. Los fondos solo se muestran en Inicio; el panel superpuesto y el video conservan su comportamiento.

## Elegir sin pasos innecesarios

La vista previa está a la izquierda y los cuatro modos a la derecha. Las flechas cambian la vista previa; **OK aplica** una elección válida. La marca **En uso** identifica lo que está guardado, independientemente del foco. Aplicar mantiene abierta la configuración. **Ver inicio** permite comprobarla y **Volver** sale.

| Modo | Qué hace OK |
|---|---|
| Fondo original | Aplica el color de Z6, sin imágenes ni descargas. |
| Foto fija | Usa la última foto elegida. Si falta, abre Mis fotos. |
| Presentación | Activa las fotos participantes; si no hay, abre su selección. |
| Paisaje diario | Utiliza la última imagen disponible de Wikimedia o descarga y aplica la primera. Un fallo conserva el fondo anterior. |

Al enfocar Presentación aparecen **Fotos: Todas/Elegidas**, el intervalo y **Ordenar las fotos participantes**. Sobre el intervalo, izquierda/derecha eligen 5, 15, 30 o 60 minutos. **Todas** incluye importaciones futuras; **Elegir fotos** conserva solo las marcadas. Se usa el orden de la biblioteca. Al ordenar participantes, las fotos ajenas a la presentación mantienen sus posiciones. Si eliminas la última participante, se recupera Fondo original.

Al enfocar Paisaje diario aparecen su estado, actualizar/reintentar cuando corresponde y los créditos. No se vuelve a descargar un paisaje ya actualizado hoy. El último paisaje válido permanece sin Internet. Mover el foco no hace descargas.

## Mis fotos

El pie mantiene visibles **Desde el celular**, **Desde TV o USB** y **Mis fotos (N)**. La galería muestra ocho miniaturas por página, con nombre, dimensiones y tamaño.

- **OK** usa la foto y vuelve a Fondos del inicio.
- **Opciones de la foto** o OK sostenido abre la vista previa, pantalla completa, información y eliminación individual.
- **Organizar** reúne orden, selección para borrar y Eliminar todas. Se pide confirmación antes de eliminar; se borran únicamente las copias de Z6.
- Para ordenar, elige una foto, pulsa OK para moverla, usa flechas y confirma con OK. Atrás cancela el movimiento pendiente.

Eliminar el fondo activo recupera Fondo original. Los archivos e identificadores existentes se conservan sin reconvertirlos durante la actualización.

## Resolución del fondo

Es la cantidad de píxeles que Z6 utiliza para mostrar el fondo según el área efectiva del inicio y los recursos disponibles. **No indica la resolución máxima del TV Box ni la resolución nativa del televisor.** Android distingue el área disponible para la app de otros datos de la pantalla: [Display](https://developer.android.com/reference/android/view/Display).

En el TV original se muestra **Automática · hasta 720p para este inicio**, porque su inicio efectivo es 1280 × 720. Z6 no modifica HDMI ni deduce las capacidades del televisor. Información separa inicio de Android, límite utilizado por el fondo y dimensiones del archivo guardado.

Automática usa hasta 1080p; en equipos con poca RAM, hasta 720p. 4K es manual y requiere inicio efectivo de al menos 3840 × 2160, equipo sin clasificación de poca RAM, heap de al menos 192 MB y 96 MB de margen al cargar. Solo se ofrecen alternativas útiles. Con una sola resolución aparece información y **Ver a pantalla completa**, sin un selector redundante. Con alternativas, **Comparar resoluciones** usa izquierda/derecha; OK aplica y Atrás cancela conservando la elección anterior.

**Puede verse borrosa** indica que faltan píxeles para el encuadre. Más resolución no recupera detalle perdido: hay que reenviar el original si una copia anterior ya se redujo.

## Desde el celular

1. Selecciona **Desde el celular** y conecta ambos equipos a la misma red local.
2. Escanea el QR o escribe la dirección mostrada. En el navegador selecciona y envía fotos.
3. El TV muestra miniatura, confirmación, resolución, tamaño y cantidad recibida.
4. **Usar esta foto** aplica explícitamente la última y cierra la recepción. **Ver recibidas** abre la galería enfocando la nueva foto. **Seguir recibiendo** mantiene el mismo QR.

Recibir nunca cambia el fondo automáticamente. El puerto solo existe con la recepción abierta; salir, perder la red o caducar a los 15 minutos lo cierra. No se requieren aplicaciones adicionales. El TV puede usar Ethernet si pertenece a la misma LAN. Redes de invitados o aislamiento entre equipos pueden impedirlo.

Se usa HTTP local con una sesión aleatoria; no publiques el enlace ni abras puertos del router. En el emulador no se incluye un puente al celular: las pruebas usan el navegador del PC con reenvío ADB temporal. El QR directo se comprueba aparte en hardware real.

## Desde TV o USB

Se usa el selector de documentos cuando es funcional. La alternativa de Z6 muestra carpetas/volúmenes de MediaStore y páginas de ocho miniaturas con selección múltiple. Solicita permiso únicamente al entrar; consulta hasta 500 imágenes por volumen y permite importar hasta 50. Si Android no expone un USB, copia las fotos a Imágenes o envíalas desde el celular. No solicita acceso completo al almacenamiento ni root.

La importación no aplica un fondo. Al terminar muestra cuántas fotos se guardaron y los errores por archivo; las nuevas fotos quedan enfocadas. Una recreación de pantalla conserva el trabajo en curso. Los permisos temporales del selector se liberan al terminar; no se retiran permisos que ya existían.

## Guardado y fluidez

Las fotos se guardan **en el almacenamiento privado de Z6 en el TV Box**, en una sola copia JPEG optimizada. No se conserva el original pesado ni se envían fotos personales a Internet. Puedes retirar el USB después de importar.

Límites: entrada de 20 MB, 50 fotos y 100 MB de biblioteca optimizada. JPG, PNG y WebP estáticos; HEIC/HEIF depende del decodificador del firmware. Orientación corregida antes de reducir; verticales y cuadradas completas con espacio oscuro, horizontales con recorte central. No se agrandan fotos pequeñas. Límites por copia: 720p/1 MB, 1080p/2 MB o 4K/4 MB, JPEG de 85 a 75.

Una sola vista previa hasta 960 × 540; miniaturas de la página visible y cachés limitadas a 4 MB. Importación, reducción y descargas fuera del hilo de interfaz, con colas acotadas y respuestas antiguas descartadas. No se reconstruye la pantalla al mover el foco. Presentación y consultas automáticas solo con Inicio visible; sin videos, animaciones ni servicios de recepción permanentes. Ante presión de memoria se reduce la imagen mostrada, conservando el archivo guardado.

## Internet, respaldo y recuperación

Wikimedia proporciona paisajes, autor, licencia y origen; **Créditos** muestra esos datos. Se descargan versiones reducidas desde la [colección de paisajes destacados](https://commons.wikimedia.org/wiki/Category:Featured_pictures_of_landscapes). No es una integración con Bing.

Actualizar sin desinstalar conserva fotos y preferencias. Exportar preferencias no incluye los archivos de la biblioteca: otro TV debe importar sus propias imágenes. Las fotos, sesiones y respaldos quedan fuera de Git y del paquete portable.

La recuperación de 0.14.0 usa el comportamiento 0.13.0 con código 35 y la firma original. Si había una presentación con selección parcial, utiliza Fondo original para no mostrar imágenes excluidas; conserva la selección para una actualización futura. Después de la recuperación se necesita código 36 o superior. No desinstales para bajar una versión.

## Aplicaciones sin iconos visibles

Si una fila está vacía, **＋ Añadir aplicación** permite incorporar una app instalada u oculta con OK, sin depender de la pulsación larga. Con apps visibles se conserva la edición con OK sostenido y Ajustes → Aplicaciones. Añadir un icono no instala aplicaciones: Instalar aplicaciones abre Play o Downloader únicamente cuando existen en el equipo. No se incluyen APK ni cuentas de terceros.
