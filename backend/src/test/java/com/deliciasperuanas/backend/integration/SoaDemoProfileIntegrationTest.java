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
    }
    @Autowired MockMvc mvc;

    @Test
    void perfilDemoCargaCincoServiciosYPermiteProcesarPedido() throws Exception {
        mvc.perform(get("/soa-demo/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"loginView\"")));
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));

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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-module=\"stock\"")));

        String cliente=iniciar("cliente.demo@delicias.local", "ClienteDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local", "MozoDemoTest123!");
        String supervisor=iniciar("supervisor.demo@delicias.local", "SupervisorDemoTest123!");
        String admin=iniciar("admin.demo@delicias.local", "AdminDemoTest123!");

        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer "+cliente))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/inventario/1").header("Authorization", "Bearer "+mozo))
            .andExpect(status().isOk());
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

    private String iniciar(String correo, String pass) throws Exception {
        String json = "{\"correo\":\""+correo+"\",\"password\":\""+pass+"\"}";
        String response = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }
}
