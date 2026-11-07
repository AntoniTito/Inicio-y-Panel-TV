# Emulador Android TV 12

Se configuró el AVD existente Television_4K, Android 12/API 31/x86, usando Z6 Panel 0.12.0/código 26 y la firma original. La resolución efectiva quedó en 1280×720 con densidad 213, idioma español de Perú y zona America/Lima. El AVD original usa 3840×2160 y densidad 640. No se modificó el TV Box físico.

## Uso

1. Inicia el AVD con `.\herramientas\iniciar-emulador.ps1 -Avd Television_4K -ZonaHoraria America/Lima`.
2. Home abre el panel sobre la tarea anterior; Inicio abre la pantalla completa de Z6.
3. En Ajustes → Panel e inicio puedes activar o desactivar el reloj y fecha superiores.
4. Instala tus aplicaciones después; YouTube FOR y Xuper no se incluyeron por solicitud del usuario.

El auxiliar se recuperó después de un reinicio real del emulador sin lanzarlo desde el PC. Si Android solicita autorización local, marca Permitir siempre en el diálogo del propio emulador. Se corrigió un problema del almacén de claves de esta imagen mediante la API autorizada de Android, sin root. El reparador específico `herramientas/reparar-autorizacion-emulador.ps1` solo admite este tipo de emulador API 31 y debe usarse únicamente si una clave previamente autorizada no persiste; `-Simular` no realiza cambios.

Esta imagen anunciaba `vendor.qemu.timezone=Unknown/Unknown` y volvía a GMT al reiniciar, incluso con zona automática desactivada. Se debe pasar la zona al arrancar el emulador mediante la opción oficial [-timezone](https://developer.android.com/studio/run/emulator-commandline). El lanzador anterior hace un arranque completo con `-no-snapshot-load`, sin borrar datos ni snapshots. Cierra primero ese AVD si ya está abierto. Si lo abres directamente desde Android Studio y vuelve a GMT, ciérralo y utiliza el lanzador. En otro país, sustituye America/Lima por su zona. Esto es configuración del emulador y no modifica el reloj del PC ni se fuerza en otros TV Box.

## Diagnóstico y recuperación

```powershell
.\z6.ps1 diagnosticar -Dispositivo emulator-5554
.\z6.ps1 restaurar -Dispositivo emulator-5554
.\z6.ps1 instalar -Dispositivo emulator-5554 -SoloPanel
```

Sustituye el serial si cambia. Restaurar recupera el launcher y los ajustes gestionados por el instalador; deja la app instalada. No restablece por sí solo idioma, zona y resolución, ajustados manualmente para esta réplica.

Antes de los cambios se guardó el snapshot `z6_antes_api31` y el estado original bajo `privado/emuladores`. Para regresar al estado completo anterior, carga ese snapshot desde el emulador. Los respaldos y autorizaciones son propios de este destino y no deben distribuirse.

También se guardó el snapshot `z6_antes_0120`, con 0.11.0, antes de incorporar los fondos. La guía FONDOS.md describe las funciones nuevas y las pruebas del navegador del PC. No se preparó un puente de envío desde el celular al emulador.

## Evidencia y límites

`pruebas/resultados-api31.json` registra 27 comprobaciones aprobadas: Home, foco, ocho secciones, reloj, volumen, ruta Bluetooth nativa, temporizadores, suspensión y recuperación tras reinicio. `pruebas/emulador-api31.ps1 -Dispositivo emulator-5554 -Reiniciar` reproduce la prueba e interrumpe temporalmente el emulador.

No se comprobaron reproducción en YouTube/Xuper, audio Bluetooth físico, control remoto físico ni HDMI-CEC. Las cifras históricas de rendimiento pertenecen al TV original y no se atribuyen al emulador.

## Actualización 0.14.0

En esa entrega el emulador quedó con 0.14.0/código 34, la pantalla Fondos rediseñada y un solo APK compartido con API 29. Se mantuvieron configuración y preferencias. La biblioteca de pruebas queda sin fotos temporales. Se repitieron las 27 comprobaciones de controles, suspensión y reinicio; evidencia y límites en VERIFICACION-0.14.0.md.

## Actualización 0.15.0

Instalado 0.15.0/código 36 con la misma firma y APK del TV original. Incluye modos en fila, gestión integrada, borrado sin papelera y elección de otros paisajes con encuadre nítido. Veintitrés comprobaciones de Fondos y 27 de controles/reinicio aprobadas; las pruebas eliminaron sus propias fotos sintéticas. La comparación de navegación y los 20 ciclos de memoria están en [VERIFICACION-0.15.0.md](VERIFICACION-0.15.0.md). La prueba final de tres paisajes distintos aplicó los tres. No se añadió un puente del celular al emulador.

## Actualización 0.16.0

Catálogo diario persistente de doce imágenes, rotación opcional y MediaStore, con la misma firma y APK para API 29/31. Se conservan los datos del AVD; no se borraron snapshots previos. La recepción sigue probándose desde el PC por ADB, sin puente al celular. Las pruebas nuevas y sus límites están en VERIFICACION-0.16.0.md. Las carpetas compartidas requieren un visor instalado para abrirse fuera de Z6; no se instala una galería de terceros automáticamente.
