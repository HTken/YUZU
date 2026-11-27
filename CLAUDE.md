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
avisos de vencimiento por correo, cookie httpOnly para la sesión (en vez de guardar el token en el
navegador), usuarios, productos, mesas, pedidos, caja, cocina, inventario, recetas, informes, anulación de
ventas cobradas, ticket de venta y datos del negocio.
Migraciones Flyway: `V2` a `V16` (la última es `V16__culqi_suscripciones.sql`; este módulo no necesitó una
nueva porque no agrega tablas ni columnas).

## Estado actual

Última sesión (27 nov 2026): cookie httpOnly para la sesión (ver justo abajo). Antes de esto, en la misma
fecha: avisos de vencimiento por correo, y antes de eso: cobro recurrente + webhook + 3D Secure de Culqi,
**probado de verdad** con tarjetas de prueba y un túnel ngrok hacia tu backend local (antes de esto se
verificó que todo compilaba y se agregaron las primeras pruebas automáticas reales, ver más abajo).

**Cookie httpOnly para la sesión**

Antes, el token (JWT) quedaba guardado en `localStorage` del navegador — accesible desde JavaScript, así
que un ataque de XSS (inyectar JS en la página) podría robarlo. Ahora el token viaja en una **cookie
httpOnly**: el navegador la manda sola en cada pedido, pero ningún script (ni siquiera el de Yuzu) puede
leerla. `localStorage` solo guarda datos sin importancia para mostrar en pantalla (nombre, rol, restaurante),
nunca el token.

Cómo quedó, por partes:

- **Backend**: `AuthController` y `PlataformaAuthController` ya no devuelven el token en el cuerpo de la
  respuesta del login (ahora devuelven `SesionDto`/`PlataformaSesionDto`, sin token); lo ponen en una
  cookie httpOnly (`CookieAuth`, una por tipo de sesión: `yuzu_token` para restaurantes,
  `yuzu_plataforma_token` para la consola de plataforma, para que no se mezclen si tienes las dos pestañas
  abiertas). Se agregó `POST /api/auth/logout` y `POST /api/plataforma/auth/logout`, que es lo único que
  puede borrar esa cookie (JavaScript no puede). `SecurityConfig` ahora lee el token de la cookie primero
  (`CookieBearerTokenResolver`); si no hay cookie, sigue aceptando el encabezado `Authorization` de siempre
  (lo siguen usando las pruebas automatizadas y cualquier llamada directa a la API).
- **Un hallazgo importante, probado con un navegador de verdad (no supuesto)**: app-cliente usa subdominios
  por restaurante (`demo.localhost:4200`), y Chrome trata eso como un **sitio distinto** de donde corre el
  backend (`localhost:8080`). Con `SameSite=Lax` (lo normal) la cookie **no viajaba** entre ellos — se
  probó armando un servidor mínimo y confirmando con Playwright que la cookie no llegaba. La solución fue
  `SameSite=None; Secure`, que si funciona entre sitios distintos (confirmado con la misma prueba) y
  también funciona sobre `http://` en localhost porque Chrome lo trata como seguro. **Importante para
  cuando despliegues** (módulo pendiente "Preparar el despliegue"): `Secure` exige HTTPS de verdad fuera de
  localhost — si backend y frontends no quedan ambos por HTTPS, el navegador no va a guardar la cookie y
  nadie va a poder iniciar sesión.
- **Frontend (los 2 apps)**: `AuthService` ya no guarda el token, solo datos para mostrar (nombre, rol,
  restaurante, cuándo vence según el propio navegador). Todas las peticiones al backend ahora van con
  `withCredentials: true` (así el navegador manda la cookie). `logout()` ahora llama al backend para que
  borre la cookie, y recién después limpia lo local y vuelve al login.
- Se aprovechó para borrar el campo `tipo` ("Bearer") de `LoginResponse`/`PlataformaLoginResponse`: ya no
  lo usaba nadie (el "Bearer" que importa ahora es interno, del lado del servidor).

**Verificado de verdad:** `mvn clean test` (97 pruebas, todas en verde — incluida
`AislamientoEntreRestaurantesTest`, que confirma que el encabezado `Authorization` sigue funcionando para
pruebas), `ng build` de los 2 frontends sin errores. **No se alcanzó a probar en el navegador real del
dueño** (solo en el sandbox con Playwright, para el hallazgo de `SameSite`): falta que confirmes en tu PC
que el login, el logout y el cambio de pestaña entre `localhost:4200` o `demo.localhost:4200` y
`localhost:4300` siguen funcionando igual que antes.

Archivos nuevos o modificados — `backend/`: nuevos `config/CookieAuth.java` y
`config/CookieBearerTokenResolver.java`, `auth/SesionDto.java` y
`plataforma/PlataformaSesionDto.java`; modificados `config/SecurityConfig.java`, `auth/AuthController.java`,
`auth/AuthService.java`, `auth/LoginResponse.java`, `plataforma/PlataformaAuthController.java`,
`plataforma/PlataformaAuthService.java`, `plataforma/PlataformaLoginResponse.java`. `app-cliente/` y
`app-plataforma/`: `core/auth.service.ts` y `core/auth.interceptor.ts` en ambos.

**Avisos de vencimiento por correo**

Cuando se "revisa vencimientos" (botón en app-plataforma, o se puede llamar por cron/tarea programada),
además de pasar restaurantes a "en gracia" o "suspendido" como ya hacía, ahora también manda un correo al
o a los administradores del restaurante (rol `ADMIN`, activos, con correo registrado) en tres casos:

- El plan **todavía está activo** pero vence en `yuzu.suscripcion.dias-aviso` días (3 por defecto) — no
  cambia de estado, solo avisa.
- El restaurante **acaba de pasar a "en gracia"** (venció, pero todavía puede pagar).
- El restaurante **acaba de ser suspendido** (se acabó la gracia).

Lógica nueva: `ReglasAvisoVencimiento` (clase de reglas, probada con JUnit: construye el asunto y el cuerpo
del correo en español para cada uno de los 3 casos, con singular/plural de "día"/"días") y
`AvisoVencimientoService` (busca los administradores con correo del restaurante y los manda por
`JavaMailSender`). `SuscripcionService.procesarVencimientos()` ahora también devuelve cuántos restaurantes
fueron avisados (`ResultadoVencimientosDto.avisados`), y `app-plataforma` lo muestra en el mensaje de
"Revisar vencimientos".

**No se necesitó envío de correo real para probar esto**: si no configuras un servidor de correo, el
backend simplemente no manda nada y deja un aviso en el log (`WARN`), sin fallar — así que "Revisar
vencimientos" sigue funcionando igual aunque no tengas correo configurado. Para mandar correos de verdad en
tu PC, agrega en tu `application-local.properties` (que nunca se sube) algo como:

```properties
spring.mail.host=smtp.tu-proveedor.com
spring.mail.port=587
spring.mail.username=tu-correo@tudominio.com
spring.mail.password=tu-clave-o-token-de-aplicacion
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
yuzu.correo.remitente=no-responder@tudominio.com
```

(Cualquier proveedor SMTP sirve — Gmail con una "contraseña de aplicación", un dominio propio, etc. Nunca
pongas esas claves en el chat ni en el código, solo en ese archivo.)

Se agregaron 4 pruebas JUnit nuevas (`ReglasAvisoVencimientoTest`, las 4 en verde) y se confirmó
`mvn clean test` (97 pruebas en total, todas en verde), `ng build` de `app-plataforma` y `ng build` de
`app-cliente` (este último no se tocó, solo se confirmó que sigue compilando).

Archivos nuevos o modificados — `backend/`: `pom.xml` (dependencia `spring-boot-starter-mail`),
`RestauranteRepository.java`, `UsuarioRepository.java`, `SuscripcionService.java`,
`ResultadoVencimientosDto.java`, y nuevos `ReglasAvisoVencimiento.java` y `AvisoVencimientoService.java`
(paquete `suscripcion`), más `ReglasAvisoVencimientoTest.java`. `app-plataforma/`: `plataforma.api.ts`
(campo `avisados`) y `restaurantes.ts` (mensaje de la revisión). `app-cliente/`: sin cambios.

**Culqi: 3D Secure, webhook y cobro automático**

Se escribió siguiendo la documentación oficial de Culqi (se creó un entorno nuevo, "yuzu-culqi-docs", con
acceso a `docs.culqi.com` y `apidocs.culqi.com`, porque antes el entorno no tenía internet) y después se
probó en vivo contra la cuenta de prueba real del dueño. En el camino aparecieron varias cosas que la
documentación de Culqi no dice bien, y se fueron arreglando una por una:

- El HTTP 200 vs 201 de un cargo está al revés en dos páginas de su documentación (se usó lo que dice el
  spec formal: **201 = éxito, 200 = necesita 3DS**, confirmado con pruebas reales).
- El objeto de la tarjeta viene en el campo `source`, no `token` como dice su guía de integración.
- Crear un Cliente en Culqi exige dirección, ciudad, país y teléfono (no solo nombre y correo); se usan
  los de Configuración > Datos del negocio, con un aviso si el restaurante no los completó.
- **`GET /v2/events/{id}` (consultar un evento) no funciona en el entorno de prueba** ("No existe el
  evento que intentas consultar"), así que no sirve como verificación. El webhook se rediseñó para no
  depender de eso: el tipo de evento se toma del propio webhook, pero antes de registrar cualquier cobro
  siempre se vuelve a consultar la **suscripción real** en la API (eso sí funciona) — sigue sin confiarse
  en el cuerpo del webhook para nada que implique dinero.
- El panel de Culqi, al crear un webhook de suscripciones, solo deja elegir las acciones "creation",
  "update" o "cancel" (no existe "charge" como dice la documentación): un cobro mensual dispara
  `subscription.update.succeeded`, no `subscription.charge.succeeded`. Se aceptan ambos nombres por si acaso.
- El campo `data` del webhook no llega como objeto: llega como texto JSON anidado en
  `data.message.object.subsId`, un formato que Culqi tampoco documenta.

**Qué quedó confirmado de verdad, probando con tarjetas de prueba + ngrok:**

- ✅ Pago manual con tarjeta + 3D Secure (código de verificación del banco) de principio a fin: pago
  recibido, plan renovado.
- ✅ Activar el cobro automático guardando una tarjeta (usa las tarjetas de "Compras exitosas" de Culqi,
  por ejemplo `4111 1111 1111 1111`; **no** las de "Casos de autenticación 3DS": esas fuerzan un 3DS que
  un cobro automático, al no tener a nadie delante de la pantalla, nunca puede completar, así que el cobro
  queda rechazado).
- ✅ El webhook llega al backend y, para `subscription.cancel.succeeded`, se procesa de punta a punta
  (confirmado con logs reales: el aviso llega, se verifica la suscripción, se marca inactiva).
- ⚠️ **No confirmado todavía:** el aviso de **cobro exitoso** (`subscription.update.succeeded`). Se
  esperó más de 20 minutos después de un cobro real y exitoso, y el "Historial" de webhooks de Culqi
  nunca mostró ese evento (solo mostraba las cancelaciones, que sí llegan casi al instante). El código
  que lo procesaría es el mismo que ya se probó con la cancelación, así que debería funcionar igual, pero
  falta la confirmación real — puede ser que el entorno de prueba de Culqi no mande ese aviso en
  particular, o que tarde mucho más de lo esperado. Para retomarlo: repetir la prueba con ngrok (ver
  "Cómo correrlo" abajo) y, si vuelve a no llegar nada, confirmarlo recién cuando haya producción.
- **Cómo funciona el cobro automático (para que quede claro):** al activarlo, Culqi cobra **de
  inmediato** (no espera a que venza tu plan actual) y repite ese cobro cada mes, en esa misma fecha —
  es un ciclo propio de Culqi, aparte del vencimiento que maneja Yuzu. Cuando el webhook de un cobro
  exitoso le llega al backend, Yuzu **suma un mes al vencimiento que ya tenías** (nunca se pierden los
  días ya pagados).
- **Pendiente de hacer a mano, tuyo, antes de producción:** en el CulqiPanel de producción (no solo el de
  prueba), sección Eventos → Webhooks, crear los mismos 3 webhooks (`subscription.update.succeeded`,
  `subscription.update.failed`, `subscription.cancel.succeeded`) apuntando a la URL pública real de tu
  backend (no a ngrok, eso es solo para probar en tu PC).

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
- Siguiente paso: falta elegir el próximo módulo (ver "Pendientes propuestos" abajo).

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

Facturación electrónica SUNAT, despliegue, límite de intentos de login y 2FA, exportar informes a CSV.

## Sincronización entre módulos (backend + frontends)

- **Impacto cruzado obligatorio:** Siempre que agregues o modifiques un endpoint, DTO, campo, estado o regla de negocio en `backend/`, revisa de inmediato cómo afecta a `app-cliente/` y `app-plataforma/` y actualiza ambos frontends en el mismo turno si consumen esa funcionalidad.
- **Un módulo funcional = los 3 proyectos:** "Avanzar módulo por módulo" significa completar toda la funcionalidad de punta a punta (`backend/` + `app-cliente/` + `app-plataforma/`), nunca dejar el backend listo y los frontends pendientes para después salvo que el dueño lo pida.
- **Contratos sincronizados:** Mantén alineados los tipos/interfaces TypeScript en `app-cliente` y `app-plataforma` con los DTOs y respuestas de error (`{"mensaje": "..."}`) del `backend`.
- **Verificación conjunta antes de cerrar:** Antes de dar por terminada cualquier tarea que toque más de una carpeta, ejecuta y reporta el estado de compilación de los 3 proyectos:
  - `backend/`: `mvn clean compile` (y pruebas si aplica)
  - `app-cliente/`: `ng build`
  - `app-plataforma/`: `ng build`
- **Resumen ordenado por carpeta:** Al finalizar, lista los archivos modificados o creados agrupados por `backend/`, `app-cliente/` y `app-plataforma/`.
