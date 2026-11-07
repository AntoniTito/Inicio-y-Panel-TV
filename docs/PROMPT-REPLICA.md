# Prompt para un agente

```text
Usa este repositorio para reproducir Z6 Panel en el TV Box de destino. Lee README.md, AGENTS.md y docs/COMPATIBILIDAD.md. Identifica el dispositivo inequívocamente y utiliza siempre su serial o IP:puerto real.

1. Comprueba los APK firmados y herramientas; instala ADB si falta.
2. Ejecuta z6.ps1 instalar -Dispositivo DESTINO -Simular y diagnosticar.
3. En API 29 o 31, ejecuta instalar -Dispositivo DESTINO -Perfil replica. Añade -SoloPanel si el usuario instalará otras aplicaciones después. Conserva el respaldo inicial.
4. Solicita solo las autorizaciones de Android y los APK o datos indispensables que falten. No copies identidades, cuentas ni conexiones del original.
5. Verifica según docs/PRUEBAS-FISICAS.md y entrega el informe y método de restauración.

La entrega vigente es 0.16.1/código 40. Lee FONDOS.md: mantén el fondo actual inicialmente, no copies fotos ni IDs del origen y abre recepción por QR solo a petición del usuario. Revisa la evidencia y las limitaciones por API en COMPATIBILIDAD.md. Consulta el estado del TV original en VERIFICACION-0.16.1.md y distingue sus comprobaciones de las pruebas pendientes en cada destino. Las API distintas de 29/31 están bloqueadas hasta validación. Lee EMULADOR-TV12.md si corresponde. No cambies firmware ni apliques listas de paquetes de otros modelos. No publiques privado/ ni .local/. Usa las mismas herramientas que una persona, sin depender de instrucciones guardadas en otro chat.
```

En 0.16.0 las imágenes se optimizan y publican en álbumes de MediaStore. El instalador respalda las fotos antes de liberar una migración antigua y conserva el estado de la galería nativa. No copies la biblioteca, URI de MediaStore, IDs, catálogo, créditos locales ni marcas wallpaper_publish_hold entre equipos. La rotación diaria empieza desactivada. Usa 0.16.0/código 41 con adaptador de almacenamiento para recuperación, no un APK histórico anterior a la migración.
