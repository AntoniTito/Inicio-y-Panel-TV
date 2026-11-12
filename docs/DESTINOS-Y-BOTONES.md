# Destinos y botones — 0.20.1

En **Ajustes → Aplicaciones → Mi control y arranque**, cada botón tiene una etiqueta editable, la identidad que entrega Android y una acción. Los destinos probados están disponibles para cualquier botón y para el encendido.

## Abrir una aplicación sin recorrido

Elige **Abrir app · sin recorrido** o **Cerrar y abrir app · sin recorrido** y después la aplicación. No se pide un canal ni una sección. Cambiar a otra app deja el botón en apertura normal; una ruta de la app anterior no se hereda.

Llegar a una sección concreta es opcional y sirve también para aplicaciones sin televisión. En **Arranque y despertar** se puede abrir la pantalla habitual sin permiso de navegación ni conexión a Internet; el reinicio de la app sigue requiriendo la capacidad de cierre comprobado. Los recorridos automáticos guardados conservan su preparación de red.

## Guardar una ruta

1. Abre un botón, elige su aplicación, **Reabrir en una sección · opcional** y **Elegir destino en la app**.
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

Eliminar un botón conserva los destinos guardados. Restaurar las funciones nativas del control es una operación distinta, disponible en **Preparar teclas especiales**.

## Eliminar una ruta

En **Destinos guardados**, elige la ruta. A su derecha están **Usar en un botón**, **Usar al arrancar o despertar** y **Eliminar destino**. Atrás vuelve a Mi control.

La confirmación muestra los botones y el encendido que la usan:

- **Eliminar y quitar usos:** borra la ruta y su borrador coincidente; los botones permanecen con la acción de cerrar y abrir la app sin recorrido. El arranque/despertar asociado se desactiva. No elimina apps ni sus datos.
- **Solo de la lista:** elimina el destino común y conserva las copias ya asignadas.
- **Cancelar:** no cambia nada.

Si la ruta no tiene usos, basta **Eliminar destino**. También puedes quitar el recorrido de un botón concreto desde su editor o elegir **Abrir al inicio de la app · sin recorrido** para el encendido, conservando la ruta común.

## Datos y recuperación

Los destinos, borradores, etiquetas y códigos pertenecen al dispositivo; no forman parte del perfil de réplica ni se suben a Git. El respaldo de botones incluye esos datos. No hay consultas de pantalla ni nuevas tareas periódicas para mantener esta lista.

La recuperación 0.20.0/c61 conserva biblioteca, borrador y asignaciones. Se obtiene del commit histórico de 0.20.0 y añade únicamente la declaración singleTask necesaria para recuperar el acceso a Mi control. Conserva la interfaz anterior de esa versión; las mejoras nuevas de gestión se recuperan al actualizar después. El restaurador exporta los datos privados y restaura HOME, permisos y teclas originales del equipo. Tras instalar c61 se necesita c62 o superior. No desinstales Z6 para recuperar una versión.
