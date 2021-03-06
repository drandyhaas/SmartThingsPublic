/**
 *  HaasSomfyBlind
 *  From: https://github.com/NOTashwin/SmartThings/blob/master/somfy_shades.groovy
 *  Copyright 2015 Andrew Haas
 */
metadata {
	definition (name: "HaasSomfyBlind", namespace: "drandyhaas", author: "Andrew Haas") {
        capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"        
        fingerprint deviceId: "0x1105", inClusters: "0x2C, 0x72, 0x26, 0x20, 0x25, 0x2B, 0x86"           
    }

    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'open', action:"switch.off", icon:"st.doors.garage.garage-open", backgroundColor:"#ffdf3f"
            state "off", label:'closed', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#194775"
        }
        standardTile("on", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "on", label:'up', action:"switch.on", icon:"st.doors.garage.garage-opening"
        }
        standardTile("off", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "off", label:'down', action:"switch.off", icon:"st.doors.garage.garage-closing"
        }
        standardTile("stop", "device.level", inactiveLabel: false, decoration: "flat") {
            state "default", label:'stop', action:"switch level.setLevel", icon:"st.Transportation.transportation13"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main(["switch"])
        details(["switch", "on", "off", "stop", "refresh"])
    }
}

def parse(String description) {
    description
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd)
{
    def result
    if (cmd.value == 0) {
        result = createEvent(name: "switch", value: "off")
    } else {
        result = createEvent(name: "switch", value: "on")
    }
    return result
}

def on() {
    delayBetween([
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format(),
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format(),
        sendEvent(name: "switch", value: on)
    ], 5000)
}

def off() {
    delayBetween([
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format(),
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format(),
        sendEvent(name: "switch", value: off)
    ], 5000)
}

def setLevel() {
    delayBetween([
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),
        sendEvent(name: "switch", value: on)
    ], 5000)
}

def refresh() {
    zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def poll() {
    // zwave.switchMultilevelV1.switchMultilevelGet().format()
}