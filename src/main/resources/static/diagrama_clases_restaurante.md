# Diagrama de clases — Sistema de restaurante

```mermaid
classDiagram
  direction TB

  class Proveedor {
    +id_proveedor PK
    +razon_social
    +ruc
    +telefono
    +email
  }

  class OrdenCompra {
    +id_orden PK
    +fecha
    +estado
    +total
  }

  class OrdenDetalle {
    +id_detalle PK
    +id_orden FK
    +id_producto FK
    +cantidad
    +precio_unitario
  }

  class Productos {
    +id_producto PK
    +nombre
    +precio
    +stock
    +categoria
  }

  class Usuario {
    +id_usuario PK
    +nombre
    +email
    +contrasena
  }

  class Roles {
    +id_rol PK
    +nombre
    +permisos
  }

  class Pedidos {
    +id_pedido PK
    +fecha
    +estado
    +total
  }

  class PedidoDetalle {
    +id_detalle PK
    +id_pedido FK
    +id_producto FK
    +cantidad
    +subtotal
  }

  class Mesas {
    +id_mesa PK
    +numero
    +capacidad
    +estado
  }

  class Tickets {
    +id_ticket PK
    +id_pedido FK
    +fecha_emision
    +total
  }

  class Pago {
    +id_pago PK
    +id_pedido FK
    +monto
    +fecha
  }

  class MetodosPago {
    +id_metodo PK
    +nombre
    +descripcion
  }

  class Cliente {
    +id_cliente PK
    +nombre
    +telefono
    +email
  }

  class Natural {
    +dni
    +apellidos
  }

  class Juridico {
    +ruc
    +razon_social
    +representante
  }

  %% Relaciones — módulo compras
  Proveedor "1" --> "0..*" OrdenCompra : provee
  OrdenCompra "1" *-- "1..*" OrdenDetalle : contiene
  OrdenDetalle "0..*" --> "1" Productos : incluye

  %% Relaciones — módulo usuarios
  Usuario "1" --> "1" Roles : tiene
  Usuario "1" --> "0..*" Pedidos : gestiona

  %% Relaciones — módulo pedidos
  Pedidos "1" --> "1" Mesas : asignada a
  Pedidos "1" *-- "1..*" PedidoDetalle : contiene
  Pedidos "1" --> "0..*" Tickets : genera
  Pedidos "1" --> "1" Pago : requiere

  %% Relaciones — módulo pagos
  Pago "0..*" --> "1" MetodosPago : usa

  %% Relaciones — módulo clientes
  Cliente "1" --> "0..*" Pedidos : realiza
  Cliente "1" --> "0..*" Pago : efectua
  Natural --|> Cliente
  Juridico --|> Cliente
```

## Notas del modelo

- `PK` = llave primaria, `FK` = llave foránea
- `*--` composición (el hijo no existe sin el padre)
- `-->` asociación directa
- `--|>` herencia (Natural y Jurídico extienden Cliente)

## Módulos

| Módulo | Entidades |
|--------|-----------|
| Compras | Proveedor, OrdenCompra, OrdenDetalle, Productos |
| Usuarios | Usuario, Roles |
| Pedidos | Pedidos, PedidoDetalle, Mesas, Tickets |
| Pagos | Pago, MetodosPago |
| Clientes | Cliente, Natural, Juridico |
