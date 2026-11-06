# Aplicaciones y fondos

## Añadir aplicaciones

Si no hay aplicaciones visibles, el inicio y el panel inferior muestran **＋ Añadir aplicación**. Pulsa OK, elige una app instalada y se mostrará en la fila. Las apps ocultas también aparecen en esa lista. Con aplicaciones visibles, se conserva la pulsación larga para editar y el acceso desde Ajustes → Aplicaciones.

Añadir un icono no instala una app. **Instalar aplicaciones** abre Google Play o Downloader cuando existen en el destino. Si ninguno está disponible, vuelve a Android para instalar una aplicación por un medio de confianza. No se incluyen APK de terceros ni cuentas.

## Elegir un fondo

Abre **Ajustes → Panel e inicio → Fondos del inicio**. También hay un acceso en Personalización.

- **Actual:** conserva el degradado original. Es la configuración inicial y no carga fotos ni consulta Internet.
- **Una foto:** abre Mi biblioteca, previsualiza y pulsa Usar esta foto.
- **Mis fotos:** cambia entre las fotos de la biblioteca, en su orden. Puedes mover cada foto antes o después. Intervalos: 5, 15, 30 o 60 minutos; inicialmente 30.
- **Paisaje del día:** obtiene un paisaje destacado de Wikimedia Commons. Actualiza al volver al inicio cuando corresponde y mantiene la última copia sin conexión. Actualizar ahora respeta una espera corta entre intentos; un fallo conserva el paisaje anterior.

Las fotos solo aparecen en la pantalla de inicio. No se colocan detrás del panel sobre YouTube u otra aplicación. No se modifica el fondo de Android de otras apps ni se usan fondos animados.

Calidad **Ligera** limita la imagen a 720p; **Equilibrada** permite hasta 1080p. Ambas respetan la resolución efectiva del dispositivo. La presentación y las consultas automáticas se detienen al salir del inicio; sus imágenes se liberan de memoria.

## Enviar desde el celular

1. En Fondos, selecciona **Recibir del celular**.
2. Conecta el celular a la misma red local que el TV y escanea el QR con su cámara. También puedes escribir la dirección mostrada.
3. Selecciona fotos y pulsa Enviar fotos en el navegador. Revisa el resultado de cada archivo.
4. En el TV aparece **Foto recibida** y un contador. Pulsa **Ver foto recibida** para previsualizar y luego **Usar esta foto** para aplicarla; o **Terminar y abrir biblioteca** para ver todas. Puedes seguir enviando mientras el QR esté abierto. Atrás regresa a la biblioteca si recibiste fotos.

No se necesita instalar una app adicional. El TV puede estar conectado por Ethernet si pertenece a la misma red que el celular. Esta versión requiere una dirección IPv4 local. Las redes de invitados o con aislamiento entre equipos pueden impedir la conexión.

La recepción existe únicamente con esa pantalla abierta y dura como máximo 15 minutos. Salir, perder la red o caducar cierra el puerto e invalida el enlace. Abre otra recepción para generar un QR nuevo. No abras puertos del router ni publiques el enlace: la transferencia utiliza HTTP local con una sesión aleatoria, sin cifrado de extremo a extremo.

Los archivos se guardan en el TV y no se envían a Internet. Recibir no cambia el fondo automáticamente. Límites: 20 MB por archivo, 50 imágenes y 100 MB de biblioteca optimizada. Se admiten JPG, PNG y WebP estáticos; HEIC/HEIF depende del decodificador del TV. Si no se admite, exporta desde el celular como JPG o PNG.

## Archivos del TV o USB

**Elegir imágenes del TV o USB** utiliza el selector de documentos cuando hay uno funcional. En equipos sin él, se muestran las imágenes y carpetas consultables de Android, solicitando permiso únicamente al entrar. La alternativa ligera consulta hasta 500 imágenes por volumen y presenta listas en páginas de 50; para otras fotos puedes usar el selector del sistema o enviarlas por QR.

Si un USB no aparece, Android puede no exponer ese volumen o sus imágenes. Copia las fotos a Imágenes del TV o utiliza recepción por QR. Los permisos y selectores varían según el firmware; no se solicita acceso completo al almacenamiento ni root.

Z6 guarda una copia optimizada y normaliza la orientación. Puedes retirar el USB después de importar. Quitar una foto elimina solo su copia en la biblioteca, sin borrar el archivo original. Al quitar la foto seleccionada y volver al inicio, se recupera el fondo original si esa selección ya no existe.

## Internet y recursos

Wikimedia proporciona las fotografías, sus autores, licencias y páginas de origen. Consulta **Información del fondo** para ver los créditos. Se solicitan miniaturas reducidas mediante su API; no se suben fotos personales. La fuente es la colección de [paisajes destacados de Commons](https://commons.wikimedia.org/wiki/Category:Featured_pictures_of_landscapes), no la imagen editorial diaria de Bing.

El receptor, la biblioteca y el cliente de Internet no requieren el auxiliar ADB. Las operaciones lentas se realizan en trabajadores separados, con colas y tamaños limitados. No se crean trabajos periódicos, servicios de recepción permanentes ni procesos al arrancar para los fondos.

Si Android informa presión de memoria, se liberan las imágenes y se limita la calidad. Puedes volver a Actual en cualquier momento. Los valores del firmware no sustituyen una prueba de fluidez en el equipo real.

## Emulador y respaldos

Por decisión del usuario, no se incluye puente de red para enviar desde un celular al emulador. Sus pruebas de recepción se hacen desde un navegador del PC mediante reenvío ADB local y se retira ese reenvío al terminar. El QR directo se comprobó desde un celular al TV original; consulta VALIDACION-TV-0.12.2.md. Otro firmware necesita su propia prueba.

Las imágenes y los índices están en el almacenamiento privado de la app y permanecen al actualizar sin desinstalar. El exportador existente de preferencias no incorpora los archivos de la biblioteca: no transfieras sus identificadores como si fueran fotos. Otro TV debe importar sus propias imágenes. No desinstales para forzar una versión anterior, porque Android elimina datos privados.

Las recuperaciones con código 27 no muestran esta biblioteca, pero una actualización sin desinstalar conserva sus archivos privados. En la copia local se preservaron preferencias, APK anterior y snapshot del emulador antes de actualizar. Las fotos, claves, sesiones QR y respaldos nunca forman parte de Git ni del ZIP público del proyecto.

## Corrección 0.12.1

El fondo ocupa una capa independiente de borde a borde. Los márgenes se aplican únicamente al reloj, los controles y las aplicaciones. La foto mantiene su proporción y se centra para llenar la pantalla; se recortan los extremos necesarios en fotos verticales o panorámicas, sin deformarlas. Los límites de resolución y memoria no aumentan. Las recuperaciones de 0.12.1 usan código 29. La recuperación vigente para 0.12.2 vuelve a 0.12.1 con código 31 en ambas API; no desinstales para bajar una versión.

## Recepción más clara en 0.12.2

Cada foto guardada produce una confirmación visible y actualiza el contador sin reconstruir la pantalla ni generar otro QR. El primer éxito enfoca Ver foto recibida. Abrir la vista previa o terminar cierra el receptor. Los archivos inválidos muestran el fallo sin incrementar el contador. Una foto ya guardada sigue disponible aunque caduque la sesión. No se añaden consultas periódicas, servicios ni animaciones.
