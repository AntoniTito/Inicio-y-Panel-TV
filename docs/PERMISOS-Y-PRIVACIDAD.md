# Permisos, auxiliar y privacidad

Inicio y Panel TV agrupa funciones de inicio, panel y personalización. Cada capacidad se comprueba antes de ejecutar una operación; el sistema puede exigir autorizaciones o bloquearla según el firmware.

| Acceso | Para qué se utiliza |
|---|---|
| Accesibilidad principal | Recibir las teclas y mostrar el panel. Mantiene `canRetrieveWindowContent=false`. |
| Asistencia opcional de TV | Reconocer controles de la app elegida mientras enseñas, pruebas o ejecutas un destino. En reposo no consulta ventanas. |
| Acceso de uso | Verificar que una app dejó de ejecutarse antes de volver a abrirla, durante la acción solicitada. |
| Ajustes seguros mediante ADB | Preparar ajustes concretos, accesibilidad y permisos que gestione el asistente. |
| Bluetooth/ubicación | Dispositivos cercanos en API 31; los permisos de ubicación requeridos por Bluetooth en API 29. No se consulta GPS. |
| Alarmas exactas | Temporizador de suspensión en los Android que lo requieran. |
| Imágenes/documentos | Importar las fotos seleccionadas; MediaStore publica la copia optimizada. No se pide acceso completo al almacenamiento. |
| Red | Clima, paisajes de Wikimedia y sesión temporal de recepción local. |

## Qué hace el auxiliar

Es un proceso del mismo proyecto autorizado mediante ADB, para operaciones concretas que Android reserva a un contexto con más capacidades. El puente comprueba UID y no ofrece ejecución arbitraria. El proveedor de diagnóstico requiere permiso DUMP y acceso shell.

La autorización del PC sirve para instalar/configurar. La clave local del auxiliar se autoriza por separado cuando el firmware ofrece el transporte compatible. **Un botón de uso normal no debería exigir reactivar depuración cada vez.** Algunas operaciones avanzadas pueden seguir dependiendo del auxiliar; la capacidad debe comprobarse tras reiniciar sin el PC. Un PING durante la instalación no demuestra esa autonomía.

Diagnóstico y permisos muestra el estado y permite recuperar el acceso avanzado. La recuperación no reinicia adbd: ese reinicio terminaría el proceso que se intenta recuperar. No se usa root ni se modifican particiones del sistema para la configuración del control soportado.

## Fotos y red local

Las imágenes se optimizan y se guardan en el TV. Sus índices y créditos quedan privados; los archivos publicados en MediaStore son visibles para las apps multimedia que lo consulten. Eliminar desde el panel borra su copia y miniaturas sin papelera, no el original del celular o USB ni las copias que otra app haya creado.

La recepción HTTP solo permanece abierta mientras está visible esa pantalla, hasta 15 minutos. Usa una sesión aleatoria y una transferencia simultánea. Celular y TV necesitan una red local que permita comunicarse entre ellos. Al cerrar, caducar o perder la red se cierran las conexiones. Las fotos personales no se envían a Wikimedia ni a un servicio de generación de QR.

Los paisajes se obtienen de Wikimedia mediante HTTPS; autor, licencia y origen son accesibles en la interfaz. Las miniaturas y la última imagen válida se conservan localmente según los límites documentados en [Fondos](FONDOS.md).

## Qué no se publica

Claves de firma, contraseñas, redes, identidades ADB, cuentas, fotos personales, recorridos, perfiles `*-local.json` y respaldos individuales no pertenecen a Git ni al portable. El correo de autor de los commits se conserva por decisión del autor; no es una credencial de acceso.

Las capturas públicas utilizan un emulador separado y datos de demostración. Las evidencias del TV original se distinguen de esas capturas. [Capturas](CAPTURAS.md) · [Pruebas de la entrega](VERIFICACION-0.20.2.md).
