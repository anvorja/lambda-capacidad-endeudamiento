package com.crediya.capacidad.infrastructure.json;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PedidoDeValidacionMapper")
class PedidoDeValidacionMapperTest {

    private final PedidoDeValidacionMapper mapper = new PedidoDeValidacionMapper();

    @Test
    @DisplayName("lee el pedido completo e ignora los campos que no conoce")
    void pedidoCompleto() {
        var pedido = mapper.aDominio(Pedidos.VALIDO);

        assertThat(pedido.solicitud().idSolicitud().toString()).isEqualTo(Pedidos.ID);
        assertThat(pedido.solicitud().monto()).isEqualByComparingTo("5000000");
        assertThat(pedido.solicitud().plazo()).isEqualTo(24);
        assertThat(pedido.solicitud().tasaInteres()).isEqualByComparingTo("0.018");
        assertThat(pedido.solicitante().email()).isEqualTo("maria@example.com");
        assertThat(pedido.solicitante().nombre()).isEqualTo("Maria Gomez");
        assertThat(pedido.solicitante().salarioBase()).isEqualByComparingTo("4500000");
        assertThat(pedido.prestamosAprobados()).hasSize(1);
        assertThat(pedido.prestamosAprobados().get(0).plazo()).isEqualTo(12);
    }

    @Test
    @DisplayName("la lista de prestamos aprobados es opcional y tipo y version tambien (el endpoint de consulta no los exige)")
    void minimo() {
        var pedido = mapper.aDominio("""
                {"solicitud":{"id_solicitud":"%s","monto":1000,"plazo":6,"tasa_interes":0},
                 "solicitante":{"salario_base":1000000}}""".formatted(Pedidos.ID));

        assertThat(pedido.prestamosAprobados()).isEmpty();
        assertThat(pedido.solicitante().nombre()).isNull();
    }

    @ParameterizedTest(name = "mensaje = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "no es json", "null", "[]",
            "{\"tipo\":\"OTRO\",\"solicitud\":{},\"solicitante\":{}}",
            "{\"version\":2,\"solicitud\":{},\"solicitante\":{}}",
            "{}", "{\"solicitud\":{\"id_solicitud\":\"x\"}}"})
    @DisplayName("un mensaje que no cumple el contrato se rechaza con PedidoInvalidoException")
    void invalidos(String cuerpo) {
        assertThatThrownBy(() -> mapper.aDominio(cuerpo)).isInstanceOf(PedidoInvalidoException.class);
    }

    @Test
    @DisplayName("un id que no es UUID, un plazo faltante o un salario no positivo se rechazan")
    void datosImposibles() {
        String base = "{\"solicitud\":{\"id_solicitud\":\"%s\",\"monto\":1000,\"plazo\":6,\"tasa_interes\":0},\"solicitante\":{\"salario_base\":%s}}";

        assertThatThrownBy(() -> mapper.aDominio(base.formatted("no-uuid", "1000"))).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> mapper.aDominio(base.formatted(Pedidos.ID, "0"))).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> mapper.aDominio("{\"solicitud\":{\"id_solicitud\":\"%s\",\"monto\":1000,\"tasa_interes\":0},\"solicitante\":{\"salario_base\":1000}}".formatted(Pedidos.ID)))
                .isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> mapper.aDominio(Pedidos.VALIDO.replace("\"id_solicitud\":\"" + Pedidos.ID + "\"", "\"id_solicitud\":null")))
                .isInstanceOf(PedidoInvalidoException.class);
    }

    @Test
    @DisplayName("el mensaje de error no incluye el contenido del pedido (datos personales)")
    void errorSinContenido() {
        assertThatThrownBy(() -> mapper.aDominio("{\"solicitante\":{\"email\":\"secreto@example.com\", rota"))
                .isInstanceOf(PedidoInvalidoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("secreto@example.com"));
    }
}
