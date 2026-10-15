package com.crediya.capacidad.domain;

import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.PrestamoAprobado;
import com.crediya.capacidad.domain.model.Solicitante;
import com.crediya.capacidad.domain.model.SolicitudAEvaluar;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class Fixtures {

    public static final UUID ID = UUID.fromString("6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11");

    private Fixtures() {
    }

    public static PedidoDeValidacion pedido(String salario, String monto, int plazo, String tasa, PrestamoAprobado... aprobados) {
        return new PedidoDeValidacion(new SolicitudAEvaluar(ID, new BigDecimal(monto), plazo, new BigDecimal(tasa)),
                new Solicitante("maria@example.com", "Maria Gomez", new BigDecimal(salario)), List.of(aprobados));
    }

    public static PrestamoAprobado aprobado(String monto, int plazo, String tasa) {
        return new PrestamoAprobado(new BigDecimal(monto), plazo, new BigDecimal(tasa));
    }
}
