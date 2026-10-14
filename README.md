# BoutiqueStyle

Aplicación Android de gestión local para una boutique dominicana. Controla prendas con inventario por talla y color, registra ventas y apartados, y genera reportes PDF/CSV.

> **Estado:** entrega funcional para demostración académica. Ver [requisitos y alcance](./docs/01-requisitos.md); no se declara lista para operación comercial hasta validarla en dispositivo real.

## Funcionalidades incluidas

- Login local ADMIN/EMPLEADO y hash PBKDF2 con salt.
- ADMIN puede crear, editar y eliminar cuentas; protege la última cuenta administradora.
- Room con seis entidades y relaciones FK.
- CRUD de prendas, variantes (SKU único, talla, color, stock) y ventas.
- Venta de variante con ITBIS 18 % y actualización transaccional del stock.
- Apartados con reserva y devolución de stock al cancelar.
- Dashboard, gráfico de ventas de 7 días, búsqueda de ventas/apartados.
- PDF de inventario, PDF/CSV de ventas y resumen gerencial con top 5.
- Respaldo/restauración SQLite mediante selector de documentos.
- Material 3, Espresso/Rosa/Marfil, tema oscuro y logo.

## Tecnologías

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, Room/SQLite, corrutinas/Flow, PdfDocument, Gradle Kotlin DSL y catálogo de versiones TOML.

## Arquitectura

`ui (Compose/ViewModel) → domain (UseCase/Repository interface) → data (RepositoryImpl/DAO/Room)`.
La UI no accede a DAO. Ventas, apartados y restauración usan transacciones Room.

## Requisitos para compilar

- JDK 17.
- Android SDK Platform 35 y Build Tools compatibles.
- Windows: abre el directorio en Android Studio y permite sincronizar Gradle. El wrapper descarga Gradle 8.9 la primera vez.
- Los intermediarios Gradle se generan fuera de OneDrive (`%LOCALAPPDATA%\BoutiqueStyleBuild`) para evitar bloqueos de archivos; el APK final se copia al proyecto.

```powershell
.\gradlew.bat assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

### APK release firmado

Genera un keystore propio fuera del repositorio y añade **solo localmente** a `local.properties`:

```properties
storeFile=C:/ruta/privada/boutiquestyle-release.jks
storePassword=CAMBIAR
keyAlias=boutiquestyle
keyPassword=CAMBIAR
```

Luego:

```powershell
.\gradlew.bat assembleRelease
```

No compartir `local.properties` ni el keystore. Release requiere estas propiedades; la firma de debug no debe publicarse.

## Inicio de sesión demo

- ADMIN: `admin` / `admin123`
- EMPLEADO: `empleado` / `oper123`

La base de datos se inicializa localmente con prendas, variantes, una venta y un apartado de ejemplo. Cambia o elimina las credenciales iniciales antes de una distribución real.

## Documentos

- [Requisitos](./docs/01-requisitos.md)
- [Arquitectura](./docs/02-arquitectura.md)
- [Modelo de datos](./docs/03-modelo-datos.md)
- [Casos de uso](./docs/04-casos-uso.md)
- [Manual de usuario](./docs/05-manual-usuario.md)
- [Bitácora de IA](./docs/prompts/bitacora-ia.md)

## Capturas y video

Capturas: `docs/capturas/` (pendientes de generar desde un dispositivo/emulador).  
Video de demostración: **[añadir enlace al video]**.

## Datos del proyecto

- Estudiante: Ámbar Nicol De La Rosa Durán (lista 07)
- Negocio: BoutiqueStyle · moneda RD$ · UI en español
- Paquete: `com.sanvalero.boutiquestyle`
