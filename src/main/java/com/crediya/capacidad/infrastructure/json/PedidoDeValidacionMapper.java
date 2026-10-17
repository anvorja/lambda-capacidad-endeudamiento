package com.crediya.capacidad.infrastructure.json;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;
import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.PrestamoAprobado;
import com.crediya.capacidad.domain.model.Solicitante;
import com.crediya.capacidad.domain.model.SolicitudAEvaluar;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Traduce el JSON que publica SOLICITUDES (version 1) al pedido del dominio. Lector tolerante: ignora campos nuevos; si traen
 * {@code tipo} o {@code version} deben ser los esperados. Los datos faltantes o imposibles los rechaza el dominio.
 */
public class PedidoDeValidacionMapper {

    static final String TIPO_EVENTO = "VALIDACION_AUTOMATICA_SOLICITADA";
    static final int VERSION_SOPORTADA = 1;

    private final ObjectMapper json = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .build();

    public PedidoDeValidacion aDominio(String cuerpo) {
        Pedido pedido = leer(cuerpo);
        if (pedido.tipo() != null && !TIPO_EVENTO.equals(pedido.tipo())) {
            throw new PedidoInvalidoException("Tipo de mensaje no soportado: " + pedido.tipo());
        }
        if (pedido.version() != null && pedido.version() != VERSION_SOPORTADA) {
            throw new PedidoInvalidoException("Version de mensaje no soportada: " + pedido.version());
        }
        if (pedido.solicitud() == null || pedido.solicitante() == null) {
            throw new PedidoInvalidoException("El pedido no trae la solicitud o el solicitante");
        }
        var solicitud = pedido.solicitud();
        var solicitante = pedido.solicitante();
        return new PedidoDeValidacion(
                new SolicitudAEvaluar(uuid(solicitud.idSolicitud()), solicitud.monto(),
                        solicitud.plazo() == null ? 0 : solicitud.plazo(), solicitud.tasaInteres()),
                new Solicitante(solicitante.email(), solicitante.nombre(), solicitante.salarioBase()),
                prestamos(pedido.prestamosAprobados()));
    }

    private Pedido leer(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new PedidoInvalidoException("El mensaje esta vacio");
        }
        try {
            Pedido pedido = json.readValue(cuerpo, Pedido.class);
            if (pedido == null) {
                throw new PedidoInvalidoException("El mensaje esta vacio");
            }
            return pedido;
        } catch (JsonProcessingException e) {
            // No se incluye el contenido del mensaje: puede traer datos personales
            throw new PedidoInvalidoException("El mensaje no es un JSON valido del pedido", e);
        }
    }

    private static UUID uuid(String texto) {
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PedidoInvalidoException("El identificador de la solicitud no es un UUID valido");
        }
    }

    private static List<PrestamoAprobado> prestamos(List<PrestamoJson> prestamos) {
        if (prestamos == null) {
            return List.of();
        }
        return prestamos.stream()
                .map(p -> new PrestamoAprobado(p.monto(), p.plazo() == null ? 0 : p.plazo(), p.tasaInteres()))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Pedido(Integer version, String tipo, SolicitudJson solicitud, SolicitanteJson solicitante,
                  List<PrestamoJson> prestamosAprobados) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SolicitudJson(String idSolicitud, BigDecimal monto, Integer plazo, BigDecimal tasaInteres) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SolicitanteJson(String email, String nombre, BigDecimal salarioBase) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PrestamoJson(BigDecimal monto, Integer plazo, BigDecimal tasaInteres) {
    }
}
