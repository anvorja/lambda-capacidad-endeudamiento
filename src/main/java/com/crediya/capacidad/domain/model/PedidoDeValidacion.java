package com.crediya.capacidad.domain.model;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;

import java.util.List;

/**
 * Todo lo necesario para evaluar la capacidad de endeudamiento, sin consultar a nadie mas.
 */
public record PedidoDeValidacion(SolicitudAEvaluar solicitud, Solicitante solicitante, List<PrestamoAprobado> prestamosAprobados) {

    public PedidoDeValidacion {
        if (solicitud == null || solicitante == null) {
            throw new PedidoInvalidoException("El pedido debe traer la solicitud y el solicitante");
        }
        prestamosAprobados = prestamosAprobados == null ? List.of() : List.copyOf(prestamosAprobados);
    }
}
