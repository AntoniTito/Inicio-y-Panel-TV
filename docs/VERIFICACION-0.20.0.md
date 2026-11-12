# Verificación de 0.20.0

Entrega instalada, comprobación física del flujo pendiente: 0.20.0/c58, APK común a API 29 y 31. Recuperación 0.19.0/c59 desde la etiqueta histórica, con la firma original. Después de la recuperación se requiere c60 o superior. Los hashes están registrados en `entregas/manifest.json`.

## Problema y cambios

El usuario informó que el destino no quedaba guardado después de probarlo, no podía reutilizarlo y encontraba confusa la gestión de botones. En el dispositivo, su asignación nueva seguía sin ruta confirmada y la última acción indicaba interrupción por el control. No se dispone de evidencia suficiente para afirmar que falló una escritura después de pulsar Guardar; se corrigen también las pérdidas anteriores a esa confirmación.

- Antes de probar se conserva un borrador privado con su botón de origen. La prueba completada se registra antes de mostrar la confirmación; una interrupción mantiene el borrador recuperable.
- Guardar actualiza la asignación y añade una ruta a la biblioteca común, sin duplicar un destino idéntico. El borrador se retira solo después de las escrituras correctas. Los fallos de guardado conservan el borrador.
- Los recorridos verificados existentes se incorporan una sola vez a Destinos guardados. Los de apps retiradas se conservan identificados, pero no se aplican a variantes distintas ni firmas nuevas.
- Usar un destino copia la ruta conservando la identidad de la tecla, etiqueta e interruptores del receptor. No enlaza cambios futuros de asignaciones distintas.
- Etiqueta separada de la identidad física; captura genérica sin menú de Netflix/YouTube obligatorio y eliminación con confirmación desde el editor o la detección.

## Validaciones

**Emulador API 31:** `destinos-0200.cjs`, 20 comprobaciones. Configuración con navegación real de una app sintética; prueba terminada, interrupción con Home y recreación real del proceso de Z6 mediante force-stop y nueva apertura. El borrador conserva su resultado, se confirma y se asigna a otro botón y al arranque. Se comprueban códigos/etiqueta separados, cambio de nombre sin alterar la tecla, eliminación y cancelación desde el editor, eliminación de una tarjeta nueva desde la detección, exportación/restauración, deduplicación y reintento de una prueba incompleta. Una prueba incompleta no puede guardarse como probada.

`mando-0190.cjs` se reutiliza sin duplicar su batería y registra el nombre de la versión instalada: 25 comprobaciones de botones, rutas con controles y ListView, cierre nativo, foco real, teclas protegidas, conflictos, cancelación y ausencia de consultas de contenido en reposo. Los dos JSON de resultados incluyen el hash del APK probado. No se considera probado video comercial mediante la app sintética.

**Windows, ADB simulado:** `verificar.ps1`, 78 comprobaciones de asistente, hashes, identidad, instalación, permisos, respaldo y recuperación para API 29/31; no contactó hardware. La recuperación c59 se compiló desde `v0.19.0` y verificó su certificado original. No se instaló c59 en el TV: impediría volver a c58 sin otra actualización.

**TV original API 29:** respaldo previo de APK, preferencias, botones y bytes de las dos imágenes. Instalación sin desinstalar, asignaciones originales conservadas y hashes de las dos imágenes idénticos. Se revisaron capturas reales de la pantalla principal y del editor completos a 1280 × 720; el editor muestra `KEYCODE_BUTTON_2`, 189 y scan 88 separados de la etiqueta. Se migró un destino anterior; no se aplica a una variante distinta si su app fue retirada.

**Pendiente del usuario:** guardar un nuevo recorrido real en la app de reemplazo y reutilizarlo; confirmar manejo físico del cambio de etiqueta y borrado. Las capturas y los datos de prueba individuales permanecen en `privado/`. No se modifica la lectura de contenido del servicio principal, el límite al paquete elegido de la asistencia opcional ni el protocolo del auxiliar.

No se incorporan consultas periódicas, dependencias gráficas o procesos nuevos. La biblioteca tiene un máximo de 64 destinos y un solo borrador, ambos privados. La recepción de teclas conserva su caché y la navegación conserva las optimizaciones anteriores. Las medidas de rendimiento de versiones previas no se atribuyen a esta versión.

`resultados-memoria-0.20.0-api31.json`: cinco ciclos de calentamiento y veinte aperturas/cierres adicionales de Mi control, PSS 33 358 → 32 306 KB (−1052 KB), consultas de navegación estables en reposo. No se midió latencia de cuadros ni una comparación con la versión anterior bajo condiciones equivalentes; esta muestra no demuestra una aceleración ni garantiza el uso de memoria en todos los equipos.

## Seguimiento posterior

El usuario confirmó que el destino se guarda, pero encontró un bloqueo al volver a Mi control y dificultades para gestionar rutas y otras apps. El acceso se reprodujo físicamente; su causa y las correcciones de gestión se registran en VERIFICACION-0.20.1.md. Esta entrega histórica no incorpora esos arreglos.
