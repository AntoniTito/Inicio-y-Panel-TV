# Componentes de terceros

## Generación local de QR

Se incluyen las cuatro clases necesarias de la versión Java estándar de **QR Code generator de Project Nayuki 1.8.0**: BitBuffer, DataTooLongException, QrCode y QrSegment. No se incluye la demo de escritorio ni dependencias de Java AWT.

- Proyecto: https://www.nayuki.io/page/qr-code-generator-library
- Fuente: https://github.com/nayuki/QR-Code-generator
- Revisión fijada: `720f62bddb7226106071d4728c292cb1df519ceb` (`v1.8.0`).
- Ruta original: `java/src/main/java/io/nayuki/qrcodegen`.
- Licencia: MIT. Los avisos de copyright y el texto íntegro de la licencia se conservan en la cabecera de cada archivo.

| Archivo | SHA-256 original |
|---|---|
| BitBuffer.java | D5496452B435423BEEAD30AA356E10A7C0BA4790B7DA37EE4A1B80B4DF136447 |
| DataTooLongException.java | 7661186DDE4B27334FD94F4950F58522EAAB256274388152E5CB8E35EF463E1A |
| QrCode.java | AB2D9F05BEF0C2BEDFAA888256F09987B2DBEF5A4F432C540B9E549801C030EC |
| QrSegment.java | 70F10E518D3A8F1A1862E598A249ABAAC036A9C6FAE4E5892C4B6D24995BB8D7 |

El QR se genera sin conectarse a servicios externos. Las imágenes de Wikimedia mantienen sus licencias individuales, con créditos consultables en la interfaz; no se redistribuye una colección de fotos dentro del APK.

Playwright se usa únicamente como herramienta local de pruebas del navegador. No se incorpora a la aplicación ni a la entrega trasladable. Las pruebas usan imágenes sintéticas generadas localmente.
