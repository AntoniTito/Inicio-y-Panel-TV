# Trabajo en Z6 Panel

Lee README.md y docs/COMPATIBILIDAD.md. Usa z6.ps1 como interfaz común; no reconstruyas instrucciones de memoria ni ejecutes scripts históricos del equipo original sobre otro TV.

Consulta docs/VERSIONES.md: v0.10.4/android10 es la base anterior y v0.11.0 conserva la adaptación anterior y v0.12.0 conserva los fondos iniciales y v0.12.1 corrige su cobertura de borde a borde. v0.12.2 conserva la recepción confirmada. v0.13.0 conserva la biblioteca visual y fotos adaptadas. v0.14.0 conserva el rediseño anterior y v0.15.0 conserva modos en fila, fotos integradas, borrado sin papelera y elección de paisajes. La base anterior rediseñó Fondos con vista previa, aplicación directa y presentaciones seleccionadas. Selecciona la linea conforme al encargo; no muevas etiquetas existentes ni cambies la rama con trabajo pendiente. Cambiar de version en Git no actualiza ni restaura un dispositivo.

- La base vigente es 0.18.0/código 52. Lee docs/VERIFICACION-0.18.0.md para la evidencia física, separada de los emuladores. Fotos/recepción probadas en emuladores API 29/31; la superposición nativa de la imagen stock API 29 tiene una limitación registrada. La base histórica 0.10.4 y su firma están preservadas privadamente. No actualices el TV físico sin un encargo específico y validación de regresión.
- Para instalar, pide o descubre el identificador del destino y pásalo explícitamente. Empieza por `instalar -Simular` y `diagnosticar`. Ejecuta los cambios solo dentro del encargo del usuario.
- Conserva los respaldos iniciales e identidades; no restaures un respaldo de otro TV. No copies redes, autorizaciones ADB, cuentas ni temporizadores.
- No publiques privado/, .local/, claves, contraseñas ni datos personales. La firma original es necesaria para actualizar conservando datos; no la sustituyas.
- Ejecuta pruebas/verificar.ps1 para los cambios del instalador. No declares pruebas físicas o soporte de Android reciente sin evidencia.
- No deshabilites paquetes de modelos desconocidos por su nombre. Verifica un HOME funcional antes de retirar el anterior.

Los fondos son opcionales, solo de Home, y el receptor debe cerrarse al salir o caducar. Mantén la biblioteca y los enlaces QR fuera de Git; no transfieras IDs de imágenes entre equipos sin sus archivos. Consulta docs/FONDOS.md.

Las herramientas son las mismas para una persona y para un agente. No necesitan una suscripción de IA para instalar.

La entrega histórica 0.16.0 incorporó 12 paisajes diarios, rotación optativa y MediaStore. Usa WallpaperSource para leer archivos antiguos o URI; no reconstruyas rutas físicas para imágenes compartidas. Antes de migrar una actualización antigua, el instalador conserva preferencias, pone wallpaper_publish_hold, instala el lector compatible, respalda los bytes y libera la migración. No distribuyas esta marca interna. Las operaciones de inyección de fallos del proveedor solo admiten el emulador AOSP y siguen protegidas por UID shell y DUMP.

0.16.1/código 40 retira únicamente el botón de galería nativa de Fondos. Las imágenes siguen compartidas en MediaStore y la galería instalada conserva su estado. Recuperación 0.16.0/código 41, sin migración nueva.

0.16.2/código 42 conserva el bitmap durante onPause y lo libera en onStop o pantalla apagada. No vuelvas a borrar la imagen durante la pausa breve de un nuevo intent HOME. Las consultas y descargas siguen detenidas durante la pausa. Recuperación 0.16.1/código 43; tras usarla, una actualización necesita código 44 o superior.

El retorno a Home prepara la copia local antes del mantenimiento/red y difiere el primer dibujo hasta tener imagen o resolver su ausencia, con límite de 1000 ms. Mantén ese retorno al modificar el ciclo de vida. La corrección de entrada y retorno quedó confirmada por el usuario en el TV original.

0.16.3/código 44 omite mantenimiento y carga solo al reanudar un fondo visible validado y sin cambios, con caducidad comprobada al entrar de 60 segundos. Invalida por preferencias, eventos de MediaStore, día, resolución o memoria; no conserva bitmaps ocultos. El observador de Home solo invalida mientras está pausado y se retira en onStop. La rotación conserva su vencimiento y la preparación opcional del siguiente paisaje. Recuperación 0.16.2/código 45; después requiere código 46 o superior.

0.16.4/código 46 modifica solo los dos colores del foco de las tarjetas de Home. Relleno #48121A20, borde #FFE5F0F3; conserva geometría y lógica. Recuperación 0.16.3/código 47, actualización posterior código 48 o superior.

0.16.5/código 48 atiende primero la pila de páginas en Atrás: salir de Añadir app conserva la edición; Atrás en el panel inferior la cancela. No dibujes el panel inferior dejando una página hija activa. El puente key conserva sus códigos cortos y admite code:duración (hasta 1500 ms, pulsación larga solo OK/Atrás), siempre restringido a UID shell y DUMP. Recuperación 0.16.4/código 49; después requiere código 50 o superior.

0.17.0/código 50 separa las filas de PanelService mediante BottomNavigation: izquierda/derecha circulan solo dentro de su fila; arriba Apps → Acciones y abajo Acciones → Apps. IDs de paquete y de acción, memoria en proceso y actualización de los dos resaltados, sin preferencias por flecha. Edición solo Apps: reordenamiento acotado, arriba oculta, abajo no cambia de fila; al ocultar la última quedan Añadir/Guardar y se enfoca Guardar. Los menús hijos salen antes de cancelar edición. Se retiraron las ramas antiguas y vistas inaccesibles acordadas, conservando el auxiliar y Bluetooth. Recuperación 0.16.5/código 51; después requiere código 52 o superior.

0.18.0/código 52 incorpora TvRecovery y la ayuda optativa TvNavigationService. PanelService conserva canRetrieveWindowContent=false. Consulta TV-AUTOMATICA.md: app, firma, recorrido y tecla (incluido scan code) son privados y no se transfieren en replica. La ayuda solo consulta el paquete elegido durante una tarea. Para ListView valida el elemento seleccionado antes de cada flecha y OK; no sustituyas esto por secuencias temporizadas. El auxiliar 0.18.0-tv2 conserva autenticación por UID y limita TV_FOCUS/TV_NAV a la app elegible en primer plano (arriba, abajo y OK). No ejecutes uiautomator dump durante una tarea: UiAutomation desconecta temporalmente los servicios de accesibilidad y cancela la prueba. Antes de recuperar 0.17.0/c53, cancela la automatización y respalda la configuración privada con las herramientas comunes; después requiere código 54 o superior.

La deteccion ampliada de teclas solo admite funciones F1-F24 y colores. RemoteKeys lee entradas mediante Os.poll y una tuberia de cancelacion, sin sondeos ni procesos getevent persistentes. No extiendas esa captura a texto, navegacion o audio. Identidad de control sin MAC/serie. Antes de restaurar, tv_remote_stop debe confirmar el cierre; no desconectes el auxiliar dejando una escucha activa.
