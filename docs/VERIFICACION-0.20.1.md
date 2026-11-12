# Revisión completa de Mi control — 0.20.1/c60

Un APK para API 29/31, con firma original. Recuperación 0.20.0/c61: commit histórico `96c622fd53cbdcef371f373905fce630fc34a211`, con el único arreglo adicional de declarar Mi control singleTask. Después se necesita c62 o superior. Los hashes finales están en el manifiesto.

## Fallos encontrados

El usuario confirmó que 0.20.0 ya guardaba, pero no conseguía volver a Mi control y encontraba instrucciones de canal al configurar otra app. El acceso se reprodujo desde el panel real del TV: la llamada era aceptada, pero seguía mostrándose Home. `dumpsys activity` mostró Mi control como raíz de una tarea con Home encima; lanzar solo con NEW_TASK recuperaba la actividad superior. El mismo problema no apareció en la disposición inicial de tareas del emulador; esa primera muestra no sustituye la evidencia física.

Las pruebas anteriores habían abierto Mi control directamente y no cubrieron ese regreso desde el panel con esa disposición de tareas. La nueva prueba entra por Aplicaciones → Mi control, vuelve a Home y repite la entrada, incluyendo un diálogo pendiente de detección. Las entradas de la app usan ahora un destino común con CLEAR_TOP/SINGLE_TOP y la actividad singleTask; al volver se retiran los diálogos anteriores y se presenta la página principal.

La eliminación existía dentro de diálogos anidados y no se había comprobado todo su flujo. Ahora Destinos guardados tiene lista y acciones visibles. Se comprueba cancelación, borrado solo de biblioteca y borrado con usos: este último conserva los botones como reapertura normal, desactiva el arranque coincidente y elimina únicamente el borrador de la misma ruta. Hay estado vacío y Volver accesible.

Cambiar de app ya no conserva la acción de recorrido de otra variante. Abrir/reabrir sin recorrido y elegir una sección opcional están separados; la selección de destinos de un botón filtra por app y firma. El encendido normal sin recorrido no necesita asistencia de navegación ni conexión de red. Los textos del asistente describen pantallas/secciones.

La prueba de una biblioteca sin video encontró una referencia inestable: se elegía un título que incluía el PID y cambiaba al reabrir. La elección de referencias nombradas prefiere ahora identificadores de vista estables y coincidencias exactas; mantiene el comportamiento anterior para superficies de video. No se relajan las comprobaciones de firma, coincidencias ambiguas ni el alcance de accesibilidad.

## Evidencia

Los JSON de resultados identifican el APK ejecutado. Se registran por separado las pruebas de la app sintética en API 31, las del TV API 29, el asistente simulado y las comprobaciones que requieran al usuario. No se extrapola reproducción comercial ni control físico desde el emulador.

- API 31: 20 comprobaciones de la revisión (`revision-control-0201.cjs`), 25 del control y recorridos (`mando-0190.cjs`) y 20 de persistencia/reutilización (`destinos-0200.cjs`). Todas pasaron con el APK registrado en sus informes. Incluyen confirmación real de una sección sin video y apertura normal al arrancar con la asistencia deshabilitada.
- Windows: 79 comprobaciones con ADB simulado, sin contactar hardware. Certificado original, hashes, versiones, respaldo y recuperación seleccionados comprobados. La recuperación c61 se inspeccionó: versión 0.20.0, código 61 y Mi control singleTask; no se instaló en el TV.
- Memoria API 31: cinco ciclos de calentamiento y veinte aperturas/cierres más, PSS 38 473 → 31 063 KB, consultas de navegación estables en reposo. Es una muestra, no una comparación controlada de velocidad con la versión anterior ni una medida de cuadros.
- API 29: el fallo de acceso se reprodujo en 0.20.0 y el usuario confirmó el guardado de la ruta. El puerto ADB se cerró antes de instalar 0.20.1. **Actualización y comprobaciones físicas de la corrección pendientes de reactivar la conexión del PC.** No se declara instalado el arreglo en ese equipo.

Se preservan fotos, claves, destinos y asignaciones antes de instalar. Los datos individuales y capturas quedan en `privado/`. No hay nuevas tareas periódicas, lectores de entrada, dependencias gráficas ni consultas del catálogo por cada flecha; en el gestor solo cambia el texto de detalle al mover el foco. No se atribuyen métricas de versiones anteriores a esta entrega.
