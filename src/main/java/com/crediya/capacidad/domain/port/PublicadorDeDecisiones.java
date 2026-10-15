package com.crediya.capacidad.domain.port;

import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.ResultadoDeValidacion;

public interface PublicadorDeDecisiones {

    /**
     * Informa el resultado a SOLICITUDES. Falla si no se pudo publicar, para que el pedido se reintente.
     */
    void publicar(PedidoDeValidacion pedido, ResultadoDeValidacion resultado);
}
