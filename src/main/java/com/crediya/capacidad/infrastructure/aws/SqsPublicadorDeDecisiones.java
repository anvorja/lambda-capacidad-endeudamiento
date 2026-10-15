package com.crediya.capacidad.infrastructure.aws;

import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;
import com.crediya.capacidad.domain.port.PublicadorDeDecisiones;
import com.crediya.capacidad.infrastructure.json.ResultadoMapper;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Publica la decision en la cola que escucha SOLICITUDES.
 */
public class SqsPublicadorDeDecisiones implements PublicadorDeDecisiones {

    private final SqsClient sqs;
    private final String colaUrl;
    private final ResultadoMapper mapper;

    public SqsPublicadorDeDecisiones(SqsClient sqs, String colaUrl, ResultadoMapper mapper) {
        this.sqs = sqs;
        this.colaUrl = colaUrl;
        this.mapper = mapper;
    }

    @Override
    public void publicar(PedidoDeValidacion pedido, ResultadoDeValidacion resultado) {
        sqs.sendMessage(r -> r.queueUrl(colaUrl).messageBody(mapper.aMensajeDeDecision(pedido, resultado)));
    }
}
