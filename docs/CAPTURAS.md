# Recorrido visual

Capturas reales de las **interfaces actuales de Inicio y Panel TV 0.20.2/c62**, obtenidas en un emulador Android TV 12/API 31 separado, a **1280 × 720**. Usan nombres ficticios, aplicaciones de prueba y gráficos de demostración. El auxiliar de la demostración se inició desde ADB autorizado; estas imágenes no prueban recuperación autónoma tras reiniciar. No son fotografías del TV Box original ni pruebas físicas de reproducción o de un control Bluetooth.

Las interfaces nativas previas al uso del proyecto se consultan en [Android TV de fábrica](ANDROID-TV-DE-FABRICA.md). El archivo del desarrollo está separado en [Evolución del proyecto](EVOLUCION-DEL-PROYECTO.md).

## Inicio

Reloj, fecha, saludo, fondo y aplicaciones en la pantalla principal. La configuración de demostración usa una imagen abstracta propia.

![Inicio personalizable](capturas/inicio.png)

## Panel superpuesto

Apps y Acciones tienen selección circular independiente. Arriba y abajo cambian de fila; la edición permanece en Apps. El reloj superior se puede desactivar.

![Panel superpuesto sobre una aplicación de demostración](capturas/panel.png)

## Ajustes

Ocho entradas para aplicaciones, equipo, sonido, conexiones, personalización y los ajustes completos de Android. Las capacidades dependen de los permisos y del firmware.

![Menú de Ajustes](capturas/ajustes.png)

## Fondos del inicio

Cuatro modos en una fila, vista previa y controles contextuales. Las flechas previsualizan; las elecciones se aplican con la acción correspondiente.

![Fondos con vista previa y gestión de fotos](capturas/fondos.png)

## Mis fotos

Miniaturas, información y organización. Las imágenes recibidas se guardan optimizadas en el TV; borrar desde el panel elimina su copia y miniaturas, sin papelera.

![Biblioteca de imágenes de demostración](capturas/biblioteca.png)

## Paisajes diarios

Hasta doce paisajes, con miniaturas persistentes durante el día, conservación permanente y rotación opcional. Las fotografías mantienen sus propias licencias, separadas de la GPL del código.

![Catálogo diario de paisajes](capturas/paisajes.png)

## Mi control y arranque

Cada botón tiene su aplicación, etiqueta y acción. Las teclas y recorridos de las capturas son sintéticos; no representan una asignación universal de Netflix o YouTube.

![Configuración independiente de los botones](capturas/control.png)

## Destinos guardados

Una sección probada puede reutilizarse en botones y arranque/despertar. Las acciones de usar y eliminar permanecen visibles junto a la lista.

![Lista y acciones de destinos guardados](capturas/destinos.png)

## Recursos y créditos

Los tres gráficos abstractos de la biblioteca fueron generados para esta demostración por el proyecto, sin usar fotos personales. Las aplicaciones de demostración proceden de la fixture incluida en `pruebas/fixture-tv`. Las capturas conservan la interfaz real; no son maquetas generadas.

Los créditos de las fotografías visibles del catálogo y los avisos sobre las capturas históricas se registran en [CRÉDITOS DE LAS CAPTURAS](CREDITOS-CAPTURAS.md). Los paisajes se muestran como miniaturas o encuadres de la interfaz, sin incorporar una colección al APK.
