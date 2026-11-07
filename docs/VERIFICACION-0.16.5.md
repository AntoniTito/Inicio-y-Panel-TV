# Verificación de Z6 Panel 0.16.5

Entrega del 7 de noviembre de 2026: **0.16.5/código 48**, mismo APK para Android 10 y 12.

## Problema y corrección

Al volver de Añadir app durante la edición, Atrás cancelaba primero la edición y dibujaba el panel inferior, dejando `panelAddApp` en la pila de navegación. Las teclas seguían dirigidas a filas del menú ya oculto.

Ahora Atrás sale primero de la página hija. Al regresar queda seleccionado Añadir y la edición sigue activa. Una segunda pulsación de Atrás cancela la edición; otra cierra el panel. Volver usa la misma navegación. También se retiró un renderizado duplicado al cancelar desde el panel inferior.

Se conservan fondos, foco translúcido, optimizaciones, permisos y preferencias. El puente de pruebas, protegido por UID shell y DUMP, admite pulsaciones pareadas con duración opcional de hasta 1500 ms; las largas se limitan a OK y Atrás. No espera esa duración en el hilo de interfaz, ni añade botones, servicios o permisos.

## Evidencia

- API 31: se reprodujo el comportamiento anterior de Atrás en una compilación aislada para el emulador, conservando el puente de pruebas. El estado mostraba `panelAddApp` activo, edición cancelada y título inferior vacío. Esa compilación de diagnóstico permanece fuera de entregas y Git.
- API 29 físico y API 31 emulado: retorno al panel de edición con selección restaurada, flecha izquierda, reapertura del selector, botón Volver, regreso desde Instalar aplicaciones, añadir una app y cancelar restaurando su visibilidad, navegación normal y cierre. Preferencias y hashes de las imágenes iguales al inicio de cada prueba. Informes `pruebas/resultados-retorno-anadir-0.16.5-api29.json` y `api31.json`.
- Las pruebas automáticas ejercitan el manejador de teclas mediante el puente protegido con una aplicación nativa de ajustes detrás. La reproducción real con el control físico se verifica por separado. Home inyectado por ADB tiene un comportamiento distinto del control en este firmware y no se toma como prueba de continuidad de video.
- Duraciones fuera de límite o largas sobre flechas rechazadas. Asistente: 56 comprobaciones con ADB simulado.
- Las preferencias originales del emulador se restauraron. Se respaldaron APK, preferencias y hashes del TV antes de actualizar, sin reaplicar el perfil. No se repitieron pruebas de rendimiento o imágenes ajenas a esta navegación.

## Recuperación

Recuperación **0.16.4/código 49**, firma original y misma selección para API 29/31. `classes.dex` idéntico al APK histórico: `FB00FE7A46DFBF5F65801E150354A17959F9DFD98186AB6D5C6FDB6CC164DD12`. Se verificó equivalencia sin instalar la recuperación en el TV. Después de utilizarla, una actualización requiere código 50 o superior.

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.16.5.apk | `018429192EEBD4FF1258B7D2D35359C07E15D0986A430731232564C91176F007` |
| Z6-Panel-recuperacion-0.16.4-c49.apk | `025D24B9C103C6949273F7F7F2EA84E902E62B1578FCB52E3B4EA205EE7823FA` |

Certificado SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`. Portable y Git excluyen fotos, preferencias, claves, respaldos y la compilación de diagnóstico.
