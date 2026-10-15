package com.crediya.capacidad.infrastructure.json;

public final class Pedidos {

    public static final String ID = "6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11";

    public static final String VALIDO = """
            {"version":1,"id_evento":"e1","tipo":"VALIDACION_AUTOMATICA_SOLICITADA","ocurrido_en":"2026-10-15T10:00:00Z",
             "campo_futuro":true,
             "solicitud":{"id_solicitud":"%s","monto":5000000.00,"plazo":24,"tasa_interes":0.018,"tipo_prestamo":"PERSONAL"},
             "solicitante":{"email":"maria@example.com","nombre":"Maria Gomez","salario_base":4500000},
             "prestamos_aprobados":[{"monto":2000000.00,"plazo":12,"tasa_interes":0.022}]}""".formatted(ID);

    private Pedidos() {
    }
}
