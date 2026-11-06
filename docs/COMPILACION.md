# Compilación independiente

`z6.ps1 compilar` usa `app`, las herramientas de `.local/sdk` y la firma privada. La salida predeterminada es `.local/build/Z6-Panel-recompilado.apk`; no reemplaza los APK verificados de `entregas`.

Requisitos: JDK 21, Android Build Tools 35.0.0, plataforma Android 35 revisión 2 y Platform Tools 37.0.1. Las tres herramientas Android están copiadas y verificadas en la consolidación. En otra descarga, `herramientas/preparar-herramientas.ps1` las obtiene de Google y valida el SHA-256 fijado en `dependencias.json`. Los SDK no forman parte de Git.

Configura `JAVA_HOME` al JDK 21 o utiliza `-Jdk RUTA\bin`. Java y Git son programas instalados en el PC y pueden permanecer en sus ubicaciones habituales; el código no depende de las carpetas fechadas eliminadas.

La clave está en `privado/firma/original.p12`, con una copia en `privado/respaldo-firma/original.p12`. La contraseña local está protegida con DPAPI de Windows: solo la cuenta/máquina que la protegió puede recuperarla. En otro PC proporciona la contraseña mediante solicitud segura o la variable temporal `Z6_STORE_PASSWORD`, y transfiere la clave por separado. No se genera otra firma automáticamente.

La compilación restaura las variables de entorno al terminar. Verifica firma y hash. La fuente vigente produce 0.12.0/código 26. La equivalencia con 0.10.4 se comprobó durante la consolidación histórica, antes de adaptar Android 12; el nuevo DEX cambia deliberadamente. La base histórica se conserva en `privado/historico/codigo-0.10.4.zip`. Las recuperaciones código 27 conservan 0.10.4 en API 29 y 0.11.0 en API 31; no cruces sus destinos. El manifiesto registra sus hashes y el asistente selecciona por API. La biblioteca Nayuki integrada se documenta en TERCEROS.md.
