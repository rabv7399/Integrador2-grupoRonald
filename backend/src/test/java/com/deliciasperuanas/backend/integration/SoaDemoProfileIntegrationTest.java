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


    @Test
    void administradorConsultaTablaRealYAuditaSalidaSinDescontarPedidos() throws Exception {
        String admin=iniciar("admin.demo@delicias.local","AdminDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        mvc.perform(get("/api/v1/inventario").header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(22));

        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":2,\"comentario\":\"Merma por cocción\"}"))
            .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":3,\"comentario\":\"Merma por cocción\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPreparado").value(27))
            .andExpect(jsonPath("$.disponibles").value(27));

        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipo").value("SALIDA"))
            .andExpect(jsonPath("$[0].comentario").value("Merma por cocción"))
            .andExpect(jsonPath("$[0].usuarioCorreo").value("admin.demo@delicias.local"));

        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":28,\"comentario\":\"No alcanza el stock\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("SALIDA_SUPERA_DISPONIBLE"));

        mvc.perform(get("/api/v1/inventario/1")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk()).andExpect(jsonPath("$.disponibles").value(27));
    }

    @Test
    void mozoAgregaPlatosEntregaYCajaRegistraYapeYCuadre() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String pedido=mvc.perform(post("/api/v1/pedidos")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","pedido-lineas-acad-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"MESA\",\"mesaId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1},{\"productoId\":2,\"cantidad\":1}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(47.00))
            .andExpect(jsonPath("$.items[0].fechaInicio").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(pedido,"$.pedidoId");

        String nueva="{\"items\":[{\"productoId\":4,\"cantidad\":1}]}";
        String url="/api/v1/pedidos/"+id.longValue()+"/items";
        mvc.perform(post(url).header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","adicion-lineas-acad-001")
                .contentType(MediaType.APPLICATION_JSON).content(nueva))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(65.00))
            .andExpect(jsonPath("$.items.length()").value(3));
        mvc.perform(post(url).header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","adicion-lineas-acad-001")
                .contentType(MediaType.APPLICATION_JSON).content(nueva))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(65.00))
            .andExpect(jsonPath("$.items.length()").value(3));

        mvc.perform(post("/api/v1/pedidos/{id}/confirmacion",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        for(int indice=0;indice<3;indice++){
            String actual=mvc.perform(get("/api/v1/pedidos/{id}",id.longValue())
                .header("Authorization","Bearer "+mozo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            Number detalle=JsonPath.read(actual,"$.items["+indice+"].detalleId");
            mvc.perform(post("/api/v1/pedidos/{id}/detalles/{detalle}/listo",
                    id.longValue(),detalle.longValue())
                    .header("Authorization","Bearer "+cocinero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items["+indice+"].fechaListo").isNotEmpty());

            mvc.perform(post("/api/v1/pedidos/{id}/detalles/{detalle}/entregado",
                    id.longValue(),detalle.longValue())
                    .header("Authorization","Bearer "+mozo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items["+indice+"].estadoPreparacion").value("ENTREGADO"))
                .andExpect(jsonPath("$.items["+indice+"].fechaEntrega").isNotEmpty());
        }

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"YAPE\",\"referenciaPago\":\"YAPE-SOA-001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(65.00))
            .andExpect(jsonPath("$.cambio").value(0))
            .andExpect(jsonPath("$.medioPago").value("YAPE"))
            .andExpect(jsonPath("$.referenciaPago").value("YAPE-SOA-001"));

        mvc.perform(post(url).header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","adicion-lineas-acad-002")
                .contentType(MediaType.APPLICATION_JSON).content(nueva))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PEDIDO_CERRADO"));

        String hoy=java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).toString();
        mvc.perform(get("/api/v1/ventas/resumen").param("fecha",hoy)
                .header("Authorization","Bearer "+caja))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ventasFecha").value(65.00))
            .andExpect(jsonPath("$.porMedioDia.YAPE").value(65.00))
            .andExpect(jsonPath("$.porMedioDia.EFECTIVO").value(0))
            .andExpect(jsonPath("$.operacionesDia.length()").value(1));

        mvc.perform(post("/api/v1/ventas/cuadres").header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fecha\":\""+hoy+"\",\"efectivoContado\":0.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.efectivoEsperado").value(0))
            .andExpect(jsonPath("$.diferencia").value(0));
        mvc.perform(post("/api/v1/ventas/cuadres").header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fecha\":\""+hoy+"\",\"efectivoContado\":0.00}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CUADRE_EXISTENTE"));

        mvc.perform(get("/api/mesas").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.numero == 1)].estado")
                .value(org.hamcrest.Matchers.hasItem("DISPONIBLE")));
    }

    @Test
    void posRequiereReferenciaYNoGeneraVuelto() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String pedido=mvc.perform(post("/api/v1/pedidos")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","pedido-pos-academico")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"LLEVAR\",\"items\":[{\"productoId\":4,\"cantidad\":1}]}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(pedido,"$.pedidoId");
        mvc.perform(post("/api/v1/pedidos/{id}/confirmacion",id.longValue())
                .header("Authorization","Bearer "+mozo)).andExpect(status().isOk());
        String detalleJson=mvc.perform(get("/api/v1/pedidos/{id}",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andReturn().getResponse().getContentAsString();
        Number det=JsonPath.read(detalleJson,"$.items[0].detalleId");
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{detalle}/listo",
                id.longValue(),det.longValue())
                .header("Authorization","Bearer "+cocinero)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"POS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("REFERENCIA_PAGO_REQUERIDA"));

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"POS\",\"referenciaPago\":\"VOUCHER-DEMO-01\",\"recargoInformadoAceptado\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medioPago").value("POS"))
            .andExpect(jsonPath("$.subtotalBase").value(18))
            .andExpect(jsonPath("$.recargoPos").value(1.00))
            .andExpect(jsonPath("$.total").value(19.00))
            .andExpect(jsonPath("$.cambio").value(0));
    }


    @Test
    void agregarUnPlatoALaMesaYaConfirmadaSeVeEnCocinaYSeBloqueaPagoPendiente() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String cliente=iniciar("cliente.demo@delicias.local","ClienteDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");

        String inicial=mvc.perform(post("/api/v1/pedidos")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","orden-confirmada-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"MESA\",\"mesaId\":3,\"items\":[{\"productoId\":1,\"cantidad\":1}]}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(inicial,"$.pedidoId");
        mvc.perform(post("/api/v1/pedidos/{id}/confirmacion",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk());

        String url="/api/v1/pedidos/"+id+"/items";
        String adicion="{\"items\":[{\"productoId\":2,\"cantidad\":1}]}";
        mvc.perform(post(url)
                .header("Authorization","Bearer "+cliente)
                .header("Idempotency-Key","adicion-cliente-no-permitida")
                .contentType(MediaType.APPLICATION_JSON).content(adicion))
            .andExpect(status().isForbidden());

        mvc.perform(post(url)
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","adicion-confirmada-001")
                .contentType(MediaType.APPLICATION_JSON).content(adicion))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.total").value(47.00))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[1].estadoPreparacion").value("PENDIENTE"));

        mvc.perform(get("/api/v1/inventario/2")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendidas").value(1))
            .andExpect(jsonPath("$.disponibles").value(19));

        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoRecibido\":50.00,\"medioPago\":\"EFECTIVO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PLATOS_PENDIENTES"));
    }


    @Test
    void mozoEnviaPedidoMesaEnUnaSolaOperacionYAbreComandaSinDuplicar() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String pedidoJson="{\"tipoAtencion\":\"MESA\",\"mesaId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1}]}";
        String pedido=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-mesa-envio-001")
                .contentType(MediaType.APPLICATION_JSON).content(pedidoJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.mesaId").value(1))
            .andExpect(jsonPath("$.total").value(25.00))
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(pedido,"$.pedidoId");

        mvc.perform(get("/api/v1/pedidos/{id}",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].nombre").value("Lomo Saltado"));

        mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-mesa-envio-001")
                .contentType(MediaType.APPLICATION_JSON).content(pedidoJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedidoId").value(id.longValue()))
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-mesa-envio-002")
                .contentType(MediaType.APPLICATION_JSON).content(pedidoJson))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("MESA_OCUPADA"));
        mvc.perform(get("/api/v1/inventario/1")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibles").value(29))
            .andExpect(jsonPath("$.vendidas").value(1));

        // Puede cancelarse antes de que Cocina marque algún plato LISTO.
        mvc.perform(post("/api/v1/pedidos/{id}/cancelacion",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));
        mvc.perform(get("/api/v1/inventario/1")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibles").value(30))
            .andExpect(jsonPath("$.vendidas").value(0));
        mvc.perform(get("/api/mesas").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.numero == 1)].estado")
                .value(org.hamcrest.Matchers.hasItem("DISPONIBLE")));
    }

    @Test
    void reservaTieneIdentificadorYUnaComandaEnCocinaNoPuedeCancelarse() throws Exception {
        mvc.perform(get("/soa-demo/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"reservedCards\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"selectedOrderDetails\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Enviar pedido a Cocina")));

        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String cuerpo="{\"cliente\":\"Cliente de reserva\",\"fecha\":\"2026-12-14\","
            +"\"horaInicio\":\"20:00\",\"horaFin\":\"21:00\",\"cantidadPersonas\":3,\"mesa\":{\"id\":2}}";
        String reserva=mvc.perform(post("/api/reservas")
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.mesa.id").value(2))
            .andReturn().getResponse().getContentAsString();
        Number idReserva=JsonPath.read(reserva,"$.id");
        mvc.perform(get("/api/reservas/{id}",idReserva.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cliente").value("Cliente de reserva"))
            .andExpect(jsonPath("$.horaInicio").value("20:00:00"));

        String pedido=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-en-cocina-no-anular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"LLEVAR\",\"items\":[{\"productoId\":4,\"cantidad\":1}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(pedido,"$.pedidoId");
        Number detalle=JsonPath.read(pedido,"$.items[0].detalleId");
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{detalle}/listo",
                id.longValue(),detalle.longValue())
                .header("Authorization","Bearer "+cocinero))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/{id}/cancelacion",id.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("ORDEN_EN_PREPARACION"));
    }


    @Test
    void cotizacionPosYTransferenciaConCuadreCorrecto() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("27.50"),
            com.deliciasperuanas.backend.service.VentaService.totalPos(new java.math.BigDecimal("26.00")));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("22.50"),
            com.deliciasperuanas.backend.service.VentaService.totalPos(new java.math.BigDecimal("21.26")));
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String cuerpo=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","transferencia-academica-uno")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"LLEVAR\",\"items\":[{\"productoId\":4,\"cantidad\":1}]}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(cuerpo,"$.pedidoId");
        Number itemId=JsonPath.read(cuerpo,"$.items[0].detalleId");
        mvc.perform(get("/api/v1/ventas/pedidos/{id}/cotizacion",id.longValue())
                .param("medioPago","POS").header("Authorization","Bearer "+caja))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotalBase").value(18))
            .andExpect(jsonPath("$.recargoPos").value(1.00))
            .andExpect(jsonPath("$.totalCobro").value(19.00));
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{itemId}/listo",
                id.longValue(),itemId.longValue())
                .header("Authorization","Bearer "+cocinero)).andExpect(status().isOk());
        // La cuenta de Caja no puede confirmar entrega a mesa.
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{itemId}/entregado",
                id.longValue(),itemId.longValue())
                .header("Authorization","Bearer "+caja)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{itemId}/entregado",
                id.longValue(),itemId.longValue())
                .header("Authorization","Bearer "+mozo)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",id.longValue())
                .header("Authorization","Bearer "+caja).contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"TRANSFERENCIA\",\"referenciaPago\":\"TRANSFER-DEMO-999\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medioPago").value("TRANSFERENCIA"))
            .andExpect(jsonPath("$.total").value(18))
            .andExpect(jsonPath("$.recargoPos").value(0));
        mvc.perform(get("/api/v1/ventas/resumen").header("Authorization","Bearer "+caja))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.porMedioDia.TRANSFERENCIA").value(18))
            .andExpect(jsonPath("$.mediosCuadrados").value(true));
    }

    @Test
    void fotoSeGuardaYCambiaEnCartaPublicaSinDarPermisoACaja() throws Exception {
        String admin=iniciar("admin.demo@delicias.local","AdminDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        java.awt.image.BufferedImage imagen=new java.awt.image.BufferedImage(
            90,90,java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream salida=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagen,"png",salida);
        org.springframework.mock.web.MockMultipartFile foto=
            new org.springframework.mock.web.MockMultipartFile("archivo","plato.png",
                "image/png",salida.toByteArray());
        mvc.perform(multipart("/api/productos/1/imagen").file(foto)
                .header("Authorization","Bearer "+caja))
            .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/productos/1/imagen").file(foto)
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imagenUrl").isNotEmpty());
        mvc.perform(get("/api/productos/1/imagen"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(salida.toByteArray()));
        mvc.perform(get("/api/productos/1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.imagenUrl").isNotEmpty());
    }


    @Test
    void cajaSupervisionControlanSalidaConMotivoYCartaAdminSeMantieneEnUnMismoPanel() throws Exception {
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String supervisor=iniciar("supervisor.demo@delicias.local","SupervisorDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        mvc.perform(get("/soa-demo/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "data-module=\"menu\" data-roles=\"ADMIN\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("productImageUpload")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("TRANSFERENCIA")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("cashPosConsent")));

        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1,\"comentario\":\"Rotura justificada\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1,\"comentario\":\"Porción con error\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibles").value(29));
        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+supervisor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1,\"comentario\":\"Merma registrada\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibles").value(28));
        mvc.perform(get("/api/v1/inventario")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/inventario")
                .header("Authorization","Bearer "+caja))
            .andExpect(status().isOk());
    }


    @Test
    void salidasDeStockYVentasCobradasPorFechaSinDuplicarMontos() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String cocina=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        mvc.perform(get("/soa-demo/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "id=\"module-salesDaily\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "id=\"dailySalesTable\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "id=\"cashReceiptId\"")));

        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":2,\"comentario\":\"Merma auditada del turno\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.salidasManuales").value(2));
        mvc.perform(get("/api/v1/inventario")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.productoId == 1)].salidasManuales")
                .value(org.hamcrest.Matchers.hasItem(2)));
        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isForbidden());

        String creado=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","fecha-cobro-inventario-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"LLEVAR\",\"items\":[{\"productoId\":1,\"cantidad\":2}]}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Number pedido=JsonPath.read(creado,"$.pedidoId");
        Number detalle=JsonPath.read(creado,"$.items[0].detalleId");
        mvc.perform(get("/api/v1/inventario/1")
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.salidasManuales").value(2))
            .andExpect(jsonPath("$.vendidas").value(2))
            .andExpect(jsonPath("$.disponibles").value(26));

        mvc.perform(post("/api/v1/pedidos/{pedido}/detalles/{detalle}/listo",
                pedido.longValue(),detalle.longValue())
                .header("Authorization","Bearer "+cocina))
            .andExpect(status().isOk());

        mvc.perform(post("/api/v1/ventas/pedidos/{pedido}/cobrar",pedido.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"EFECTIVO\",\"montoRecibido\":60.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotalBase").value(50))
            .andExpect(jsonPath("$.total").value(50))
            .andExpect(jsonPath("$.cambio").value(10));

        String fecha=java.time.LocalDate.now(
            java.time.ZoneId.of("America/Lima")).toString();
        mvc.perform(get("/api/v1/ventas/detalle-diario")
                .param("fecha",fecha)
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/ventas/detalle-diario")
                .param("fecha",fecha)
                .header("Authorization","Bearer "+caja))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ventasCobradas").value(1))
            .andExpect(jsonPath("$.unidadesVendidas").value(2))
            .andExpect(jsonPath("$.lineas[0].producto").value("Lomo Saltado"))
            .andExpect(jsonPath("$.lineas[0].cantidad").value(2))
            .andExpect(jsonPath("$.lineas[0].precioUnitario").value(25))
            .andExpect(jsonPath("$.lineas[0].subtotal").value(50))
            .andExpect(jsonPath("$.subtotalPlatos").value(50))
            .andExpect(jsonPath("$.recargosPos").value(0))
            .andExpect(jsonPath("$.totalCobrado").value(50))
            .andExpect(jsonPath("$.cuadreDetalle").value(true));
        mvc.perform(get("/api/v1/ventas/pedidos/{pedido}/cotizacion",
                pedido.longValue()).header("Authorization","Bearer "+caja))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PEDIDO_YA_PAGADO"));
    }


    @Test
    void mozoGestionaCadaPlatoConObservacionCantidadAnulacionStockYCaja() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String cocina=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String solicitud="{\"tipoAtencion\":\"LLEVAR\",\"items\":["+
            "{\"productoId\":1,\"cantidad\":1,\"observacion\":\"Bajo en sal\"},"+
            "{\"productoId\":1,\"cantidad\":1,\"observacion\":\"Sin cebolla\"},"+
            "{\"productoId\":4,\"cantidad\":1}]}";
        String body=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-observaciones-001")
                .contentType(MediaType.APPLICATION_JSON).content(solicitud))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.total").value(68))
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].observacion").value("Bajo en sal"))
            .andExpect(jsonPath("$.items[1].observacion").value("Sin cebolla"))
            .andReturn().getResponse().getContentAsString();
        Number pedido=JsonPath.read(body,"$.pedidoId");
        Number bajoSal=JsonPath.read(body,"$.items[0].detalleId");
        Number sinCebolla=JsonPath.read(body,"$.items[1].detalleId");
        Number anticucho=JsonPath.read(body,"$.items[2].detalleId");

        mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-observaciones-001")
                .contentType(MediaType.APPLICATION_JSON).content(solicitud))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedidoId").value(pedido.intValue()));
        mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","comanda-observaciones-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitud.replace("Bajo en sal","Bajo en azúcar")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("REFERENCIA_REUTILIZADA"));

        mvc.perform(patch("/api/v1/pedidos/{id}/detalles/{linea}/observacion",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacion\":\"Mucho picante\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/pedidos/{id}/detalles/{linea}/observacion",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacion\":\"Bajo en sal y bien cocido\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].observacion").value("Bajo en sal y bien cocido"));

        mvc.perform(patch("/api/v1/pedidos/{id}/detalles/{linea}/cantidad",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(93))
            .andExpect(jsonPath("$.items[0].cantidad").value(2))
            .andExpect(jsonPath("$.items[0].subtotal").value(50));
        mvc.perform(get("/api/v1/inventario/1").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendidas").value(3))
            .andExpect(jsonPath("$.disponibles").value(27));
        mvc.perform(patch("/api/v1/pedidos/{id}/detalles/{linea}/cantidad",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(68));
        mvc.perform(delete("/api/v1/pedidos/{id}/detalles/{linea}",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"El cliente cambió de opinión\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/pedidos/{id}/detalles/{linea}",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"El cliente cambió de opinión\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.total").value(43))
            .andExpect(jsonPath("$.items[0].estadoPreparacion").value("ANULADO"))
            .andExpect(jsonPath("$.items[0].motivoAnulacion").value("El cliente cambió de opinión"))
            .andExpect(jsonPath("$.items[0].anuladoPor").value("mozo.demo@delicias.local"));
        mvc.perform(get("/api/v1/inventario/1").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vendidas").value(1))
            .andExpect(jsonPath("$.disponibles").value(29));
        mvc.perform(delete("/api/v1/pedidos/{id}/detalles/{linea}",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Anulación duplicada\"}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",
                    pedido.longValue(),bajoSal.longValue())
                .header("Authorization","Bearer "+cocina))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PLATO_ANULADO"));

        for(Number linea:java.util.List.of(sinCebolla,anticucho)){
            mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",
                    pedido.longValue(),linea.longValue())
                .header("Authorization","Bearer "+cocina))
                .andExpect(status().isOk());
        }
        mvc.perform(patch("/api/v1/pedidos/{id}/detalles/{linea}/observacion",
                    pedido.longValue(),sinCebolla.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacion\":\"Demasiado tarde\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PLATO_EN_PREPARACION_O_ANULADO"));
        mvc.perform(post("/api/v1/ventas/pedidos/{id}/cobrar",pedido.longValue())
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"EFECTIVO\",\"montoRecibido\":50.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(43))
            .andExpect(jsonPath("$.items.length()").value(2));
        String hoy=java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).toString();
        mvc.perform(get("/api/v1/ventas/detalle-diario")
                .param("fecha",hoy).header("Authorization","Bearer "+caja))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCobrado").value(43))
            .andExpect(jsonPath("$.unidadesVendidas").value(2))
            .andExpect(jsonPath("$.cuadreDetalle").value(true));
    }

    @Test
    void anularUltimoPlatoCierraComandaYLiberaMesaConMotivo() throws Exception {
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String solicitud="{\"tipoAtencion\":\"MESA\",\"mesaId\":1,\"items\":["+
                "{\"productoId\":4,\"cantidad\":1,\"observacion\":\"Sin ají\"}]}";
        String body=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","anulacion-mesa-ultimo-001")
                .contentType(MediaType.APPLICATION_JSON).content(solicitud))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number pedido=JsonPath.read(body,"$.pedidoId");
        Number linea=JsonPath.read(body,"$.items[0].detalleId");
        mvc.perform(delete("/api/v1/pedidos/{id}/detalles/{linea}",
                    pedido.longValue(),linea.longValue())
                .header("Authorization","Bearer "+mozo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Cliente desistió del pedido\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"))
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.items[0].estadoPreparacion").value("ANULADO"))
            .andExpect(jsonPath("$.items[0].motivoAnulacion").value("Cliente desistió del pedido"));
        mvc.perform(get("/api/mesas").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.numero == 1)].estado")
                .value(org.hamcrest.Matchers.hasItem("DISPONIBLE")));
        mvc.perform(get("/api/v1/inventario/4").header("Authorization","Bearer "+mozo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibles").value(12))
            .andExpect(jsonPath("$.vendidas").value(0));
    }


    @Test
    void historialInventarioPermiteConsultarSalidasDeUnDiaYOtrosTiposConSeguridad() throws Exception {
        String caja=iniciar("caja.demo@delicias.local","CajaDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");

        mvc.perform(get("/soa-demo/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "id=\"stockMovementsDate\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "id=\"stockMovementsType\"")));

        mvc.perform(patch("/api/v1/inventario/1/salida")
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":2,\"comentario\":\"Porciones deterioradas\"}"))
            .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/inventario/1/reponer")
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":4}"))
            .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/inventario/4/salida")
                .header("Authorization","Bearer "+caja)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1,\"comentario\":\"Error de emplatado\"}"))
            .andExpect(status().isOk());

        String lista=mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+caja))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andReturn().getResponse().getContentAsString();
        String fecha=JsonPath.read(lista,"$[0].fecha").toString().substring(0,10);

        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+caja)
                .param("fecha",fecha).param("tipo","SALIDA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].tipo").value("SALIDA"))
            .andExpect(jsonPath("$[1].tipo").value("SALIDA"))
            .andExpect(jsonPath("$[0].comentario").value("Error de emplatado"))
            .andExpect(jsonPath("$[1].comentario").value("Porciones deterioradas"));
        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+caja)
                .param("fecha",fecha).param("tipo","ENTRADA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tipo").value("ENTRADA"));
        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+caja)
                .param("fecha",fecha))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+caja)
                .param("fecha","2030-12-31").param("tipo","SALIDA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+caja)
                .param("tipo","VENTA"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("TIPO_MOVIMIENTO_INVALIDO"));
        mvc.perform(get("/api/v1/inventario/movimientos")
                .header("Authorization","Bearer "+mozo)
                .param("fecha",fecha).param("tipo","SALIDA"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/inventario/movimientos")
                .param("fecha",fecha).param("tipo","SALIDA"))
            .andExpect(status().isUnauthorized());
    }


    @Test
    void administradorSupervisaCocinaPeroSoloCocineroMarcaListo() throws Exception {
        String admin=iniciar("admin.demo@delicias.local","AdminDemoTest123!");
        String supervisor=iniciar("supervisor.demo@delicias.local","SupervisorDemoTest123!");
        String mozo=iniciar("mozo.demo@delicias.local","MozoDemoTest123!");
        String cocinero=iniciar("cocinero.demo@delicias.local","CocineroDemoTest123!");
        String pedido=mvc.perform(post("/api/v1/pedidos/envio")
                .header("Authorization","Bearer "+mozo)
                .header("Idempotency-Key","cocina-lectura-admin-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAtencion\":\"LLEVAR\",\"items\":[{\"productoId\":4,\"cantidad\":1,\"observacion\":\"Bajo en sal\"}]}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(pedido,"$.pedidoId");
        Number linea=JsonPath.read(pedido,"$.items[0].detalleId");
        mvc.perform(get("/api/v1/pedidos")
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].items[0].observacion").value("Bajo en sal"))
            .andExpect(jsonPath("$[0].items[0].estadoPreparacion").value("PENDIENTE"));
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",
                id.longValue(),linea.longValue())
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",
                id.longValue(),linea.longValue())
                .header("Authorization","Bearer "+supervisor))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",
                id.longValue(),linea.longValue())
                .header("Authorization","Bearer "+mozo))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos/{id}",id.longValue())
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].estadoPreparacion").value("PENDIENTE"));
        mvc.perform(post("/api/v1/pedidos/{id}/detalles/{linea}/listo",
                id.longValue(),linea.longValue())
                .header("Authorization","Bearer "+cocinero))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].estadoPreparacion").value("LISTO"));
        mvc.perform(get("/api/v1/pedidos/{id}",id.longValue())
                .header("Authorization","Bearer "+admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].estadoPreparacion").value("LISTO"));
    }

    private String iniciar(String correo, String pass) throws Exception {
        String json = "{\"correo\":\""+correo+"\",\"password\":\""+pass+"\"}";
        String response = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }
}
