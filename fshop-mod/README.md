# FShop — Tienda portátil y mercado de jugadores (Forge 1.20.1)

Mod de comercio para servidores Forge 1.20.1. Permite delimitar **zonas de mercado**
con un selector tipo WorldEdit; dentro de ellas los jugadores desbloquean el comando
`/fshop` para **crear su propia tienda**, **comprar** en las tiendas de otros y **vender**
(gestionar su stock y precios). La moneda oficial es **FantasticCoins** (`athens_coins`).

## Requisitos
- Minecraft 1.20.1 + Forge 47.x
- **FantasticCoins** (`athens_coins`) — moneda del servidor (dependencia recomendada).

## Cómo funciona

### 1. Delimitar una zona de mercado (admin)
1. `/fshop admin wand` — recibes el **Selector de Zona de Mercado**.
2. **Click izquierdo** en un bloque = esquina 1. **Click derecho** = esquina 2.
3. `/fshop admin zone create <nombre>` — guarda la zona a partir de la selección.

Dentro de cualquier zona, los jugadores pueden usar `/fshop`.

### 2. Comandos de jugador (dentro de la zona)
- `/fshop create <nombre>` — crea tu tienda y abre la GUI de gestión.
- `/fshop buy` — abre la lista de todas las tiendas del servidor. Elige una para comprar.
- `/fshop sell` — abre la GUI de gestión de tu tienda (añadir stock y precios).
- `/fshop collect` — cobra las ganancias acumuladas de tus ventas.
- `/fshop balance` — muestra tu saldo en monedas.

### 3. Vender / gestionar tu tienda
En la GUI de gestión (`/fshop sell`):
- **Click en un ítem de tu inventario** → escribe el precio por unidad → queda a la venta.
- **Click izquierdo en una oferta** → cambiar su precio.
- **Click derecho en una oferta** → retirarla y recuperar el stock.
- Botón **Cobrar** → recibes las monedas ganadas.

### 4. Comprar
`/fshop buy` → elige una tienda → click en un ítem → selecciona la cantidad
(+1/+10/x64/MAX) → **Confirmar**. Se te cobran las monedas y el vendedor acumula la ganancia.

## Comandos de administración
- `/fshop admin wand` — da el selector de zona.
- `/fshop admin zone create <nombre>` / `remove <nombre>` / `list`
- `/fshop admin shop list` — lista todas las tiendas.
- `/fshop admin shop removeall <jugador>` — elimina las tiendas de un jugador.
- `/fshop admin coins give|take <jugador> <cantidad>` — gestiona monedas.
- `/fshop admin reload` — información de recarga de configuración.

## Configuración (`config/fshop-common.toml`)
- IDs de las monedas y su valor relativo (bronce = 1, plata = 100, oro = 10000 por defecto).
- Máximo de tiendas por jugador y de ofertas por tienda.
- Precio máximo por unidad.
- Si se requiere estar dentro de una zona para comprar / vender / crear.

## Compilar
```bash
export JAVA_HOME=<java17>
./gradlew build
# salida: build/libs/fshop-1.0.0.jar
```

## Créditos de diseño
- Selector de zona inspirado en el sistema de selección de **Fantastic Terraform**.
- Estilo visual y flujo de compra/venta inspirados en **Spectra ShopGUI+**.
- Moneda basada en **FantasticCoins**.
