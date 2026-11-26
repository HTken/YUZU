# YUZU: SaaS para restaurantes

Sistema multi-restaurante (cada restaurante tiene sus datos separados) con planes, suspensión y renovación
automática, y cobro con Culqi (modo prueba). El dueño es de Lima, Perú; todo el texto de pantalla y las
respuestas van **en español**.

## Estructura

| Carpeta | Qué es | Puerto |
|---|---|---|
| `backend/` | Spring Boot 4.1.2, Java 21, Maven, Flyway, MySQL 8 (Docker), Spring Security 7 con JWT | 8080 |
| `app-cliente/` | Angular 22: lo que usa el restaurante (mesas, caja, cocina, inventario, informes, configuración) | 4200 |
| `app-plataforma/` | Angular: consola de YUZU para administrar restaurantes, planes y cobranzas (rol `SUPERADMIN`) | 4300 |

Módulos hechos: planes y vencimientos, Culqi, usuarios, productos, mesas, pedidos, caja, cocina, inventario,
recetas, informes, anulación de ventas cobradas, ticket de venta y datos del negocio.
Migraciones Flyway: `V2` a `V15` (la última es `V15__datos_negocio.sql`).

## Cómo correrlo

- Backend: `cd backend`, `mvn clean compile`, y arrancar con el perfil `local`. En el log debe aparecer
  `now at version vN` con la última migración. Necesita MySQL 8 corriendo en Docker.
- Frontends: `ng serve` dentro de `app-cliente` (4200) y de `app-plataforma` (4300).
- Las claves (Culqi, contraseña de MySQL, secreto del JWT) están en `backend/src/main/resources/application-local.properties`.
  **Ese archivo no se sube nunca.** Nunca pidas ni escribas claves o contraseñas en el chat ni en el código.

## Reglas del backend (no romperlas)

- El restaurante **siempre sale del token** (`SesionActual.restauranteId()`), jamás de lo que mande el cliente.
  Toda consulta filtra por restaurante; un dato de otro restaurante responde 404.
- `spring.jpa.hibernate.ddl-auto=none`: cualquier cambio de base de datos va en una **migración Flyway nueva**
  (`V16__...`). Nunca se edita una migración ya aplicada.
- Errores como `{"mensaje": "..."}` con `ManejadorErrores`. Los mensajes van en español y dicen qué hacer.
- Fechas: `LocalDateTime` en UTC con `Tiempo.ahora()` / `Tiempo.iso()`. Lima es UTC-5: el frontend convierte al mostrar.
- Permisos en `config/SecurityConfig.java`: gana la **primera** regla que coincide, así que las reglas específicas
  van antes que las generales. Roles: `SUPERADMIN`, `ADMIN`, `CAJERO`, `MOZO`, `COCINA`.
- La lógica con reglas va en clases pequeñas y probables (`Reglas*`, `Calculos*`), separadas de los servicios.
- Las ventas anuladas nunca reutilizan el número de ticket y no suman en totales, cierre de caja ni informes.

## Reglas del frontend

- Angular standalone con signals, `@if` / `@for`, `<dialog>` para ventanas, `output()` para eventos.
- Helpers de formato en `core/formato.ts` (`soles`, `hora`, `fechaHora`, `ticketNumero`, ...). Variables CSS:
  `--yuzu`, `--hoja`, `--hoja-suave`, `--texto`, `--texto-suave`, `--borde`, `--error`.
- **Los colores y detalles de CSS los ajusta el dueño a mano en VS Code.** Para cambios de lógica no reescribas los
  `.css` que ya existen; los estilos nuevos van en un archivo aparte (ejemplo: `caja-anulaciones.css`, `caja-ticket.css`).
- Cada pantalla se revisa en computadora y en celular (los mozos usan el celular).

## Cómo trabajar con el dueño

- Avanza módulo por módulo, y al final di qué archivos existentes se modificaron.
- Sé honesto con lo que no se pudo compilar o probar. Si no ejecutaste algo, dilo.
- Antes de dar un módulo por terminado: `mvn clean compile` (y pruebas), `ng build` o `ng serve` sin errores.
- El ticket de venta es **interno**: no es un comprobante electrónico de SUNAT.

## Pendientes propuestos (el dueño decide el orden)

Facturación electrónica SUNAT, cobro recurrente y webhook de Culqi (3D Secure), avisos de vencimiento por correo,
despliegue, límite de intentos de login y 2FA, cookie httpOnly en lugar de guardar el token en el navegador,
pruebas de aislamiento entre restaurantes con Testcontainers, exportar informes a CSV.
