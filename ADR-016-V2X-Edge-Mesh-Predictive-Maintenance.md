# ADR-016: Global Autonomous Fleet Telemetry & Predictive Maintenance (V2X Edge Mesh)

## Status
Accepted

## Context
One million autonomous electric vehicles generating high-frequency V2X sensor telemetry would exhaust core SAP S/4HANA database threads and incur unsustainable cloud egress costs if streamed directly to the ERP core.

## Decision
1. Ingest raw streams at local 5G Kubernetes clusters using SAP Edge Integration Cell (EIC).
2. Execute non-buffering Groovy micro-batch stream aggregation to calculate statistical variance.
3. Perform in-flight anomaly scoring against SAP AI Core battery degradation models.
4. Dispatch verified anomalies as CNCF CloudEvents 1.0 to SAP Advanced Event Mesh over AMQP for asynchronous S/4HANA PM and SAP Ariba spare parts reservation.

## Consequences
- Eliminates 99.8% of redundant telemetry ingress from S/4HANA.
- Enables autonomous spare part procurement prior to vehicle breakdown.
- End-to-end incident turnaround reduced to sub-second window.
