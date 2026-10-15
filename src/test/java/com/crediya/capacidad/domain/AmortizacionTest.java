package com.crediya.capacidad.domain;

import com.crediya.capacidad.domain.exception.PedidoInvalidoException;
import com.crediya.capacidad.domain.service.Amortizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Amortizacion (sistema frances)")
class AmortizacionTest {

    @ParameterizedTest(name = "{0} a {1} meses al {2} mensual = {3}")
    @CsvSource({"5000000,24,0.018,258404.18", "2000000,12,0.022,191449.77", "1000000,12,0.018,93401.98", "6000000,24,0.018,310085.01"})
    @DisplayName("la cuota mensual coincide con la formula P*i*(1+i)^n/((1+i)^n-1)")
    void cuotaMensual(String monto, int plazo, String tasa, String esperada) {
        assertThat(Amortizacion.cuotaMensual(new BigDecimal(monto), new BigDecimal(tasa), plazo)).isEqualByComparingTo(esperada);
    }

    @Test
    @DisplayName("con tasa cero la cuota es el monto repartido en partes iguales")
    void tasaCero() {
        assertThat(Amortizacion.cuotaMensual(new BigDecimal("3000000"), BigDecimal.ZERO, 6)).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("el plan de pago separa capital e interes, y la ultima cuota deja el saldo exactamente en cero")
    void planDePago() {
        var plan = Amortizacion.planDePago(new BigDecimal("1000000"), new BigDecimal("0.018"), 12);

        assertThat(plan).hasSize(12);
        var primera = plan.get(0);
        assertThat(primera.numero()).isEqualTo(1);
        assertThat(primera.cuota()).isEqualByComparingTo("93401.98");
        assertThat(primera.interes()).isEqualByComparingTo("18000.00");
        assertThat(primera.capital()).isEqualByComparingTo("75401.98");
        assertThat(primera.saldo()).isEqualByComparingTo("924598.02");
        var segunda = plan.get(1);
        assertThat(segunda.interes()).isEqualByComparingTo("16642.76");
        assertThat(segunda.capital()).isEqualByComparingTo("76759.22");
        var ultima = plan.get(11);
        assertThat(ultima.saldo()).isEqualByComparingTo("0");
        assertThat(ultima.cuota()).isEqualByComparingTo("93401.93");
        assertThat(plan.stream().map(c -> c.capital()).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000000");
        assertThat(plan).allSatisfy(c -> assertThat(c.cuota()).isEqualByComparingTo(c.capital().add(c.interes())));
    }

    @Test
    @DisplayName("con tasa cero el plan no tiene intereses")
    void planSinIntereses() {
        var plan = Amortizacion.planDePago(new BigDecimal("3000000"), BigDecimal.ZERO, 6);

        assertThat(plan).allSatisfy(c -> assertThat(c.interes()).isEqualByComparingTo("0"));
        assertThat(plan.get(5).saldo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("rechaza montos, plazos y tasas imposibles")
    void validar() {
        var tasa = new BigDecimal("0.01");
        assertThatThrownBy(() -> Amortizacion.validar(null, 12, tasa)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> Amortizacion.validar(BigDecimal.ZERO, 12, tasa)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> Amortizacion.validar(BigDecimal.TEN, 0, tasa)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> Amortizacion.validar(BigDecimal.TEN, 361, tasa)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> Amortizacion.validar(BigDecimal.TEN, 12, null)).isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> Amortizacion.validar(BigDecimal.TEN, 12, new BigDecimal("-0.01"))).isInstanceOf(PedidoInvalidoException.class);
        Amortizacion.validar(BigDecimal.TEN, 360, BigDecimal.ZERO);
    }
}
