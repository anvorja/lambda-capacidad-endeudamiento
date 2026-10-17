package com.crediya.capacidad.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param capacidadMaxima      35% del salario base
 * @param deudaActual          suma de las cuotas mensuales de los prestamos aprobados
 * @param capacidadDisponible  capacidad maxima menos deuda actual (puede ser negativa si ya esta sobreendeudado)
 * @param cuotaMensual         cuota de la solicitud nueva
 * @param planDePago           solo cuando la decision es APROBADA
 */
public record ResultadoDeValidacion(Decision decision,
                                    BigDecimal capacidadMaxima,
                                    BigDecimal deudaActual,
                                    BigDecimal capacidadDisponible,
                                    BigDecimal cuotaMensual,
                                    List<CuotaPlanDePago> planDePago) {

    public ResultadoDeValidacion {
        planDePago = planDePago == null ? List.of() : List.copyOf(planDePago);
    }
}
