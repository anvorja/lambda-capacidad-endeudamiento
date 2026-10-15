package com.crediya.capacidad.application;

import com.crediya.capacidad.domain.model.Decision;
import com.crediya.capacidad.domain.port.PublicadorDeDecisiones;
import com.crediya.capacidad.domain.service.EvaluadorDeCapacidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.crediya.capacidad.domain.Fixtures.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Casos de uso de capacidad de endeudamiento")
class CasosDeUsoTest {

    @Mock
    private PublicadorDeDecisiones publicador;

    private final EvaluarCapacidadUseCase evaluar = new EvaluarCapacidadUseCase(new EvaluadorDeCapacidad());

    @Test
    @DisplayName("evaluar solo calcula: no publica nada")
    void evaluarNoPublica() {
        var resultado = evaluar.ejecutar(pedido("4500000", "5000000", 24, "0.018"));

        assertThat(resultado.decision()).isEqualTo(Decision.APROBADA);
        verifyNoInteractions(publicador);
    }

    @Test
    @DisplayName("validar evalua y publica la decision con el pedido")
    void validarPublica() {
        var pedido = pedido("4500000", "5000000", 24, "0.018");

        var resultado = new ValidarSolicitudUseCase(evaluar, publicador).ejecutar(pedido);

        assertThat(resultado.decision()).isEqualTo(Decision.APROBADA);
        verify(publicador).publicar(pedido, resultado);
    }

    @Test
    @DisplayName("si no se puede publicar, el error se propaga para que SQS reentregue el pedido")
    void falloAlPublicarSePropaga() {
        doThrow(new IllegalStateException("cola caida")).when(publicador).publicar(any(), any());

        assertThatThrownBy(() -> new ValidarSolicitudUseCase(evaluar, publicador).ejecutar(pedido("4500000", "5000000", 24, "0.018")))
                .isInstanceOf(IllegalStateException.class);
    }
}
