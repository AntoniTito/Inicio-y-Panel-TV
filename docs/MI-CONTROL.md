# Mi control y arranque

Z6 0.20.1/c60 corrige la reapertura de Mi control y ofrece [gestión visible de destinos y botones](DESTINOS-Y-BOTONES.md), borrador persistente, nombres editables y códigos internos de la tecla. Abrir/reabrir sin recorrido y una sección opcional son elecciones distintas. Un solo APK para API 29 y 31; las capacidades se comprueban en cada firmware.

Entra en **Ajustes → Aplicaciones → Mi control y arranque**. Cada botón tiene su propia tarjeta, acción, aplicación, destino e interruptor. Cambiar YouTube no modifica Netflix ni el arranque. Las acciones disponibles son abrir app, cerrar y reabrir, reabrir y llegar al destino, Inicio de Z6, Ajustes de Android y silencio.

**Aplicación** muestra las apps instaladas que tienen un acceso de inicio. Las instalaciones y desinstalaciones actualizan el catálogo mediante eventos, sin reiniciar Z6 ni consultar periódicamente. No hay una lista fija de variantes: un nombre de paquete que empiece por `com.android.`, `com.google.android.` o `android.` no convierte por sí solo una app instalada por el usuario en un componente del sistema. Para reiniciar se comprueban sus atributos reales, UID propio y ausencia de función HOME; los componentes protegidos pueden aparecer al elegir **Abrir app**, pero no al elegir una acción que los reinicie. Servicios sin pantalla de inicio no son accesos seleccionables.

Si sustituyes una app por otra variante: abre la tarjeta del botón → **Aplicación** → elige la nueva. Para llegar a un canal, usa **Reabrir y llegar al destino** y vuelve a enseñar su recorrido. El arranque se cambia por separado. No se copia el recorrido de una app distinta ni se acepta automáticamente una firma nueva.

Para otro botón: **Añadir botón → Comenzar → pulsa la tecla → Asignar**. Después elige su acción. Puedes editar su Etiqueta y ver el keycode y scan code reales; Eliminar botón está disponible también en la detección. Home, Atrás, flechas, OK, volumen y encendido están protegidos. Una tecla que el fabricante no entrega a Android puede requerir una preparación específica; no se promete compatibilidad con cualquier tecla o control. Mantener una tecla pulsada no acumula reinicios.

## Elegir un canal o pantalla

1. Abre la tarjeta del botón y elige **Cambiar destino → Comenzar**.
2. Navega directamente en la app con flechas, OK y Atrás. Una etiqueta pequeña muestra el avance sin tomar el foco.
3. Cuando llegues, pulsa **Home → Probar y guardar este destino**. Comprueba imagen y sonido y confirma.

El destino anterior se conserva hasta confirmar la prueba desde un proceso nuevo. La configuración registra elecciones de controles accesibles, no un video ni una secuencia de coordenadas. No registra campos editables o contraseñas. Si la app cambia sus menús o no expone controles adecuados, el recorrido puede necesitar repetirse o no estar disponible.

**Arranque y despertar** tiene su propia app, recorrido e interruptores. Apagar solo el televisor puede dejar encendido el TV Box sin generar un evento de despertar; en ese caso usa el botón del control.

Cada solicitud cierra y abre la app una sola vez. Si sus menús tardan, Z6 espera sin volver a reiniciarla. Si ya aparece el destino exacto guardado, continúa desde ahí. Si no puede comprobar el destino a tiempo, deja la app abierta para continuar con el control; no anuncia que el canal está listo sin verificarlo.

## Android, Z6 y el auxiliar

Estas funciones pertenecen al APK de Z6. El servicio de accesibilidad principal recibe teclas y conserva `canRetrieveWindowContent=false`. La asistencia adicional solo reconoce controles de la app elegida mientras se configura o ejecuta un recorrido; al terminar retira los eventos y las ventanas.

En el control Remoter ATV3 comprobado, una configuración nativa de teclado de Android convierte únicamente las funciones de Netflix y YouTube en dos teclas que Accesibilidad puede recibir. Android guarda esa elección. No se edita Generic.kl, no se modifica la partición del sistema y no se usa root. Otros modelos requieren comprobar sus propios códigos; no se aplica este mapa por semejanza del nombre del botón.

**Preparar Netflix y YouTube** necesita acceso inicial del auxiliar o del PC. Después la recepción de esas teclas no depende del auxiliar ni mantiene una lectura de dispositivos de entrada. **Restaurar teclas originales** o el asistente del PC retiran el mapa de Z6 conservando la selección previa. Desactivar una tarjeta solo detiene su acción de Z6; para recuperar las funciones nativas de esas dos teclas se restaura el mapa.

La reapertura intenta primero el cierre normal de procesos en segundo plano de Android. Z6 se coloca delante, solicita el cierre y verifica que el UID anterior dejó de ejecutarse antes de relanzar. Necesita el acceso inicial de uso para comprobar ese estado; no consulta periódicamente el historial. Si el firmware bloquea esa comprobación o la app mantiene servicios protegidos, puede requerir el auxiliar. Nunca se anuncia una reapertura correcta si el cierre no se confirmó. Abrir una app normalmente no requiere ese permiso.

**Diagnóstico y permisos** informa capacidades y permite reparar operaciones avanzadas, como las que Android reserva al auxiliar. No es una configuración que deba repetirse por cada pulsación. El auxiliar es otro proceso del mismo código, autorizado mediante ADB; sus otras funciones avanzadas pueden seguir dependiendo de depuración. No se reinicia adbd para recuperarlo.

## Preparación y recuperación desde Windows

```powershell
.\herramientas\mando.ps1 -Dispositivo IP:PUERTO -Simular
.\herramientas\mando.ps1 -Dispositivo IP:PUERTO
.\herramientas\mando.ps1 -Dispositivo IP:PUERTO -Restaurar
```

El asistente identifica el destino y conserva asignaciones y permiso originales en `privado/dispositivos/`. La preparación del mapa exige un Remoter ATV3 conectado y comprobable; no copia MAC, números de serie, cuentas ni recorridos de otro TV. Despiértalo con una flecha si está en reposo.

La recuperación 0.20.0/c61 conserva fotos, biblioteca y recorridos asignados e incorpora la declaración singleTask para poder entrar de nuevo a Mi control. El restaurador exporta privadamente botones, biblioteca y borrador antes de detener automatismos y devolver HOME, permisos y mapa a sus estados originales. Mantiene la interfaz anterior de 0.20.0; después de usar c61 se requiere c62 o superior para actualizar. No recuperes instalando un APK antiguo a mano sin revisar el mapa y los permisos.

## Referencias técnicas

[Mapas de caracteres y overlays de Android](https://source.android.com/docs/core/interaction/input/key-character-map-files), [mapas de teclas](https://source.android.com/docs/core/interaction/input/key-layout-files), [cierre de procesos en segundo plano](https://developer.android.com/reference/android/app/ActivityManager#killBackgroundProcesses(java.lang.String)) y [servicios de accesibilidad](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). Los permisos y APIs no sustituyen las pruebas del equipo real.

## Pruebas en un emulador preparado

Instala el APK vigente mediante el asistente, habilita Asistencia de TV y compila la app sintética con `pruebas/preparar-fixture-tv.ps1`. Instala su APK generado y ejecuta `node pruebas/mando-0190.cjs SERIAL`, `node pruebas/arranque-mando-0190.cjs SERIAL` y `node pruebas/apertura-unica-0190.cjs SERIAL`. Exigen un emulador explícito y restauran la configuración que modifican. Las pruebas de arranque y apertura única utilizan exclusivamente el contrato de la app sintética incluida; no dependen de recorridos o archivos privados de una sesión anterior. Las pruebas físicas se registran por separado.
