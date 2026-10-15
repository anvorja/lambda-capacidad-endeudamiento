package com.crediya.capacidad.infrastructure.json;

import com.crediya.capacidad.domain.model.CuotaPlanDePago;
import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

/**
 * Escribe el resultado como JSON: el mensaje de decision que consume SOLICITUDES (version 1) y la respuesta del endpoint de
 * consulta.
 */
public class ResultadoMapper {

    static final String TIPO_DECISION = "DECISION_AUTOMATICA";
    static final int VERSION = 1;

    private final ObjectMapper json = new ObjectMapper();

    /**
     * Mensaje para la cola de decisiones. {@code id_evento} es nuevo en cada publicacion; la deduplicacion real la hace
     * SOLICITUDES por el estado de la solicitud.
     */
    public String aMensajeDeDecision(PedidoDeValidacion pedido, ResultadoDeValidacion resultado) {
        ObjectNode raiz = json.createObjectNode();
        raiz.put("version", VERSION);
        raiz.put("id_evento", UUID.randomUUID().toString());
        raiz.put("tipo", TIPO_DECISION);
        raiz.put("id_solicitud", pedido.solicitud().idSolicitud().toString());
        raiz.put("decision", resultado.decision().name());
        ObjectNode destinatario = raiz.putObject("destinatario");
        destinatario.put("email", pedido.solicitante().email());
        if (pedido.solicitante().nombre() != null) {
            destinatario.put("nombre", pedido.solicitante().nombre());
        }
        ObjectNode calculo = raiz.putObject("calculo");
        escribirCalculo(calculo, resultado);
        escribirPlan(raiz, resultado);
        return escribir(raiz);
    }

    /** Respuesta del endpoint de consulta: el calculo y, si se aprueba, el plan de pago. */
    public String aRespuestaDeConsulta(ResultadoDeValidacion resultado) {
        ObjectNode raiz = json.createObjectNode();
        raiz.put("decision", resultado.decision().name());
        escribirCalculo(raiz, resultado);
        escribirPlan(raiz, resultado);
        return escribir(raiz);
    }

    public String aRespuestaDeError(String codigo, String mensaje) {
        ObjectNode raiz = json.createObjectNode();
        raiz.put("codigo", codigo);
        raiz.put("mensaje", mensaje);
        return escribir(raiz);
    }

    private static void escribirCalculo(ObjectNode nodo, ResultadoDeValidacion resultado) {
        nodo.put("capacidad_maxima", resultado.capacidadMaxima());
        nodo.put("deuda_actual", resultado.deudaActual());
        nodo.put("capacidad_disponible", resultado.capacidadDisponible());
        nodo.put("cuota_mensual", resultado.cuotaMensual());
    }

    private static void escribirPlan(ObjectNode raiz, ResultadoDeValidacion resultado) {
        if (resultado.planDePago().isEmpty()) {
            return;
        }
        ArrayNode plan = raiz.putArray("plan_de_pago");
        for (CuotaPlanDePago cuota : resultado.planDePago()) {
            ObjectNode fila = plan.addObject();
            fila.put("numero", cuota.numero());
            fila.put("cuota", cuota.cuota());
            fila.put("capital", cuota.capital());
            fila.put("interes", cuota.interes());
            fila.put("saldo", cuota.saldo());
        }
    }

    private String escribir(ObjectNode nodo) {
        try {
            return json.writeValueAsString(nodo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la respuesta", e);
        }
    }
}
