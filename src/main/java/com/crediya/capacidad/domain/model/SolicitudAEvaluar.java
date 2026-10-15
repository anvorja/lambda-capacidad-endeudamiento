package com.crediya.capacidad.domain.model;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;
import com.crediya.capacidad.domain.service.Amortizacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * La solicitud nueva. {@code tasaInteres} es mensual, como fraccion (0.018 = 1.8% mensual).
 */
public record SolicitudAEvaluar(UUID idSolicitud, BigDecimal monto, int plazo, BigDecimal tasaInteres) {

    public SolicitudAEvaluar {
        if (idSolicitud == null) {
            throw new PedidoInvalidoException("Falta el identificador de la solicitud");
        }
        Amortizacion.validar(monto, plazo, tasaInteres);
    }
}
