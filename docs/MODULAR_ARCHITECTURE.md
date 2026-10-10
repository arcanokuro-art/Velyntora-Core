# Arquitectura modular de Velyntora-Core

Esta rama organiza la migración gradual del núcleo Android. **No mover clases a ciegas**: primero aislar dependencias, introducir interfaces, migrar llamadas y ejecutar pruebas.

## Directorios objetivo

- `tools/<tool_name>/`: lógica, estado, renderizado y pruebas propias de cada herramienta.
- `canvas/`: transformación de coordenadas, viewport, píxeles y composición.
- `engines/drawing/`: motor de dibujo compartido.
- `engines/animation/`: motor de animación cuando se implemente.
- `history/`: comandos deshacer/rehacer.
- `layers/`: modelo y composición de capas.
- `formats/<format>/`: importación/exportación y pruebas de cada formato.
- `ui/menus/`, `ui/dialogs/`, `ui/workspace/`: componentes visuales.
- `shared/`: contratos y utilidades sin dependencias circulares.
- `native/`: puente JNI y componentes C++ separados por responsabilidad.

En Android, los recursos de iconos compilados se guardan en `app/src/main/res/drawable*` (con prefijos de herramienta), aunque su propiedad funcional se documente junto a cada módulo. No colocar iconos arbitrariamente en paquetes Java: Android no los compilará como recursos drawable.

## Migración priorizada

1. Registrar inventario de clases, recursos, tests, JNI y dependencias.
2. Extraer herramientas desde `DrawingView.java`, manteniendo la API pública y el comportamiento existente.
3. Separar coordinación de pantalla desde `MainActivity.java`.
4. Dividir `native_bridge.cpp` sin romper firmas JNI ni configuración de compilación.
5. Modularizar formatos, ajustes, menú, iconos y componentes auxiliares.
6. Compilar debug/release, ejecutar tests unitarios e instrumentados disponibles y corregir regresiones antes de fusionar.

## Contrato de herramienta

Cada herramienta debe tener identificador estable, ciclo de vida de activación/desactivación, manejo de entrada, estado transitorio, operaciones sobre el documento, dibujo de previsualización y pruebas. El motor compartido es dueño de capas, zoom y deshacer/rehacer.

## Condiciones de aceptación

- No cambiar la rama `main` hasta verificar la migración.
- Mantener el comportamiento de Línea/Curva: Enter confirma; deshacer recupera el estado según el historial implementado.
- Zoom de píxeles sin suavizado al ampliar cuando se requiera vista pixelada nítida.
- Sin referencias rotas a recursos ni firmas JNI incompatibles.
- CI verde y comprobación de APK en dispositivo antes de declarar 100 %.

**Estado:** diseño de migración iniciado; este documento no representa una refactorización completada ni una compilación verificada.
