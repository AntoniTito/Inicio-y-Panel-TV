# Un solo prompt para replicar el entorno

Copia el siguiente texto al agente que abra la descarga del proyecto. El procedimiento completo está en [REPLICAR-ENTORNO.md](REPLICAR-ENTORNO.md); no depende de este chat ni de archivos privados del equipo original.

```text
Instala y configura el entorno vigente de Z6 Panel en el Android TV de destino, conservando su diseño, funciones y optimizaciones. No desarrolles otra app ni cambies el APK para instalarlo.

1. Lee AGENTS.md, README.md, docs/REPLICAR-ENTORNO.md y COMPATIBILIDAD.md. Usa main y entregas/manifest.json para identificar el APK firmado y su recuperación; no elijas android10 ni una versión histórica por el Android del equipo. La entrega de referencia es 0.20.1/c60, común a API 29/31.
2. Prepara ADB si falta, identifica inequívocamente el destino y autoriza ese PC. Consigue los APK de la misma entrega si la descarga de Git no los incluye. Valida hashes y firma. Empieza por z6.ps1 instalar -Dispositivo DESTINO -Perfil replica -Simular y diagnosticar; las API distintas de 29/31 requieren adaptación y validación antes de instalar.
3. Revisa los paquetes reales de las apps elegidas. Si hay otra variante, adapta una copia local del perfil, sin heredar rutas por el nombre. Ejecuta instalar con el perfil correcto y -SoloPanel si las apps se instalarán después; si se suministran APK coincidentes, usa -CarpetaApks. Conserva el respaldo inicial del destino y comprueba Z6 como HOME antes de retirar reversiblemente el launcher anterior.
4. Completa la configuración descrita en REPLICAR-ENTORNO.md: apps visibles y orden, reloj y aspecto, nombre, fondo, permisos y auxiliar. No copies cuentas, redes, autorizaciones, URI de fotos, borradores, códigos del control ni respaldos del original. No cambies HDMI o paquetes de otros fabricantes por suposición.
5. Configura Mi control y arranque en el nuevo equipo: identifica cada tecla y su etiqueta; el acceso de TV reabre la app y llega al destino elegido, y el de YouTube cierra y reabre YouTube sin pedir canal. Para otras teclas, permite las acciones elegidas. Enseña y prueba una sola vez cada destino y reutilízalo en los botones y en arranque/despertar. Confirma los interruptores deseados. Usa la preparación nativa solo si se verifica el control compatible; no edites Generic.kl ni uses root.
6. Comprueba con el control físico el panel sobre video, edición/Añadir/Atrás, carrusel por filas, fondos sin parpadeo, permisos, botones, destinos guardados y borrado, arranque y despertar. Distingue las pruebas realizadas de las pendientes y las restricciones del firmware. No reinicies adbd para reparar el auxiliar ni exijas reactivar depuración para cada uso de un botón.
7. Entrega un informe privado por destino con versión, diferencias, pendientes, respaldo y restauración. No declares una réplica idéntica sin comprobarla. No publiques datos privados ni claves. No desinstales Z6 para recuperar: usa el restaurador y la recuperación de manifest.json; tras c61 una actualización requiere c62 o superior.

Solicita únicamente datos o acciones indispensables que no puedas resolver en el proyecto. Sigue hasta completar la configuración y las verificaciones autorizadas, sin repetir preguntas ya respondidas.
```

El catálogo diario, las fotos y la configuración individual no se distribuyen. El instalador, las funciones incluidas en el APK y las configuraciones que deben completarse se distinguen en la guía de réplica.
