package com.yuzu.backend.multiempresa;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.yuzu.backend.catalogo.Categoria;
import com.yuzu.backend.catalogo.CategoriaRepository;
import com.yuzu.backend.catalogo.Producto;
import com.yuzu.backend.catalogo.ProductoRepository;
import com.yuzu.backend.plan.Plan;
import com.yuzu.backend.plan.PlanRepository;
import com.yuzu.backend.restaurante.Restaurante;
import com.yuzu.backend.restaurante.RestauranteRepository;
import com.yuzu.backend.usuario.Usuario;
import com.yuzu.backend.usuario.UsuarioRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Prueba "de verdad" (contra un MySQL 8 real, levantado con Testcontainers) de la regla central del backend:
 * el restaurante siempre sale del token, nunca de lo que mande el cliente, y un dato de otro restaurante
 * responde 404. Usa la carta (productos) como caso representativo: si dos restaurantes con los mismos
 * datos (misma categoría, mismo producto) no se mezclan ni se pueden tocar entre sí, la regla funciona.
 *
 * Requiere Docker. Si no hay Docker disponible (por ejemplo, en una máquina sin él), esta prueba falla
 * al no poder levantar el contenedor; no es un fallo del código, sino del entorno donde se ejecuta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "yuzu.jwt.secret=clave-de-pruebas-automatizadas-no-es-la-real-0123456789")
class AislamientoEntreRestaurantesTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestauranteRepository restaurantes;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PlanRepository planes;

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private ProductoRepository productos;

    private Long restauranteA;
    private Long restauranteB;
    private Long usuarioAdminB;
    private Long categoriaDeA;
    private Long productoDeA;

    /** Cada prueba usa el mismo contenedor de MySQL: un sufijo único evita chocar con datos de otra prueba. */
    private String sufijo;

    @BeforeEach
    void preparaDosRestaurantesConDatosDelMismoNombre() {
        sufijo = java.util.UUID.randomUUID().toString().substring(0, 8);
        Plan plan = planes.findByCodigo("ESTANDAR").orElseThrow();

        restauranteA = crearRestaurante(plan, "cocina-criolla-a-" + sufijo);
        restauranteB = crearRestaurante(plan, "cocina-criolla-b-" + sufijo);
        crearUsuarioAdmin(restauranteA, "admin.a");
        usuarioAdminB = crearUsuarioAdmin(restauranteB, "admin.b");

        Categoria categoria = new Categoria();
        categoria.setRestauranteId(restauranteA);
        categoria.setNombre("Platos de fondo");
        categoria.setOrden(1);
        categoria.setActivo(true);
        categoriaDeA = categorias.save(categoria).getId();

        Producto producto = new Producto();
        producto.setRestauranteId(restauranteA);
        producto.setCategoriaId(categoriaDeA);
        producto.setNombre("Lomo saltado");
        producto.setPrecio(new BigDecimal("35.00"));
        producto.setActivo(true);
        productoDeA = productos.save(producto).getId();

        // El mismo nombre de producto en el restaurante B: si el filtro por restaurante fallara,
        // una búsqueda "a secas" por nombre podría confundirlos.
        Producto productoDeB = new Producto();
        productoDeB.setRestauranteId(restauranteB);
        productoDeB.setCategoriaId(crearCategoriaDeB());
        productoDeB.setNombre("Lomo saltado");
        productoDeB.setPrecio(new BigDecimal("28.00"));
        productoDeB.setActivo(true);
        productos.save(productoDeB);
    }

    @Test
    void elRestauranteBSoloVeSusPropiosProductosEnLaLista() throws Exception {
        mockMvc.perform(get("/api/productos").header("Authorization", "Bearer " + token(restauranteB, usuarioAdminB, "admin.b", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Lomo saltado"))
                .andExpect(jsonPath("$[0].precio").value(28.00))
                .andExpect(jsonPath("$[?(@.id == " + productoDeA + ")]").doesNotExist());
    }

    @Test
    void elRestauranteBNoPuedeModificarUnProductoDelRestauranteA() throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "categoriaId", categoriaDeA,
                "nombre", "Nombre cambiado por otro restaurante",
                "descripcion", "",
                "precio", new BigDecimal("1.00"),
                "activo", true));

        mockMvc.perform(put("/api/productos/" + productoDeA)
                        .header("Authorization", "Bearer " + token(restauranteB, usuarioAdminB, "admin.b", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"));

        // El producto del restaurante A sigue intacto.
        Producto producto = productos.findByIdAndRestauranteId(productoDeA, restauranteA).orElseThrow();
        assertThatSigueIntacto(producto);
    }

    private static void assertThatSigueIntacto(Producto producto) {
        if (!"Lomo saltado".equals(producto.getNombre()) || producto.getPrecio().compareTo(new BigDecimal("35.00")) != 0) {
            throw new AssertionError("El producto del restaurante A fue modificado por una petición del restaurante B: "
                    + producto.getNombre() + " / " + producto.getPrecio());
        }
    }

    private Long crearCategoriaDeB() {
        Categoria categoria = new Categoria();
        categoria.setRestauranteId(restauranteB);
        categoria.setNombre("Platos de fondo");
        categoria.setOrden(1);
        categoria.setActivo(true);
        return categorias.save(categoria).getId();
    }

    private Long crearRestaurante(Plan plan, String subdominio) {
        Restaurante restaurante = new Restaurante();
        restaurante.setNombre("Restaurante de prueba " + subdominio);
        restaurante.setSubdominio(subdominio);
        restaurante.setEstado("ACTIVO");
        restaurante.setPlanId(plan.getId());
        restaurante.setVenceEn(LocalDate.now().plusDays(30));
        return restaurantes.save(restaurante).getId();
    }

    private Long crearUsuarioAdmin(Long restauranteId, String usuario) {
        Usuario admin = new Usuario();
        admin.setRestauranteId(restauranteId);
        admin.setUsuario(usuario);
        admin.setNombre("Administrador de prueba");
        admin.setRol("ADMIN");
        admin.setActivo(true);
        admin.setClaveHash(new BCryptPasswordEncoder().encode("clave-de-prueba"));
        return usuarios.save(admin).getId();
    }

    /** Un token firmado igual que AuthService.login(), para no depender del endpoint de login en esta prueba. */
    private String token(Long restauranteId, Long usuarioId, String usuario, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("yuzu")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(10, ChronoUnit.MINUTES))
                .subject(String.valueOf(usuarioId))
                .claim("restauranteId", restauranteId)
                .claim("usuario", usuario)
                .claim("rol", rol)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
