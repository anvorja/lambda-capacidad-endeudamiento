package com.crediya.capacidad.domain.service;

import com.crediya.capacidad.domain.model.CuotaPlanDePago;
import com.crediya.capacidad.domain.model.Decision;
import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.PrestamoAprobado;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Regla de negocio de HU7.
 * <ul>
 *   <li>Capacidad maxima = 35% del salario base.</li>
 *   <li>Deuda actual = suma de las cuotas mensuales de los prestamos aprobados.</li>
 *   <li>Capacidad disponible = capacidad maxima - deuda actual.</li>
 *   <li>Se aprueba si la cuota de la nueva solicitud cabe en la capacidad disponible; si no, se rechaza.</li>
 *   <li>Una aprobacion de un monto superior a 5 salarios base pasa a revision manual de un asesor.</li>
 * </ul>
 */
public class EvaluadorDeCapacidad {

    static final BigDecimal PORCENTAJE_ENDEUDAMIENTO = new BigDecimal("0.35");
    static final BigDecimal SALARIOS_PARA_REVISION_MANUAL = new BigDecimal("5");

    public ResultadoDeValidacion evaluar(PedidoDeValidacion pedido) {
        var solicitud = pedido.solicitud();
        BigDecimal salario = pedido.solicitante().salarioBase();

        BigDecimal capacidadMaxima = salario.multiply(PORCENTAJE_ENDEUDAMIENTO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deudaActual = sumarCuotas(pedido.prestamosAprobados());
        BigDecimal capacidadDisponible = capacidadMaxima.subtract(deudaActual);
        BigDecimal cuota = Amortizacion.cuotaMensual(solicitud.monto(), solicitud.tasaInteres(), solicitud.plazo());

        Decision decision = decidir(cuota, capacidadDisponible, solicitud.monto(), salario);
        var plan = decision == Decision.APROBADA
                ? Amortizacion.planDePago(solicitud.monto(), solicitud.tasaInteres(), solicitud.plazo())
                : List.<CuotaPlanDePago>of();
        return new ResultadoDeValidacion(decision, capacidadMaxima, deudaActual, capacidadDisponible, cuota, plan);
    }

    private static Decision decidir(BigDecimal cuota, BigDecimal disponible, BigDecimal monto, BigDecimal salario) {
        if (cuota.compareTo(disponible) > 0) {
            return Decision.RECHAZADA;
        }
        return monto.compareTo(salario.multiply(SALARIOS_PARA_REVISION_MANUAL)) > 0 ? Decision.REVISION_MANUAL : Decision.APROBADA;
    }

    private static BigDecimal sumarCuotas(List<PrestamoAprobado> prestamos) {
        return prestamos.stream().map(PrestamoAprobado::cuotaMensual).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
