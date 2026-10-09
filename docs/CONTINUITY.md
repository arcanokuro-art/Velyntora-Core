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

Consultar `PROGRESS.md`, no aumentar el porcentaje por cantidad de commits. Completar los bloques abiertos 09, 12, 16, 17, 18 y 20. Las implementaciones parciales se describen expresamente y no puntúan como hitos cerrados. Para la paridad exhaustiva, comparar los comandos y herramientas con la referencia Pinta: el denominador de 20 hitos no es una lista de cada comando de Pinta.

Comprobar las ejecuciones Android APK y Core tests del SHA publicado. Core tests incluye pruebas de píxeles, capas, proyectos, redimensionado, pinceles y efectos, coordenadas Java y pruebas del puente JNI con llamadas desde una JVM. Mantener el documento intacto al rechazar archivos o entradas inválidas.
