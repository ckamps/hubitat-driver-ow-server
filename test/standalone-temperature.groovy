#!/usr/local/bin/groovy

@Grab(group='org.ccil.cowan.tagsoup', module='tagsoup', version='1.2')
import groovy.util.XmlSlurper
import groovy.xml.*

def parser = new XmlSlurper(new org.ccil.cowan.tagsoup.Parser())

def sensorId = 'BA0008014D226310'

def get = new URL('http://192.168.2.241/details.xml').openConnection()

def message = "Address_Array=${sensorId}"
get.setRequestMethod('POST')
get.setDoOutput(true)
get.setRequestProperty('Content-Type', 'text/xml')
get.getOutputStream().write(message.getBytes('UTF-8'))
def getRC = get.getResponseCode()
if(getRC.equals(200)) {sensorId
    resultText = get.getInputStream().getText()
}

println(resultText)

document = parser.parseText(resultText)
println(document.dump())

document.owd_DS18S20.each{
    sensor->
    print("Description:")
    println "${sensor['@description']}"

    print("Name:")
    println "${sensor.Name.text()}"
			
    print("Family:")
    println "${sensor.Family[0].text()}"
			
    print("ROMId:")
    println "${sensor.ROMId[0].text()}"

    print("Temperature:")
    println "${sensor.Temperature[0].text()}"
}

//def sensorElements = []
//sensorElements = document.'**'.findAll{ it.owd_DS18S20.@Description.text().startsWith('P') }
//println("number of sensor elements found: ${sensorElements.size()}")

element = document.'**'.find{ it.ROMId == sensorId }
println(element)
temp_c = element.Temperature.toFloat().round(1)
println("Temp: ${temp_c} C")
temp_f = ((9.0/5.0)*temp_c + 32).round(1)
println("Temp: ${temp_f} F")