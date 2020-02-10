def version() {'v0.1.5'}

metadata {
    definition (name: 'OW-Server 1-Wire - Child - Humidity',
                namespace: 'ckamps', 
                author: 'Christopher Kampmeier',
                importUrl: 'https://raw.githubusercontent.com/ckamps/hubitat-drivers-ow-server/master/ow-server-child-humidity.groovy') {
        
        capability 'RelativeHumidityMeasurement'

        capability 'Refresh'
    }

    preferences {
        input name: 'offset',    type: 'decimal', title: 'Humidity Offset',   description: '-n, +n or n to adjust sensor reading', range:'*..*'
        input name: 'logEnable', type: 'bool',    title: 'Enable debug logging', defaultValue: false
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

def poll() {
    refresh()   
}

def refresh() {
    sensorId = device.deviceNetworkId
    // Since AAG TAI-8540 sensors can have the same 1-Wire ID for both humidity and temp, by convention, we appended
    // a trailing ".1" to the 1-Wire ID when we registered the humidity device.
    sensorId = device.deviceNetworkId.substring(0, device.deviceNetworkId.length() - 2)

    if (logEnable) log.debug("Getting humidity for sensor: ${sensorId}")

    try {
        humidity = getHumidity(sensorId)
    }
    catch (Exception e) {
        log.warn("Can't obtain humidity for sensor ${sensorId}: ${e}")
        return
    }

    if (logEnable) log.debug("Humidity: ${humidity}")
    humidity = offset ? (humidity + offset) : humidity
    humidity = humidity.round(1)

    sendEvent(
        name: 'humidity',
        value: humidity,
        unit: "%RH",
        descriptionText: "Humidity is ${humidity}%",
    )
  
    def nowDay = new Date().format('MMM dd', location.timeZone)
    def nowTime = new Date().format('h:mm a', location.timeZone)

    sendEvent(
        name: 'lastUpdated',
        value: nowDay + " at " + nowTime,
        displayed: false
    )
}

private float getHumidity(sensorId) {
    def uri = "http://${parent.getOwServerAddress()}/details.xml"

    response = parent.doHttpGet(uri)

    if (!response) throw new Exception("doHttpGet to get humidity returned empty response ${sensorId}")

    element = response.'**'.find{ it.ROMId == sensorId }
    
    if (!element) throw new Exception("Can't find matching ROMId element in response from OW-Server for sensor ${sensorId}")

    if (!element.Humidity) throw new Exception("Humidity element does not exist in response from OW-Server for sensor ${sensorId}")

    return(element.Humidity.toFloat())
}