# Verificación de Z6 Panel 0.19.0

Entrega: 0.19.0/c56, un APK para API 29 y 31. Recuperación: comportamiento original de v0.18.1, código 57 y firma original. Después de instalar c57 se requiere c58 o superior. Los hashes exactos se registran en `entregas/manifest.json`.

## Cambios comprobables

- Botones independientes con acciones y destinos separados de arranque/despertar. La configuración anterior se migra por copia y permanece privada.
- Nueva pantalla Mi control y arranque, tarjetas de botones y edición individual. Se revisó su distribución a 1280 × 720 en emulador y captura del TV original.
- Configuración del destino navegando en la app, con una etiqueta pequeña; Home abre la revisión. Se retiraron la antigua lista duplicada de controles y sus callbacks sin entrada.
- Reapertura mediante Android, con comprobación de que desapareció el UID anterior; se usa el auxiliar solo si la capacidad normal falla y está disponible. No se anuncia éxito sin verificar el cierre.
- Mapa de teclado de Android para los dos botones del Remoter ATV3 comprobado, sin editar Generic.kl ni particiones. La recepción normal ya no necesita el lector de archivos de entrada. Otros modelos necesitan su propia comprobación.
- Diagnóstico distingue reapertura normal y control avanzado. No se reinicia adbd ni se cambia la depuración durante las pruebas de esta entrega.

## Emulador Android TV 12 / API 31

`pruebas/mando-0190.cjs` y `resultados-mando-0.19.0-api31.json`: 25 comprobaciones con app sintética. Incluyen dos botones simultáneos, grabación con navegación real de botones y ListView, prueba antes de guardar, destinos independientes, cierre nativo verificado, selección nativa sin flechas del auxiliar, repetición sostenida sin reinicios acumulados, conflictos rechazados, protección de Home y cancelación. Al finalizar no quedan consultas de controles ni ventanas de la tarea.

La inyección ADB no reproduce por sí sola la entrada de Accesibilidad del control físico: el arnés notifica el evento al motor y envía la tecla a la app de prueba. No valida recepción Bluetooth ni video de servicios comerciales.

`resultados-mapa-nativo-0.19.0-api31.json`: selección y retirada real de la preferencia nativa de teclado, restaurando el valor original. Se usó el identificador de fabricante/modelo sin un control físico conectado; no se declara validado ese hardware en Android 12.

`arranque-mando-0190.cjs` añade siete comprobaciones de orden de inicialización: asistencia tardía, pantalla apagada, continuación por eventos y deduplicación del arranque. Son eventos controlados del emulador, no un reinicio físico.

## TV original Android 10 / API 29

Respaldo previo del APK instalado, preferencias, configuración del destino y bytes de las dos imágenes actuales. Clave, rutas, canales, identidad, capturas y datos individuales se conservan en `privado/`, fuera de Git.

- El usuario confirmó Netflix con el mapa nativo de Android en la prueba previa a integrar la interfaz.
- Se instalaron los dos botones independientes: Netflix conserva la app y destino anteriores; YouTube usa cerrar/reabrir la variante instalada. Arranque y despertar conservaron sus interruptores y recorrido.
- Con el auxiliar detenido y su recuperación automática temporalmente desactivada, una invocación controlada cerró y reabrió la app de TV y completó los tres pasos guardados mediante Android. Otra comprobación reinició YouTube con cambio de PID. Se verificó que el auxiliar seguía ausente.
- La prueba no desactivó Depuración USB: demuestra funcionamiento sin el proceso auxiliar, no una prueba física de arranque con la depuración apagada.
- Se restituyó la preferencia de inicio autónomo del auxiliar. Volvió a conectarse con un solo enlace y su lector físico quedó detenido, sin dispositivos abiertos; los botones nativos siguen siendo independientes de ese lector.
- Los hashes de ambas imágenes coincidieron con el respaldo. El APK de prueba para investigar el mapa fue desinstalado; el mapa definitivo pertenece a Z6.

El usuario confirmó ambos botones físicos y el canal con imagen y sonido. Las pruebas controladas sin auxiliar se registran por separado arriba.

El usuario confirmó también Netflix después de un reinicio real, sin reactivar depuración, y la nueva interfaz completa y con buen foco.

Se detectó un fallo del arranque automático: la solicitud podía descartarse si la asistencia aún no estaba conectada o la pantalla seguía apagada. Se corrigió manteniendo la preparación pendiente, reanudándola por eventos y creando la pantalla de preparación solo cuando se cumplen las condiciones.

**Pendiente:** configuración de un nuevo recorrido con el control real y confirmación del arranque automático tras la corrección. Las pruebas controladas anteriores no sustituyen estas comprobaciones. No se extrapola el resultado del TV original a otros firmwares.

## Rendimiento

Sin tarea, la asistencia mantiene eventos de contenido desactivados. No se añadieron sondeos de entrada, reconstrucciones por flecha, dependencias gráficas ni trabajo de imágenes. Se serializan las tareas; el cierre nativo tiene un plazo de cuatro segundos y como máximo cuatro solicitudes durante una acción explícita.

`resultados-fluidez-mando-0.19.0-api31.json` registra cinco aperturas de calentamiento y otras veinte con la misma implementación de interfaz: PSS 37 947 → 36 471 KB, sin crecimiento sostenido en esa muestra. El hash del APK medido figura en el archivo; los cambios posteriores corresponden a restauración y cancelación de la captura, no al dibujo de la pantalla.

La muestra gráfica registró 336 cuadros y p95 de 4950 ms. **No es una aceptación gráfica válida ni evidencia de mejora:** el emulador sin ventana informa errores de texturas/color buffer, el juego Dota 2 estaba abierto en Windows y no se recogió una referencia anterior bajo condiciones controladas. Se conserva el dato y sus limitaciones, sin atribuir toda la demora al juego ni afirmar un porcentaje de aceleración. La fluidez física de esta interfaz sigue pendiente del usuario.

## Instalador, recuperación y privacidad

`pruebas/verificar.ps1`: 77 comprobaciones locales con ADB simulado, sin contactar hardware. Incluyen identidad, firmas/hashes, rechazo de downgrade, conservación de fotos y preferencias, permisos iniciales, exportación de botones, retirada del mapa antes de recuperar el APK antiguo y restitución del modo de acceso de uso.

La recuperación c57 se compiló desde la etiqueta v0.18.1 sin modificar su comportamiento y se verificó el certificado original. No se instaló sobre el TV físico; hacerlo impediría volver a c56. El asistente conserva los botones nuevos privadamente, cancela tareas/escucha y retira el mapa antes de instalar un APK que no incluye su receptor.

El portable incluye código, guías, asistentes y los dos APK. Excluye SDK, claves, imágenes personales, sesiones y respaldos. El trabajo se registra localmente en Git; no se publica ni se mueven etiquetas anteriores.
