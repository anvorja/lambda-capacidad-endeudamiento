package com.crediya.capacidad.domain.model;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;

import java.math.BigDecimal;

public record Solicitante(String email, String nombre, BigDecimal salarioBase) {

    public Solicitante {
        if (salarioBase == null || salarioBase.signum() <= 0) {
            throw new PedidoInvalidoException("El salario base debe ser mayor a cero");
        }
    }
}
