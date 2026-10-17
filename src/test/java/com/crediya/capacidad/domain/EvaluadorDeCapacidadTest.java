package com.crediya.capacidad.domain;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;
import com.crediya.capacidad.domain.model.Decision;
import com.crediya.capacidad.domain.model.PedidoDeValidacion;
import com.crediya.capacidad.domain.model.PrestamoAprobado;
import com.crediya.capacidad.domain.model.Solicitante;
import com.crediya.capacidad.domain.model.SolicitudAEvaluar;
import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.crediya.capacidad.domain.Fixtures.aprobado;
import static com.crediya.capacidad.domain.Fixtures.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EvaluadorDeCapacidad (HU7)")
class EvaluadorDeCapacidadTest {

    private final EvaluadorDeCapacidad evaluador = new EvaluadorDeCapacidad();

    @Test
    @DisplayName("aprueba cuando la cuota cabe en el 35% del salario y devuelve el calculo completo con el plan de pago")
    void aprueba() {
        var resultado = evaluador.evaluar(pedido("4500000", "5000000", 24, "0.018"));

        assertThat(resultado.decision()).isEqualTo(Decision.APROBADA);
        assertThat(resultado.capacidadMaxima()).isEqualByComparingTo("1575000.00");
        assertThat(resultado.deudaActual()).isEqualByComparingTo("0");
        assertThat(resultado.capacidadDisponible()).isEqualByComparingTo("1575000.00");
        assertThat(resultado.cuotaMensual()).isEqualByComparingTo("258404.18");
        assertThat(resultado.planDePago()).hasSize(24);
        assertThat(resultado.planDePago().get(0).interes()).isEqualByComparingTo("90000.00");
        assertThat(resultado.planDePago().get(23).saldo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("la deuda actual es la suma de las cuotas de los prestamos aprobados y reduce la capacidad disponible")
    void descuentaLaDeuda() {
        var resultado = evaluador.evaluar(pedido("4500000", "5000000", 24, "0.018", aprobado("2000000", 12, "0.022")));

        assertThat(resultado.deudaActual()).isEqualByComparingTo("191449.77");
        assertThat(resultado.capacidadDisponible()).isEqualByComparingTo("1383550.23");
        assertThat(resultado.decision()).isEqualTo(Decision.APROBADA);
    }

    @Test
    @DisplayName("rechaza cuando la cuota supera la capacidad disponible, sin plan de pago")
    void rechazaPorCuotaAlta() {
        // 35% de 1.000.000 = 350.000; tres prestamos vigentes suman 574.349,31 y ya superan la capacidad
        var resultado = evaluador.evaluar(pedido("1000000", "1000000", 12, "0.018",
                aprobado("2000000", 12, "0.022"), aprobado("2000000", 12, "0.022"), aprobado("2000000", 12, "0.022")));

        assertThat(resultado.decision()).isEqualTo(Decision.RECHAZADA);
        assertThat(resultado.deudaActual()).isEqualByComparingTo("574349.31");
        assertThat(resultado.capacidadDisponible()).isEqualByComparingTo("-224349.31");
        assertThat(resultado.planDePago()).isEmpty();
    }

    @Test
    @DisplayName("rechaza cuando la cuota por si sola supera el 35% del salario")
    void rechazaPorCuotaSolaAlta() {
        var resultado = evaluador.evaluar(pedido("700000", "5000000", 24, "0.018"));

        assertThat(resultado.capacidadMaxima()).isEqualByComparingTo("245000.00");
        assertThat(resultado.decision()).isEqualTo(Decision.RECHAZADA);
    }

    @Test
    @DisplayName("aprueba cuando la cuota es exactamente igual a la capacidad disponible")
    void cuotaIgualADisponible() {
        // 35% de 1.428.571,43 = 500.000,00 y 3.000.000 a 6 meses sin interes cuesta 500.000,00 por mes
        var resultado = evaluador.evaluar(pedido("1428571.43", "3000000", 6, "0"));

        assertThat(resultado.capacidadDisponible()).isEqualByComparingTo("500000.00");
        assertThat(resultado.cuotaMensual()).isEqualByComparingTo("500000.00");
        assertThat(resultado.decision()).isEqualTo(Decision.APROBADA);
    }

    @Test
    @DisplayName("un centavo por encima de la capacidad disponible ya se rechaza")
    void cuotaUnCentavoSobreElDisponible() {
        var resultado = evaluador.evaluar(pedido("1428571.40", "3000000", 6, "0"));

        assertThat(resultado.capacidadDisponible()).isEqualByComparingTo("499999.99");
        assertThat(resultado.decision()).isEqualTo(Decision.RECHAZADA);
    }

    @Test
    @DisplayName("una aprobacion de un monto mayor a 5 salarios pasa a revision manual, sin plan de pago")
    void revisionManual() {
        // salario 1.000.000: 6.000.000 > 5 salarios; la cuota (310.085,01) cabe en 350.000
        var resultado = evaluador.evaluar(pedido("1000000", "6000000", 24, "0.018"));

        assertThat(resultado.cuotaMensual()).isEqualByComparingTo("310085.01");
        assertThat(resultado.decision()).isEqualTo(Decision.REVISION_MANUAL);
        assertThat(resultado.planDePago()).isEmpty();
    }

    @Test
    @DisplayName("exactamente 5 salarios no es mayor a 5 salarios: se aprueba")
    void cincoSalariosExactos() {
        var resultado = evaluador.evaluar(pedido("1000000", "5000000", 24, "0.018"));

        assertThat(resultado.decision()).isEqualTo(Decision.APROBADA);
    }

    @Test
    @DisplayName("si la cuota no cabe, se rechaza aunque el monto supere 5 salarios (revision manual solo aplica a aprobados)")
    void montoAltoPeroCuotaNoCabe() {
        var resultado = evaluador.evaluar(pedido("1000000", "20000000", 24, "0.018"));

        assertThat(resultado.decision()).isEqualTo(Decision.RECHAZADA);
    }

    @Test
    @DisplayName("el pedido y sus partes rechazan datos imposibles")
    void validaciones() {
        var solicitud = new SolicitudAEvaluar(UUID.randomUUID(), BigDecimal.TEN, 12, BigDecimal.ZERO);
        var solicitante = new Solicitante("a@b.co", null, BigDecimal.TEN);

        assertThatThrownBy(() -> new Solicitante("a@b.co", "A", BigDecimal.ZERO)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new Solicitante("a@b.co", "A", null)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new SolicitudAEvaluar(null, BigDecimal.TEN, 12, BigDecimal.ZERO)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new PedidoDeValidacion(null, solicitante, null)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new PedidoDeValidacion(solicitud, null, null)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> new PrestamoAprobado(BigDecimal.ZERO, 12, BigDecimal.ZERO)).isInstanceOf(PedidoInvalidoException.class);
        assertThat(new PedidoDeValidacion(solicitud, solicitante, null).prestamosAprobados()).isEmpty();
    }
}
