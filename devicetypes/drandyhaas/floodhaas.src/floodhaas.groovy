/**
 *  FloodHaas
 *
 *  Copyright 2014 Andrew Haas
 *
 */
metadata {
	definition (name: "FloodHaas", namespace: "drandyhaas", author: "Andrew Haas") {
	
		capability "Water Sensor"
		capability "Configuration"
		capability "Sensor"
		capability "Battery"
        
        attribute "updated", "number"

		fingerprint deviceId: "0xA102", inClusters: "0x86,0x72,0x85,0x84,0x80,0x70,0x9C,0x20,0x71"
	}

	simulator {
		status "dry": "command: 9C02, payload: 00 05 00 00 00"
		status "wet": "command: 9C02, payload: 00 05 FF 00 00"
		for (int i = 0; i <= 100; i += 20) {
			status "battery ${i}%": new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: i).incomingMessage()
		}
	}
	tiles {
		standardTile("water", "device.water", width: 2, height: 2) {
			state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#53a7c0"
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        valueTile("updatedlast", "device.updated", decoration: "flat", inactiveLabel: false) {
			state "default", label:'${currentValue} updated'
		} 
		main ("battery")
		details(["water", "battery", "updatedlast", "configure"])
	}
}

def parse(String description) {
	def result = null
	def parsedZwEvent = zwave.parse(description, [0x9C: 1, 0x71: 1, 0x84: 2, 0x30: 1])
	if (parsedZwEvent) {
		result = zwaveEvent(parsedZwEvent)
	}
	log.debug "Parse '${description}' returned ${result}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd)
{
    //log.debug("woke up")
    if (!device.currentValue("updated")) {
        sendEvent(name:"updated", value: 1)
    }
    else{
        if (device.currentValue("updated") > 1000) sendEvent(name:"updated", value: 1)
    	else sendEvent(name:"updated", value: device.currentValue("updated")+1)
	}
	def result = [createEvent(descriptionText: "${device.displayName} woke update", isStateChange: false)]
	def now = new Date().time
	if (!state.battreq || now - state.battreq > 53*60*60*1000) {
		state.battreq = now
        log.debug "getting battery"
		result << response(zwave.batteryV1.batteryGet())
	} else {
    	log.debug "setting as dry, then going to sleep"
        sendEvent(name:"water", value: "dry")
    	result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd)
{
	def map = [:]
	if (cmd.sensorType == 0x05) {
		map.name = "water"
		map.value = cmd.sensorState ? "wet" : "dry"
		map.descriptionText = "${device.displayName} is ${map.value}"
	} else {
		map.descriptionText = "${device.displayName}: ${cmd}"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	def map = [:]
	map.name = "water"
	map.value = cmd.sensorValue ? "wet" : "dry"
	map.descriptionText = "${device.displayName} is ${map.value}"
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)
{
	def map = [:]
	if (cmd.alarmType == 1 && cmd.alarmLevel == 0xFF) {
		map.name = "battery"
		map.value = 1
		map.unit = "%"
		map.descriptionText = "${device.displayName} has a low battery"
		map.displayed = true
		map
	} else if (cmd.alarmType == 2 && cmd.alarmLevel == 1) {
		map.descriptionText = "${device.displayName} powered up"
		map.displayed = false
		map
	} else {
		map.descriptionText = "${device.displayName}: ${cmd}"
		map.displayed = false
	}
	createEvent(map)
}


def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [:]
	if (cmd.batteryLevel == 0xFF) {
		map.name = "battery"
		map.value = 1
		map.unit = "%"
		map.descriptionText = "${device.displayName} has a low battery"
		map.displayed = true
	} else {
		map.name = "battery"
		map.value = cmd.batteryLevel > 0 ? cmd.batteryLevel.toString() : 1
		map.unit = "%"
		map.displayed = false
	}
	[createEvent(map), response(zwave.wakeUpV1.wakeUpNoMoreInformation())]
}

def zwaveEvent(physicalgraph.zwave.Command cmd)
{
	createEvent(descriptionText: "${device.displayName}: ${cmd}", displayed: false)
}

def configure()
{
    log.debug("config heard")
	if (!device.currentState("battery")) {
		sendEvent(name: "battery", value:100, unit:"%", descriptionText:"(Default battery event)", displayed:false)
	}
	zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: [zwaveHubNodeId]).format()
}