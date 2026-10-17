package com.crediya.capacidad.infrastructure.entrypoint;

import com.crediya.capacidad.application.EvaluarCapacidadUseCase;
import com.crediya.capacidad.application.ValidarSolicitudUseCase;
import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;
import com.crediya.capacidad.infrastructure.aws.SqsPublicadorDeDecisiones;
import com.crediya.capacidad.infrastructure.json.PedidoDeValidacionMapper;
import com.crediya.capacidad.infrastructure.json.ResultadoMapper;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;

/**
 * Raiz de composicion. Se crea una vez por contenedor de Lambda (arranque en frio) y se reutiliza.
 * <p>
 * Variable de entorno: {@code COLA_DECISIONES_URL} (solo la necesita el handler de SQS; el de la API no publica nada).
 * Region y credenciales las aporta Lambda; el SDK tambien respeta {@code AWS_ENDPOINT_URL} (LocalStack).
 */
public record Dependencias(PedidoDeValidacionMapper pedidos, ResultadoMapper resultados,
                           EvaluarCapacidadUseCase evaluar, java.util.function.Supplier<ValidarSolicitudUseCase> validar) {

    static final String VARIABLE_COLA = "COLA_DECISIONES_URL";

    public static Dependencias desdeEntorno() {
        return desde(System.getenv());
    }

    static Dependencias desde(Map<String, String> entorno) {
        var resultados = new ResultadoMapper();
        var evaluar = new EvaluarCapacidadUseCase(new EvaluadorDeCapacidad());
        // El cliente de SQS y la variable solo se necesitan (y se exigen) al validar por cola, no al consultar por la API
        return new Dependencias(new PedidoDeValidacionMapper(), resultados, evaluar, new java.util.function.Supplier<>() {
            private ValidarSolicitudUseCase instancia;

            @Override
            public synchronized ValidarSolicitudUseCase get() {
                if (instancia == null) {
                    String cola = entorno.get(VARIABLE_COLA);
                    if (cola == null || cola.isBlank()) {
                        throw new IllegalStateException("Falta la variable de entorno " + VARIABLE_COLA);
                    }
                    SqsClient sqs = SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
                    instancia = new ValidarSolicitudUseCase(evaluar, new SqsPublicadorDeDecisiones(sqs, cola, resultados));
                }
                return instancia;
            }
        });
    }
}
