package com.crediya.capacidad.domain.exception;

/**
 * El pedido de validacion no cumple el contrato o tiene datos imposibles (salario negativo, plazo cero...). Reintentarlo tal
 * cual no sirve: va a la cola de mensajes fallidos para que alguien lo revise.
 */
public class PedidoInvalidoException extends RuntimeException {

    public PedidoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public PedidoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
