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

Módulos hechos: planes y vencimientos, Culqi (cobro manual, 3D Secure y cobro automático por suscripción),
usuarios, productos, mesas, pedidos, caja, cocina, inventario, recetas, informes, anulación de ventas
cobradas, ticket de venta y datos del negocio.
Migraciones Flyway: `V2` a `V16` (la última es `V16__culqi_suscripciones.sql`).

## Estado actual

Última sesión (26 nov 2026): cobro recurrente + webhook + 3D Secure de Culqi (módulo completo). Antes de
esto se verificó que todo compilaba y se agregaron las primeras pruebas automáticas reales (ver más abajo).

**Culqi: 3D Secure, webhook y cobro automático (módulo nuevo)**

Para esto leí la documentación oficial de Culqi (antes el entorno no tenía acceso a internet; se creó un
entorno nuevo llamado "yuzu-culqi-docs" con acceso solo a `docs.culqi.com` y `apidocs.culqi.com` para
poder leerla). Quedó:

- **3D Secure de verdad** en el cobro manual (botón "Pagar con tarjeta o Yape" de Mi Plan): si el banco lo
  pide, el backend responde 428 y el frontend abre la autenticación de Culqi3DS
  (`culqi.service.ts` → `Culqi3dsService`) y reintenta el cobro con el resultado. Antes esto se rechazaba
  siempre con "todavía no está disponible".
- **Cobro automático del plan** (sección nueva "Cobro automático" en Mi Plan): guardas una tarjeta (Culqi
  le hace una validación de S/ 3.00 que devuelve de inmediato) y Culqi cobra solo cada mes, sin que el
  restaurante tenga que entrar a pagar. También usa 3D Secure si el banco lo pide. Se puede cancelar
  cuando se quiera.
  - El Plan de cobro en Culqi (`planes.culqi_plan_id`) se crea solo, la primera vez que un restaurante
    activa el cobro automático de ese plan: **no hace falta crear nada a mano en el CulqiPanel para esto.**
  - Backend nuevo: `CulqiSuscripcion` (tabla `culqi_suscripciones`, V16), `CulqiSuscripcionService`,
    `ReglasCulqiSuscripcion` (lógica pura, con pruebas), endpoints `GET/POST/DELETE /api/pagos/culqi/suscripcion`.
- **Webhook**: `POST /api/pagos/culqi/webhook` (público, sin login: Culqi no manda token) recibe el aviso
  de Culqi cuando cobra una suscripción (éxito, fallo o cancelación) y es la única forma en que una
  renovación automática se refleja en el plan del restaurante — **incluida la primera renovación**.
  Culqi no firma sus webhooks, así que el código nunca confía en lo que llega en el cuerpo: solo usa el
  id del evento para preguntarle a la API de Culqi (con la llave secreta) qué pasó de verdad
  (`CulqiSuscripcionService.procesarEvento`).
  - **Pendiente de hacer a mano, tuyo:** entrar al CulqiPanel (de prueba primero, luego el de producción),
    sección Eventos → Webhooks, y pegar ahí la URL pública de tu backend terminada en
    `/api/pagos/culqi/webhook`. Sin esto, el cobro automático nunca actualiza el vencimiento del plan
    (se cobra en Culqi pero Yuzu no se entera). En tu PC, local, necesitarías algo como ngrok para
    probarlo de verdad; si prefieres, lo deja pendiente hasta el módulo de despliegue.
- No pude probar nada de esto contra la Culqi real: no tengo tus llaves de prueba
  (`application-local.properties`) ni puedo recibir un webhook desde aquí. Si lo escribí fue siguiendo al
  pie de la letra la documentación oficial (`apidocs.culqi.com`, el spec `apiculqi.yaml`), pero igual
  pruébalo primero en tu entorno de integración antes de pasarlo a producción.
- Ojo: la documentación de Culqi se contradice sobre si el HTTP 200 o el 201 de un cargo significa
  "necesita 3DS" (dos páginas dicen cosas distintas). Usé lo que dice el spec formal (`apiculqi.yaml`):
  **201 = éxito, 200 = necesita 3DS**, que es lo más consistente con el resto de la documentación.

**Antes de este módulo (compilación y pruebas automáticas):**

- `mvn clean compile` en `backend/`, y `ng build` en `app-cliente/` y `app-plataforma/`, sin errores
  (`app-cliente` tiene advertencias de presupuesto de CSS en algunas pantallas, no son errores y no se
  tocó ningún `.css` existente).
- **Pruebas JUnit en `backend/`** (89 pruebas, `mvn test` en verde):
  - Sin base de datos, para la lógica de las clases `Reglas*`/`Calculos*`: `ReglasAnulacionTest` y
    `ReglasTicketTest` (caja), `ReglasStockTest` (inventario), `ReglasNegocioTest` (negocio, incluye el
    cálculo del dígito verificador del RUC), `CalculosInformeTest` (informes), `ReglasCulqiSuscripcionTest`
    y `RespuestaCulqiTest` (pago).
  - `AislamientoEntreRestaurantesTest` (paquete `multiempresa`): levanta un MySQL 8 real con
    **Testcontainers**, corre las migraciones de Flyway de verdad, crea dos restaurantes con una
    categoría y un producto del mismo nombre, y comprueba por HTTP (con tokens JWT reales, uno por
    restaurante) que el restaurante B no ve el producto del restaurante A en su lista y que si intenta
    modificarlo (`PUT`) recibe 404 "Producto no encontrado". Se confirmó que la prueba detecta el problema
    de verdad: se rompió a propósito el filtro por restaurante en `CatalogoService`, la prueba falló, y
    se restauró el código original.
  - Se agregó Testcontainers al `pom.xml` (`spring-boot-testcontainers`, `testcontainers-junit-jupiter`,
    `testcontainers-mysql`), y la prueba de humo `YuzuBackendApplicationTests` (que ya existía) ahora
    también usa un MySQL de Testcontainers en vez de depender de tu MySQL local y de
    `application-local.properties`.
  - **Para correr `mvn test` hace falta Docker corriendo** (el mismo Docker Desktop que ya usas para
    MySQL): las pruebas bajan una imagen `mysql:8.0` aparte y la usan solo para probar, sin tocar tu
    base de datos real.
- Sigue pendiente: probar el ticket de venta en una impresora térmica real.
- Siguiente paso: falta elegir el próximo módulo (ver "Pendientes propuestos" abajo). Antes de dar este
  módulo de Culqi por cerrado del todo: probarlo con tarjetas de prueba en tu entorno de integración y
  configurar el webhook en el CulqiPanel.

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

## Pendientes propuestos (el dueño decide el orden)

Facturación electrónica SUNAT, avisos de vencimiento por correo, despliegue, límite de intentos de login y
2FA, cookie httpOnly en lugar de guardar el token en el navegador, exportar informes a CSV.

## Sincronización entre módulos (backend + frontends)

- **Impacto cruzado obligatorio:** Siempre que agregues o modifiques un endpoint, DTO, campo, estado o regla de negocio en `backend/`, revisa de inmediato cómo afecta a `app-cliente/` y `app-plataforma/` y actualiza ambos frontends en el mismo turno si consumen esa funcionalidad.
- **Un módulo funcional = los 3 proyectos:** "Avanzar módulo por módulo" significa completar toda la funcionalidad de punta a punta (`backend/` + `app-cliente/` + `app-plataforma/`), nunca dejar el backend listo y los frontends pendientes para después salvo que el dueño lo pida.
- **Contratos sincronizados:** Mantén alineados los tipos/interfaces TypeScript en `app-cliente` y `app-plataforma` con los DTOs y respuestas de error (`{"mensaje": "..."}`) del `backend`.
- **Verificación conjunta antes de cerrar:** Antes de dar por terminada cualquier tarea que toque más de una carpeta, ejecuta y reporta el estado de compilación de los 3 proyectos:
  - `backend/`: `mvn clean compile` (y pruebas si aplica)
  - `app-cliente/`: `ng build`
  - `app-plataforma/`: `ng build`
- **Resumen ordenado por carpeta:** Al finalizar, lista los archivos modificados o creados agrupados por `backend/`, `app-cliente/` y `app-plataforma/`.
