package com.crediya.capacidad.application;

import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;
import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;

/**
 * Calcula la capacidad de endeudamiento sin efectos: lo usa el endpoint de consulta (API Gateway).
 */
public class EvaluarCapacidadUseCase {

    private final EvaluadorDeCapacidad evaluador;

    public EvaluarCapacidadUseCase(EvaluadorDeCapacidad evaluador) {
        this.evaluador = evaluador;
    }

    public ResultadoDeValidacion ejecutar(PedidoDeValidacion pedido) {
        return evaluador.evaluar(pedido);
    }
}
