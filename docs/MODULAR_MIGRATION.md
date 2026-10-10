# Migración modular por componentes (Velyntora-Core)

## Estado real (10-10-2026)

Se inspeccionó el árbol de `main`. `MainActivity.java` tiene 3391 líneas y `DrawingView.java` 1241. Hay clases en `color/`, `workspace/`, `document/`, `formats/` y `effects/`. La migración de lógica de producción todavía está pendiente.

## Convención final

- `app/src/main/java/art/velyntora/core/components/<componente>/`: código Java Android agrupado por responsabilidad funcional. Cada componente puede contener `panel/`, `model/`, `operations/`, `rendering/` solo cuando tenga código real.
- `core/`: implementaciones C++ existentes, reorganizadas de manera incremental sin romper CMake ni ABI.
- `tools/`: **no modificar**. Línea/Curva permanece congelada.
- `app/src/main/res/`: recursos Android con nombres prefijados por componente; Android no admite recursos arbitrarios en carpetas por herramienta.
- `tests/` y `app/src/androidTest/`: pruebas vinculadas al componente.
- `docs/`: contratos, decisiones y mapa de dependencias.

## Contratos

`app` coordina componentes; los componentes no dependen de `MainActivity`. Los paneles llaman a interfaces del componente, no a datos privados de otro panel. `tools/` usa los contratos existentes de lienzo, color, capas e historial. El código de `DrawingView` y los símbolos JNI solo se extraen con pruebas de regresión y revisión de ABI.

## Orden de ejecución

1. Congelar contratos y medir compilación base.
2. Extraer `components/color/` y conectar al estado existente.
3. Extraer `components/layers/` con pruebas de orden, opacidad y visibilidad.
4. Extraer `components/history/` y `components/selection/`.
5. Extraer `components/workspace/`, menús y barras.
6. Separar `components/canvas/` de la vista Android, preservando zoom, eventos y renderizado.
7. Migrar formatos, efectos y persistencia sin romper API/JNI.
8. Verificar pruebas Java/C++, compilación APK y pruebas en dispositivo.

Cada paso requiere compilación y pruebas antes de integrar. No mover carpetas solo por estética. No crear motor de animación en Core sin autorización explícita.

## Progreso

**Fase de arquitectura y controles de migración**. No se ha demostrado aún un porcentaje global de migración funcional. No confundir documentación con código migrado.
