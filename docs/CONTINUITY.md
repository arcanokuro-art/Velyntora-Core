# Alcance y continuidad del desarrollo

Fuente revisada: conversación compartida https://chatgpt.com/share/6ac933b5-76ec-83e8-9de4-11ffd736be8f («Continuar programación real»).

## Decisiones del usuario

- Programar únicamente `arcanokuro-art/Velyntora-Core`.
- Crear una aplicación Android de dibujo equivalente a Pinta, como proyecto independiente antes de trasladar sus funciones a Velyntora.
- Interfaz Android en Java y motor en C++; otros sistemas quedan para el futuro.
- No introducir animación en Velyntora Core.
- Pinta-2.0 se consulta como referencia; no se modifica.
- Conservar los recursos originales de Pinta en `reference/pinta-assets`; vincular los necesarios a la interfaz Android.
- Mantener Remove en pausa.
- Continuar implementación, revisión y correcciones sin confirmaciones entre etapas.
- Publicar porcentaje basado en entregables verificados. El 100 % significa terminar la programación; la prueba manual posterior en el teléfono no se incluye, pero sí las compilaciones y pruebas de desarrollo.

## Cómo continuar

El plan de 20 hitos queda cerrado en `PROGRESS.md` con la integración del PR #3. No aumentar ni reinterpretar el porcentaje por cantidad de commits. Las diferencias de formatos y efectos permanecen en `FORMAT_SUPPORT.md` y `EFFECTS_PARITY.md`; el 100 % del plan no equivale a paridad exhaustiva con cada comando o variante de Pinta. La revisión del APK en el teléfono es la siguiente fase independiente. Nuevas ampliaciones requieren un inventario propio y no deben alterar retroactivamente este denominador.

Comprobar las ejecuciones Android APK y Core tests del SHA publicado. Core tests incluye pruebas de píxeles, capas, proyectos, redimensionado, pinceles y efectos, coordenadas Java y pruebas del puente JNI con llamadas desde una JVM. Mantener el documento intacto al rechazar archivos o entradas inválidas.
