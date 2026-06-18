# Letrero Ruleta — Pantalla Digital de Mesa

Sistema de pantalla digital para mesa de ruleta (americana y francesa). Se compone de dos páginas web que funcionan juntas en el mismo dispositivo.

---

## Componentes

| URL | Descripción |
|-----|-------------|
| `/` | **Pantalla de la mesa** — se proyecta a los jugadores (TV / monitor externo) |
| `/operador/` | **Panel del operador** — lo maneja el croupier desde su dispositivo |

---

## Cómo usar

### 1. Preparar el equipo
- Conectar el dispositivo (tablet / computador) al monitor o TV externo via HDMI o inalámbrico.
- En el dispositivo, abrir el navegador y navegar a la URL del sitio.

### 2. Abrir las dos pantallas
1. Abrir `/operador/` en el navegador del dispositivo del croupier.
2. Hacer clic en **"📺 Abrir Pantalla de la Mesa en Nueva Ventana"** — esto abre `/` en una nueva ventana.
3. Arrastrar esa ventana al monitor externo (el que ven los jugadores).
4. **Pantalla completa**: tocar el logo de Dreams en la pantalla de la mesa para activar o desactivar pantalla completa.

### 3. Configurar la mesa
En el panel del operador:
- **Nombre de la ruleta**: seleccionar "RULETA AMERICANA" o "RULETA FRANCESA" — la pantalla se actualiza al instante.
- **Límites de apuesta**: ingresar los montos mínimos y máximos (Pleno, Dobles, Simples).
- **Animación de bola**: activar o desactivar el efecto de giro de la bola.

### 4. Ingresar números durante el juego
1. Escribir el número en el teclado numérico del panel (o teclear directamente).
2. Presionar **✔ CONFIRMAR NÚMERO**.
3. La pantalla muestra el número al instante (con animación de bola si está activada).
4. El operador queda listo para el siguiente número.

### 5. Pantalla Demo
- Activar desde el panel del operador con el botón **PANTALLA DEMO**.
- La pantalla muestra una simulación de números automática con el logo de Dreams.
- Al confirmar el primer número real, la demo se desactiva automáticamente.

### 6. Tutorial de jugadas
- Activar con el botón **TUTORIAL** en el panel.
- La pantalla muestra los tipos de apuesta con sus posiciones en la mesa.
- Se filtra automáticamente según si es ruleta americana o francesa.

---

## Pantalla completa
Tocar el **logo de Dreams** en la pantalla de la mesa activa o desactiva la pantalla completa. No hay ningún botón visible para los jugadores.

---

## Indicadores en la pantalla de la mesa

| Elemento | Descripción |
|----------|-------------|
| Número grande central | Último número jugado |
| Rueda de ruleta animada | Gira continuamente; la bola cae en el número confirmado |
| Historial (tablero) | Últimos 12 números con colores rojo / negro / verde |
| Números calientes | Los 3 números que más salieron |
| Números fríos | Los 3 números que menos salieron |
| Estadísticas | % Rojo/Negro, Par/Impar, 1-18/19-36, Docenas |
| Límites de mesa | Apuestas mínimas y máximas vigentes |

---

## Ruleta Francesa vs Americana

- **Americana**: incluye el 00 (casillas 0 y 00 en la mesa).
- **Francesa**: solo el 0 (sin 00). El tutorial oculta las jugadas exclusivas de la versión americana.

---

## Notas técnicas

- La comunicación entre el operador y la pantalla ocurre via `localStorage` del navegador — ambas páginas deben estar abiertas en el **mismo navegador** del **mismo dispositivo**.
- La pantalla usa un Service Worker (PWA) para funcionar sin conexión una vez cargada.
- La pantalla nunca se apaga sola gracias a la **Screen Wake Lock API**.
