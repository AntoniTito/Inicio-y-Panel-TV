# Trabajo en Z6 Panel

Lee README.md y docs/COMPATIBILIDAD.md. Usa z6.ps1 como interfaz común; no reconstruyas instrucciones de memoria ni ejecutes scripts históricos del equipo original sobre otro TV.

Consulta docs/VERSIONES.md: v0.10.4/android10 es la base anterior y v0.11.0 conserva la adaptación anterior y v0.12.0 conserva los fondos iniciales y v0.12.1 corrige su cobertura de borde a borde. v0.12.2/main mejora la confirmación y navegación de la recepción de fotos. Selecciona la linea conforme al encargo; no muevas etiquetas existentes ni cambies la rama con trabajo pendiente. Cambiar de version en Git no actualiza ni restaura un dispositivo.

- La base vigente es 0.12.2/código 30. Lee docs/VALIDACION-TV-0.12.2.md para la evidencia física, separada de los emuladores. Fotos/recepción probadas en emuladores API 29/31; la superposición nativa de la imagen stock API 29 tiene una limitación registrada. La base histórica 0.10.4 y su firma están preservadas privadamente. No actualices el TV físico sin un encargo específico y validación de regresión.
- Para instalar, pide o descubre el identificador del destino y pásalo explícitamente. Empieza por `instalar -Simular` y `diagnosticar`. Ejecuta los cambios solo dentro del encargo del usuario.
- Conserva los respaldos iniciales e identidades; no restaures un respaldo de otro TV. No copies redes, autorizaciones ADB, cuentas ni temporizadores.
- No publiques privado/, .local/, claves, contraseñas ni datos personales. La firma original es necesaria para actualizar conservando datos; no la sustituyas.
- Ejecuta pruebas/verificar.ps1 para los cambios del instalador. No declares pruebas físicas o soporte de Android reciente sin evidencia.
- No deshabilites paquetes de modelos desconocidos por su nombre. Verifica un HOME funcional antes de retirar el anterior.

Los fondos son opcionales, solo de Home, y el receptor debe cerrarse al salir o caducar. Mantén la biblioteca y los enlaces QR fuera de Git; no transfieras IDs de imágenes entre equipos sin sus archivos. Consulta docs/FONDOS.md.

Las herramientas son las mismas para una persona y para un agente. No necesitan una suscripción de IA para instalar.
