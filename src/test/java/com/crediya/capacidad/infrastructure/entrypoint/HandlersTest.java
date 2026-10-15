package com.crediya.capacidad.infrastructure.entrypoint;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.crediya.capacidad.application.EvaluarCapacidadUseCase;
import com.crediya.capacidad.application.ValidarSolicitudUseCase;
import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;
import com.crediya.capacidad.domain.port.PublicadorDeDecisiones;
import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;
import com.crediya.capacidad.infrastructure.json.PedidoDeValidacionMapper;
import com.crediya.capacidad.infrastructure.json.Pedidos;
import com.crediya.capacidad.infrastructure.json.ResultadoMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Handlers de la Lambda de capacidad de endeudamiento")
class HandlersTest {

    private final List<ResultadoDeValidacion> publicados = new ArrayList<>();
    private boolean colaCaida;

    private final PublicadorDeDecisiones publicador = (PedidoDeValidacion pedido, ResultadoDeValidacion resultado) -> {
        if (colaCaida) {
            throw new IllegalStateException("cola caida");
        }
        publicados.add(resultado);
    };

    private Dependencias dependencias() {
        var evaluar = new EvaluarCapacidadUseCase(new EvaluadorDeCapacidad());
        return new Dependencias(new PedidoDeValidacionMapper(), new ResultadoMapper(), evaluar,
                () -> new ValidarSolicitudUseCase(evaluar, publicador));
    }

    private static SQSEvent.SQSMessage mensaje(String id, String cuerpo) {
        var mensaje = new SQSEvent.SQSMessage();
        mensaje.setMessageId(id);
        mensaje.setBody(cuerpo);
        return mensaje;
    }

    private static SQSEvent lote(SQSEvent.SQSMessage... mensajes) {
        var evento = new SQSEvent();
        evento.setRecords(List.of(mensajes));
        return evento;
    }

    @Test
    @DisplayName("SQS: un pedido valido se evalua y se publica la decision; no hay fallos")
    void sqsOk() {
        var respuesta = new ValidacionSqsHandler(dependencias()).handleRequest(lote(mensaje("m1", Pedidos.VALIDO)), null);

        assertThat(respuesta.getBatchItemFailures()).isEmpty();
        assertThat(publicados).hasSize(1);
    }

    @Test
    @DisplayName("SQS: solo fallan los mensajes malos; el resto del lote se procesa")
    void sqsFalloParcial() {
        var respuesta = new ValidacionSqsHandler(dependencias())
                .handleRequest(lote(mensaje("malo", "basura"), mensaje("bueno", Pedidos.VALIDO)), null);

        assertThat(respuesta.getBatchItemFailures()).extracting("itemIdentifier").containsExactly("malo");
        assertThat(publicados).hasSize(1);
    }

    @Test
    @DisplayName("SQS: si no se puede publicar la decision, el mensaje se reporta como fallido para que se reintente")
    void sqsColaCaida() {
        colaCaida = true;

        var respuesta = new ValidacionSqsHandler(dependencias()).handleRequest(lote(mensaje("m1", Pedidos.VALIDO)), null);

        assertThat(respuesta.getBatchItemFailures()).extracting("itemIdentifier").containsExactly("m1");
    }

    @Test
    @DisplayName("API: un pedido valido responde 200 con el calculo y no publica nada")
    void apiOk() throws Exception {
        var peticion = new APIGatewayProxyRequestEvent().withBody(Pedidos.VALIDO);

        var respuesta = new CalculoApiHandler(dependencias()).handleRequest(peticion, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(200);
        assertThat(respuesta.getHeaders()).containsEntry("Content-Type", "application/json");
        var json = new ObjectMapper().readTree(respuesta.getBody());
        assertThat(json.get("decision").asText()).isEqualTo("APROBADA");
        assertThat(json.get("deuda_actual").decimalValue()).isEqualByComparingTo("191449.77");
        assertThat(publicados).isEmpty();
    }

    @Test
    @DisplayName("API: un pedido invalido responde 400 con codigo PEDIDO_INVALIDO")
    void apiInvalido() throws Exception {
        var respuesta = new CalculoApiHandler(dependencias()).handleRequest(new APIGatewayProxyRequestEvent().withBody("{}"), null);

        assertThat(respuesta.getStatusCode()).isEqualTo(400);
        assertThat(new ObjectMapper().readTree(respuesta.getBody()).get("codigo").asText()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("API: un error inesperado responde 500 generico sin detalles internos")
    void apiErrorInesperado() throws Exception {
        var roto = new Dependencias(new PedidoDeValidacionMapper() {
            @Override
            public PedidoDeValidacion aDominio(String cuerpo) {
                throw new IllegalStateException("detalle interno secreto");
            }
        }, new ResultadoMapper(), null, null);

        var respuesta = new CalculoApiHandler(roto).handleRequest(new APIGatewayProxyRequestEvent().withBody("{}"), null);

        assertThat(respuesta.getStatusCode()).isEqualTo(500);
        assertThat(respuesta.getBody()).doesNotContain("secreto");
    }

    @Test
    @DisplayName("Dependencias: el handler de SQS exige COLA_DECISIONES_URL pero la API no")
    void dependenciasExigenLaColaSoloAlValidar() {
        var sinCola = Dependencias.desde(Map.of());

        assertThat(sinCola.evaluar()).isNotNull();
        assertThatThrownBy(() -> sinCola.validar().get()).isInstanceOf(IllegalStateException.class).hasMessageContaining("COLA_DECISIONES_URL");
        assertThatThrownBy(() -> Dependencias.desde(Map.of("COLA_DECISIONES_URL", "  ")).validar().get())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Dependencias: con la cola configurada arma el caso de uso una sola vez")
    void dependenciasConCola() {
        // El cliente de SQS toma region y credenciales del entorno de Lambda; aqui se simulan con propiedades del sistema
        System.setProperty("aws.region", "us-east-1");
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
        try {
            var dependencias = Dependencias.desde(Map.of("COLA_DECISIONES_URL", "http://localhost:4566/000000000000/cola"));

            assertThat(dependencias.validar().get()).isSameAs(dependencias.validar().get());
        } finally {
            System.clearProperty("aws.region");
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }
}
