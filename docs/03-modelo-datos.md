# 03 · Modelo de datos

```mermaid
erDiagram
    USER ||--o{ SALE : registra
    USER ||--o{ LAYAWAY : atiende
    GARMENT ||--o{ VARIANT : ofrece
    VARIANT ||--o{ SALE_ITEM : vendido_en
    SALE ||--|{ SALE_ITEM : contiene
    VARIANT ||--o{ LAYAWAY : reservado_en
    USER {
      long id PK
      string username UK
      string passwordHash
      string role
      string name
      long createdAt
    }
    GARMENT {
      long id PK
      string name
      string category
      double basePrice
      long createdAt
      long updatedAt
    }
    VARIANT {
      long id PK
      long garmentId FK
      string size
      string color
      int stock
      string sku UK
      string barcode
    }
    SALE {
      long id PK
      long userId FK
      string customerName
      string customerCedulaOrRnc
      double subtotal
      double itbis
      double total
      long saleDate
    }
    SALE_ITEM {
      long id PK
      long saleId FK
      long variantId FK
      int quantity
      double unitPrice
      double lineTotal
    }
    LAYAWAY {
      long id PK
      long variantId FK
      long userId FK
      int quantity
      double totalAmount
      double amountPaid
      double balance
      string status
      long dueDate
    }
```

Las FK de prendas eliminan sus variantes en cascada. El detalle de ventas se elimina junto con la venta; ventas y apartados protegen la referencia a las variantes y usuarios.
