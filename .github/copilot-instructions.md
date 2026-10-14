# Instrucciones para GitHub Copilot

Sigue las convenciones de [AGENTS.md](../AGENTS.md).

- Kotlin en inglés; UI y textos de negocio en español.
- Arquitectura obligatoria: Compose → ViewModel → UseCase → repositorio → Room.
- No exponer DAO a la UI ni agregar otro motor de datos.
- Las ventas/apartados descuentan stock de la variante de talla y color en una transacción.
- Mantener ITBIS 18 %, RD$, cédula/RNC y fechas `dd/MM/yyyy`.
- Usar la paleta Material 3 establecida. Evitar layouts XML.
- Manejar fallos de forma visible y no guardar contraseñas en texto plano.
