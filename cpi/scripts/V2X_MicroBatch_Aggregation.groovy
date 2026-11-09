package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

Message processData(Message message) {
    def reader = message.getBody(java.io.Reader)
    def jsonSlurper = new JsonSlurper()
    def payload = jsonSlurper.parse(reader)

    String vin = payload.vehicleChassisVIN ?: "VIN-UNKNOWN-AUTONOMOUS-00"
    def telemetryTicks = payload.highFrequencySensors ?: []

    if (telemetryTicks.isEmpty()) {
        throw new IllegalArgumentException("V2X_PAYLOAD_INVALID: High frequency sensor ticks list is empty.")
    }

    double totalRotorTemp = 0.0
    double maxCellTempDelta = 0.0
    double maxVibrationHarmonic = 0.0
    int tickCount = telemetryTicks.size()

    telemetryTicks.each { tick ->
        totalRotorTemp += (tick.rotorTemperatureCelsius ?: 0.0) as Double
        double cellDelta = (tick.batteryCellDeltaTemp ?: 0.0) as Double
        if (cellDelta > maxCellTempDelta) {
            maxCellTempDelta = cellDelta
        }
        double vibration = (tick.statorVibrationGForce ?: 0.0) as Double
        if (vibration > maxVibrationHarmonic) {
            maxVibrationHarmonic = vibration
        }
    }

    double avgRotorTemp = totalRotorTemp / tickCount

    message.setProperty("Prop_Chassis_VIN", vin)
    message.setProperty("Prop_Avg_Rotor_Temp", avgRotorTemp)
    message.setProperty("Prop_Max_Cell_Delta", maxCellTempDelta)
    message.setProperty("Prop_Max_Vibration", maxVibrationHarmonic)
    message.setProperty("Prop_Batch_Ticks_Count", tickCount)

    def aggregatedData = [
        vin: vin,
        edgeAggregatedMetrics: [
            sampleCount: tickCount,
            meanRotorTemperatureC: avgRotorTemp.round(2),
            peakCellDeltaTempC: maxCellTempDelta.round(2),
            peakVibrationHarmonicG: maxVibrationHarmonic.round(3),
            calculatedAt: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        ]
    ]

    message.setBody(JsonOutput.toJson(aggregatedData))
    return message
}
