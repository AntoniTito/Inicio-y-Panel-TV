# Verificación de Inicio y Panel TV 0.20.2/c62

La entrega cambia la marca visible y prepara la distribución pública. Mantiene paquete `local.z6panel`, firma original, componentes, preferencias, protocolo del auxiliar e índices/rutas de imágenes. El TV Box físico no se actualiza durante esta preparación.

## Artefactos

APK 0.20.2/c62 y recuperación 0.20.1/c63 compilados con el certificado SHA-256 `1446d3adb55774747cadf9c8f7c814652fb2d0a97e0368094d1c251572dfe2d5`. Los hashes están en `entregas/manifest.json`. La recuperación procede de la etiqueta inmutable v0.20.1 cambiando solo su código a 63; conserva la marca Z6 Panel y sus formatos de datos. Tras usar c63 se requiere c64 o superior.

## Actualización y recuperación en API 31

Emulador nuevo de demostración, Android TV 12/API 31 y 1280 × 720, separado de los AVD del usuario. Se instaló 0.20.1/c60 y se prepararon tres imágenes sintéticas, dos botones y un destino. La actualización a c62 conservó las preferencias comprobadas, botones, arranque y manifiesto de fotos: nueve comprobaciones registradas en `pruebas/resultados-actualizacion-0.20.2-api31.json`.

Después de guardar un snapshot de c62 se instaló la recuperación c63, comprobando nuevamente esos nueve resultados. Se cargó el snapshot y se verificó el regreso a c62, sin downgrade ni desinstalación. Evidencia: `pruebas/resultados-recuperacion-0.20.2-api31.json`.

## Comprobaciones funcionales y presentación

- API 31: 20 comprobaciones de Mi control, 25 del mando y recorridos y 20 de destinos/persistencia. Las 65 pasaron con el APK c62; incluyen reapertura genérica, confirmación, borrador tras recrear el proceso, reutilización y borrado con/sin retirar usos. Fueron entradas ADB sobre apps sintéticas, no un control físico ni video comercial.
- Receptor API 31: seis comprobaciones de marca en la página móvil, rechazo de sesión inválida, foto optimizada, ausencia de aplicación automática, cierre del puerto y retirada de la transferencia sintética. Se usó localhost mediante ADB; no prueba el teléfono físico.
- Windows: 81 comprobaciones del asistente con ADB falso. No se contactó ningún TV físico. Evidencia local, exportada con el resto de informes de esta entrega.
- Integridad: 44 APK actuales e históricos comprobados por hash, certificado y metadatos. Los sufijos de versionName de recuperaciones históricas se registran explícitamente; el código Android coincide con su manifiesto.
- Auditoría de cambio de marca: en los 13 archivos Java modificados de la app solo cambiaron literales de texto. Permisos, paquete, componentes, descriptores, claves de preferencias, rutas y formato de datos se conservaron.
- Presentación: ocho capturas reales de 1280 × 720 revisadas visualmente. Controles y pie visibles; tres gráficos propios y seis miniaturas de Wikimedia con atribución. El auxiliar del emulador se inició desde ADB autorizado; la captura no demuestra recuperación autónoma.
- Desarrollo: compilación completa con una clave desechable distinta, solicitud de su contraseña y certificado diferente del oficial. La contraseña DPAPI de la clave original no se reutilizó. Ese APK de desarrollo no se instaló ni se publica.
- Distribución: portable extraído en una carpeta nueva, con 81 comprobaciones del asistente y simulación sin ADB. Incluye ocho capturas, código, guías, licencia, créditos y dos APK con hashes verificados. No necesitó la carpeta privada ni el SDK originales para instalar con ADB simulado. Un perfil local de prueba quedó excluido del archivo.

Resultados en `pruebas/resultados-revision-control-0.20.2-api31.json`, `resultados-mando-0.20.2-api31.json`, `resultados-destinos-0.20.2-api31.json`, `resultados-recepcion-0.20.2-api31.json`, `resultados-artefactos-0.20.2-local.json` y `resultados-capturas-0.20.2-api31.json`.

No se publica una comparación nueva de p95, FPS o velocidad respecto de hardware distinto. Las comprobaciones de reposo del control pasaron; eso no equivale a un benchmark gráfico del TV original.

## Alcance de la evidencia

Las capturas públicas y las pruebas nuevas corresponden al emulador o al PC, no al TV original. Las comprobaciones del portable están en `pruebas/resultados-distribucion-0.20.2-local.json`; la compilación propia, en `pruebas/resultados-compilacion-desarrollo-0.20.2-local.json`. La publicación de GitHub se verifica después de subir los archivos, sin convertir esos resultados en pruebas físicas del TV.

La evidencia física anterior pertenece a [0.20.1](VERIFICACION-0.20.1.md). La actualización de marca no certifica otros firmwares, teclas, USB, Bluetooth, HDMI-CEC o reproducción comercial. Las pruebas pendientes en un nuevo equipo impiden aprobar su réplica conforme a [CONTRATO-REPLICA.md](CONTRATO-REPLICA.md).
