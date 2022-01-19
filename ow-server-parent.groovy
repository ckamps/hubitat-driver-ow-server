def version() {'v0.2.0'}

import groovy.xml.*

metadata {
    definition (name: 'OW-Server 1-Wire - Parent',
                namespace: 'ckamps', 
                author: 'Christopher Kampmeier',
                importUrl: 'https://raw.githubusercontent.com/ckamps/hubitat-drivers-ow-server/master/ow-server-parent.groovy') {
        
        capability 'Refresh'

        command 'createChildren'
        command 'deleteChildren'
        command 'deleteUnmatchedChildren'
        command 'recreateChildren'
        command 'refreshChildren'
    }

    preferences {
        input name: 'address',   type: 'text', title: 'OW-Server Address', description: 'FQDN or IP address', required: true
        input name: 'logEnable', type: 'bool', title: 'Enable debug logging', defaultValue: false
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()   
}

def initialize() {
    state.version = version()
}

def refresh() {
    refreshChildren()
}

def createChildren() {
    if (logEnable) log.debug 'Creating children devices'
    getSensors()
}

def refreshChildren(){
    if (logEnable) log.info 'Refreshing children devices'
    def children = getChildDevices()
    children.each {child->
        child.refresh()
    }
}

def recreateChildren(){
    if (logEnable) log.info 'Recreating children devices'
    // To Do: Based on a new preference, capture the name and label of each child device and reapply those names and labels
    // for all discovered sensors that were previously known.
    deleteChildren()
    createChildren()
}

def deleteChildren() {
    if (logEnable) log.info 'Deleting children devices'
    def children = getChildDevices()
    children.each {child->
        deleteChildDevice(child.deviceNetworkId)
    }
}

def deleteUnmatchedChildren() {
    if (logEnable) log.info 'Deleting unmatched children devices'
    // To Do: Not yet implemnted.
    discoveredSensors = getSensors()
    getChildDevices().each { device ->
        if (logEnable) log.debug('Found an existing child device')
    }
}

private def getSensors() {
    if (logEnable) log.info 'Getting list of sensors known to OW-Server'

    def uri = "http://${address}/details.xml"

    response = doHttpGet(uri)
    
    response.owd_DS18S20.each{ sensor->
        sensorId = sensor.ROMId[0].text()
        if (getChildDevice(sensorId) == null) {
            if (logEnable) log.debug "Discovered DS18S20 temperature sensor: ${sensorId}"
            child = addChildDevice("ckamps", "OW-Server 1-Wire - Child - Temperature", sensorId, [name: sensorId, label: "${sensorId} - DS18S20 Temperature", isComponent: false])
            child.refresh()
        } else {
            if (logEnable) log.debug("Child device already exists for sensor: ${sensorId}")
        }
    }

    response.owd_DS18B20.each{ sensor->
        sensorId = sensor.ROMId[0].text()
        if (getChildDevice(sensorId) == null) {
            if (logEnable) log.debug "Discovered DS18B20 temperature sensor: ${sensorId}"
            child = addChildDevice("ckamps", "OW-Server 1-Wire - Child - Temperature", sensorId, [name: sensorId, label: "${sensorId} - DS18B20 Temperature", isComponent: false])
            child.refresh()
        } else {
            if (logEnable) log.debug("Child device already exists for sensor: ${sensorId}")
        }
    }

    response.owd_DS2438.each{ sensor->
        sensorId = sensor.ROMId[0].text()
        if (getChildDevice(sensorId) == null) {
            // Since it appears that OW Server returns a negative humidity value for DS2438 sensors
            // that don't support humidity readings, use this observed behavior to identify temperature
            // only sensors.
            if (sensor.Humidity.toFloat() < 0) {
              if (logEnable) log.debug "Discovered DS2438 temperature sensor: ${sensorId}"
              child = addChildDevice("ckamps", "OW-Server 1-Wire - Child - Temperature", sensorId, [name: sensorId, label: "${sensorId} - DS2438 Temperature", isComponent: false])
              child.refresh()
            } else {
              if (logEnable) log.debug "Discovered DS2438 temperature + humidity sensor: ${sensorId}"
              child = addChildDevice("ckamps", "OW-Server 1-Wire - Child - Humidity", sensorId, [name: sensorId, label: "${sensorId} - DS2438 Humidity + Temperature" , isComponent: false])
              child.refresh()
            }
        } else {
            if (logEnable) log.debug("Child device already exists for sensor: ${sensorId}")
        }
    }
    response.owd_EDS0065.each{ sensor->
        sensorId = sensor.ROMId[0].text()
        if (getChildDevice(sensorId) == null) {
          if (logEnable) log.debug "Discovered EDS0065 temperature + humidity sensor: ${sensorId}"
          child = addChildDevice("ckamps", "OW-Server 1-Wire - Child - Humidity", sensorId, [name: sensorId, label: "${sensorId} - EDS0065 Humidity + Temperature" , isComponent: false])
          child.refresh()
        } else {
          if (logEnable) log.debug("Child device already exists for sensor: ${sensorId}")
        }
    }
}

def doHttpGet(uri) {
    if (logEnable) log.debug("doHttpGet called: uri: ${uri}")
    def response = []
    int retries = 0
    def cmds = []
    cmds << 'delay 1'

    while(retries++ < 3) {
        try {
            httpGet(uri) { resp ->
                if (resp.success) {
                    response = resp.data
                    if ((logEnable) && (response)) {
                        serializedDocument = XmlUtil.serialize(response)
                        log.debug(serializedDocument.replace('\n', '').replace('\r', ''))
                    }
                } else {
                    throw new Exception("httpGet() not successful for: ${uri}") 
                }
            }
            return(response)
        } catch (Exception e) {
            log.warn "httpGet() of ${uri} to OW-Server failed: ${e.message}"
            // When read time out error occurs, retry the operation. Otherwise, throw
            // an exception.
            if (!e.message.contains('Read timed out')) throw new Exception("httpGet() failed for: ${uri}")
        }
        log.warn('Delaying 1 second before next httpGet() retry')
        cmds
    }
    throw new Exception("httpGet() exceeded max retries for: ${uri}")
}

// To Do: Is there a more direct means for child devices to access parent preferences/settings?

def getOwServerAddress() {
    return(address)   
}