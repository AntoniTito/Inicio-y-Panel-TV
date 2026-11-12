# Destinos y botones — 0.20.0

En **Ajustes → Aplicaciones → Mi control y arranque**, cada botón tiene una etiqueta editable, la identidad que entrega Android y una acción. Los destinos probados están disponibles para cualquier botón y para el encendido.

## Guardar una ruta

1. Abre un botón, elige **Reabrir y llegar al destino**, su aplicación y **Elegir destino en la app**.
2. Navega como siempre. En el canal o pantalla elegidos, pulsa Home y **Probar y guardar este destino**.
3. Al terminar la prueba, comprueba el resultado y pulsa **Guardar destino**.

Antes de la prueba se conserva un borrador privado. Salir, interrumpir la prueba o recrear el proceso de Z6 no lo descarta. En **Destino pendiente**, puedes reintentar sin volver a navegar, guardar una prueba ya completada o descartar el borrador. Una prueba incompleta no se puede confirmar como válida. El destino anterior permanece asignado hasta guardar el nuevo.

## Reutilizar un destino

En el editor del botón o en **Arranque y despertar**, elige **Usar destino guardado**. No hace falta volver a enseñar ni probar el mismo recorrido. También puedes asignarlo desde **Destinos guardados**.

Cada asignación conserva una copia del recorrido y sus propias teclas e interruptores. Cambiar o borrar una asignación no altera las demás. El arranque y el regreso de suspensión utilizan la misma ruta elegida en **Arranque y despertar**, con interruptores independientes. Apagar únicamente el televisor puede no producir un evento de despertar del TV Box.

Los destinos se identifican por aplicación, firma y controles. Una variante distinta o una firma nueva requiere su propia configuración. Un destino cuya app ya no está disponible permanece visible para gestionarlo, pero no se ejecuta en otra app por semejanza de nombre.

## Añadir, renombrar y eliminar botones

**Añadir botón → Comenzar → pulsa la tecla → Asignar**. No se elige primero entre nombres fijos. Android muestra su nombre interno, keycode y scan code. Algunas teclas del fabricante requieren la preparación nativa de teclas especiales; las funciones esenciales conservan su uso.

En el editor:

- **Etiqueta** cambia únicamente el nombre visible, hasta 40 caracteres. Vacía recupera el nombre de la tecla.
- **Tecla detectada** permite cambiar la asignación y muestra sus códigos reales.
- **Eliminar botón** solicita confirmación. También está disponible desde la pantalla de detección, aunque todavía no se haya asignado una tecla.

Eliminar un botón conserva los destinos guardados. Eliminar un destino de la lista común no borra los recorridos que ya tienen asignados otros botones o el arranque. Restaurar las funciones nativas del control es una operación distinta, disponible en **Preparar teclas especiales**.

## Datos y recuperación

Los destinos, borradores, etiquetas y códigos pertenecen al dispositivo; no forman parte del perfil de réplica ni se suben a Git. El respaldo de botones incluye esos datos. No hay consultas de pantalla ni nuevas tareas periódicas para mantener esta lista.

La recuperación 0.19.0/c59 conserva los recorridos ya asignados por su formato compatible. Esa versión no ofrece la biblioteca común, los borradores ni las etiquetas nuevas en su interfaz; el restaurador conserva su exportación privada para una actualización posterior. Tras instalar c59 se necesita c60 o superior. No desinstales Z6 para recuperar una versión.
