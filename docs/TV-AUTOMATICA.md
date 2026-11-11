# Inicio y recuperación de TV

Disponible en Z6 Panel 0.18.0/código 52 para API 29 y 31. Se configura en **Ajustes → Aplicaciones → Inicio y recuperación de TV**.

1. Elige la aplicación instalada. Puede ser Xuper u otra variante compatible; Z6 no instala aplicaciones ni copia cuentas.
2. Habilita **Asistencia de TV de Z6** desde el acceso a Accesibilidad. Esta ayuda adicional reconoce controles de la app elegida únicamente mientras configuras o ejecutas el recorrido. El servicio principal del panel sigue sin leer contenido.
3. En **Configurar canal**, elige los controles de la app: Televisión, Perú y el canal que prefieras. Puedes desplazar listas o volver dentro de la app desde la ayuda. No se guardan contraseñas, capturas ni campos editables.
4. Cuando se reproduzca el canal, elige **Ya se ve el canal · probar desde cero**. Z6 cerrará el proceso y repetirá el recorrido. Confirma que la imagen y el sonido corresponden al canal elegido.
5. Activa **Al arrancar Android**, **Al despertar** y, si tienes una tecla libre, **Botón de recuperación**. Los interruptores son independientes. **Guardar y activar** habilita arranque y despertar y también el botón si ya lo asignaste.

**Recuperar ahora** cierra y vuelve a abrir la app aunque ya esté en primer plano. Mantiene sus datos y cuenta. Con un recorrido probado intenta llegar al canal; sin recorrido solo reabre la aplicación. No equivale a borrar datos ni garantiza que un servicio externo autorice una cuenta o sesión.

**Cambiar el canal o la variante:** vuelve a Configurar canal. Elegir otra app desactiva los automatismos y descarta el recorrido anterior; hay que enseñarlo y comprobarlo de nuevo. Las aplicaciones del sistema, los launchers, componentes compartidos y el propio panel no son elegibles.

**Botón del control:** Asignar botón escucha una tecla durante un minuto y pide confirmar. Elige Netflix, YouTube u Otro botón y pulsa la tecla física que desees. Z6 reconoce su código y, cuando existe, su scan code; las etiquetas impresas no se adivinan. Se admiten accesos de aplicaciones, teclas de color, funciones y otras teclas libres que Android entregue. Solo se asigna un botón de recuperación a la vez. Home, Atrás, flechas, OK, volumen y encendido están protegidos. La tecla libre que confirmes sustituirá su función habitual mientras el botón esté activado. Puedes cambiarla o quitarla en el mismo menú. Mantenerla pulsada no acumula órdenes. Otra pulsación durante la recuperación la cancela.

Algunos fabricantes ocultan Netflix o YouTube antes de Accesibilidad. El auxiliar puede reconocer sus señales de función F1–F24 y de color cuando el firmware permite leerlas; no requiere root ni cambia el mapa de teclas de Android. La detección ampliada se cierra al salir o cancelar. Después de asignar, escucha únicamente el dispositivo elegido, sin sondeo periódico, y reacciona a su reconexión. No todas las teclas ni todos los controles son interceptables: si el fabricante bloquea también esa lectura, utiliza otra tecla o Recuperar ahora. En el TV original se comprobó Netflix por esta vía. Después de reiniciar, un control Bluetooth puede aparecer pendiente hasta que vuelva a conectarse.

**Cancelar:** Home o Atrás, navegar manualmente con las flechas durante el recorrido automático, cambiar de aplicación, apagar la pantalla o desactivar una opción detienen la tarea. No hay un bucle de reinicio. La preparación automática espera hasta 90 segundos al control auxiliar, la red y el desbloqueo; si el auxiliar sigue ocupado, reintenta solo esa preparación dentro del mismo límite. Al arrancar espera además cinco segundos para la restauración de servicios de Android. El recorrido tiene un límite de 60 segundos. La configuración y su confirmación disponen de cinco minutos. Si falla el recorrido después de abrir la app (por ejemplo, cambió el menú o falta iniciar sesión), esta queda abierta para continuar manualmente y puede ser necesario enseñar otra vez el recorrido.

**Qué significa encender:** arranque es un inicio real de Android, deduplicado por su contador de arranques. Despertar requiere que Android haya informado pantalla apagada y encendida. Si solo apagas el televisor y el TV Box permanece activo, puede no existir tal evento: no se promete detectar ese caso mediante HDMI-CEC. Utiliza el botón o Recuperar ahora. No se reinicia por actualizar Z6 o reconectar su auxiliar.

Al arrancar, Z6 deja que el reproductor restaure su sesión inicial antes de recorrer los menús. Si después de un paso no aparece el siguiente control, puede cerrar y abrir la app **una sola vez más**. Esto también se aplica al despertar, sin cambiar el canal guardado. No repite esta recuperación indefinidamente: si tampoco funciona, deja la app disponible para continuar con el control. La apertura manual con el botón no acumula reintentos.

## Rendimiento y privacidad

Sin tarea activa, la ayuda no consulta ventanas ni controles y no realiza sondeos periódicos. Las consultas se limitan al paquete elegido, excluyen campos editables y contraseñas y se detienen al finalizar. Las operaciones pesadas se serializan fuera del hilo de interfaz. Las elecciones del usuario tienen prioridad sobre lecturas pendientes. En listas nativas se reconoce el elemento seleccionado después de cada movimiento; el auxiliar solo puede preparar el foco y enviar arriba, abajo u OK a la app elegible en primer plano, con límites de tiempo y pasos. Se rechazan controles ambiguos y no se ejecutan secuencias de coordenadas o flechas a ciegas.

La escucha ampliada usa una espera bloqueante de entrada en el auxiliar; no ejecuta un comando cada pocos milisegundos. Filtra funciones y colores, ignora texto y teclas esenciales, y no registra pulsaciones. La identidad del control contiene nombre, bus, fabricante y producto; no guarda su dirección Bluetooth ni número de serie.

Aplicación, certificado, canal, recorrido y asignación del control pertenecen al dispositivo. Se guardan en preferencias privadas separadas de `panel`, fuera del perfil `replica` y de la exportación transferible. Las variantes necesitan su propia comprobación; un APK común para Android 10/12 no implica compatibilidad de todos los menús de terceros.

## Recuperación

La recuperación firmada es **0.17.0/código 53**. Antes de restaurar, `z6.ps1 restaurar` guarda la configuración de TV privadamente, cancela los automatismos, comprueba el cierre de la escucha física del control y retira solo la ayuda nueva de Accesibilidad, conservando los otros servicios. Después aplica la restauración de HOME y ajustes que corresponda al mismo destino. No instales la recuperación manualmente sin cancelar primero los automatismos. Después de código 53, la siguiente actualización necesita código 54 o superior. Los archivos y preferencias personales no se desinstalan.

Las comprobaciones y sus límites se registran en [VERIFICACION-0.18.0.md](VERIFICACION-0.18.0.md).

En reproductores que ocultan el nombre del canal, la verificación puede usar la selección semántica del canal y su transición fuera de la lista después de OK. La confirmación inicial de imagen y sonido sigue siendo necesaria: seleccionar un canal no demuestra que el proveedor esté transmitiendo.
