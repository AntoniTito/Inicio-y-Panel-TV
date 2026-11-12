# Requisitos obligatorios de la réplica

Este documento establece los criterios de aceptación de **Inicio y Panel TV 0.20.2/c62**. El cambio de marca fue solicitado expresamente; las funciones y la configuración de referencia se conservan. Leerlo antes de [REPLICAR-ENTORNO.md](REPLICAR-ENTORNO.md) y ejecutar el [prompt de réplica](PROMPT-REPLICA.md). No es una certificación de compatibilidad universal ni una comprobación automática del instalador: obliga al agente a configurar, probar y demostrar el resultado.

## Resultado exigido

**Reproduce el mismo entorno del panel: aspecto, distribución, navegación, funciones, configuración y comportamiento de arranque. No lo declares terminado mientras falte un requisito o su comprobación.**

La igualdad exigida comprende el entorno instalado por este proyecto. Una réplica del entorno no convierte el firmware, las pantallas nativas de Android o los servicios externos de marcas distintas en el mismo sistema operativo. Si se exige también identidad de esas pantallas o de cada píxel, debe existir una referencia comparable y un destino capaz de reproducirla; no afirmes haberla obtenido a partir de la instalación del panel.

Las únicas diferencias técnicas admitidas sin cambiar el objetivo son identidades propias del destino: serial/IP, autorizaciones, referencias locales de archivos, firmas de apps comprobadas, códigos físicos de su control y límites de memoria/resolución del hardware. Esos valores deben producir la misma acción y conservar todos los requisitos. Un límite que altere el aspecto exigido o impida una función **no queda aprobado por mencionarlo en un informe**.

## Reglas que debes cumplir

1. **Fija la referencia antes de configurar.** Usa `main`, `entregas/manifest.json`, `perfiles/replica.json` y la matriz de este documento. Comprueba versión, hashes y firma. No elijas una base histórica por el nombre Android 10/12.
2. **Completa la instalación y la configuración.** El asistente no configura por sí solo todos los botones, destinos, fondos y cuentas. Ejecuta los pasos pendientes hasta obtener el comportamiento completo. `-SoloPanel` es una etapa provisional cuando faltan apps, no una réplica terminada.
3. **Conserva diseño y funciones.** No cambies colores, geometría, navegación, etiquetas o acciones para facilitar la instalación. No sustituyas el launcher ni los fondos por otra app. No retires una función que falle para poder declarar éxito.
4. **Resuelve los problemas comprobados.** Revisa los errores, aplica la configuración o adaptación pertinente y vuelve a probar lo afectado. No cierres el encargo con una instalación parcial presentada como equivalente. Mantén intactos firma, datos del usuario y método de recuperación.
5. **Configura los valores propios del destino.** Detecta sus teclas, apps, permisos, red y resolución. No inventes datos ni restaures identidades, autorizaciones o respaldos de otro equipo. Enseña el recorrido en la variante instalada y reutilízalo en los usos exigidos.
6. **Obtén los recursos indispensables.** Si no están disponibles la app requerida, el destino exacto, una cuenta o una foto que debe ser idéntica, solicita solo ese dato/recurso y conserva el requisito pendiente. No elijas otra app, canal o imagen silenciosamente. Las preferencias individuales no se publican en Git.
7. **Prueba en el equipo real.** Usa control físico, reproducción y reinicio donde corresponda. No sustituyas evidencia física por inyección ADB, emuladores, un PING o resultados históricos. Registra lo efectivamente observado.
8. **Demuestra el rendimiento.** No aceptes bloqueos, doble apertura, parpadeo ni trabajo permanente nuevo para conseguir una función. Compara secuencias equivalentes y comprueba memoria; consulta el criterio de rendimiento de la matriz.
9. **No elimines controles para saltar una incompatibilidad.** Otra API necesita adaptación y validación, no cambiar únicamente la lista de API admitidas. Si cambia el APK, usa una entrega nueva con la firma original y código 62 o superior; conserva las etiquetas y la entrega de referencia. Una firma improvisada o desinstalar perdiendo datos no es una solución.
10. **Declara el estado con exactitud.** Todos los requisitos deben estar verificados. Si falta alguno, continúa el trabajo que sea posible y registra qué impide los demás. Una restricción demostrada de hardware, firmware o servicio externo deja la réplica incompleta o incompatible; escribir instrucciones imperativas no elimina esa restricción.

## Matriz mínima de aceptación

Para cada fila registra valor de referencia, resultado del destino, prueba, evidencia y estado. Divide las filas en comprobaciones individuales cuando sea necesario. No marques una función obligatoria como «no aplica» para evitar comprobarla.

| ID | Resultado obligatorio | Evidencia mínima |
|---|---|---|
| R01 | Entrega correcta y recuperable: APK vigente, misma firma, hashes válidos, respaldo inicial del destino y APK de recuperación disponible. | Manifiesto, verificación de artefactos e identidad del respaldo. No instalar la recuperación solo para probar su existencia. |
| R02 | Inicio y Panel TV es HOME; el inicio anterior queda retirado reversiblemente, sin aparecer en el flujo normal. | Arranque, Home y estado respaldado del launcher. |
| R03 | Mismo diseño del panel: panel inferior compacto, tamaños y posiciones de iconos/botones, foco translúcido y reloj gris superior derecho. | Capturas comparables y prueba con control. Si falta referencia visual suficiente, obtenerla antes de aprobar. |
| R04 | Preferencias transferibles de `replica.json`: reloj/fecha, 12 horas, saludo, opacidad 80 %, sin animación del panel y escalas de animación a cero; mismas tarjetas visibles y orden. | Comparación de preferencias y tarjetas reales. Los paquetes de otra variante se resuelven por su función, sin perder accesos. |
| R05 | Home abre el panel sobre video conservando imagen/audio; Apps y Acciones dan la vuelta por separado y cambian de fila solo verticalmente. | Control físico y reproducción continua. |
| R06 | Edición confinada a Apps: reordenar, ocultar, añadir, guardar/cancelar y ocultar la última. Añadir app → Atrás no bloquea ni pierde la sesión. | Prueba de cada acción con el control, restaurando las tarjetas de referencia. |
| R07 | Ocho apartados de Ajustes, regreso con foco estable, volumen/silencio, salida de audio, permisos, reinicio de app, temporizador y suspensión. | Resultado real de cada función. Conservar ajustes originales usados durante la prueba. |
| R08 | Fondos completos: cuatro modos en fila, vista previa persistente, fotos/presentación gestionables, recepción QR, borrado efectivo y MediaStore. La configuración de referencia usa Paisaje diario, Automática y rotación desactivada. | Navegación, recepción real y eliminación de imagen de prueba. El fondo exacto exige importar su recurso; el catálogo diario puede cambiar por fecha. |
| R09 | Catálogo de hasta 12 paisajes persistente durante el día, selección/conservación, rotación configurable y limpieza de temporales; sin descargas durante otras apps. | Reapertura con caché, estados/archivos y prueba de rotación/limpieza sin alterar fotos personales. |
| R10 | Mismas tarjetas de botones, etiquetas, asignaciones e interruptores de referencia, incluidos los dos accesos independientes: tecla de TV reabre la app y llega al destino exacto; tecla YouTube cierra y reabre YouTube sin canal obligatorio. Etiqueta editable e identidad interna visible. | Comprobar toda la configuración local de referencia y ambas teclas físicas desdel panel y otra app; una apertura por pulsación, sin acumulación al mantenerla. No basta que abran cualquier canal o pantalla. |
| R11 | Destino confirmado persiste y se reutiliza entre botones y arranque/despertar. Añadir/eliminar botón y eliminar destino, con o sin quitar usos, funcionan. | Guardar tras prueba, salir/reentrar, recrear proceso y gestionar datos de prueba sin borrar destinos reales. |
| R12 | Arranque real y despertar ejecutan el comportamiento configurado una sola vez, sin depender del PC ni de reactivar depuración para el uso normal. | Reinicio/despertar y canal o pantalla correcta con imagen/audio cuando corresponda. Esperar hasta tres minutos; apagar solo el TV no demuestra suspensión del Box. |
| R13 | Retorno de Panel e inicio y Ajustes de Android sin parpadeo; Mi control se vuelve a abrir desde Inicio y Ajustes. | Repetición con el control físico. |
| R14 | Fluidez conservada y trabajo acotado: sin sondeos nuevos en reposo, recargas por cada flecha ni imágenes ocultas retenidas; sin bloqueos. | Tres rondas equivalentes y 20 ciclos tras calentamiento. Respecto de una referencia comparable del mismo destino: incremento de p95 no superior al mayor entre 10 % y 8 ms; crecimiento de memoria inferior a 8 MB. No comparar milisegundos de hardware distinto como si fuera el mismo. |

Los recursos individuales de referencia que no se distribuyen deben completarse privadamente. Si no se conoce el nombre/variante exacta de una app, el destino de TV, el fondo o el ajuste personalizado que se exige copiar, **la fila correspondiente permanece pendiente**. No deduzcas un canal por un nombre histórico del repositorio ni sustituyas el recurso por el primero disponible.

En rendimiento, la ausencia de una referencia comparable no constituye un aprobado: registra la medición disponible y el dato faltante. Los umbrales son un requisito de aceptación para la nueva réplica, no una afirmación de que toda versión histórica ya los haya pasado en todos los equipos.

## Estados e informe obligatorio

Usa estados por requisito: **VERIFICADO**, **PENDIENTE**, **FALLA** o **IMPEDIDO**, con su evidencia. Los últimos tres impiden aprobar el conjunto. No uses casillas vacías ni omitas filas.

El informe privado del destino debe identificar referencia, equipo/firmware/API, apps, control, configuración, respaldo, recuperación, matriz completa y trabajo pendiente. Mantén los datos individuales fuera de Git. El estado global será:

- **REPLICA_VERIFICADA:** todos los requisitos tienen evidencia suficiente y están VERIFICADOS. Indica el alcance del panel; no lo presentes como clonación del firmware de la marca.
- **EN_CURSO / INCOMPLETA:** queda trabajo, falta un recurso o no se ha podido comprobar una función. No entregar como réplica terminada.
- **INCOMPATIBLE:** una restricción demostrada impide alcanzar un requisito con el equipo y alcance disponibles. Identifica exactamente el requisito y la evidencia; no afirmar que funcionará solo porque el dispositivo tenga Android TV.

Un informe del instalador como `configured-with-limitations` o `configured-not-physically-validated` describe una etapa técnica y **no autoriza REPLICA_VERIFICADA**. Un resultado parecido, una función desactivada o una alternativa ofrecida no cumplen el requisito original. Solo un cambio explícito del objetivo por el usuario permite otro resultado; debe identificarse como un objetivo distinto, no aprobar retrospectivamente la réplica original.
