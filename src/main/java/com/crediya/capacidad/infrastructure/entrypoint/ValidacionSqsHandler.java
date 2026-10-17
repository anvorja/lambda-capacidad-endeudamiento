package com.crediya.capacidad.infrastructure.entrypoint;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada de la validacion automatica (HU7): consume los pedidos de SOLICITUDES desde SQS, evalua la capacidad de
 * endeudamiento y publica la decision. Responde con fallos parciales del lote ({@code ReportBatchItemFailures}): solo los
 * pedidos que fallaron vuelven a la cola y, tras agotar los reintentos, a su cola de mensajes fallidos.
 */
public class ValidacionSqsHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final System.Logger LOG = System.getLogger(ValidacionSqsHandler.class.getName());

    private final Dependencias dependencias;

    /** Constructor que usa Lambda. */
    public ValidacionSqsHandler() {
        this(Dependencias.desdeEntorno());
    }

    ValidacionSqsHandler(Dependencias dependencias) {
        this.dependencias = dependencias;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent evento, Context contexto) {
        List<SQSBatchResponse.BatchItemFailure> fallos = new ArrayList<>();
        for (SQSEvent.SQSMessage mensaje : evento.getRecords()) {
            try {
                var resultado = dependencias.validar().get().ejecutar(dependencias.pedidos().aDominio(mensaje.getBody()));
                LOG.log(System.Logger.Level.INFO, "Solicitud evaluada messageId={0} decision={1}",
                        mensaje.getMessageId(), resultado.decision());
            } catch (RuntimeException e) {
                // Solo el tipo de error y el id del mensaje: el cuerpo trae datos personales y financieros
                LOG.log(System.Logger.Level.ERROR, "Fallo la validacion messageId={0} error={1}",
                        mensaje.getMessageId(), e.getClass().getSimpleName());
                fallos.add(new SQSBatchResponse.BatchItemFailure(mensaje.getMessageId()));
            }
        }
        return new SQSBatchResponse(fallos);
    }
}
