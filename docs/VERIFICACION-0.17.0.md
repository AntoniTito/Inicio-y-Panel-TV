# Verificación de Z6 Panel 0.17.0

Entrega del 7 de noviembre de 2026: **0.17.0/código 50**, mismo APK firmado para Android 10/API 29 y Android 12/API 31.

## Cambio

Apps y Acciones tienen navegación circular horizontal independiente. Arriba pasa de Apps a Acciones; abajo de Acciones a Apps. Cada fila conserva su selección por ID dentro del proceso, incluso al cerrar el panel. No se duplican tarjetas ni se escriben preferencias al pulsar flechas. El proceso comienza en Audio y con la primera app como selección inicial de Apps.

La edición permanece en Apps: izquierda/derecha reordenan con límites, arriba oculta, OK guarda y Atrás cancela. Al ocultar la última app quedan Añadir/Guardar y se enfoca Guardar. Los menús de Añadir e Instalar conservan la sesión y vuelven primero a su página anterior. Las opciones de añadir usan IDs de paquete.

`BottomNavigation` separa las selecciones de las filas. `PanelService` mantiene la edición y la pila de páginas. El resaltado actualiza solo la tarjeta anterior y la nueva. Se conserva la geometría, el reloj, el diseño y el código de Home y fondos.

Se retiraron las 15 ramas antiguas sustituidas por `buildModern`, las vistas sin entrada `connections`, `wifiScan`, `screenProfiles`, `appinfo`, `btLocation` y `localControl`, sus ayudantes de Wi-Fi/brillo inutilizados, el callback `trimRecentTasks` y la bandera `frozen`. El inicio autónomo y el servicio auxiliar, Bluetooth, permisos y preferencias siguen presentes.

## Pruebas disponibles

- Controlador puro Java: **19 comprobaciones** de vueltas, independencia, memoria, IDs estables, desaparición de apps, cero/una/50 entradas y límites de edición.
- API 31 emulado y TV original API 29: fila vacía, una app, ambas vueltas, memoria entre filas y aperturas, reordenamiento y cancelación, ocultar/guardar la última app, Añadir, regreso desde Instalar, añadir/cancelar y 16 páginas activas conservadas. Preferencias restauradas y hashes de imágenes iguales al inicio. Informes `resultados-carrusel-0.17.0-api29.json` y `api31.json`.
- API 31: seis aplicaciones sintéticas adicionales generaron una fila mayor que el ancho disponible. Los extremos quedaron visibles; desinstalar la seleccionada conservó la posición válida más cercana. Se retiraron todos los paquetes sintéticos y se restauraron las preferencias originales. Capturas de comprobación locales fuera de Git.
- Asistente: **58 comprobaciones con ADB simulado**, incluidas selección de recuperación, hashes, rechazo de downgrade, instalación, restauración, permisos y protección de imágenes. No contacta al TV físico.
- Compilación desde la carpeta vigente y firma original verificadas. La recompilación del árbol final produjo el mismo SHA-256 del APK instalado.
- Respaldo privado del APK anterior, preferencias y bytes de las imágenes del TV verificados antes de actualizar. Se instaló con `install -r`, sin desinstalar ni reaplicar el perfil. El auxiliar respondió PING tras la actualización.
- Las preferencias del AVD se restauraron y el emulador iniciado para estas pruebas se apagó.

## Rendimiento

Tres rondas equivalentes de 60 movimientos entre Audio y Wi-Fi mediante el puente protegido. Cada ronda tuvo al menos 61 cuadros, suficientes para informar el p95 de `gfxinfo`.

| Entorno | p95 mediano antes → después | Límite aceptado | CPU mediana por ronda antes → después |
|---|---|---|---|
| TV original API 29 | 14 → 14 ms | 22 ms | 660 → 740 ms |
| Emulador API 31 | 17 → 17 ms | 25 ms | 220 → 260 ms |

No hubo escaneos del catálogo ni reconstrucciones por flecha. El p95 corresponde a duración de cuadros, no a latencia entre pulsación y pantalla. La CPU es tiempo agregado del proceso durante toda la ronda, incluido trabajo de fondo: no representa latencia de una tecla ni demuestra ahorro de CPU. En el TV, el auxiliar estaba desconectado en la referencia y se recuperó después; no se atribuye la diferencia de CPU exclusivamente a la navegación. Los valores completos están en `pruebas/resultados-rendimiento-panel-0.17.0.json`.

Tras cinco ciclos de calentamiento y 20 aperturas/cierres, sin reiniciar el proceso:

| Entorno | PSS inicial → final | Variación | Límite |
|---|---|---|---|
| TV original API 29 | 33415 → 35762 KB | +2347 KB, estabilizado en las últimas muestras | <8192 KB |
| Emulador API 31 | 37132 → 34720 KB | −2412 KB | <8192 KB |

Son muestras de estas sesiones, sin deducir ausencia universal de fugas ni consumo durante reproducción. Informes `pruebas/resultados-memoria-panel-0.17.0-api29.json` y `api31.json`.

## Control físico y límites

El 7 de noviembre de 2026 el usuario confirmó en el TV original, con video reproduciéndose y su control físico, que funcionan las vueltas de Apps y Acciones, el cambio de fila con arriba/abajo, la flecha sostenida y edición → Añadir app → Atrás. Confirmó que la edición permanece en Apps, el video continúa sin bloqueos y todo responde con fluidez. También confirmó que volver a Inicio desde Panel e inicio y desde Ajustes de Android sigue sin parpadear. Es evidencia manual comunicada por el usuario, separada de las pruebas automáticas y de las confirmaciones históricas.

En este TV las 17 flechas inyectadas con `input dpad keyevent` no llegaron al panel: cero actualizaciones del foco y selección intacta. Ese ensayo nativo permanece inconcluso, no aprobado, en `pruebas/resultados-rafaga-panel-0.17.0-api29.json`; la confirmación con el control físico no cambia su resultado. No se deduce continuidad de video de los eventos del puente.

La etiqueta fija `v0.17.0` conserva el informe inicial, que señalaba esta confirmación como pendiente. Un commit posterior registra la confirmación; la etiqueta no se mueve. No cambió el APK.

No se ejecutaron apagado, reinicio, borrado de fotos, cambios de red ni alteraciones HDMI. El alcance es navegación del panel y conservación de sus menús; no es una certificación de otros TV Box, reproducción 4K o todos los firmwares.

## Recuperación y entrega

Recuperación **0.16.5/código 51**, firma original y selección común para API 29/31. `classes.dex` coincide exactamente con el APK histórico 0.16.5: `BBBBAAC7616D591075578467148407F2F98249C2450E79FB7F56770DE39264A2`. Su equivalencia y selección se verificaron sin instalarla en el TV. Una actualización posterior a la recuperación requiere **código 52 o superior**.

| Archivo | SHA-256 |
|---|---|
| Z6-Panel-0.17.0.apk | `00A5E17305D4929E038DF484987118E04C6CC35C46B4EE649F1A587630911217` |
| Z6-Panel-recuperacion-0.16.5-c51.apk | `12D4E1A2095E1852E4349015D6C67777AC448D8B685FAD59E18FF073367D3CD1` |

Certificado SHA-256: `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`. El portable incluye solo estos dos APK y los archivos del proyecto aptos para Git; SDK, fotos, preferencias, identidades, claves, respaldos y compilaciones de diagnóstico quedan fuera. Las etiquetas anteriores se conservan sin moverlas.

## Repetir las comprobaciones

Desde la raíz del proyecto, sustituir `SERIAL` por un destino autorizado; los ensayos de edición restauran las preferencias al terminar:

```powershell
node pruebas/carrusel-panel-0170.cjs SERIAL
node pruebas/rendimiento-panel-0170.cjs SERIAL antes
# Instalar la actualización verificada antes de la muestra posterior.
node pruebas/rendimiento-panel-0170.cjs SERIAL despues
node pruebas/memoria-panel-0170.cjs SERIAL
powershell -File pruebas/verificar.ps1
```

Para el catálogo largo, únicamente en un emulador: preparar los seis APK sin permisos de las fixtures con `pruebas/preparar-fixtures-carrusel.ps1`, después ejecutar `node pruebas/catalogo-carrusel-0170.cjs SERIAL .local/fixtures-carrusel`. El ensayo retira sus seis paquetes y restaura las preferencias. No instalar estas fixtures en el TV del usuario.
