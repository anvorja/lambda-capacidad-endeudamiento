# CrediYa - Lambda de capacidad de endeudamiento (HU7)

Evalua si un solicitante puede asumir un credito nuevo. Dos puntos de entrada sobre el mismo dominio:

| Entrada | Handler | Que hace |
|---------|---------|----------|
| Cola SQS `crediya-validacion-automatica` | `com.crediya.capacidad.infrastructure.entrypoint.ValidacionSqsHandler::handleRequest` | Evalua el pedido que publica SOLICITUDES y publica la **decision** en `crediya-decision-automatica` |
| API Gateway `POST /api/v1/calcular-capacidad` | `com.crediya.capacidad.infrastructure.entrypoint.CalculoApiHandler::handleRequest` | Calcula y responde 200 con el resultado; **no publica nada** (sirve para simular) |

## Regla de negocio

- **Capacidad maxima** = 35% del salario base.
- **Deuda actual** = suma de las cuotas mensuales de los prestamos que el solicitante ya tiene aprobados.
- **Capacidad disponible** = capacidad maxima - deuda actual.
- **Cuota** de la solicitud nueva (sistema frances): `P * i * (1+i)^n / ((1+i)^n - 1)`, con `i` = tasa mensual y `n` = meses.
- `RECHAZADA` si la cuota supera la capacidad disponible; si cabe: `APROBADA`, salvo que el monto supere **5 salarios base**, en cuyo caso
  pasa a `REVISION_MANUAL` (un asesor decide). Cuota igual a la capacidad disponible se aprueba; exactamente 5 salarios tambien.
- Solo las aprobaciones llevan **plan de pago** (cuota, capital, interes y saldo de cada mes; la ultima cuota ajusta los centavos de
  redondeo para que el saldo termine en cero).

## Contratos (version 1, JSON snake_case, lector tolerante)

Pedido (lo publica SOLICITUDES; el endpoint de consulta recibe el mismo cuerpo, y `version`/`tipo` son opcionales):

```json
{ "version": 1, "id_evento": "uuid", "tipo": "VALIDACION_AUTOMATICA_SOLICITADA", "ocurrido_en": "2026-10-15T10:00:00Z",
  "solicitud": { "id_solicitud": "uuid", "monto": 5000000.00, "plazo": 24, "tasa_interes": 0.018, "tipo_prestamo": "PERSONAL" },
  "solicitante": { "email": "maria@example.com", "nombre": "Maria Gomez", "salario_base": 4500000 },
  "prestamos_aprobados": [ { "monto": 2000000.00, "plazo": 12, "tasa_interes": 0.022 } ] }
```

Decision (la lee SOLICITUDES):

```json
{ "version": 1, "id_evento": "uuid", "tipo": "DECISION_AUTOMATICA", "id_solicitud": "uuid", "decision": "APROBADA",
  "destinatario": { "email": "maria@example.com", "nombre": "Maria Gomez" },
  "calculo": { "capacidad_maxima": 1575000.00, "deuda_actual": 191449.77, "capacidad_disponible": 1383550.23, "cuota_mensual": 258404.18 },
  "plan_de_pago": [ { "numero": 1, "cuota": 258404.18, "capital": 168404.18, "interes": 90000.00, "saldo": 4831595.82 } ] }
```

Respuesta de la API: `{ "decision": ..., "capacidad_maxima": ..., "deuda_actual": ..., "capacidad_disponible": ..., "cuota_mensual": ..., "plan_de_pago": [...] }`;
`400 {"codigo":"PEDIDO_INVALIDO","mensaje":...}` si el pedido no cumple el contrato; `500` generico ante un error inesperado.

## Comportamiento ante fallos

- Fallos parciales del lote (`ReportBatchItemFailures`): solo los pedidos que fallaron vuelven a la cola y, tras agotar los reintentos,
  a su cola de mensajes fallidos. La asociacion con la cola debe declarar `FunctionResponseTypes=ReportBatchItemFailures`.
- Reintentar es seguro: el calculo es determinista y SOLICITUDES ignora una decision para una solicitud que ya no esta pendiente.
- Los logs registran el id del mensaje y el tipo de error, **nunca el contenido** (datos personales y financieros).

## Configuracion

| Variable | Descripcion |
|----------|-------------|
| `COLA_DECISIONES_URL` | URL de la cola donde se publica la decision (obligatoria para el handler de SQS; la API no la necesita) |
| `AWS_REGION`, credenciales | Las aporta Lambda (rol de ejecucion) |
| `AWS_ENDPOINT_URL` | Solo para pruebas locales (LocalStack): el SDK la respeta |

Permisos del rol: lectura de la cola de validacion (`AWSLambdaSQSQueueExecutionRole`) y `sqs:SendMessage` sobre la cola de decisiones.

## Compilar, probar y desplegar

```bash
./gradlew check        # pruebas + arquitectura + cobertura minima 90% en dominio y aplicacion
./gradlew shadowJar    # build/libs/lambda-capacidad-endeudamiento.jar
```

Runtime `java21`; memoria sugerida 512 MB; timeout 30 s (menor que el *visibility timeout* de la cola). Los tests de LocalStack usan la imagen
`localstack/localstack:4.9.2` (las posteriores exigen token de cuenta).

## Arquitectura

`domain` (modelo, `EvaluadorDeCapacidad`, `Amortizacion`, puerto `PublicadorDeDecisiones`) -> `application` (casos de uso) ->
`infrastructure` (JSON, SQS, handlers). El dominio no conoce AWS, Jackson ni logs: lo verifica ArchUnit en `ArquitecturaTest`.
La formula de la cuota tambien existe en SOLICITUDES (`Amortizacion`, para el listado de HU4): si cambia una, cambia la otra.
