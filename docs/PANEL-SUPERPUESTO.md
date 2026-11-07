# Navegar por el panel superpuesto

Desde otra aplicación, Home abre el panel. Apps y Acciones mantienen su propia selección.

| Control | Uso normal | Edición de Apps |
|---|---|---|
| Izquierda / derecha | Recorre la fila; el último vuelve al primero y viceversa | Mueve la app, con límites; después de la última está Añadir |
| Arriba | Apps → Acciones; en Acciones permanece | Oculta la app seleccionada |
| Abajo | Acciones → Apps; en Apps permanece | Permanece en Apps |
| OK | Abre la app o ejecuta la acción | Guarda; sobre Añadir abre el selector |
| OK sostenido en una app | Entra en edición | Conserva los controles de edición |
| Atrás | Vuelve un nivel; desde el panel lo cierra | Sale primero del menú hijo; desde Apps cancela la edición |

Las flechas muestran la tarjeta seleccionada mediante desplazamiento nativo. No se duplican tarjetas. Cerrar y reabrir el panel recuerda las selecciones dentro del mismo proceso; al reiniciar el proceso, Acciones comienza en Audio y Apps en su primera tarjeta.

Si no hay aplicaciones, aparece **Añadir aplicación**. Al ocultar la última durante la edición quedan **Añadir** y **Guardar**, con Guardar seleccionado. Arriba no oculta estas dos tarjetas. Cancelar restaura el orden, la visibilidad y una selección válida de Apps.

Entrar en Añadir y después en Instalar aplicaciones conserva la sesión: cada Atrás vuelve un nivel. Elegir una app la coloca al final y deja el foco sobre ella; cancelar la edición restaura los cambios previos.

## Verificación técnica

`BottomNavigation` mantiene IDs de paquete y de acción en memoria. `PanelService` conserva la edición y la pila de páginas. Las flechas actualizan solo los resaltados anterior y nuevo; no consultan iconos, escanean aplicaciones ni escriben preferencias.

El diagnóstico ADB protegido conserva `bottomSelection` como índice combinado calculado y añade `bottomSection`, `bottomFocusedId` y `bottomSectionIndex`. `bottomSaveVisible` permite comprobar el caso de la última app oculta.

Las pruebas de esta versión están en [VERIFICACION-0.17.0.md](VERIFICACION-0.17.0.md). Las de 0.16.5 son históricas; para las filas actuales usa `pruebas/carrusel-panel-0170.cjs`, que selecciona mediante IDs y no presupone un índice global fijo.
