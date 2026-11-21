package com.deliciasperuanas.backend.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.security.SecureRandom;
import java.util.Base64;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soa-demo")
@Transactional
class SoaDemoProfileIntegrationTest {
    private static final String SECRET;
    static {
        byte[] b = new byte[32]; new SecureRandom().nextBytes(b);
        SECRET = Base64.getEncoder().encodeToString(b);
    }
    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("jwt.secret", () -> SECRET);
        r.add("soa.demo.admin-password", () -> "AdminDemoTest123!");
        r.add("soa.demo.client-password", () -> "ClienteDemoTest123!");
        r.add("soa.demo.mozo-password", () -> "MozoDemoTest123!");
        r.add("soa.demo.supervisor-password", () -> "SupervisorDemoTest123!");
        r.add("soa.demo.caja-password", () -> "CajaDemoTest123!");
        r.add("soa.demo.cocinero-password", () -> "CocineroDemoTest123!");
    }
    @Autowired MockMvc mvc;

    @Test
    void perfilDemoCargaCincoServiciosYPermiteProcesarPedido() throws Exception {
        mvc.perform(get("/soa-demo/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"loginView\"")));
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(22));

        String admin = iniciar("admin.demo@delicias.local", "AdminDemoTest123!");
        String cliente = iniciar("cliente.demo@delicias.local", "ClienteDemoTest123!");

        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.disponibles").value(30));

        String body = mvc.perform(post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .header("Idempotency-Key", "demo-integracion-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productoId\":1,\"cantidad\":2}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andReturn().getResponse().getContentAsString();
        Number pedidoId = JsonPath.read(body, "$.pedidoId");

        mvc.perform(post("/api/v1/pedidos/{id}/confirmacion", pedidoId.longValue())
                .header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendidas").value(2))
            .andExpect(jsonPath("$.disponibles").value(28));
    }

    @Test
    void menuPorRolesYPermisosAutorizadosPorBackend() throws Exception {
        mvc.perform(get("/soa-demo/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"loginView\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"appView\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-module=\"stock\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"inventoryAvailable\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"inventoryBadge\"")));

        String cliente=iniciar("cliente.demo@delicias.local", "ClienteDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local", "MozoDemoTest123!");
        String supervisor=iniciar("supervisor.demo@delicias.local", "SupervisorDemoTest123!");
        String admin=iniciar("admin.demo@delicias.local", "AdminDemoTest123!");

        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer "+cliente))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPreparado").value(30))
            .andExpect(jsonPath("$.reservadas").value(0))
            .andExpect(jsonPath("$.vendidas").value(0))
            .andExpect(jsonPath("$.disponibles").value(30));
        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer "+supervisor))
            .andExpect(status().isOk());
        mvc.perform(put("/api/v1/inventario/1").header("Authorization", "Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON).content("{\"totalPreparado\":31}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/inventario/1").header("Authorization", "Bearer "+supervisor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"totalPreparado\":31}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/inventario/1").header("Authorization", "Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"totalPreparado\":31}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalPreparado").value(31));
    }

    @Test
    void clienteSoloCreaReservasYPuedeLeerlasElPersonal() throws Exception {
        String cliente=iniciar("cliente.demo@delicias.local", "ClienteDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local", "MozoDemoTest123!");
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer "+cliente))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer "+mozo))
            .andExpect(status().isOk());
        mvc.perform(get("/api/mesas").header("Authorization", "Bearer "+mozo))
            .andExpect(status().isOk());
        String cuerpo = """
            {"cliente":"Cliente de prueba","fecha":"2026-12-05",
             "horaInicio":"18:00","horaFin":"19:00","cantidadPersonas":2,
             "mesa":{"id":1},"estado":"CONFIRMADA"}
            """;
        String reserva = mvc.perform(post("/api/reservas")
                .header("Authorization", "Bearer "+cliente)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(reserva,"$.id");
        mvc.perform(patch("/api/reservas/{id}/cancelar",id.longValue())
                .header("Authorization", "Bearer "+cliente))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/reservas/{id}/cancelar",id.longValue())
                .header("Authorization", "Bearer "+mozo))
            .andExpect(status().isNoContent());
    }

    @Test
    void mesaCocinaCajaTicketYPedidoParaLlevar() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");

        String pedidoJson="""
          {"tipoAtencion":"MESA","mesaId":1,"items":[
            {"productoId":1,"cantidad":1},{"productoId":2,"cantidad":1}]}
          """;
        String pedido=mvc.perform(post("/api/v1/pedidos")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","integracion-mesa-001")
                .contentType(MediaType.APPLICATION_JSON).content(pedidoJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoAtencion").value("MESA"))
            .andExpect(jsonPath("$.mesaId").value(1))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(pedido,"$.pedidoId");

        mvc.perform(get("/api/mesas").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.numero == 1)].estado")
                .value(org.hamcrest.Matchers.hasItem("OCUPADA")));

        mvc.perform(post("/api/v1/pedidos")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","integracion-mesa-002")
                .contentType(MediaType.APPLICATION_JSON).content(pedidoJson))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("MESA_OCUPADA"));

        mvc.perform(post("/api/v1/pedidos/{id}/confirmacion",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoRecibido\":100.00}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PLATOS_PENDIENTES"));

        Number linea0=JsonPath.read(pedido,"$.items[0].detalleId");
        Number linea1=JsonPath.read(pedido,"$.items[1].detalleId");
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",id.longValue(),linea0.longValue())
                .header("Authorization","Bearer "+cocinero))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].estadoPreparacion").value("LISTO"))
            .andExpect(jsonPath("$.items[1].estadoPreparacion").value("PENDIENTE"));
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",id.longValue(),linea1.longValue())
                .header("Authorization","Bearer "+cocinero))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[1].estadoPreparacion").value("LISTO"));

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoRecibido\":100.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(47.00))
            .andExpect(jsonPath("$.cambio").value(53.00))
            .andExpect(jsonPath("$.aviso").isNotEmpty());

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoRecibido\":100.00}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PEDIDO_YA_PAGADO"));

        mvc.perform(get("/api/mesas").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.numero == 1)].estado")
                .value(org.hamcrest.Matchers.hasItem("DISPONIBLE")));

        mvc.perform(post("/api/v1/pedidos")
                .header("Authorization","Bearer "+caja)
                .header("Idempotency-Key","integracion-llevar-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"LLEVAR\",\"items\":[{\"productoId\":1,\"cantidad\":1}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoAtencion").value("LLEVAR"))
            .andExpect(jsonPath("$.mesaId").doesNotExist());

        mvc.perform(patch("/api/v1/inventario/1/reponer")
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":15}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPreparado").value(45));
    }

    @Test
    void cajaYCocinaNoPuedenGestionarUsuariosYElAdminSi() throws Exception {
        String admin=iniciar("admin.demo@delicias.local","AdminDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        mvc.perform(get("/api/v1/usuarios").header("Authorization","Bearer "+caja))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/ventas/resumen").header("Authorization","Bearer "+cocinero))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].password").doesNotExist());

        String created=mvc.perform(post("/api/v1/usuarios")
                .header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Mozo Nuevo\",\"correo\":\"nuevo@delicias.local\",\"rol\":\"MOZO\",\"password\":\"Prueba45678!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rol").value("MOZO"))
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(created,"$.id");
        mvc.perform(delete("/api/v1/usuarios/{id}",id.longValue())
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(put("/api/v1/configuracion")
                .header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Delicias Peruanas Demo\",\"telefono\":\"966 100 348\",\"logoRuta\":null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Delicias Peruanas Demo"));
    }

    private String iniciar(String correo, String pass) throws Exception {
        String json = "{\"correo\":\""+correo+"\",\"password\":\""+pass+"\"}";
        String response = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }
}
