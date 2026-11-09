package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    def exception = message.getProperty("CamelExceptionCaught")
    String vin = message.getProperty("Prop_Chassis_VIN") ?: "UNKNOWN_VIN"
    String edgeCell = message.getProperty("Prop_Edge_Cell_ID") ?: "UNKNOWN_EDGE_CELL"
    String correlationId = message.getHeader("SAP_MplCorrelationId", String) ?: UUID.randomUUID().toString()
    String errorMsg = exception ? exception.getMessage() : "Telemetry stream aggregation error."

    def deadLetterPayload = [
        edgeTelemetryDeadLetterQueue: [
            incidentTrackingId: correlationId,
            edgeClusterOrigin: edgeCell,
            chassisVIN: vin,
            failureReason: errorMsg,
            edgeBufferPolicy: "RETAIN_IN_K8S_RING_BUFFER_12H",
            timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(deadLetterPayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("X-V2X-Status", "QUARANTINED")
    return message
}
