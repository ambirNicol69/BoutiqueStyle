# Instrucciones para Claude

Consulta [AGENTS.md](./AGENTS.md) para las convenciones comunes del repositorio.

- Conserva el español en la interfaz y el inglés en nombres de código.
- Respeta Clean Architecture: la UI jamás accede al DAO.
- Usa Room como almacenamiento único y protege las operaciones de inventario con transacciones.
- Mantén Material 3 y la paleta Espresso/Rosa/Marfil.
- No añadas dependencias de nube ni layouts XML.
- Al cambiar Room, revisa integridad referencial, migraciones y respaldo.
- Ejecuta `.\gradlew.bat assembleDebug` cuando JDK 17 y Android SDK 35 estén disponibles; informa si el entorno impide validar.
