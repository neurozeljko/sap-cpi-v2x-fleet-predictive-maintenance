package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    String vin = message.getProperty("Prop_Chassis_VIN")
    String correlationId = message.getProperty("Prop_Correlation_ID") ?: UUID.randomUUID().toString()
    String faultCategory = message.getProperty("Prop_Fault_Category")
    String targetPart = message.getProperty("Prop_Target_Part_Number")
    String riskFactor = message.getProperty("Prop_Thermal_Runaway_Risk")

    def cloudEventPayload = [
        specversion: "1.0",
        type: "com.sap.evfleet.predictivemaintenance.criticalalert.v1",
        source: "/edge/cluster/5g/berlin-01/telemetry",
        id: "EV-EVT-" + UUID.randomUUID().toString(),
        time: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        datacontenttype: "application/json",
        correlationId: correlationId,
        data: [
            chassisVIN: vin,
            fleetOperator: "GLOBAL_EV_FLEET_OPERATIONS",
            telemetryDiagnosis: [
                failureMode: faultCategory,
                predictedFailureWindowHours: 48,
                thermalRunawayProbability: riskFactor,
                recommendedSparePart: targetPart
            ],
            s4HanaPlantMaintenanceTrigger: [
                maintenanceOrderType: "PM02",
                priority: "1",
                workCenter: "EV_MAINT_BERLIN",
                shortText: "Kritična anomalija baterije: Prediktivna zamena modula za ${vin}"
            ],
            aribaReplenishmentTrigger: [
                autoSourcingEnabled: true,
                materialGroup: "EV_PROPULSION_BATTERIES",
                urgency: "CRITICAL_AOG"
            ]
        ]
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(cloudEventPayload)))
    message.setHeader("Content-Type", "application/cloudevents+json")
    message.setHeader("X-AEM-Destination-Topic", "v2x/fleet/telemetry/anomaly/v1")
    return message
}
