package com.crediya.capacidad.domain.model;

import java.math.BigDecimal;

public record CuotaPlanDePago(int numero, BigDecimal cuota, BigDecimal capital, BigDecimal interes, BigDecimal saldo) {
}
