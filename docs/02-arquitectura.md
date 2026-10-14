# 02 · Arquitectura

```mermaid
flowchart TD
    UI[Compose UI / Navigation] --> VM[ViewModel + StateFlow]
    VM --> UC[Use cases / validaciones]
    UC --> RI[StoreRepository interface]
    RI --> IMPL[DefaultStoreRepository]
    IMPL --> DB[Room AppDatabase]
    DB --> DAO[BoutiqueDao]
    DAO --> TABLES[(SQLite: users, garments, variants, sales, sale_items, layaways)]
    IMPL --> TX[Room transactions: venta / apartado / respaldo]
    REPORT[PdfDocument + CSV] --> UI
```

## Paquetes

- `ui`: Compose, tema, navegación y estado de presentación.
- `domain`: modelos, contrato de repositorio y casos de uso.
- `data`: entidades Room, DAO e implementación del repositorio.
- `di`: configuración Hilt de Room/repositorio.
- `report`: creación y compartición de PDF/CSV.
- `util`: formato dominicano de dinero y fecha.

Las ventas actualizan stock e insertan cabecera y detalle dentro de una transacción. Los apartados descuentan stock en el momento de reservarlo; la cancelación repone la cantidad.
