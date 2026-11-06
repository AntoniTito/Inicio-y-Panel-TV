# Compatibilidad real de la base conservada

Esta consolidación preserva 0.10.4, código 22. No modifica sus fuentes Java ni añade soporte que aún no está comprobado.

| Entorno | Estado |
|---|---|
| TV Box original, Android 10/API 29 | Base funcional previamente comprobada. Esta consolidación no modifica el TV. |
| Otro Android 10 | Instalador disponible; permisos, control físico y arranque deben verificarse en ese firmware. |
| Android 11 | Diagnóstico disponible; pendiente de adaptación y validación. |
| Android 12–16 | Diagnóstico disponible; instalación automática bloqueada para no presentar un APK antiguo como adaptado. |
| Emuladores | Pruebas físicas de Bluetooth/HDMI no sustituibles. No se ejecutaron emuladores durante la consolidación. |

La conexión A2DP directa usa compatibilidad específica de API 29. Las versiones nuevas requieren BLUETOOTH_SCAN/CONNECT, manejo de permisos y nuevas restricciones de activar/desactivar Bluetooth. El temporizador necesita declarar y gestionar acceso a alarmas exactas en Android reciente. El auxiliar usa interfaces internas cuyo transporte y firma pueden variar. Son tareas para una actualización separada y con recuperación firmada.

Mantener los valores propios del destino no significa inventar capacidades: Wi-Fi, Bluetooth, HDMI-CEC, suspensión, Home y recuperación autónoma dependen del firmware. Los ajustes nativos son la alternativa donde estén disponibles. No se eliminan confirmaciones de Android ni se promete apagar el televisor mediante CEC.

El perfil no deshabilita paquetes del fabricante. La lista histórica sirve para estudiar el modelo original; aplicarla automáticamente a otro equipo puede afectar servicios necesarios.
