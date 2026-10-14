# 04 · Casos de uso

| Caso | Actor | Resultado / reglas |
|---|---|---|
| Registrar prenda y variantes | EMPLEADO | Crear una prenda con marca, categoría, precio, tallas, colores y existencias iniciales en una transacción. |
| Solicitar reposición | EMPLEADO | Enviar a administración una variante, cantidad y motivo; consultar el estado. |
| Revisar reposición | ADMIN | Aprobar o rechazar solicitudes pendientes. |
| Registrar llegada de stock | EMPLEADO | Registrar fecha/hora y cantidades recibidas; actualizar existencias en una transacción. |
| Iniciar sesión | ADMIN / EMPLEADO | Verifica usuario y hash PBKDF2; semilla local al primer acceso. |
| Administrar catálogo | ADMIN / EMPLEADO | Crear/editar prendas con nombre, categoría y precio. |
| Administrar variante | ADMIN / EMPLEADO | Asignar talla, color, stock y SKU único a una prenda. |
| Registrar venta | ADMIN / EMPLEADO | Validar cliente e identificación, stock suficiente, calcular ITBIS 18 % y descontar inventario en transacción. |
| Crear apartado | ADMIN / EMPLEADO | Validar cliente, contacto, identificación, cantidad y abono; reservar inventario. |
| Cancelar apartado | ADMIN / EMPLEADO | Cambiar estado y devolver la cantidad reservada al inventario. |
| Administrar usuarios | ADMIN | Crear/editar/eliminar cuentas; hash de contraseña, al menos un ADMIN y protección de la sesión propia. |
| Editar/eliminar venta | ADMIN / EMPLEADO | Editar datos del cliente/pago; al eliminar la venta, reponer las unidades de sus detalles en una transacción. |
| Consultar dashboard | ADMIN / EMPLEADO | Ventas, ingresos, reservas activas, stock bajo y barras de 7 días. |
| Exportar informes | ADMIN / EMPLEADO | PDF de inventario, ventas con CSV, resumen gerencial y top 5 por variante. |
| Respaldo/restauración | ADMIN / EMPLEADO | Seleccionar archivo local SQLite; restaurar reemplaza los datos locales. |

Las cuentas iniciales de demostración son `admin / admin123` y `empleado / oper123`. No utilizarlas en una distribución real.
