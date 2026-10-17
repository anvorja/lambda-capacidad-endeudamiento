package com.crediya.capacidad.infrastructure.entrypoint;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.crediya.capacidad.domain.exception.PedidoInvalidoException;

import java.util.Map;

/**
 * {@code POST /api/v1/calcular-capacidad} (API Gateway): calcula la capacidad de endeudamiento de un pedido sin publicar nada ni
 * tocar solicitudes. Sirve para simular antes de pedir un credito. El cuerpo es el mismo pedido que llega por SQS.
 */
public class CalculoApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final System.Logger LOG = System.getLogger(CalculoApiHandler.class.getName());
    private static final Map<String, String> CABECERAS = Map.of("Content-Type", "application/json");

    private final Dependencias dependencias;

    /** Constructor que usa Lambda. */
    public CalculoApiHandler() {
        this(Dependencias.desdeEntorno());
    }

    CalculoApiHandler(Dependencias dependencias) {
        this.dependencias = dependencias;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent peticion, Context contexto) {
        try {
            var pedido = dependencias.pedidos().aDominio(peticion.getBody());
            var resultado = dependencias.evaluar().ejecutar(pedido);
            return respuesta(200, dependencias.resultados().aRespuestaDeConsulta(resultado));
        } catch (PedidoInvalidoException e) {
            return respuesta(400, dependencias.resultados().aRespuestaDeError("PEDIDO_INVALIDO", e.getMessage()));
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Error inesperado calculando la capacidad error={0}", e.getClass().getSimpleName());
            return respuesta(500, dependencias.resultados().aRespuestaDeError("ERROR_INTERNO",
                    "Ocurrio un error interno. Intenta de nuevo mas tarde."));
        }
    }

    private static APIGatewayProxyResponseEvent respuesta(int estado, String cuerpo) {
        return new APIGatewayProxyResponseEvent().withStatusCode(estado).withHeaders(CABECERAS).withBody(cuerpo);
    }
}
