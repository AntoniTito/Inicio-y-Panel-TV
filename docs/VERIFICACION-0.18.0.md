# Verificación de Z6 Panel 0.18.0

Entrega 0.18.0/código 52, APK común para API 29 y 31. Comprobaciones realizadas el 11 de noviembre de 2026. La firma original se conserva. No se distribuyen Xuper, cuentas, fotos, emparejamientos ni recorridos personales.

## Función y límites

La configuración se encuentra en Ajustes → Aplicaciones → Inicio y recuperación de TV. Permite elegir una app elegible, enseñar y confirmar un recorrido al canal y activar por separado arranque, despertar y un botón del control. Reiniciar la app significa cerrar su proceso y abrir su acceso MAIN, conservando sus datos.

El servicio adicional de Accesibilidad permanece sin consultas cuando no hay tarea. El servicio principal mantiene `canRetrieveWindowContent=false`. La ayuda se limita al paquete elegido y excluye contraseñas y campos editables. Las operaciones del auxiliar siguen autenticadas por UID.

En listas nativas se comprueban la identidad y selección antes de activar una fila. Se prepara el foco; se utiliza el clic accesible de la fila seleccionada cuando está disponible o la activación del control, con navegación verificada. El último canal puede reconocerse por su nombre exacto en una lista alternativa, sin admitir coincidencias parciales ni varias filas con el mismo nombre.

La preparación del arranque espera cinco segundos a la restauración de servicios y concede al reproductor una breve preparación inicial. Si el auxiliar está ocupado, solo se reintenta la preparación dentro del límite. Cuando un recorrido automático queda sin el siguiente control esperado, se admite una única reapertura adicional. Una segunda falta no genera otra cadena de reinicios; la tarea termina por su límite y permite continuar manualmente.

## TV físico Android 10/API 29

Se respaldaron el APK anterior, las preferencias, el recorrido y los bytes de las dos imágenes existentes. La comparación posterior de identificadores, tamaños y SHA-256 confirmó que las fotos no cambiaron.

- Xuper Hydra 4.34.4: apertura desde un proceso nuevo y selección de America TV Peru HD. El usuario confirmó imagen en movimiento y sonido en la prueba guiada y al usar el botón físico Netflix.
- Reinicio real de Android: el panel y el auxiliar ya estaban ejecutándose antes de consultar el proveedor de diagnóstico. No se inició el auxiliar desde el PC. El recorrido llegó al canal; el usuario también confirmó que el TV mostraba Xuper en América TV HD.
- Suspensión y despertar de Android mediante teclas explícitas de suspensión/despertar: contador de arranques sin cambios, proceso nuevo de Xuper y canal comprobado. Tampoco requirió arrancar el auxiliar desde el PC.
- En este equipo las pruebas automáticas necesitaron la única reapertura de recuperación. Las primeras candidatas fallaban durante la disponibilidad inicial del auxiliar o al continuar desde el primer menú; esos intentos no se cuentan como pruebas superadas de la entrega final.
- Netflix y YouTube de este control no llegaban a la captura habitual de Android. Se comprobaron señales de función en el dispositivo de entrada; Netflix quedó aprendido y confirmado como botón de recuperación. La escucha ampliada usa `Os.poll`, se cierra por una tubería de cancelación y no modifica mapas del sistema ni requiere root. Un control Bluetooth desconectado se muestra pendiente hasta su reconexión.

Los eventos anteriores prueban el ciclo de Android. No prueban que apagar únicamente el televisor por HDMI produzca un despertar del TV Box ni garantizan HDMI-CEC. La autorización local de depuración del auxiliar sigue siendo necesaria; no se desactivó para la prueba.

Evidencia sin datos personales: `pruebas/resultados-tv-0.18.0-api29.json`, `resultados-arranque-tv-0.18.0-api29.json` y `resultados-despertar-tv-0.18.0-api29.json`.

La confirmación física de Netflix con vídeo y audio se recibió antes de los últimos ajustes del arranque. El APK final completó posteriormente el recorrido manual, el arranque y el despertar por diagnóstico. La repetición final de Netflix y navegación con el mando quedó pendiente de respuesta. Tras terminar las mediciones, ADB pasó a rechazar la conexión; una muestra adicional de CPU en reposo no pudo concluir y no se cuenta como consumo cero. Las mediciones completas anteriores sí se conservaron.

## Emulador Android TV 12/API 31

Se utilizó una app de prueba propia, sin red, cuentas ni vídeo real. Los resultados no acreditan reproducción de un proveedor comercial ni controles físicos de Android 12.

| Suite | Comprobaciones |
|---|---:|
| Recorrido, proceso nuevo, configuraciones independientes, cancelación, menús distintos, campos secretos excluidos y ausencia de consultas en reposo | 35 |
| Listas nativas, disposición alternativa de la fila y rechazo de canales ambiguos | 6 |
| Aprendizaje con confirmación, scan code, tecla reservada, eliminación y pulsación mantenida | 12 |
| Captura ampliada por callback sintético, identidad del dispositivo, confirmación y cierre del lector | 7 |
| Una reapertura ante fallo transitorio, ausencia de bucle y vencimiento ante fallo persistente | 3 |
| Reinicio real del emulador, despertar, deduplicación y recreación del proceso, en una candidata anterior de 0.18.0 | 5 |

El ciclo del emulador necesitó inicio del auxiliar desde el PC y una reconexión asistida para recrear el proceso; está marcado en su JSON. Al repetir el ciclo con el APK final, el emulador se cerró durante el reinicio y dejó de aparecer en ADB. Ese intento queda incompleto en `resultados-ciclo-tv-0.18.0-api31-final-intento.json`; se volvió a iniciar el AVD para comprobar el panel y restaurar su configuración. No se cuenta como un reinicio final superado. La captura ampliada del emulador inyecta un callback protegido, no una pulsación de hardware. La prueba real del botón corresponde únicamente al TV original.

Las suites de regresión del carrusel registraron 53 comprobaciones en cada API: filas independientes, vueltas, memoria de selección, edición, retorno desde Añadir app y menús conservados. Las flechas no recargaron el catálogo ni reconstruyeron el panel.

## Rendimiento

Tres rondas de 60 movimientos por versión, con intervalo de 40 ms dentro del comando protegido y al menos 61 cuadros por ronda. La tabla muestra la mediana de los tres p95 de `gfxinfo`, **no una medición de latencia del control**. La CPU es tiempo consumido por el proceso de Z6 durante cada ronda.

| Entorno | p95 0.17.0 → 0.18.0 | Límite de aceptación | CPU por ronda, antes → después | PSS después de 20 ciclos |
|---|---:|---:|---:|---:|
| TV físico API 29, comparación pareada | 14 → 18 ms | 22 ms | 690 → 670 ms | +727 KiB |
| Emulador API 31 | 17 → 18 ms | 25 ms | 140 → 120 ms | −9498 KiB |

Ambos cumplen el margen del mayor valor entre 10 % y 8 ms. No se atribuye a esta función una mejora de rendimiento: las muestras muestran variabilidad de CPU y dibujo. Una referencia física inicial de 9 ms y una medición posterior de 18 ms estaban separadas por reinicios; se repitió la comparación con las dos versiones, el mismo estado de ajustes y calentamiento. Esas muestras iniciales se conservan dentro del JSON físico para que la variación sea revisable. La medición final del emulador procede de otra sesión tras volver a abrir el AVD y se marca como tal.

Tras cinco ciclos de calentamiento y veinte aperturas/cierres, el proceso se mantuvo estable y no hubo crecimiento de PSS superior a 8 MiB. La caída de memoria del emulador refleja liberación de memoria durante la muestra, no una reducción garantizada del consumo. No hubo nuevos escaneos de catálogo ni reconstrucciones completas por las flechas. Los detalles están en `resultados-rendimiento-panel-0.18.0-api29.json`, `resultados-rendimiento-panel-0.18.0-api31.json` y los archivos `resultados-memoria-panel-0.18.0-*` de `pruebas/`.

## Recuperación e integridad

Se preparó 0.17.0/código 53 con la firma original. Su DEX coincide con el comportamiento 0.17.0 preservado. El restaurador exporta la configuración privada de TV, cancela las tareas, espera la confirmación de cierre de la escucha física y retira únicamente la ayuda nueva antes de restaurar. Distingue la versión funcional del código: la recuperación 0.17.0/c53 no se trata como una app con automatización de TV.

El ensayo de vuelta al comportamiento anterior utiliza un APK de laboratorio con código 52 para poder reinstalar la entrega 52 sin desinstalar ni quemar el código 53. Se verifican fotos y preferencias después de regresar. No equivale a haber instalado físicamente el código 53 en el TV del usuario. La selección de ese artefacto se comprueba mediante el asistente simulado. Después de instalar la recuperación real 53 se necesita código 54 o superior.

El ensayo se completó tanto en el emulador API 31 como en el TV físico API 29. Para las mediciones de laboratorio se inició el auxiliar desde el PC; esto se distingue de las pruebas físicas de arranque/despertar autónomos descritas arriba. El emulador quedó con sus preferencias y servicios originales, automatismos de TV desactivados y la app sintética retirada; después se cerró el AVD que se había iniciado para estas pruebas.

El asistente superó 68 comprobaciones sin contactar dispositivos físicos. El formato binario de las teclas y la identidad sin MAC/serie superaron 48 comprobaciones locales. Los hashes de ambos APK están en `entregas/manifest.json`; el portable excluye claves, fotos, recorridos y respaldos personales.

## Reproducción de las pruebas

`pruebas/verificar.ps1` comprueba el asistente. `pruebas/verificar-teclas-formato.ps1` comprueba el lector de metadatos. Las suites `recuperacion-tv-0180.cjs`, `listas-tv-0180.cjs`, `teclas-tv-0180.cjs`, `teclas-auxiliares-tv-0180.cjs`, `reintento-tv-0180.cjs` y `ciclo-tv-0180.cjs` exigen el serial de un emulador. Preparar e instalar previamente la app de `pruebas/fixture-tv`, conservar las preferencias del destino y habilitar la ayuda de prueba. Los recorridos verificados de esa app se generan localmente, no se transfieren entre dispositivos.

El ciclo guarda su configuración anterior en `.local/ciclo-tv-0180-original.json` antes de cambiarla. Si el emulador desaparece, conserva ese respaldo para restaurar al volver y no imprime errores ADB continuamente durante la limpieza.

`despertar-tv-0180.cjs`, `memoria-panel-0180.cjs`, `rendimiento-panel-0180.cjs` y `carrusel-panel-0180.cjs` requieren un destino explícito. La prueba de despertar cambia realmente el estado de pantalla; las de navegación usan el proveedor restringido a ADB. No ejecutar `uiautomator dump` durante una tarea: interrumpe temporalmente los servicios de Accesibilidad y puede cancelar el recorrido.
