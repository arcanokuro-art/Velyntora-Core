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
- [x] 12. Mover, recortar y transformar selecciones.
- [x] 13. Herramientas de texto y formas adicionales.
- [x] 14. Pinceles, borrador alfa y configuración avanzada.
- [x] 15. Zoom, panorámica y rotación del lienzo.
- [ ] 16. Gestión completa de tamaños y formatos.
- [x] 17. Ajustes de color de Pinta.
- [ ] 18. Efectos y filtros de Pinta.
- [x] 19. Guardar y abrir proyectos editables con capas.
- [ ] 20. Pulido de interfaz, accesibilidad y corrección de defectos detectados durante desarrollo.

**Avance verificado por entregables: 16/20 = 80.00%**. El entregable 15 incluye zoom de 1–7000 %, panorámica y rotación con dos dedos, controles del menú Ver y coordenadas inversas para las herramientas. Validado con pruebas Java y compilación Android del commit `e90a779f6c8dc8f6c5cea4a1bf33d7ae634e9507` (Actions 37975450908 y 37975450586). La cifra mide estos hitos y no representa paridad exhaustiva con cada comando de Pinta. Las funcionalidades marcadas pueden tener limitaciones y defectos pendientes.

## Regla del 100.00 %

El 100.00 % se alcanza al terminar **la programación** de los 20 entregables, no al realizar la prueba manual del APK en el teléfono del usuario. La instalación, ejecución y revisión visual por parte del usuario son una **fase posterior e independiente** que no se incluye en el denominador. Las compilaciones y pruebas automatizadas de desarrollo siguen siendo controles de calidad del código.

## Proyectos editables (entregable 19, verificado)

Formato `.vlycore` versión 1: dimensiones, orden y nombres de capas, visibilidad, opacidad, capa activa y píxeles ARGB completos. Lectura/escritura por bloques de 4 KiB en un hilo de trabajo; no se crea una copia completa del archivo. Se valida el formato, dimensiones, cantidad de capas, propiedades y archivo completo antes de sustituir el documento. Un archivo inválido conserva el dibujo y su historial. Límites: 32 capas y 24 millones de píxeles acumulados; tras el bloque de tamaños, abre proyectos con dimensiones variables dentro del límite Android. Undo/Redo comienza vacío al abrir y funciona con las ediciones posteriores. Validado con pruebas C++ y JNI, y compilación Android del commit `c4c80f337bd76350642b2aaedc66ece5eb822620` (Actions 37975859273 y 37975859142).

## Tamaños y formatos (entregable 16, parcial)

Nuevo documento con ancho/alto, tamaño de imagen con remuestreo por píxel cercano, tamaño del lienzo desde la esquina superior izquierda y recorte multicapa a los límites de la selección. Las dimensiones forman parte de Undo/Redo y de los proyectos. Importación sin deformar la relación de aspecto y exportaciones PNG, JPEG sobre blanco y WebP. Límite Android: 4 millones de píxeles por documento, 8192 px por lado y 24 millones acumulados en capas; otros formatos de Pinta siguen pendientes. Este bloque no se cuenta como entregable 16 completo.

## Pincel y borrador (entregable 14, verificado)

Puntas circular/cuadrada; radio de 1–128 px; opacidad y dureza de 0–100 %; presión del stylus sobre el radio. Mezcla alfa source-over y borrador que reduce alfa sin pintar el fondo. El segmento se rasteriza una vez por píxel para no multiplicar artificialmente la opacidad por el solapamiento de dabs. Levantar el dedo sin desplazamiento no aplica tinta otra vez. Pruebas de mezcla, borrador parcial, bordes suaves y punta cuadrada más APK verificados en `ff72e6ad3ae9f4529e95d97e788a03a769aa2689` (Actions 37976717112 y 37976717255). Quedan detalles transversales de selección y cancelación de gestos dentro del pulido del entregable 20.

## Texto y formas (entregable 13, verificado)

Texto multilínea colocado al tocar el lienzo, con tamaño de 4–256 px, negrita/cursiva y familias sans-serif/serif/monospace. Se rasteriza en la capa activa; se puede deshacer, seleccionar y mover, pero no reabrir como objeto tipográfico editable. Rectángulo redondeado y triángulo con variantes de contorno/relleno y alfa. Paleta de herramientas en tres columnas con desplazamiento; capas ocultables para dar espacio al lienzo en teléfonos. Recursos originales de Pinta adaptados como vectores Android para texto, selección libre, varita, movimiento y rectángulo redondeado; icono propio de triángulo.

Validación de texto/formas: compilación Android y pruebas C++/Java/JNI del commit `c42c98c56e1106df744385e0715bec0daef442c2` (Actions 37977246850 y 37977246851). La prueba JNI comprueba transparencia, mezcla de los píxeles insertados y Undo/Redo; el aspecto y la interacción en pantalla quedan para revisión en Android.

## Efectos (entregable 18, parcial)

Siete efectos parametrizados sobre la capa activa: desenfoque de caja (premultiplicación de alfa para evitar halos), enfoque, detección de bordes, relieve, pixelado, ruido y viñeta. Se calculan en un hilo de trabajo con un único punto de Undo/Redo. Este bloque **no completa** el catálogo de Pinta: siguen pendientes los otros desenfoques, distorsiones, efectos artísticos y generadores. No puntúa como entregable 18 completo.

## Correcciones transversales

El pincel/borrador respeta selecciones rectangulares, elípticas, libres y de varita mediante una máscara. El primer toque de un gesto de dos dedos no modifica el documento ni añade un punto al historial; un toque de pintura se aplica al levantar el dedo. Los gestos iniciados después de un trazo ya dibujado conservan ese tramo. El límite acumulado de 24 millones de píxeles se comprueba también al añadir capas.

## Selecciones: mover, recortar y transformar (entregable 12, verificado)

Movimiento por arrastre y desplazamiento numérico, recorte real del documento en todas las capas, escalado horizontal/vertical de 10–400 %, rotación de −180 a +180° y reflejos. Se aplica la máscara de cualquier tipo de selección, conserva los píxeles excluidos y mezcla el alfa sobre el destino. El resultado mantiene una máscara de selección transformada y usa un solo punto de Undo/Redo. Remuestreo por píxel cercano; la parte que salga del documento se recorta, recuperable mediante Deshacer.

Validación del entregable 12: pruebas C++ del remuestreo y máscaras, pruebas de llamadas JNI desde Java con reflejo/Undo/Redo y compilación APK del commit `453f8f1f76ced79c6ebeaddbe9aeb901f732ad6b` (Actions 37978432820 y 37978432930). Se corrigió una publicación incompleta y se verificó el árbol final completo frente a la revisión anterior: ningún archivo eliminado.

**Resumen actual: 16 de 20 entregables verificados = 80.00 %.** Abiertos: 09 (paridad/adaptación completa de interfaz), 16 (todos los formatos), 18 (catálogo completo de efectos), 20 (pulido/accesibilidad/defectos transversales). Los diálogos de parámetros usan desplazamiento para que todos los controles sean accesibles en pantallas pequeñas y con el teclado visible.

## Ajustes de color avanzados (entregable 17, verificado)

Curvas suaves editables con hasta 32 puntos y canal RGB/R/G/B/luminosidad; niveles independientes con entrada, salida y gamma por canal; niveles automáticos con recorte de histogramas; posterización RGB independiente y tono/saturación/luminosidad combinados. Procesamiento fuera del hilo de interfaz, alfa conservado, píxeles totalmente transparentes intactos y un solo paso de Undo/Redo. Los valores inválidos y operaciones sin cambios no alteran el historial. Se incluyen pruebas C++, interpolación Java e integración JNI. Brillo/contraste combinados y blanco y negro, invertir y sepia completan las nueve familias de ajustes base en un menú propio. Validado con C++/Java/JNI y APK del commit `0955b4fec74da61437ef092bbaa3f0b9ce89039d` (Actions 37980592850 y 37980592486). El procesamiento afecta a la capa activa; no incluye previsualización interactiva ni igualdad numérica exacta con Pinta.

## Exportación adicional (entregable 16, parcial)

BMP de 24 bits con fondo blanco y TGA de 32 bits con alfa, escritos fila a fila en un hilo de trabajo. Se comprueban cabeceras, orden de filas, relleno, transparencia y composición mediante pruebas Java. Siguen pendientes formatos adicionales. Importación TGA de 24/32 bits, sin compresión o con RLE, con ambos orígenes y validación de paquetes truncados o fuera de límites antes de sustituir el documento. No se suma el entregable 16.

## Desenfoques adicionales (entregable 18, parcial)

Gaussiano separable con radio 0–32 px, movimiento con distancia y dirección, radial con centro configurable y arco, y zoom con centro e intensidad. Muestreo bilineal y mezcla premultiplicada por alfa para no contaminar bordes con RGB oculto de píxeles transparentes. Cada aplicación se ejecuta fuera de la interfaz y ocupa un solo paso del historial. Pruebas de simetría, constantes, identidad, direcciones, transparencia, parámetros inválidos y llamadas JNI con Undo/Redo. El catálogo completo de efectos sigue pendiente y no suma el entregable 18. Se corrigió también la curva de luminosidad identidad para colores próximos al negro.

## Espacio adaptable (entregable 09, parcial)

A partir de 720 dp, herramientas a la izquierda, lienzo central y capas a la derecha. En teléfonos o ventanas más estrechas, los paneles son superpuestos y se abren desde botones; nunca restan ancho al lienzo, sólo uno queda abierto, y elegir herramienta cierra su panel. Los cambios de orientación/tamaño conservan la misma vista de dibujo, sus ajustes y selección. Política de ancho probada de 240 a 1280 dp. Los colores tienen descripción accesible y superficie táctil de 48 dp; Deshacer/Rehacer tienen etiquetas completas. La paridad visual completa continúa pendiente y no suma el entregable 09.

Validación de formatos BMP/TGA: pruebas Java y compilación Android del commit `35b9d6025eef758f0cbdef494de5ef522b7f8969` (Actions 37981022159 y 37981022107). Validación de los cuatro desenfoques y corrección de curvas: C++/Java/JNI y APK del commit `524e47a3c047f3f4d6336904428c51c515ee5b9d` (Actions 37981283105 y 37981283130).

Pulido adicional: selector de color RGBA completo, controles de curva con entrada/salida numérica además del gráfico, etiquetas habladas en sliders y acciones de capas. Estos cambios no cierran la revisión transversal de accesibilidad del entregable 20.

## OpenRaster (entregable 16, parcial)

Abrir/guardar `.ora` con capas PNG normales, nombres UTF-8, orden, visibilidad, opacidad, desplazamiento de capas y capa activa. Archivo ZIP con `mimetype` primero y sin compresión, `stack.xml` y `mergedimage.png`. La conversión se realiza en un hilo de trabajo, con archivos temporales eliminados al finalizar. La importación se valida antes de reemplazar atómicamente el documento nativo. Grupos y modos de mezcla distintos de normal se rechazan; no se importan con una apariencia incorrecta. Límites de tamaño, número de capas, XML y entradas ZIP; no se extraen rutas ZIP al sistema de archivos. La gestión completa de formatos sigue pendiente: no se suma el entregable 16.

## Distorsiones y selecciones (entregable 18, parcial)

Remolino, abombar/pellizcar, ondas radiales, cristales y escarcha con intensidad, radio/periodo, dirección y centro según el efecto. Interpolación bilineal premultiplicada por alfa; escarcha reproducible. Curvas, ajustes avanzados, los siete efectos iniciales, desenfoques y distorsiones respetan la máscara de selección capturada antes del hilo de trabajo y conservan el contorno. Los niveles automáticos usan sólo el histograma seleccionado. Las operaciones sin cambio no añaden historial; cada aplicación efectiva ocupa un solo paso de Undo/Redo. Los antiguos extras de color conservan su comportamiento sobre toda la capa. Estos avances no completan el catálogo: el porcentaje permanece en 80.00 %.

Validación OpenRaster: pruebas con PNG reales y compilación APK del commit `6a8e771e4e27a91fc458064ce7d327fc89f07178` (Actions 37984360200 y 37984360235). Referencia de formato: https://www.openraster.org/baseline/file-layout-spec.html y https://www.openraster.org/baseline/layer-stack-spec.html . Las pruebas cubren orden, UTF-8, visibilidad, opacidad, capa activa, composición, miniatura y metadatos inválidos; la interacción visual en Android queda para la fase manual.

## TIFF (entregable 16, parcial)

Exportación TIFF RGB de 8 bits por canal con alfa no asociado, sin compresión, en orden de filas superior a inferior y escrita por bloques de fila en el hilo de exportación. Las pruebas verifican IFD, etiquetas, offsets, alfa e ida/vuelta con el decodificador TIFF de Java. La importación TIFF y otros formatos aún están pendientes, por lo que no se suma el entregable 16. La lectura de `stack.xml` OpenRaster valida UTF-8 y admite BOM.

## Arte y fotografía (entregable 18, parcial)

Pintura al óleo con radio y niveles de intensidad, boceto a lápiz, boceto a tinta con umbral de contorno, resplandor, retrato suave, mediana/percentil y reducción de ojos rojos. Conservan el alfa original y los píxeles totalmente transparentes; el óleo ignora los colores ocultos y pondera por alfa. Procesamiento fuera del hilo de interfaz, máscara de selección y un único punto de historial. Implementaciones propias: no se afirma igualdad numérica con Pinta. Pruebas del motor y llamadas JNI con Undo/Redo y máscara vacía. El catálogo completo sigue pendiente; avance global 16/20 = 80.00 %.

Validación de los siete efectos de arte/foto: pruebas C++ y JNI con Undo/Redo y APK del commit `37763930b6da60d1f571c45fd15389f57d23a37c` (Actions 37986391469 y 37986391407). Optimización de óleo y mediana mediante ventanas deslizantes; comparación exacta contra la implementación previa sobre dimensiones y radios variados, y prueba de percentiles contra una referencia independiente por ordenación. Medición local orientativa de 512×512, compilación `-O2`: óleo 347→95 ms; mediana 164→57 ms. Estas cifras no corresponden a un dispositivo Android y no forman parte del porcentaje global.

## Generadores (entregable 18, parcial)

Nubes por ruido suave multiescala, Voronoi, celdas, Mandelbrot y Julia. Escala/zoom, octavas/iteraciones y semilla según el generador; primer color de dibujo y segundo blanco/negro/transparente. Interpolación de colores premultiplicada por alfa. Generan contenido nuevo en la capa activa o selección; los píxeles excluidos se conservan y el cambio se deshace en un solo paso. Pruebas de determinismo, semillas, paletas uniformes, transparencia, límites y JNI. El catálogo sigue parcial; avance 80.00 %.

## Resto del catálogo de filtros y objetos (entregable 18, pendiente de validación)

Fragmentar, desenfoque de lente mediante disco muestreado, abolladuras por ruido suave, inversión polar, reducción de ruido con tolerancia de color, contorno de bordes, relieve direccional y tramado ordenado. Objetos: alinear en nueve posiciones dentro del lienzo/selección, suavizar alfa hacia dentro y dibujar un contorno detrás del objeto. La detección de objetos se basa en alfa, por lo que requiere fondo transparente. Distancias euclidianas calculadas en dos pasadas; no se realiza una búsqueda de todos los puntos de borde para cada píxel. Los efectos respetan selecciones y tienen Undo/Redo. Implementaciones propias con parámetros acotados para Android; no se promete reproducción píxel a píxel de Pinta ni sus opciones de previsualización. El entregable 18 todavía no se suma hasta verificar toda la integración y el APK.
