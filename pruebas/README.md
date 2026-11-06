# Repetir las comprobaciones

Estas pruebas modifican temporalmente un emulador, sus preferencias y archivos sintéticos. No se ejecutan en un TV físico: los scripts comprueban serial, propiedad qemu y API. Guarda un snapshot antes de repetirlas y utiliza un AVD de pruebas. Los resultados distinguen API 29/31 y no certifican Bluetooth, USB, teléfono físico ni reproducción de apps externas.

## Asistente

`powershell -File .\pruebas\verificar.ps1` utiliza ADB falso y no contacta ningún dispositivo. Comprueba instalación, identidad, hashes, permisos, selección de HOME y recuperación específica por API.

## Aplicación y navegador

Requisitos: APK actual instalado, acceso ADB autorizado, servicio de accesibilidad activo, Node y Chromium para las pruebas del navegador. Las dependencias de pruebas no forman parte de la aplicación:

```powershell
npm install --prefix .local\browser-test playwright-core@1.65.0 --ignore-scripts --no-audit --no-fund
$env:Z6_TEST_CHROMIUM = 'RUTA_REAL\chrome.exe'
node pruebas\fondos.cjs emulator-5554
node pruebas\biblioteca.cjs emulator-5554
node pruebas\galeria.cjs emulator-5554
```

Fondos genera imágenes sintéticas para el resto de pruebas. El navegador es una instancia de prueba separada; no se utiliza la sesión personal. Se reenvía un puerto ADB solo a localhost y se retira al terminar. No se implementó un puente desde el celular al emulador.

En la imagen stock Android TV 10 r3, los paneles pueden abortar SurfaceFlinger, también con 0.10.4. Allí se ejecutó `node pruebas\fondos.cjs emulator-5556 --photos-only`, iniciando la pantalla de fondos mediante la interfaz ADB protegida. Consulta limitaciones-api29-0.12.0.json. No se da por aprobada la navegación nativa ni la autonomía en esa imagen.

## Edición y eventos de paquetes

ControlHold.java inyecta una pulsación real de 850 ms como shell, exclusivamente en emuladores. Antes de ejecutar edicion.cjs, compílalo con el JDK y SDK del proyecto:

```powershell
New-Item -ItemType Directory -Force .local\native-tests\classes
& "$env:JAVA_HOME\bin\javac.exe" --release 8 -classpath .local\sdk\platforms\android-35\android.jar -d .local\native-tests\classes pruebas\ControlHold.java
& "$env:JAVA_HOME\bin\jar.exe" cf .local\native-tests\classes.jar -C .local\native-tests\classes .
& .local\sdk\build-tools\35.0.0\d8.bat --min-api 29 --lib .local\sdk\platforms\android-35\android.jar --output .local\native-tests\hold.jar .local\native-tests\classes.jar
.\herramientas\compilar.ps1 -Proyecto (Join-Path $PWD 'pruebas\fixture-app') -Salida (Join-Path $PWD '.local\native-tests\fixture.apk')
node pruebas\edicion.cjs emulator-5554
```

La app de prueba no solicita permisos y se desinstala al terminar. Su compilación usa la firma privada local del propietario, igual que el compilador existente. La firma no se distribuye en Git.

## Regresión y rendimiento

`pruebas/emulador-api31.ps1 -Dispositivo emulator-5554 -Reiniciar` comprueba controles, suspensión y reinicio. Interrumpe temporalmente el emulador.

`pruebas/rendimiento-fondos.ps1` mide tres rondas por modo y 20 ciclos de memoria. Necesita fotos sintéticas importadas y una referencia tomada en el mismo equipo y configuración. No ejecutes otros emuladores ni cargas intensivas simultáneamente. referencia-emulador-api31-0.11.0.json conserva la referencia de esta entrega; no la uses para afirmar mejoras en otro hardware. El informe incluye transporte ADB y no mide latencia aislada de entrada a imagen.

Los scripts restauran las preferencias guardadas y retiran conexiones temporales. Fondos conserva copias sintéticas para las pruebas posteriores; usa el snapshot para limpiar un AVD de pruebas al terminar. En esta entrega se eliminaron esas copias y se restauraron las preferencias originales antes de dejar el emulador al usuario.

## Rediseño 0.14.0

`node pruebas/fondos-0140.cjs emulator-5554` prueba foco/aplicación, navegación, selección y orden parcial, borrado, recepción, importación parcial, permiso denegado, caché diaria y recreación 1080p/4K. Exige una biblioteca sin fotos personales; elimina solo las imágenes sintéticas creadas y restaura preferencias, tamaño y permiso de lectura.

`node pruebas/rendimiento-0140.cjs emulator-5554 before|after` repite tres rondas nativas por modo y 20 ciclos. Guarda preferencias privadamente antes de empezar. Una comparación necesita la versión anterior y la nueva bajo las mismas condiciones: repetir si cambian la carga del PC o la configuración del emulador. Incluye proceso ADB y transporte; no equivale a medir pulsación a imagen. No compilar ni instalar durante una ronda.

`node pruebas/galeria-0140.cjs emulator-5554` cubre nueve fotos, páginas de ocho, Organizar, movimiento a otra página, cancelación, OK sostenido real y Eliminar todas. Requiere el helper ControlHold y biblioteca de pruebas vacía. `--timing-only` repite los tiempos sin sobrescribir el informe de memoria.

`node pruebas/paisaje-0140.cjs emulator-5554` prueba fallo de descarga y paisaje conservado sin conexión. Desactiva únicamente el Wi-Fi del emulador, confirma que no hay red activa y lo restaura al terminar. Requiere un paisaje guardado previamente. No ejecutarlo en hardware del usuario.
