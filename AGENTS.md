# BoutiqueStyle — guía del proyecto

BoutiqueStyle es una aplicación Android local para administrar una boutique dominicana: catálogo, existencias por talla/color, ventas con ITBIS y apartados.

## Pila y reglas

- Kotlin, Jetpack Compose + Material 3, Navigation Compose y Hilt.
- Room/SQLite es la única base de datos. No añadir Firebase, Realm ni acceso de UI a DAO.
- Capas: `ui` → ViewModel → UseCase → `domain/repository` → `data/repository` → Room DAO.
- Operaciones de inventario/ventas/apartados deben ser transaccionales. Una variante no puede quedar con stock negativo.
- Las contraseñas se guardan como PBKDF2-HMAC-SHA256 con salt individual.
- UI y documentación para usuarios en español; identificadores de código en inglés.
- Moneda RD$, fecha `dd/MM/yyyy`, ITBIS 18 %. Validar cédula/RNC antes de vender/apartar.
- Paleta: Deep Espresso `#3E2723`, Petal Reverie `#F4C9D6`, Ivory Hearth `#F5F0E6`.
- No añadir layouts XML. Los XML de recursos/manifest sí se usan cuando Android los requiere.

## Comandos

- Windows: `.\gradlew.bat assembleDebug`
- Pruebas: `.\gradlew.bat testDebugUnitTest`
- Release firmado: configurar `storeFile`, `storePassword`, `keyAlias` y `keyPassword` localmente en `local.properties`, después ejecutar `.\gradlew.bat assembleRelease`.
- El wrapper descarga Gradle 8.9 al primer uso. Se requiere JDK 17 y Android SDK 35.
- En Windows, los intermediarios se guardan bajo `%LOCALAPPDATA%\BoutiqueStyleBuild` para evitar bloqueos de OneDrive; los APK se sincronizan a `app/build/outputs/apk/`.

## Cambios y datos

- Mantener Kotlin en inglés, validaciones explícitas y errores visibles al usuario.
- Cambios de esquema Room requieren migración/versionado; no habilitar `fallbackToDestructiveMigration`.
- Nunca incluir `local.properties`, credenciales ni keystores en Git.
- Las cuentas de demostración son exclusivamente locales: `admin / admin123` y `operador / oper123`. Cambiar claves antes de distribuir.
- Usar IA como apoyo de implementación/revisión; verificar el código, las reglas fiscales y el resultado de compilación. No introducir datos personales reales en prompts.
