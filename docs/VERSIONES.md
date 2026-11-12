# Versiones de Inicio y Panel TV

El proyecto se llamó Z6 Panel hasta 0.20.1. Inicio y Panel TV conserva el mismo paquete, firma y datos. El historial Git conserva sus entregas completas: seleccionar una versión cambia el código, perfiles y documentación, no modifica por sí solo un TV Box o emulador.

| Etiqueta fija | Rama | Contenido y evidencia |
|---|---|---|
| `v0.10.4` | `android10` | Base anterior, APK 0.10.4/codigo 22. Comportamiento comprobado en el TV Box Android 10 original. Los 21 archivos de app coinciden byte a byte con el respaldo previo a Android 12. |
| `v0.11.0` | Etiqueta histórica | Adaptacion Android TV 12, APK 0.11.0/codigo 24. Mantiene rutas para API 29; regresion fisica en Android 10 pendiente. Verificaciones del emulador registradas en pruebas/. |
| `v0.12.0` | Etiqueta histórica | Apps accesibles con fila vacía, fondos propios/diarios y QR temporal. APK código 26. Pruebas y límites por API registrados. |
| `v0.12.1` | Etiqueta histórica | Corrige el fondo para cubrir toda la pantalla. APK código 28; recuperaciones por API con código 29. |
| `v0.12.2` | Etiqueta histórica | Recepción confirmada en el TV, vista previa directa y regreso a biblioteca. APK código 30, recuperación 0.12.1/código 31 para ambas API. |
| `v0.13.0` | Etiqueta histórica | Biblioteca con miniaturas y borrado múltiple, fotos adaptadas y comparación 720p/1080p/4K según el equipo. APK código 32, recuperación 0.12.2/código 33. |
| `v0.14.0` | Etiqueta histórica | Fondos con vista previa y OK directo, galería y presentación seleccionable, importación visual. APK código 34; recuperación 0.13.0/código 35 con protección de selección parcial. |
| `v0.15.0` | Etiqueta histórica | Modos en fila, fotos y presentación integradas, eliminación sin papelera, elección de paisajes de Wikimedia. APK código 36; recuperación histórica 0.14.0/código 37. |
| `v0.16.0` | Etiqueta histórica | Doce paisajes diarios, caché persistente, rotación opcional, conservación permanente y galería MediaStore. Código 38; recuperación 0.15.0/código 39 con adaptador compartido. |
| `v0.16.1` | Etiqueta histórica | Quita el acceso a la galería nativa desde Fondos, manteniendo imágenes y funciones. Código 40; recuperación 0.16.0/código 41. |
| `v0.16.2` | Etiqueta histórica | Evita borrar el fondo durante las pausas breves de Home y ajustes. Código 42; recuperación 0.16.1/código 43. |
| `v0.16.3` | Etiqueta histórica | Reduce el trabajo de Home reutilizando el fondo validado; mantiene entrada y retorno sin parpadeo. Código 44; recuperación 0.16.2/código 45. |
| `v0.16.4` | Etiqueta histórica | Resaltado translúcido de las tarjetas del inicio. Código 46; recuperación 0.16.3/código 47. |
| `v0.16.5` | Etiqueta histórica | Corrige el regreso de Añadir app durante la edición del panel superpuesto. Código 48; recuperación 0.16.4/código 49. |
| `v0.17.0` | Etiqueta histórica | Filas circulares independientes del panel y edición confinada a Apps, con limpieza de código inaccesible. Código 50; recuperación 0.16.5/código 51. |
| `v0.18.0` | Etiqueta historica | Inicio y recuperación de TV: app y canal elegibles, recorrido guiado, arranque y despertar optativos, tecla configurable y datos privados por dispositivo. Código 52; recuperación 0.17.0/código 53. |
| `v0.18.1` | Etiqueta histórica | Recuperacion del auxiliar por eventos sin reiniciar depuracion, estado real del boton y reintentos limitados. Codigo 54; recuperacion 0.18.0/codigo 55. |
| `v0.19.0` | Etiqueta histórica | Botones independientes, destino en la app, arranque sin doble apertura y selector por capacidades reales. APK c56; recuperación 0.18.1/c57. Evidencia y límites en MI-CONTROL.md y VERIFICACION-0.19.0.md. |
| `0.20.0` | Commit histórico `96c622f` | Guardado confirmado por el usuario. El regreso a Mi control y la gestión se corrigen en 0.20.1; no se recomienda esta interfaz anterior. APK c58 y recuperación histórica 0.19.0/c59. |
| `v0.20.1` | Etiqueta histórica | Regreso a Mi control, gestor visible de rutas, borrado con usos y destinos genéricos opcionales. APK c60 instalado y comprobado en el TV API 29; pruebas de emulador API 31 separadas. Recuperación histórica 0.20.0/c61. Ver VERIFICACION-0.20.1.md. |
| `v0.20.2` | `main` | Inicio y Panel TV: cambio de marca, documentación pública y capturas de demostración. APK c62; recuperación 0.20.1/c63. Conserva funciones, componentes, firma y datos. Evidencia en VERIFICACION-0.20.2.md. |

Las etiquetas son inmutables. `main` queda en la entrega más reciente; su evidencia y pendientes físicos están en VERIFICACION-0.20.2.md. La recuperación vigente conserva 0.20.1 con código 63 y la firma original para API 29/31; después requiere código 64 o superior. Las recuperaciones y etiquetas históricas se conservan sin sobrescribir.

## Elegir una base

Para replicar **el desarrollo completo vigente**, usa `main` tanto para Android 10 como para 12 y sigue [REPLICAR-ENTORNO.md](REPLICAR-ENTORNO.md). La rama `android10` solo sirve si se pide expresamente la base histórica 0.10.4; no contiene las mejoras posteriores. Las instrucciones siguientes explican esa elección histórica.

Primero revisa `git status`: guarda cualquier trabajo pendiente antes de cambiar de rama. Git debe permitir el cambio sin descartar modificaciones; no uses opciones de fuerza.

```powershell
git switch android10
```

Selecciona el proyecto anterior para replicar el entorno Android 10. Lee el README de esa rama: su instalador utiliza 0.10.4/codigo 22 y solo admite API 29. Para regresar al proyecto adaptado:

```powershell
git switch main
```

Para revisar una entrega fija sin cambiar de rama de trabajo puedes usar `git show v0.10.4:app/AndroidManifest.xml` o comparar con `git diff v0.10.4 v0.11.0 -- app`.

Los APK firmados estan en entregas/ en esta copia local, excluidos de Git. Una descarga del repositorio necesita los APK de su entrega, cuyos hashes figuran en el manifiesto de esa version. La firma privada, las preferencias personales, SDK y respaldos se mantienen fuera del historial. Cambiar de rama no elimina esos archivos locales ignorados.

Elegir la version anterior del codigo no permite bajar automaticamente la version instalada en Android: si un destino ya tiene codigo 24, Android rechazara el APK original codigo 22. Usa el procedimiento de recuperacion aplicable a ese destino; no desinstales para forzar un downgrade y perder datos.

## Procedencia y siguientes cambios

El historial comienza con una instantanea verificada de la consolidacion 0.10.4. No se inventaron commits ni fechas de trabajos anteriores. El siguiente commit registra la adaptación 0.11.0, con sus herramientas y pruebas existentes. Otro commit registra 0.12.0 sin mover las etiquetas anteriores. Autor configurado localmente en este repositorio: Antoni, con el correo indicado por el usuario.

Los proximos cambios se registran con commits nuevos. Si cambia el APK, aumenta su version y codigo, conserva la misma firma y prepara su recuperacion antes de etiquetar otra entrega. Las pruebas necesarias y los limites de compatibilidad deben quedar en esa version.

El repositorio público es [AntoniTito/Inicio-y-Panel-TV](https://github.com/AntoniTito/Inicio-y-Panel-TV). Las etiquetas históricas conservan sus nombres, código y autoría originales; los APK están en Releases, fuera del historial de código. No se incluyen respaldos, fotos personales ni claves.

0.12.1 conserva los controles y la proporción de las fotos. Su recuperación API 29 conserva 0.10.4; la de API 31 vuelve a 0.12.0. Después de instalar una recuperación código 29 se necesita código 30 o superior para actualizar. Las recuperaciones código 27 corresponden a la entrega anterior.
