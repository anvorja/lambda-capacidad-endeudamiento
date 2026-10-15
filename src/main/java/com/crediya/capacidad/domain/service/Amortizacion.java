package com.crediya.capacidad.domain.service;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;
import com.crediya.capacidad.domain.model.CuotaPlanDePago;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema frances de amortizacion: cuota fija. Cuota = P * i * (1 + i)^n / ((1 + i)^n - 1), con P = monto, i = tasa mensual y
 * n = plazo en meses. Los valores en dinero se redondean a 2 decimales (mitad hacia arriba).
 */
public final class Amortizacion {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int ESCALA = 2;
    private static final int PLAZO_MAXIMO = 360;

    private Amortizacion() {
    }

    public static void validar(BigDecimal monto, int plazo, BigDecimal tasaMensual) {
        if (monto == null || monto.signum() <= 0) {
            throw new PedidoInvalidoException("El monto debe ser mayor a cero");
        }
        if (plazo < 1 || plazo > PLAZO_MAXIMO) {
            throw new PedidoInvalidoException("El plazo debe estar entre 1 y " + PLAZO_MAXIMO + " meses");
        }
        if (tasaMensual == null || tasaMensual.signum() < 0) {
            throw new PedidoInvalidoException("La tasa de interes no puede ser negativa");
        }
    }

    public static BigDecimal cuotaMensual(BigDecimal monto, BigDecimal tasaMensual, int plazo) {
        if (tasaMensual.signum() == 0) {
            return monto.divide(BigDecimal.valueOf(plazo), ESCALA, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(plazo, PRECISION);
        return monto.multiply(tasaMensual).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), PRECISION)
                .setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Plan de pago mes a mes. La ultima cuota se ajusta para que el saldo termine exactamente en cero (los redondeos de cada
     * mes dejan un residuo de centavos).
     */
    public static List<CuotaPlanDePago> planDePago(BigDecimal monto, BigDecimal tasaMensual, int plazo) {
        BigDecimal cuota = cuotaMensual(monto, tasaMensual, plazo);
        BigDecimal saldo = monto.setScale(ESCALA, RoundingMode.HALF_UP);
        List<CuotaPlanDePago> plan = new ArrayList<>(plazo);
        for (int numero = 1; numero <= plazo; numero++) {
            BigDecimal interes = saldo.multiply(tasaMensual).setScale(ESCALA, RoundingMode.HALF_UP);
            boolean ultima = numero == plazo;
            BigDecimal capital = ultima ? saldo : cuota.subtract(interes);
            BigDecimal cuotaDelMes = ultima ? capital.add(interes) : cuota;
            saldo = saldo.subtract(capital);
            plan.add(new CuotaPlanDePago(numero, cuotaDelMes, capital, interes, saldo));
        }
        return plan;
    }
}
