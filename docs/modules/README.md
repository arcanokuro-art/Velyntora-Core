# Catálogo de componentes

Este índice define los propietarios de funciones. Los módulos se crean físicamente cuando su primera clase esté lista para migrarse.

| Componente | Responsabilidad | Contrato esperado |
| --- | --- | --- |
| color | color activo/secundario, paletas, selector y panel | API de lectura/escritura de color |
| layers | modelo, operaciones, composición y panel | API de capas con comandos reversibles |
| history | deshacer, rehacer y transacciones | API de historial |
| selection | máscaras, selección y transformaciones | API de selección |
| canvas | viewport, zoom, render y entrada | API de coordenadas y eventos |
| workspace | distribución de paneles, menús y barras | interfaz de comandos |
| document | sesión, dimensiones, guardado y recuperación | API de documento |
| formats | codecs y exportación | lectores/escritores sin dependencia de UI |
| effects | ajustes y filtros | operación sobre documento/selección |
| settings | preferencias y persistencia | API de configuración |

### Regla de README por componente

Documentar propósito, archivos, API pública, dependencias permitidas, recursos, pruebas y pasos de migración. No agregar directorios vacíos ni duplicar `tools/`.

### Compatibilidad

Mantener los paquetes y clases actuales hasta que cada migración esté conectada, compilada y probada. Los recursos siguen bajo `app/src/main/res`; los iconos de herramientas actuales no se alteran.
