# Compatibilidad comprobada

La entrega vigente es 0.16.0/código 38. La base histórica 0.10.4 y la firma están preservadas. Hay evidencia separada del emulador API 31 y del TV original API 29 en [VERIFICACION-0.16.0.md](VERIFICACION-0.16.0.md); no equivale a compatibilidad universal.

| Entorno | Estado |
|---|---|
| TV Box original, Android 10/API 29 | Actualizado a 0.16.0/código 38: 22 comprobaciones, foto personal y paisaje activo respaldados antes de migrar; URI, bytes y hash conservados. Galería nativa visible, recepción LAN y borrado externo comprobados. Ver VERIFICACION-0.16.0.md. |
| Otro Android 10/API 29 | Instalador admitido; cada firmware de destino requiere su propia validación de 0.16.0. |
| Emulador Android TV 12/API 31 | 0.16.0: Fondos y medios compartidos, rotación por horario real, migración interrumpida, limpieza, recuperación y controles/reinicio comprobados. Recreación a 1080p; 4K conserva evidencia histórica. Ver VERIFICACION-0.16.0.md. |
| Emulador Android TV 10/API 29 r3 | Fotos y recepción comprobadas. Su compositor falla con ciertas ventanas; el fallo se reprodujo con 0.10.4. Auxiliar iniciado desde PC para la integración. No valida control físico ni autonomía. |
| Otro equipo Android 12/API 31 | Instalador admitido; las diferencias de firmware requieren validación propia. |
| API distintas de 29/31 | Diagnóstico disponible; instalación automática bloqueada hasta adaptación y validación. |

Android 12 usa permisos BLUETOOTH_SCAN/CONNECT y acceso a alarmas exactas. Bluetooth se administra mediante ajustes nativos; no se promete conexión A2DP directa con interfaces de Android 10. El auxiliar adapta las llamadas internas de Android 12 y continúa autenticando por UID. Home recupera una tarea existente y abre el panel; el foco se comprobó mediante eventos nativos del emulador. La recuperación vigente conserva el comportamiento 0.15.0 con código 39 y adaptador para MediaStore y la misma firma para API 29 y 31. El manifiesto selecciona ese APK para ambas API. Tras instalarla se necesita código 40 o superior para actualizar. La restauración permite también recuperar HOME y ajustes originales.

Mantener los valores propios del destino no significa inventar capacidades: Wi-Fi, Bluetooth, HDMI-CEC, suspensión, Home y recuperación autónoma dependen del firmware. Los ajustes nativos son la alternativa donde estén disponibles. No se eliminan confirmaciones de Android ni se promete apagar el televisor mediante CEC.

El perfil no aplica listas de paquetes del fabricante. Solo se deshabilita reversiblemente el launcher anterior después de probar que Z6 abre y verificar la selección HOME. Las aplicaciones externas se instalarán después por decisión del usuario; su reproducción, audio JBL, control físico y HDMI-CEC siguen pendientes en el emulador. Consulta VERIFICACION.md, FONDOS.md y EMULADOR-TV12.md.

## 0.12.2: recepción confirmada en el TV

Se muestran confirmación, contador de fotos y accesos a la vista previa y la biblioteca, sin aplicar automáticamente un fondo. El flujo del emulador API 31 tiene 18 comprobaciones específicas en pruebas/resultados-recepcion-0.12.2-api31.json. El alcance del TV físico y sus pendientes se registran en VALIDACION-TV-0.12.2.md. Los apartados anteriores describen las pruebas realizadas en cada entrega histórica.

## 0.13.0: biblioteca visual y calidad adaptada

La misma entrega admite API 29/31. La biblioteca usa ocho miniaturas por página y una caché de hasta 4 MB. Los límites de guardado son 720p/1 MB, 1080p/2 MB y 4K/4 MB, conservando una sola copia optimizada. Las verticales y cuadradas se muestran completas; las horizontales se encuadran para llenar el inicio. 4K es manual y requiere un inicio efectivo 3840×2160, equipo sin poca RAM, heap de al menos 192 MB y 96 MB libres antes de cargar. La salida de video 4K no garantiza una interfaz 4K. Consultar VERIFICACION-0.13.0.md para los resultados y pendientes físicos.

## 0.16.0: almacenamiento compartido

Se publican imágenes mediante MediaStore en API 29/31, sin acceso completo al almacenamiento ni root. Las copias originales privadas solo se retiran tras verificar la publicación. Si el proveedor de un firmware falla, se conserva el archivo privado y se muestra la publicación pendiente. El instalador habilita la galería AOSP si existe, con estado original respaldado; otros visores y su papelera dependen del firmware. La ausencia de visor no impide gestionar Mis fotos. Ningún perfil copia URI, índices, archivos personales ni estados de migración de otro TV.
