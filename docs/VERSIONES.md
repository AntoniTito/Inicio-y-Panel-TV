# Versiones del proyecto

El historial Git conserva sus entregas completas del proyecto. Seleccionar una version cambia el codigo, el instalador, los perfiles y la documentacion correspondientes; no modifica por si solo un TV Box o el emulador.

| Etiqueta fija | Rama | Contenido y evidencia |
|---|---|---|
| `v0.10.4` | `android10` | Base anterior, APK 0.10.4/codigo 22. Comportamiento comprobado en el TV Box Android 10 original. Los 21 archivos de app coinciden byte a byte con el respaldo previo a Android 12. |
| `v0.11.0` | Etiqueta histórica | Adaptacion Android TV 12, APK 0.11.0/codigo 24. Mantiene rutas para API 29; regresion fisica en Android 10 pendiente. Verificaciones del emulador registradas en pruebas/. |

| `v0.12.0` | Etiqueta histórica | Apps accesibles con fila vacía, fondos propios/diarios y QR temporal. APK código 26. Pruebas y límites por API registrados. |

| `v0.12.1` | `main` | Corrige el fondo para cubrir toda la pantalla. APK código 28; recuperaciones por API con código 29. |

Las etiquetas identifican entregas y no deben moverse ni sobrescribirse. Las ramas permiten continuar el trabajo de cada linea. `main` queda en la adaptacion mas reciente; el TV Box fisico sigue en 0.10.4. La entrega vigente tiene dos recuperaciones con código 29: 0.10.4 para API 29 y 0.12.0 para API 31; no son las etiquetas históricas ni la entrega original código 22. El instalador las selecciona por API; una actualización posterior necesita código 30 o superior.

## Elegir una base

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

El historial es local y no hay un remoto configurado. Publicarlo requiere elegir un repositorio remoto; no se publico ni se subieron datos a servicios externos durante este trabajo.

0.12.1 conserva los controles y la proporción de las fotos. Su recuperación API 29 conserva 0.10.4; la de API 31 vuelve a 0.12.0. Después de instalar una recuperación código 29 se necesita código 30 o superior para actualizar. Las recuperaciones código 27 corresponden a la entrega anterior.
