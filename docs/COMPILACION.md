# Compilación independiente

`z6.ps1 compilar` usa `app`, las herramientas de `.local/sdk` y la firma privada. La salida predeterminada es `.local/build/Z6-Panel-recompilado.apk`; no reemplaza los APK verificados de `entregas`.

Requisitos: JDK 21, Android Build Tools 35.0.0, plataforma Android 35 revisión 2 y Platform Tools 37.0.1. Las tres herramientas Android están copiadas y verificadas en la consolidación. En otra descarga, `herramientas/preparar-herramientas.ps1` las obtiene de Google y valida el SHA-256 fijado en `dependencias.json`. Los SDK no forman parte de Git.

Configura `JAVA_HOME` al JDK 21 o utiliza `-Jdk RUTA\bin`. Java y Git son programas instalados en el PC y pueden permanecer en sus ubicaciones habituales; el código no depende de las carpetas fechadas eliminadas.

La clave está en `privado/firma/original.p12`, con una copia en `privado/respaldo-firma/original.p12`. La contraseña local está protegida con DPAPI de Windows: solo la cuenta/máquina que la protegió puede recuperarla. En otro PC proporciona la contraseña mediante solicitud segura o la variable temporal `Z6_STORE_PASSWORD`, y transfiere la clave por separado. No se genera otra firma automáticamente.

La compilación restaura las variables de entorno al terminar. Verifica firma y hash. El APK recompilado puede variar por metadatos ZIP; para comprobar equivalencia se comparan además certificado, manifiesto y contenido DEX/recursos con el APK de referencia.
