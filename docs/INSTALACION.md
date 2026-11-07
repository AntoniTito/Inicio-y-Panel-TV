# Instalación y recuperación

## Preparación

Windows PowerShell 5.1 o posterior, ADB y conexión autorizada al destino. Instalar no requiere Java ni la clave de firma: utiliza el APK firmado. Compilar sí requiere JDK y SDK.

No reutilices una IP de ejemplo ni el identificador del equipo original. Con varios dispositivos conectados, `-Dispositivo` siempre es obligatorio. Si Android ofrece depuración inalámbrica con emparejamiento, usa primero `adb pair HOST:PUERTO` y su código de autorización, y después el puerto real de conexión. El asistente no fuerza el puerto ni cambia el firmware.

## Instalación

Ejecuta `z6.ps1 instalar -Dispositivo IP:PUERTO`. El primer respaldo se guarda en `privado/dispositivos/IDENTIDAD/original.json` y no se reemplaza al repetir. Incluye HOME, estado del launcher, preferencias previas del panel, permisos modificables y valores de animación. Si el panel ya existía, también conserva su APK para referencia; no promete que Android permita instalar después un APK con código inferior.

La identidad del respaldo utiliza huella de firmware, número de serie y Android ID; esos datos no se aplican a otro TV. Revisa el destino antes de instalar. Si no puede identificar un HOME recuperable, no continúa.

Si Accesibilidad no se activa automáticamente, habilita Z6 Panel en Android y repite el mismo comando. Las otras aplicaciones y sus sesiones no se recrean con las preferencias del panel. El listado de aplicaciones faltantes y permisos pendientes queda en `ultimo-informe.json`.

El auxiliar autónomo requiere la autorización propia del destino y un transporte local compatible. Se solicita desde Diagnóstico. Se comprobó su recuperación tras reinicio en el emulador API 31; otro firmware requiere su propia prueba. PING solo comprueba la conexión actual. Si falta capacidad se conserva el acceso nativo y se declara la limitación. Usa `-SoloPanel` para instalar las aplicaciones externas después.

## Recuperación

`z6.ps1 restaurar -Dispositivo IP:PUERTO` usa el respaldo inicial del destino. También puedes indicar `-Respaldo ARCHIVO`. Rechaza identidades diferentes, restaura el HOME y las animaciones, recupera los permisos que modificó y conserva otros servicios de accesibilidad activados posteriormente. Z6 Panel permanece instalado para evitar perder sus datos.

`-RecuperarVersion` selecciona 0.16.1/código 43 para API 29 y 31, con la misma firma. No funciona encima de códigos superiores. Tras recuperarlo, una actualización debe tener código 44 o mayor. Sin esa opción solo se restauran HOME y ajustes originales, conservando el APK instalado. La restauración cancela el temporizador y recupera el modo previo de alarmas exactas y los permisos gestionados.

Los estados originales de la limpieza antigua se conservan en `privado/referencia/original`. No son una receta para otros modelos. El restaurador específico se niega a ejecutarlos sin una identidad comprobada del TV original. Los respaldos del instalador nuevo sí incluyen su identidad desde el principio.

Las imágenes de 0.16.0 se publican en MediaStore y los índices quedan privados. El exportador de preferencias no incluye fotos. Permanecen al actualizar sin desinstalar. El perfil de réplica no copia fotos, sesiones QR, caché diaria ni IDs de otro TV. Lee FONDOS.md antes de trasladarlas.

## Git y entregas

El repositorio se prepara localmente, sin remoto ni publicación automática. Para publicarlo elige un repositorio privado y conecta su URL. Los APK locales conservados se adjuntan a la entrega junto con `entregas/manifest.json`. No añadas por fuerza `privado` o `.local` ni publiques la firma, credenciales, inventarios o sesiones de terceros.

La recuperación vigente 0.16.1/código 43 entiende Todas/Elegidas y añade el adaptador para las imágenes de MediaStore. No usar el APK histórico 0.15.0/código 36 para recuperar una biblioteca migrada. La protección que usa Fondo original se limita a las recuperaciones anteriores de código 35 o menor. No elimina la biblioteca; tampoco recupera imágenes borradas expresamente.

## Galería y respaldo de fotos desde 0.16.0

El instalador habilita reversiblemente la galería AOSP si está instalada y conserva su estado original por dispositivo; la restauración lo recupera. Si no existe un visor, Mis fotos sigue disponible sin instalar terceros. También se puede ejecutar `herramientas/galeria.ps1 -Dispositivo SERIAL`, con `-Simular` o `-Restaurar`.

Una actualización anterior a código 38 guarda preferencias y retiene la migración hasta completar un respaldo verificado de cada imagen. Si ese respaldo falla, se conserva la marca de espera: no borrar archivos ni desinstalar. Repite el mismo comando `z6.ps1 instalar -Dispositivo SERIAL`: el registro privado de migración identifica el respaldo pendiente, lo reintenta y libera la espera únicamente al verificarlo. Para copias posteriores puede usarse el mismo respaldador; sus archivos quedan en privado/fotos.
