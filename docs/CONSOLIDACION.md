# Proyecto consolidado

Una única base de código, 0.10.4/código 22, conserva el launcher, los paneles, reloj superior, Ajustes, temporizador, auxiliar y optimizaciones. No se combinaron versiones Java antiguas con la actual.

## Conservado

- Código y recursos íntegros; APK vigente y recuperación 0.10.3/código 23.
- Herramientas Android descomprimidas, con sus licencias, en `.local/sdk`.
- Firma original y copia, excluidas de Git, con contraseña local protegida por Windows.
- Preferencias verificadas al entregar 0.10.4. El respaldo anterior a compactar conserva el reloj superior; `preferences-final` era anterior y no se usa como referencia.
- Estados originales de launcher, paquetes, animaciones y accesibilidad para recuperación del equipo de referencia.
- Auditoría original, evidencias detalladas y capturas representativas, conservadas privadamente. La documentación versionable resume sus conclusiones sin copiar datos individuales.

## Descartado tras verificar

ZIP descargados de SDK ya extraídos, compilaciones intermedias, duplicados, versiones superadas, APK de investigaciones concluidas y capturas/registros redundantes. No se conserva otro ZIP completo de las carpetas viejas.

`privado/migracion/inventario-origen.csv` registra los archivos de origen. `copias-verificadas.json` relaciona los esenciales con sus destinos y hashes. Los informes de clasificación y limpieza registran qué se conserva o descarta. Estos documentos son privados porque incluyen rutas del PC.

## Independencia

El compilador resuelve las rutas desde la raíz del proyecto. Java y Git siguen siendo aplicaciones del PC; no son partes del código. Las descargas futuras pueden reconstruir el SDK a partir de las versiones y SHA-256 fijados. El APK de referencia no se reemplaza por recompilaciones.

El asistente y la documentación son nuevos; no cambian la aplicación del TV. No se ha publicado el repositorio ni modificado el dispositivo original. Las limitaciones de Android reciente están documentadas y se comprobarán en una actualización separada.
