# Componente color

**Responsabilidad:** representar el par de colores principal/secundario sin depender de la interfaz Android.

- `ColorPair.java`: modelo inmutable ARGB y operación `swapped()`.
- Consumidor inicial: `MainActivity` al intercambiar colores en el menú y el panel.
- Dependencias: ninguna; no importa clases de herramientas ni del lienzo.
- Próxima extracción: estado de paletas y panel de color, manteniendo sincronización con `DrawingView`.
- Verificación pendiente: compilación Android y pruebas de interfaz; no considerar validado hasta ejecutarlas.

La carpeta `tools/` permanece intacta.
