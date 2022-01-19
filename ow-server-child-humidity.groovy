def version() {'v0.2.0'}

metadata {
    definition (name: 'OW-Server 1-Wire - Child - Humidity',
                namespace: 'ckamps', 
                author: 'Christopher Kampmeier',
                importUrl: 'https://raw.githubusercontent.com/ckamps/hubitat-drivers-ow-server/master/ow-server-child-humidity.groovy') {
        
        capability 'RelativeHumidityMeasurement'
        capability 'TemperatureMeasurement'
        capability 'Refresh'
    }

    preferences {
        input name: 'humidityOffset',    type: 'decimal', title: 'Humidity Offset',   description: '-n, +n or n to adjust sensor reading', range:'*..*'
        input name: 'tempOffset',    type: 'decimal', title: 'Temperature Offset',   description: '-n, +n or n to adjust sensor reading', range:'*..*'
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

    if (logEnable) log.debug("Getting humidity and temperature for sensor: ${sensorId}")

    try {
        humidity = getHumidity(sensorId)
    }
    catch (Exception e) {
        log.warn("Can't obtain humidity for sensor ${sensorId}: ${e}")
        return
    }

    if (logEnable) log.debug("Humidity: ${humidity}")
    humidity = humidityOffset ? (humidity + humidityOffset) : humidity
    humidity = humidity.round(1)

    sendEvent(
        name: 'humidity',
        value: humidity,
        unit: "%RH",
        descriptionText: "Humidity is ${humidity}%",
    )

    try {
        temp = getTemperature(sensorId)
    }
    catch (Exception e) {
        log.warn("Can't obtain temperature for sensor ${sensorId}: ${e}")
        return
    }
    
    if (logEnable) log.debug("Temperature - C: ${temp}")
    temp = (location.temperatureScale == "F") ? ((temp * 1.8) + 32) : temp
    temp = tempOffset ? (temp + tempOffset) : temp
    temp = temp.round(2)

    sendEvent(
        name: 'temperature',
        value: temp,
        unit: "°${location.temperatureScale}",
        descriptionText: "Temperature is ${temp}°${location.temperatureScale}",
        translatable: true
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

private float getTemperature(sensorId) {
    def uri = "http://${parent.getOwServerAddress()}/details.xml"

    response = parent.doHttpGet(uri)

    if (!response) throw new Exception("doHttpGet to get temperature returned empty response ${sensorId}")

    element = response.'**'.find{ it.ROMId == sensorId }
    
    if (!element) throw new Exception("Can't find matching ROMId element in response from OW-Server for sensor ${sensorId}")

    if (!element.Temperature) throw new Exception("Temperature element does not exist in response from OW-Server for sensor ${sensorId}")

    return(element.Temperature.toFloat())
}