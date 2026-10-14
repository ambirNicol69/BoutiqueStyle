# 01 · Requisitos

## Alcance funcional

- Autenticación local con perfiles ADMIN y OPERATOR.
- Alta, edición y eliminación de prendas; alta y eliminación de variantes de talla/color/SKU.
- Consulta de inventario por variante, alerta cuando el stock sea menor o igual a 3.
- Registro de ventas con cliente, método de pago, subtotal e ITBIS 18 %; búsqueda por nombre o identificación.
- Registro y seguimiento de apartados activos, completados o cancelados. El stock reservado vuelve al inventario si se cancela.
- Indicadores de día/mes, ingresos, stock bajo y apartados. Gráfico de ventas recientes.
- Exportación de inventario PDF, ventas PDF/CSV y resumen gerencial PDF.
- Exportación/importación de respaldo SQLite desde el selector de archivos.

## Requisitos no funcionales

- Android minSdk 26, compile/targetSdk 35, Kotlin/JDK 17.
- Persistencia exclusivamente local con Room y claves foráneas.
- Contraseñas con PBKDF2-HMAC-SHA256 con salt aleatorio.
- UI Compose en español y soporte de tema claro/oscuro.
- Moneda `RD$`, fecha `dd/MM/yyyy`, retención de ITBIS al 18 %.
- Los errores de validación/inventario deben informarse; nunca permitir stock negativo.

## Validación de alcance actual

La app cubre las operaciones académicas de catálogo, variante, venta con carrito sencillo, apartado, gestión local de cuentas ADMIN y reportes. La gestión de usuarios está limitada a ADMIN local. Antes de uso comercial se recomienda revisar reglas fiscales, datos reales, migraciones, seguridad del dispositivo y pruebas de aceptación en varios modelos Android.
