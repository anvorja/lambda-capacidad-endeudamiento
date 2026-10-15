package com.crediya.capacidad.infrastructure.aws;

import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;
import com.crediya.capacidad.infrastructure.json.ResultadoMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static com.crediya.capacidad.domain.Fixtures.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@Testcontainers
@DisplayName("SqsPublicadorDeDecisiones contra SQS real (LocalStack)")
class SqsPublicadorDeDecisionesLocalStackTest {

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.9.2"))
            .withServices(LocalStackContainer.Service.SQS);

    private static SqsClient sqs;
    private static String cola;

    @BeforeAll
    static void preparar() {
        sqs = SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
        cola = sqs.createQueue(r -> r.queueName("crediya-decision-automatica")).queueUrl();
    }

    @Test
    @DisplayName("la decision publicada llega a la cola con el contrato v1")
    void publicaLaDecision() throws Exception {
        var pedido = pedido("4500000", "5000000", 24, "0.018");
        var resultado = new EvaluadorDeCapacidad().evaluar(pedido);

        new SqsPublicadorDeDecisiones(sqs, cola, new ResultadoMapper()).publicar(pedido, resultado);

        var mensajes = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(cola).waitTimeSeconds(10).build()).messages();
        assertThat(mensajes).hasSize(1);
        var json = new ObjectMapper().readTree(mensajes.get(0).body());
        assertThat(json.get("tipo").asText()).isEqualTo("DECISION_AUTOMATICA");
        assertThat(json.get("decision").asText()).isEqualTo("APROBADA");
        assertThat(json.get("id_solicitud").asText()).isEqualTo(pedido.solicitud().idSolicitud().toString());
    }

    @Test
    @DisplayName("si la cola no existe, publicar falla para que el pedido se reintente")
    void colaInexistente() {
        var pedido = pedido("4500000", "5000000", 24, "0.018");
        var resultado = new EvaluadorDeCapacidad().evaluar(pedido);
        var publicador = new SqsPublicadorDeDecisiones(sqs, cola.replace("crediya-decision-automatica", "no-existe"), new ResultadoMapper());

        assertThatThrownBy(() -> publicador.publicar(pedido, resultado)).isInstanceOf(SqsException.class);
    }
}
