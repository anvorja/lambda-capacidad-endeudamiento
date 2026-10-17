package com.crediya.capacidad.domain.model;

import com.crediya.capacidad.domain.service.Amortizacion;

import java.math.BigDecimal;

/**
 * Un prestamo que el solicitante ya tiene aprobado: su cuota mensual cuenta como deuda vigente.
 */
public record PrestamoAprobado(BigDecimal monto, int plazo, BigDecimal tasaInteres) {

    public PrestamoAprobado {
        Amortizacion.validar(monto, plazo, tasaInteres);
    }

    public BigDecimal cuotaMensual() {
        return Amortizacion.cuotaMensual(monto, tasaInteres, plazo);
    }
}
