# Arquitectura modular integral — Velyntora-Core

## Alcance y reglas

**Conservar `tools/` exactamente como está.** Sus 28 herramientas, controladores, iconos, recursos nativos y contratos compartidos ya están organizados. Este plan afecta al resto de la aplicación y será la referencia para migrar posteriormente al repositorio Velyntora; **no se modifica Velyntora aquí**.

Cada componente tiene propietario único, API explícita y pruebas. Evitar que un módulo lea campos internos de otro. Los cambios en servicios compartidos requieren pruebas de regresión de todos sus consumidores. No duplicar motores de capas, lienzo, colores o historial. La carpeta de cada componente aloja sus fuentes y tests; los recursos Android compilados se registran en `res/` con nombres prefijados.

## Estructura objetivo (paquetes Java bajo app/src/main/java/art/velyntora/core/)

```text
app/
  src/main/java/art/velyntora/core/
    app/                 # Inicio, navegación, ciclo de vida y composición de dependencias
    ui/
      workspace/         # Ventana de trabajo, distribución y paneles acoplables
      menus/
        file/ edit/ view/ image/ layers/ adjustments/ effects/ help/
      dialogs/           # Diálogos comunes; específicos junto al módulo propietario
      toolbar/           # Barra superior y controles de herramienta
      statusbar/         # Estado, zoom, tamaño y coordenadas
    panels/
      colors/            # Panel de colores, muestras, selector, paletas, historial de color
      layers/            # Panel de capas, miniaturas, visibilidad, orden, opacidad
      history/           # Panel de historial y navegación de acciones
      tools/             # Presentación/selección de herramientas; lógica permanece en tools/
    canvas/              # Vista, eventos, renderizado, zoom, viewport, cuadrícula
    document/            # Modelo de proyecto, dimensiones, sesiones, carga/guardado
    layers/              # Modelo de capa, operaciones y composición compartida
    history/             # Comandos, undo/redo, snapshots y transacciones
    selection/           # Selecciones y máscaras compartidas
    colors/              # Modelo de color, conversiones, paletas (no UI)
    effects/             # Ajustes y efectos, agrupados por efecto
    formats/             # Un subdirectorio por codec: png/, jpg/, bmp/, gif/, ...
    settings/            # Preferencias, almacenamiento y migraciones
    input/               # Eventos de lápiz, tacto, presión y gestos
    export/              # Opciones de exportación
    shared/              # Contratos estables y utilidades mínimas
  src/main/res/          # Drawables, strings, layouts Android compilados
  src/androidTest/       # Pruebas de integración por componente
core/
  document/ layers/ history/ canvas/ effects/ formats/  # Núcleo C++ cuando aplique
tools/                  # INTACTO: estructura aprobada
tests/                  # Pruebas JVM/C++ por componente
docs/
```

## Límites

- `panels/colors` presenta controles; `colors` gestiona datos; la selección del color llega a herramientas mediante una interfaz, no acceso directo al panel.
- `panels/layers` presenta la lista; `layers` modifica el documento; `history` registra comandos reversibles.
- `canvas` renderiza y convierte coordenadas; `tools/` recibe eventos mediante contratos existentes.
- `ui/menus` emite acciones; `app/` las coordina sin alojar sus algoritmos.
- `formats` usa el modelo de documento sin depender de UI.
- El puente JNI se separa por responsabilidad sin cambiar firmas públicas ni los símbolos que Android espera.
- `animation/` es **futuro** y no debe incorporarse a Core hasta autorizarlo; Velyntora final podrá tener motores de dibujo y animación.

## Inventario inicial observado (main, 2026-10-10)

- `MainActivity.java` y `DrawingView.java` siguen en el paquete raíz y requieren extracción gradual.
- `color/ColorPickerDialog.java` ya está separado parcialmente.
- `workspace/` contiene `WorkspaceLayout`, `WorkspaceRenderer` e iconos.
- `document/`, `effects/` y `formats/` ya contienen clases propias.
- `core/document/` y `core/effects/` contienen código C++ organizado parcialmente.
- `tools/` tiene módulos completos y no se modifica.

## Secuencia de migración segura

1. Inventariar dependencias de `MainActivity`, `DrawingView`, JNI y módulos existentes.
2. Extraer panel de colores (UI y estado), con pruebas de selección y sincronización de color.
3. Extraer panel de capas y servicios de capas, con pruebas de orden, visibilidad y undo.
4. Extraer menús, barra de herramientas, estado y diálogos.
5. Extraer canvas, entrada y composición, manteniendo los contratos de `tools/`.
6. Modularizar formatos, efectos, ajustes, configuración y puentes nativos.
7. Verificar Gradle/CMake, suites Java/C++, pruebas de dispositivo y APK antes de integrar.

**Estado:** especificación de arquitectura; no significa que la migración del código esté ejecutada ni validada.
