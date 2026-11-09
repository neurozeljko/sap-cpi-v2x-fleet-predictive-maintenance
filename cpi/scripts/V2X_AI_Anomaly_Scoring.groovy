package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

Message processData(Message message) {
    double avgRotorTemp = message.getProperty("Prop_Avg_Rotor_Temp") as Double
    double maxCellDelta = message.getProperty("Prop_Max_Cell_Delta") as Double
    double maxVibration = message.getProperty("Prop_Max_Vibration") as Double
    String vin = message.getProperty("Prop_Chassis_VIN") as String

    double thermalRunawayProbability = (maxCellDelta / 20.0) * 0.6 + (avgRotorTemp / 110.0) * 0.4
    if (thermalRunawayProbability > 1.0) thermalRunawayProbability = 1.0

    boolean isCriticalAnomaly = (maxCellDelta >= 14.5 && avgRotorTemp >= 88.0) || maxVibration >= 4.5

    message.setProperty("Prop_Anomaly_Detected", isCriticalAnomaly.toString())
    message.setProperty("Prop_Thermal_Runaway_Risk", thermalRunawayProbability.round(4).toString())

    if (isCriticalAnomaly) {
        message.setProperty("Prop_Fault_Category", "THERMAL_RUNAWAY_ROTOR_DEGRADATION")
        message.setProperty("Prop_Target_Part_Number", "EV-BAT-CELL-MOD-9941")
        message.setProperty("Prop_Recommended_Action", "IMMEDIATE_S4_MAINTENANCE_ORDER_REQUIRED")
    }

    def inferenceSummary = [
        vin: vin,
        aiCoreModelEvaluation: [
            modelRevision: "EV_BATTERY_LSTM_SURROGATE_V4",
            thermalRunawayProbability: thermalRunawayProbability.round(4),
            anomalyTriggered: isCriticalAnomaly,
            verdict: isCriticalAnomaly ? "PREDICTIVE_FAILURE_IMMINENT" : "FLEET_STABLE",
            evaluatedTimestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.toJson(inferenceSummary))
    return message
}
