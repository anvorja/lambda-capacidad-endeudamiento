package com.crediya.capacidad.infrastructure.json;

import com.crediya.capacidad.domain.model.Decision;
import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.crediya.capacidad.domain.Fixtures.pedido;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultadoMapper")
class ResultadoMapperTest {

    private final ResultadoMapper mapper = new ResultadoMapper();
    private final ObjectMapper json = new ObjectMapper();
    private final EvaluadorDeCapacidad evaluador = new EvaluadorDeCapacidad();

    @Test
    @DisplayName("el mensaje de decision cumple el contrato v1 que lee SOLICITUDES, con destinatario, calculo y plan")
    void mensajeDeDecision() throws Exception {
        var pedido = pedido("4500000", "5000000", 24, "0.018");

        JsonNode mensaje = json.readTree(mapper.aMensajeDeDecision(pedido, evaluador.evaluar(pedido)));

        assertThat(mensaje.get("version").asInt()).isEqualTo(1);
        assertThat(mensaje.get("tipo").asText()).isEqualTo("DECISION_AUTOMATICA");
        assertThat(mensaje.get("id_evento").asText()).isNotBlank();
        assertThat(mensaje.get("id_solicitud").asText()).isEqualTo(pedido.solicitud().idSolicitud().toString());
        assertThat(mensaje.get("decision").asText()).isEqualTo("APROBADA");
        assertThat(mensaje.at("/destinatario/email").asText()).isEqualTo("maria@example.com");
        assertThat(mensaje.at("/destinatario/nombre").asText()).isEqualTo("Maria Gomez");
        assertThat(mensaje.at("/calculo/capacidad_maxima").decimalValue()).isEqualByComparingTo("1575000");
        assertThat(mensaje.at("/calculo/cuota_mensual").decimalValue()).isEqualByComparingTo("258404.18");
        assertThat(mensaje.at("/plan_de_pago")).hasSize(24);
        assertThat(mensaje.at("/plan_de_pago/0/capital").decimalValue()).isEqualByComparingTo("168404.18");
    }

    @Test
    @DisplayName("una decision que no es aprobacion no lleva plan de pago")
    void sinPlan() throws Exception {
        var pedido = pedido("700000", "5000000", 24, "0.018");

        JsonNode mensaje = json.readTree(mapper.aMensajeDeDecision(pedido, evaluador.evaluar(pedido)));

        assertThat(mensaje.get("decision").asText()).isEqualTo(Decision.RECHAZADA.name());
        assertThat(mensaje.has("plan_de_pago")).isFalse();
    }

    @Test
    @DisplayName("la respuesta de consulta lleva el calculo en la raiz")
    void respuestaDeConsulta() throws Exception {
        var pedido = pedido("4500000", "5000000", 24, "0.018");

        JsonNode respuesta = json.readTree(mapper.aRespuestaDeConsulta(evaluador.evaluar(pedido)));

        assertThat(respuesta.get("decision").asText()).isEqualTo("APROBADA");
        assertThat(respuesta.get("capacidad_disponible").decimalValue()).isEqualByComparingTo("1575000");
        assertThat(respuesta.get("plan_de_pago")).hasSize(24);
    }

    @Test
    @DisplayName("sin nombre del solicitante el destinatario solo lleva el correo")
    void sinNombre() throws Exception {
        var base = pedido("4500000", "5000000", 24, "0.018");
        var anonimo = new com.crediya.capacidad.domain.model.PedidoDeValidacion(base.solicitud(),
                new com.crediya.capacidad.domain.model.Solicitante("x@y.co", null, base.solicitante().salarioBase()), null);

        JsonNode mensaje = json.readTree(mapper.aMensajeDeDecision(anonimo, evaluador.evaluar(anonimo)));

        assertThat(mensaje.at("/destinatario").has("nombre")).isFalse();
    }

    @Test
    @DisplayName("la respuesta de error lleva codigo y mensaje")
    void respuestaDeError() throws Exception {
        JsonNode error = json.readTree(mapper.aRespuestaDeError("PEDIDO_INVALIDO", "falta algo"));

        assertThat(error.get("codigo").asText()).isEqualTo("PEDIDO_INVALIDO");
        assertThat(error.get("mensaje").asText()).isEqualTo("falta algo");
    }
}
