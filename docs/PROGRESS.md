# Velyntora Core — medición de avance

Objetivo: reproducir la experiencia y las funciones de Pinta 3.1.2 en Android. El porcentaje global se calcula sobre una lista de **20 entregables** de igual peso (5 puntos cada uno). Cada entregable cuenta únicamente cuando está integrado y su compilación/pruebas pasan; una implementación parcial vale 0 en esta métrica. Este listado es una referencia de planificación, no un inventario exhaustivo de comandos individuales de Pinta.

- [x] 01. Proyecto Android con compilación automatizada.
- [x] 02. Motor de lienzo C++ y pruebas básicas.
- [x] 03. Pincel y herramientas geométricas básicas.
- [x] 04. Abrir imágenes y exportar PNG.
- [x] 05. Capas básicas conectadas al lienzo.
- [x] 06. Deshacer/rehacer multicapa básico.
- [x] 07. Panel de capas con miniaturas y opacidad (funcionalidad básica programada).
- [x] 08. Iconos originales de Pinta integrados (siete herramientas básicas; otros recursos visuales pendientes).
- [ ] 09. Diseño adaptable equivalente a Pinta.
- [x] 10. Selecciones rectangulares y elípticas (crear, borrar, copiar, cortar, pegar y mover contenido; sin selección libre).
- [x] 11. Selección libre y varita mágica (máscaras para copiar, borrar y mover; tolerancia configurable y regiones conectadas).
- [ ] 12. Mover, recortar y transformar selecciones.
- [ ] 13. Herramientas de texto y formas adicionales.
- [x] 14. Pinceles, borrador alfa y configuración avanzada.
- [x] 15. Zoom, panorámica y rotación del lienzo.
- [ ] 16. Gestión completa de tamaños y formatos.
- [ ] 17. Ajustes de color de Pinta.
- [ ] 18. Efectos y filtros de Pinta.
- [x] 19. Guardar y abrir proyectos editables con capas.
- [ ] 20. Pulido de interfaz, accesibilidad y corrección de defectos detectados durante desarrollo.

**Avance verificado por entregables: 13/20 = 65.00%**. El entregable 15 incluye zoom de 1–7000 %, panorámica y rotación con dos dedos, controles del menú Ver y coordenadas inversas para las herramientas. Validado con pruebas Java y compilación Android del commit `e90a779f6c8dc8f6c5cea4a1bf33d7ae634e9507` (Actions 37975450908 y 37975450586). La cifra mide estos hitos y no representa paridad exhaustiva con cada comando de Pinta. Las funcionalidades marcadas pueden tener limitaciones y defectos pendientes.

## Regla del 100.00 %

El 100.00 % se alcanza al terminar **la programación** de los 20 entregables, no al realizar la prueba manual del APK en el teléfono del usuario. La instalación, ejecución y revisión visual por parte del usuario son una **fase posterior e independiente** que no se incluye en el denominador. Las compilaciones y pruebas automatizadas de desarrollo siguen siendo controles de calidad del código.

## Proyectos editables (entregable 19, verificado)

Formato `.vlycore` versión 1: dimensiones, orden y nombres de capas, visibilidad, opacidad, capa activa y píxeles ARGB completos. Lectura/escritura por bloques de 4 KiB en un hilo de trabajo; no se crea una copia completa del archivo. Se valida el formato, dimensiones, cantidad de capas, propiedades y archivo completo antes de sustituir el documento. Un archivo inválido conserva el dibujo y su historial. Límites: 32 capas y 24 millones de píxeles acumulados; la interfaz de la versión validada abre proyectos de 800 × 800. Undo/Redo comienza vacío al abrir y funciona con las ediciones posteriores. Validado con pruebas C++ y JNI, y compilación Android del commit `c4c80f337bd76350642b2aaedc66ece5eb822620` (Actions 37975859273 y 37975859142).

## Tamaños y formatos (entregable 16, parcial)

Nuevo documento con ancho/alto, tamaño de imagen con remuestreo por píxel cercano, tamaño del lienzo desde la esquina superior izquierda y recorte multicapa a los límites de la selección. Las dimensiones forman parte de Undo/Redo y de los proyectos. Importación sin deformar la relación de aspecto y exportaciones PNG, JPEG sobre blanco y WebP. Límite Android: 4 millones de píxeles por documento, 8192 px por lado y 24 millones acumulados en capas; otros formatos de Pinta siguen pendientes. Este bloque no se cuenta como entregable 16 completo.

## Pincel y borrador (entregable 14, verificado)

Puntas circular/cuadrada; radio de 1–128 px; opacidad y dureza de 0–100 %; presión del stylus sobre el radio. Mezcla alfa source-over y borrador que reduce alfa sin pintar el fondo. El segmento se rasteriza una vez por píxel para no multiplicar artificialmente la opacidad por el solapamiento de dabs. Levantar el dedo sin desplazamiento no aplica tinta otra vez. Pruebas de mezcla, borrador parcial, bordes suaves y punta cuadrada más APK verificados en `ff72e6ad3ae9f4529e95d97e788a03a769aa2689` (Actions 37976717112 y 37976717255). Quedan detalles transversales de selección y cancelación de gestos dentro del pulido del entregable 20.

## Texto y formas (entregable 13, en validación)

Texto multilínea colocado al tocar el lienzo, con tamaño de 4–256 px, negrita/cursiva y familias sans-serif/serif/monospace. Se rasteriza en la capa activa; se puede deshacer, seleccionar y mover, pero no reabrir como objeto tipográfico editable. Rectángulo redondeado y triángulo con variantes de contorno/relleno y alfa. Paleta de herramientas en tres columnas con desplazamiento; capas ocultables para dar espacio al lienzo en teléfonos. Recursos originales de Pinta adaptados como vectores Android para texto, selección libre, varita, movimiento y rectángulo redondeado; icono propio de triángulo.
