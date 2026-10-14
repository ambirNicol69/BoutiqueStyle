# 05 · Manual de usuario

## Acceso

Abrir BoutiqueStyle e ingresar `admin` / `admin123` o `operador` / `oper123`. El primer acceso crea dos prendas, sus variantes, una venta y un apartado de ejemplo. Cambiar las contraseñas antes de utilizar datos reales.

## Catálogo e inventario

En **Prendas**, crear un estilo con nombre, categoría y precio. Dentro de su tarjeta, añadir variantes individuales con talla, color, existencias y SKU. El dashboard marca las variantes con 3 unidades o menos.

## Venta

En **Ventas → Registrar**, elegir variantes disponibles y añadir sus cantidades al carrito; luego indicar cliente, cédula/RNC y forma de pago. El sistema revisa existencias, aplica ITBIS de 18 % y descuenta inventario al guardar. El detalle fiscal y el total quedan en la venta.

## Apartados

En **Apartados → Nuevo**, elegir variante, indicar cliente, teléfono, identificación y abono. El inventario se reserva al crear el apartado. Se puede completar o cancelar; cancelar repone el inventario.

## Reportes y respaldo

En **Más**, crear PDF de inventario, PDF+CSV de ventas o resumen gerencial. Android permite compartir los PDF. Los archivos se guardan en Descargas en Android 10 o posterior. Para respaldar/restaurar, usar el selector de archivos; restaurar reemplaza el contenido local.

Las cuentas ADMIN también pueden gestionar usuarios desde **Más → Usuarios**. Se puede dejar la contraseña sin cambios al editar; la app impide eliminar la propia sesión o la última cuenta administradora.

## Capturas

Agregar capturas reales de la app en `docs/capturas/` y enlazarlas aquí:

- `[Inicio de sesión](./capturas/login.png)`
- `[Dashboard](./capturas/dashboard.png)`
- `[Inventario](./capturas/inventario.png)`
- `[Ventas y reportes](./capturas/reportes.png)`
