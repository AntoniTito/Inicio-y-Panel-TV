# Replicar el entorno de Z6 Panel

Referencia: **0.20.1/código 60**, un APK para Android 10/API 29 y Android TV 12/API 31. El [prompt único](PROMPT-REPLICA.md) dirige a esta guía. Debes cumplir primero [CONTRATO-REPLICA.md](CONTRATO-REPLICA.md): instalar el panel no basta, y ninguna función pendiente permite aprobar la réplica. Las instrucciones no necesitan este chat; los recursos individuales exactos se obtienen por separado cuando sean necesarios.

Instala el mismo desarrollo y reproduce su configuración, aspecto y comportamiento completos. Debes resolver y comprobar las diferencias del destino; no sustituir requisitos por un resultado parecido. Si el hardware o firmware impide cumplir alguno, la réplica permanece incompleta o incompatible, aunque se hayan documentado las diferencias. El tamaño del televisor no determina la resolución efectiva del inicio, y estas instrucciones no garantizan compatibilidad con cualquier equipo.

## Qué se replica y cómo

| Parte del entorno | Procedencia | Trabajo en el nuevo equipo |
|---|---|---|
| Inicio de Z6 y sustitución del launcher anterior | APK y asistente | Seleccionar HOME; respaldar y deshabilitar el anterior solo tras comprobar el reemplazo. No se desinstala el launcher. |
| Panel compacto, selección translúcida, Apps/Acciones circulares, edición confinada y regreso de Añadir app | APK | Probar Home, flechas, OK sostenido y Atrás sobre una app reproduciendo. |
| Ajustes, reloj superior, sonido, apps, temporizador y diagnóstico | APK, perfil y permisos | Aplicar el perfil y comprobar sonido, suspensión y auxiliar del firmware. |
| Hora/fecha, formato de 12 horas, saludo, opacidad 80 %, animación del panel desactivada y escalas de animación a cero | `perfiles/replica.json` | El asistente los aplica. Configurar nombre, hora y zona del destino. |
| Apps visibles y orden | Perfil con paquetes de referencia | Instalar las apps y revisar las tarjetas. Otra variante necesita su paquete real. |
| Foto fija, presentación, QR, biblioteca con borrado y 12 paisajes diarios | APK | Elegir el modo y añadir fotos propias. Automática adapta resolución a Inicio y memoria. Rotación diaria inicialmente desactivada. |
| Tecla de TV → reabrir y llegar al destino; YouTube → cerrar y reabrir YouTube | APK y configuración local | Identificar teclas, editar etiquetas, elegir apps y asignar acciones. |
| Destinos guardados para varios botones y arranque/despertar | APK y configuración local | Enseñar y probar cada destino allí; después reutilizarlo sin repetir la enseñanza para cada uso. |
| Cachés acotadas, imágenes fuera del hilo de interfaz, auxiliar por eventos y retorno sin parpadeo | APK | Vienen incorporados. Comprobar fluidez; no añadir sondeos ni servicios para imitarlos. |
| Red, Bluetooth, cuentas, HDMI, autorizaciones y servicios del fabricante | Android y hardware | Configurarlos o conservarlos en el destino. No se transfieren con Z6. |

El perfil se actualizó con preferencias transferibles del respaldo de referencia verificado. Incluye `shown_apps` para mostrar ciertas apps de sistema, además de orden y ocultas. Estas listas organizan tarjetas: **no deshabilitan paquetes** ni instalan apps. Los paquetes ausentes no crean tarjetas. La limpieza general del firmware original no es una receta para otros modelos; el perfil no contiene operaciones de esa limpieza.

## 1. Descargar la entrega

Descargar `main` del repositorio cuando esté publicado y los archivos indicados por `entregas/manifest.json`. Para esta referencia:

- `entregas/Z6-Panel-0.20.1.apk`.
- `entregas/Z6-Panel-recuperacion-0.20.0-c61.apk`.

Git conserva código, perfil, scripts, documentación y manifiesto. **Los APK están excluidos de Git**: deben adjuntarse a la entrega de GitHub o distribuirse en el portable. Un clon por sí solo no incluye el instalable. El proyecto local todavía no tiene un remoto de GitHub configurado; su publicación es un paso separado.

El portable local `.local/Z6-Panel-0.20.1-portable.zip` incluye ambos APK y estas guías. Instalar no requiere Android Studio, Java ni la clave privada. Desde la raíz extraída, preparar ADB si falta:

```powershell
powershell -File .\herramientas\preparar-herramientas.ps1 -SoloAdb
```

Puede reutilizarse ADB mediante `-AdbPath`. Las dependencias se verifican con los hashes de `herramientas/dependencias.json`. Comprobar los SHA-256 de los APK contra el manifiesto y su certificado con `apksigner verify --print-certs` si están disponibles las build-tools. `Get-Z6Artifact` comprueba el hash antes de instalar; Android comprueba la firma al actualizar. No recompilar con otra firma para instalar la entrega.

## 2. Identificar y revisar el destino

Activar depuración compatible con el firmware y autorizar el PC. Usar la IP/puerto o serial del **nuevo** equipo en todos los comandos. Con depuración inalámbrica emparejada, emparejar primero y utilizar el puerto de conexión real; no asumir 5555.

```powershell
.\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica -Simular
.\z6.ps1 diagnosticar -Dispositivo IP:PUERTO
```

La simulación no contacta el TV. El diagnóstico sí identifica API, modelo, firmware, HOME y resolución. Se bloquea instalación/configuración automática fuera de API 29/31. Conservar un HOME recuperable y revisar espacio libre. No copiar identidades ni respaldos del original.

## 3. Instalar el panel y las aplicaciones

Para instalar Z6 y añadir otras apps después, únicamente como etapa provisional:

```powershell
.\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica -SoloPanel
```

Si se suministran los APK de las apps del perfil, nombrados `PAQUETE.apk`:

```powershell
.\z6.ps1 instalar -Dispositivo IP:PUERTO -Perfil replica -CarpetaApks RUTA
```

El segundo flujo necesita build-tools para verificar los paquetes; `herramientas/preparar-herramientas.ps1` sin `-SoloAdb` las prepara. Solo busca apps declaradas en el perfil, no cualquier APK de una carpeta. Los APK de terceros, cuentas y licencias no forman parte del proyecto.

La referencia usa Xuper `com.android.mgstv`, YouTube FOR `io.gh.reisxd.tizentube.fortaleza`, Downloader `com.esaba.downloader` y Google Play `com.android.vending`. Completar los accesos requeridos; si el firmware carece de una app indispensable y no puede instalarse de forma compatible, no aprobar la réplica. No instalar una tienda a la fuerza. Para otra variante autorizada, comprobar paquete, actividad de inicio y mismo resultado funcional. Si se automatiza su instalación/orden, copiar el perfil a `perfiles/destino-local.json`, sustituir sus paquetes en `apps`, `app_order`, `hidden_apps` y `shown_apps`, y usar `-Perfil destino-local`. Las copias `*-local.json` están excluidas de Git. También se puede instalar la variante por separado y elegirla desde Z6; no dar por terminado el paso mientras falte.

El primer respaldo queda en `privado/dispositivos/IDENTIDAD/original.json` y no se reemplaza al repetir. El asistente configura permisos, accesibilidad principal, preferencias y HOME; solicita autorización del auxiliar cuando el transporte local es compatible. Si Android pide habilitar Z6 en Accesibilidad, hacerlo y repetir. La asistencia opcional para recorridos se habilita en el paso 5. Revisar `ultimo-informe.json` del destino.

## 4. Igualar inicio, tarjetas y fondos

En Ajustes → Aplicaciones, completar las apps requeridas y reproducir su visibilidad y orden de referencia. El perfil conserva ese orden y las apps de sistema mostradas expresamente. Las apps nuevas de usuario se descubren por eventos; una app sin actividad de inicio no es una tarjeta. Con la fila vacía, **Añadir aplicación** permite empezar sin pulsación larga, pero una fila vacía no satisface la réplica de las tarjetas actuales.

En Panel e inicio, verificar reloj superior, formato de hora, saludo y nombre. El resaltado translúcido y la altura compacta vienen en el APK. El asistente no cambia idioma, zona, emparejamientos o HDMI.

En **Fondos del inicio**, reproducir la configuración de referencia: Paisaje diario, resolución Automática y rotación desactivada; seleccionar ese modo tras comprobar Internet. El perfil no fuerza el modo al reinstalar para no reemplazar un fondo durante una actualización; la réplica exige completar este ajuste explícitamente. Para mostrar la imagen exacta de referencia, obtener su recurso/origen y prepararlo mediante el flujo del modo correspondiente; no copiar un ID/URI del otro TV ni aprobar identidad visual con una foto distinta. Importar una foto y cambiar a Foto fija no reproduce el comportamiento de Paisaje diario. Los catálogos cambian con la fecha: instalar el mismo APK no sincroniza los catálogos de dos equipos. Si falta el recurso o una adaptación necesaria, registrar el requisito pendiente. Una personalización distinta necesita un cambio explícito del objetivo.

La resolución depende del inicio efectivo: una salida HDMI 4K puede conservar la interfaz en 720p. No forzar 1080p/4K para igualar una etiqueta. Leer [FONDOS.md](FONDOS.md) para imágenes, límites, importación y borrado; no mantener descargas mientras otra app reproduce.

## 5. Configurar Mi control y arranque

1. Entrar en **Ajustes → Aplicaciones → Mi control y arranque** y conectar/despertar el control. Si se comprueba el Remoter ATV3 compatible, usar **Preparar teclas especiales** o `herramientas/mando.ps1 -Dispositivo IP:PUERTO`. El script verifica el control y guarda el mapa original. No aplicar un mapa de otro modelo por tener los mismos logos.
2. Crear o editar cada botón, detectar la tecla y poner una etiqueta. Para TV, elegir la app instalada y **Reabrir en una sección · opcional**. Para YouTube, **Cerrar y abrir app · sin recorrido**; no necesita canal. Quitar tarjetas no deseadas con **Eliminar botón**.
3. Habilitar **Asistencia de TV de Z6** al enseñar o ejecutar destinos. Navegar dentro de la app, pulsar **Home → Probar y guardar este destino**, comprobar imagen/sonido o pantalla correcta y confirmar **Guardar destino**. Sin confirmación puede quedar un borrador pendiente.
4. En **Destinos guardados**, reutilizarlo en el botón y en **Arranque y despertar**. Probar cada destino por app/variante, sin copiar rutas de otra firma. Debes configurar y activar el acceso de TV, el de YouTube y el arranque/despertar de referencia, habilitando ambos automatismos después de la prueba. El arranque tiene su propia selección de app; un botón funcionando no comprueba ese automatismo.
5. Probar las teclas desde Z6 y otra app. Una pulsación produce una sola reapertura; mantenerla no acumula reinicios. Home, Atrás, flechas, OK, volumen y encendido conservan su función. La identidad interna y la etiqueta son independientes. El gestor permite borrar una ruta conservando o retirando sus usos.

Reproduce el destino exacto de referencia, obtenido privadamente del usuario o del equipo de origen; no uses un canal de prueba o un nombre histórico como sustituto. El repositorio no publica ese recorrido individual. Variantes y menús nuevos necesitan enseñanza propia. Si el firmware no expone una tecla o los controles necesarios, investiga y resuelve lo compatible; si queda impedido, la réplica no está terminada. Las asignaciones son copias independientes: reutilizar un destino no vincula sus futuras ediciones.

El acceso de uso permite comprobar el cierre normal de la app; si el firmware lo impide puede necesitar el auxiliar. **Diagnóstico y permisos** prepara capacidades avanzadas. Autorizar al PC/auxiliar inicialmente no equivale a reactivar depuración por cada pulsación. No reiniciar adbd para recuperarlo. Un PING durante la instalación no demuestra autonomía tras reiniciar: verificarla sin PC. Apagar solo el televisor puede no generar un despertar del TV Box.

## 6. Verificar y entregar el resultado

Seguir [PRUEBAS-FISICAS.md](PRUEBAS-FISICAS.md) con control real y apps del destino. Comprobar:

- Home sobre video mantiene reproducción, reloj y panel; ambas filas dan la vuelta y edición permanece en Apps. Añadir app → Atrás conserva navegación.
- Volver de Panel e inicio/Ajustes de Android mantiene el fondo sin parpadeo. Reabrir Mi control desde Inicio y Ajustes funciona.
- Botones independientes, guardado/reutilización/borrado de destinos y reapertura sin canal obligatorio.
- Fondos, recepción QR desde un celular de la misma red, multimedia y borrado de una foto de prueba; no borrar fotos del usuario para validar.
- Volumen, Bluetooth si se usa, temporizador y suspensión. Reinicio completo y despertar, con hasta tres minutos de recuperación, sin PC ni reactivación de depuración para uso normal.

Guardar un informe privado por destino con TODA la matriz del [contrato](CONTRATO-REPLICA.md): modelo/firmware/API, versión/hash, apps/control, ajustes, evidencia por requisito, diferencias resueltas, pendientes y respaldo. Solo declarar **REPLICA_VERIFICADA** con todos los requisitos verificados. Cualquier prueba pendiente, función omitida o impedimento obliga a **EN_CURSO / INCOMPLETA** o **INCOMPATIBLE**, según corresponda. Los estados del instalador no bastan para aprobar. No dar por comprobada una función por haber pasado en el original o emulador. Esta guía no afirma que todas esas pruebas se hayan repetido en 0.20.1; la evidencia está en [VERIFICACION-0.20.1.md](VERIFICACION-0.20.1.md).

## 7. Recuperar sin perder datos

```powershell
.\z6.ps1 restaurar -Dispositivo IP:PUERTO
# Si se necesita también el APK de recuperación:
.\z6.ps1 restaurar -Dispositivo IP:PUERTO -RecuperarVersion
```

Usar solo el respaldo de ese destino. Sin `-RecuperarVersion`, recupera HOME/ajustes y conserva el APK. Con esa opción usa la recuperación firmada **0.20.0/c61** del manifiesto, común a API 29/31; conserva fotos y formatos de destinos, con el arreglo de reentrada a Mi control. El restaurador respalda botones/destinos y devuelve las teclas originales cuando corresponde. Tras c61 se requiere **c62 o superior**. No desinstalar para forzar un downgrade.

Claves, cuentas, fotos, recorridos, respaldos, herramientas descargadas e informes individuales quedan fuera de Git/portable. Publicar código/documentación y APK firmados de la misma entrega. La clave privada no hace falta para que otra IA instale y configure.
