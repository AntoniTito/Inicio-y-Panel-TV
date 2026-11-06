# Compatibilidad comprobada

La entrega vigente es 0.12.1/código 28. La base histórica 0.10.4 y la firma están preservadas. La adaptación se comprobó en Android TV 12 emulado; el TV físico no fue modificado.

| Entorno | Estado |
|---|---|
| TV Box original, Android 10/API 29 | Sigue en 0.10.4/código 22, con funciones y rendimiento previamente comprobados. |
| Otro Android 10/API 29 | Instalador admitido; regresión física de 0.12.0 pendiente en el destino. |
| Emulador Android TV 12/API 31 | 0.12.0: navegación, fondos, recepción y regresión comprobadas; ver resultados por grupo. |
| Emulador Android TV 10/API 29 r3 | Fotos y recepción comprobadas. Su compositor falla con ciertas ventanas; el fallo se reprodujo con 0.10.4. Auxiliar iniciado desde PC para la integración. No valida control físico ni autonomía. |
| Otro equipo Android 12/API 31 | Instalador admitido; las diferencias de firmware requieren validación propia. |
| API distintas de 29/31 | Diagnóstico disponible; instalación automática bloqueada hasta adaptación y validación. |

Android 12 usa permisos BLUETOOTH_SCAN/CONNECT y acceso a alarmas exactas. Bluetooth se administra mediante ajustes nativos; no se promete conexión A2DP directa con interfaces de Android 10. El auxiliar adapta las llamadas internas de Android 12 y continúa autenticando por UID. Home recupera una tarea existente y abre el panel; el foco se comprobó mediante eventos nativos del emulador. Las recuperaciones código 29 son específicas: 0.10.4 para API 29 y 0.12.0 para API 31. Ambas permiten también restaurar HOME y ajustes originales.

Mantener los valores propios del destino no significa inventar capacidades: Wi-Fi, Bluetooth, HDMI-CEC, suspensión, Home y recuperación autónoma dependen del firmware. Los ajustes nativos son la alternativa donde estén disponibles. No se eliminan confirmaciones de Android ni se promete apagar el televisor mediante CEC.

El perfil no aplica listas de paquetes del fabricante. Solo se deshabilita reversiblemente el launcher anterior después de probar que Z6 abre y verificar la selección HOME. Las aplicaciones externas se instalarán después por decisión del usuario; su reproducción, audio JBL, control físico y HDMI-CEC siguen pendientes en el emulador. Consulta VERIFICACION.md, FONDOS.md y EMULADOR-TV12.md.
