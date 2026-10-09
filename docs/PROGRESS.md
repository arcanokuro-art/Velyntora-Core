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
- [ ] 14. Pinceles, borrador alfa y configuración avanzada.
- [ ] 15. Zoom, panorámica y rotación del lienzo.
- [ ] 16. Gestión completa de tamaños y formatos.
- [ ] 17. Ajustes de color de Pinta.
- [ ] 18. Efectos y filtros de Pinta.
- [ ] 19. Guardar y abrir proyectos editables con capas.
- [ ] 20. Pulido de interfaz, accesibilidad y corrección de defectos detectados durante desarrollo.

**Avance provisional por entregables: 10/20 = 50.00%** tras CI verde de la selección libre y varita mágica, con copia, borrado y movimiento por máscaras. La cifra **no equivale a paridad funcional del 40%** con todas las opciones de Pinta, sino al cumplimiento de estos hitos definidos. Las funcionalidades marcadas pueden tener limitaciones y defectos pendientes.

## Regla del 100.00 %

El 100.00 % se alcanza al terminar **la programación** de los 20 entregables, no al realizar la prueba manual del APK en el teléfono del usuario. La instalación, ejecución y revisión visual por parte del usuario son una **fase posterior e independiente** que no se incluye en el denominador. Las compilaciones y pruebas automatizadas de desarrollo siguen siendo controles de calidad del código.
