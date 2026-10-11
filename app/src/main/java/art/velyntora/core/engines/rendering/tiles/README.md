# Texturas de presentación

`TileDisplay` separa las texturas de pantalla en bloques de 256 px. El nivel de
muestreo depende del zoom; al ampliar a escala >= 1 se conservan los píxeles
originales. Caché de hasta 128 texturas (32 MiB), invalidación sólo de las texturas
que intersectan un cambio. La región visible limita el dibujo. El calentamiento
inicial de la importación se hace en su trabajador, antes de publicar la pantalla.

Esto evita subir el bitmap completo a la GPU en cada gesto. El bitmap canónico y
el lienzo nativo aún son completos: no constituye soporte de 10000 × 10000.
Las texturas incluyen un margen de muestreo y recortan su celda al dibujar, para evitar costuras al filtrar sus bordes. La invalidación incluye ese margen. El importador conserva el diálogo hasta instalar la imagen y sus texturas preparadas; la extracción de píxeles y la transferencia JNI se hacen en el trabajador.

Las texturas modificadas se conservan. `BitmapSampler` copia y promedia únicamente
las celdas afectadas mediante lecturas acotadas; no usa `Canvas.drawBitmap` para
remuestrear el bitmap completo del documento. El muestreo pondera RGB por alfa y
conserva píxeles exactos a escala 1. El importador prepara también el siguiente
nivel de zoom cuando ambos niveles caben en la caché. Las pruebas comprueban
reutilización, cantidad de píxeles leídos y fotogramas reales durante un trazo continuo.
