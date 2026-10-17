package com.crediya.capacidad.application;

import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;
import com.crediya.capacidad.domain.port.PublicadorDeDecisiones;

/**
 * HU7: evalua la capacidad de endeudamiento de una solicitud nueva y le informa la decision a SOLICITUDES.
 * Si la publicacion falla se propaga el error: SQS reentrega el pedido (el calculo es determinista y SOLICITUDES ignora
 * las decisiones repetidas, asi que reintentar es seguro).
 */
public class ValidarSolicitudUseCase {

    private final EvaluarCapacidadUseCase evaluar;
    private final PublicadorDeDecisiones publicador;

    public ValidarSolicitudUseCase(EvaluarCapacidadUseCase evaluar, PublicadorDeDecisiones publicador) {
        this.evaluar = evaluar;
        this.publicador = publicador;
    }

    public ResultadoDeValidacion ejecutar(PedidoDeValidacion pedido) {
        ResultadoDeValidacion resultado = evaluar.ejecutar(pedido);
        publicador.publicar(pedido, resultado);
        return resultado;
    }
}
