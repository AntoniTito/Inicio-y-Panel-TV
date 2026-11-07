# Historial funcional resumido

- Inicio: auditoría de hardware, procesos y aplicaciones; limpieza específica del firmware original, con estados para recuperación.
- 0.4–0.6: inicio propio como HOME, deshabilitación reversible del launcher anterior, personalización, clima y primeros controles mediante auxiliar ADB.
- 0.10.0: ocho apartados de Ajustes; aplicación actual, mantenimiento, temporizador, diagnóstico y recuperación autónoma comprobada en el firmware original.
- 0.10.1: Binder persistente por eventos, recuperación al morir, catálogo e iconos reutilizados, actualizaciones parciales y operaciones lentas fuera del hilo de interfaz.
- 0.10.2–0.10.3: reloj y fecha opcionales, finalmente en panel superior derecho con letras grises.
- 0.10.4: menor altura inferior, manteniendo iconos, botones, reloj superior y preferencias.
- Consolidación: una carpeta independiente, instalador Windows y pruebas locales; sin nueva versión de aplicación ni modificación del firmware.
- 0.11.0/código 24: adaptación en emulador Android TV 12, permisos Bluetooth y alarmas, auxiliar compatible, Home sobre tarea existente y navegación nativa. Se mantienen reloj gris, altura compacta y optimizaciones. Recuperación histórica 0.10.4/código 25 solo para API 29. El TV físico permanece en 0.10.4.

Los datos de auditoría advierten que los valores declarados por un firmware genérico no certifican por sí solos el fabricante ni la memoria física. Tampoco se consideran malware o publicidad los paquetes solo por sus nombres. Por eso no se traslada automáticamente la limpieza histórica a otro modelo.

- 0.12.0/código 26: acceso a añadir apps sin elementos visibles, biblioteca privada de fondos, presentación, paisajes Wikimedia y recepción temporal por QR. Recuperaciones específicas código 27 y evidencia separada por API.

- 0.12.1/código 28: fondo de borde a borde en una capa independiente; controles con los mismos márgenes. Recuperaciones código 29 y prueba de píxeles en las cuatro esquinas.

- 0.12.2/código 30: confirmación de recepción y contador en el TV, vista previa directa, terminar en biblioteca y navegación Atrás por nivel. Recuperación común 0.12.1/código 31 para API 29/31.

- 0.13.0/código 32: biblioteca visual, borrado múltiple confirmado y fotos privadas adaptadas a 720p/1080p/4K, con comparación a pantalla completa y protección de memoria. Recuperación 0.12.2/código 33.

## 0.14.0

Fondos del inicio pasa de una lista a vista previa y controles, con foco independiente del modo aplicado. OK aplica una elección preparada; Mis fotos usa la foto directamente. Presentación permite Todas o Elegidas y orden reversible sin alterar posiciones ajenas. Recepción muestra miniatura y aplicación explícita. La importación alternativa usa miniaturas, selección múltiple y resultados por archivo que sobreviven a recreaciones. Resolución del fondo distingue el inicio del archivo y de la capacidad del televisor. Se conserva un solo código para API 29/31, las fotos y la firma. Recuperación 0.13.0/código 35 con protección de selección parcial. Evidencia y límites en VERIFICACION-0.14.0.md.

## 0.15.0

Modos en una sola fila y gestión de fotos junto a la vista previa. Foto fija aplica con OK; Presentación permite marcar sin salir. El borrado verifica archivo y miniatura, impide regenerarlas tras eliminar y comunica el espacio liberado. Elegir otro paisaje consulta hasta 30 opciones de Wikimedia con seis miniaturas visibles; solo descarga la imagen completa elegida. Sin nuevas dependencias ni procesos periódicos. Recuperación 0.14.0/código 37.

- 0.16.0/código 38: catálogo de hasta 12 paisajes estable por día, miniaturas persistentes, descarga progresiva, rotación por horario real y conservación permanente. Álbumes compartidos mediante MediaStore, migración por hash, reconciliación de borrados externos y respaldo por bloques. Recuperación 0.15.0/código 39 con adaptador de almacenamiento; una actualización posterior requiere código 40.

- 0.16.1/código 40: retira el botón de galería nativa de Fondos, conservando Mis fotos, MediaStore y los controles diarios. Recuperación 0.16.0/código 41.
