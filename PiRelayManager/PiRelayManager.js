/**
 *  Pi Relay Manager
 *
 *  Copyright 2016 iBeech
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Pi Relay Manager",
    namespace: "ibeech",
    author: "ibeech",
    description: "Add each Pi Relay as an individual thing.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

  section("Raspberry Pi Setup"){
  	input "piIP", "text", "title": "Raspberry Pi IP", multiple: false, required: true
    input "piPort", "text", "title": "Raspberry Pi Port", multiple: false, required: true
    input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }
  
    section("GPIO Setup") {
	input "gpioName1", "text", title: "Relay 1 Name", required:true
    input "gpio1", "number", title: "Relay 1 GPIO #", required:true
	input "gpioName2", "text", title: "Relay 2 Name", required:false
    input "gpio2", "number", title: "Relay 2 GPIO #", required:false
	input "gpioName3", "text", title: "Relay 3 Name", required:false
    input "gpio3", "number", title: "Relay 3 GPIO #", required:false
	input "gpioName4", "text", title: "Relay 4 Name", required:false
    input "gpio4", "number", title: "Relay 4 GPIO #", required:false
	input "gpioName5", "text", title: "Relay 5 Name", required:false
    input "gpio5", "number", title: "Relay 5 GPIO #", required:false
	input "gpioName6", "text", title: "Relay 6 Name", required:false
    input "gpio6", "number", title: "Relay 6 GPIO #", required:false
	input "gpioName7", "text", title: "Relay 7 Name", required:false
    input "gpio7", "number", title: "Relay 7 GPIO #", required:false
	input "gpioName8", "text", title: "Relay 8 Name", required:false
    input "gpio8", "number", title: "Relay 8 GPIO #", required:false
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
       
    unsubscribe()
    uninstalled()
    initialize();
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])    
   
	setupVirtualRelay(gpioName1, gpio1);
	setupVirtualRelay(gpioName2, gpio2);
 	setupVirtualRelay(gpioName3, gpio3);
	setupVirtualRelay(gpioName4, gpio4);
	setupVirtualRelay(gpioName5, gpio5);
	setupVirtualRelay(gpioName6, gpio6);
	setupVirtualRelay(gpioName7, gpio7);
	setupVirtualRelay(gpioName8, gpio8);
 
}

def setupVirtualRelay(gpioName, gpio){

	if(gpio){
	    log.debug "Create a Virtual Pi Relay named $gpioName"
	    def d = addChildDevice("ibeech", "Virtual Pi Relay", "piRelay." + gpio, theHub.id, [label:gpioName, name:gpioName])
	    subscribe(d, "switch", switchChange)
	    log.debug "Setting initial state of $gpioName to off"
        setDeviceState(gpio, "off");
	    d.off();
        log.debug "Virtual Pi Relay $gpioName created"
	}
}

def uninstalled() {
  unsubscribe()
  def delete = getChildDevices()
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }   
}

def response(evt){
	log.debug evt
    log.debug evt.value
}

def switchChange(evt){

	log.debug "Switch event!";
    
    if(evt.value == "on" || evt.value == "off") return;
    
	log.debug evt.value;
    
    def parts = evt.value.tokenize('.');
    def GPIO = parts[0];
    def state = parts[1];
    
    setDeviceState(GPIO, state);
    
    def children = getChildDevices()
  	def theSwitch = children.find{ d -> d.deviceNetworkId == "piRelay." + GPIO }  
    log.debug "Got switch $theSwitch"
    
    switch(theSwitch){    
    	case "on":
        	theSwitch.on();
            break;
            
        case "off":
        	theSwitch.off();
            break;
    }
	
}

def setDeviceState(gpio, state) {
	log.debug "Executing 'setDeviceState'"
     
    // Determine the path to post which will set the switch to the desired state
    def Path = "/GPIO/" + gpio + "/value/";
	Path += (state == "on") ? "1" : "0";
    
    executeRequest(Path, "POST", true, gpio);
}

def executeRequest(Path, method, setGPIODirection, gpioPin) {
		   
	log.debug "The " + method + " path is: " + Path;
	    
    def headers = [:] 
    headers.put("HOST", "$settings.piIP:$settings.piPort")
    
    try {    	
        
        if(setGPIODirection) {
        	def setDirection = new physicalgraph.device.HubAction(
            	method: "POST",
            	path: "/GPIO/" + gpioPin  + "/function/OUT",
            	headers: headers)
            
        	sendHubCommand(setDirection);
        }
        
        def actualAction = new physicalgraph.device.HubAction(
            method: method,
            path: Path,
            headers: headers)
        
        sendHubCommand(actualAction)        
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

/* Helper functions to get the network device ID */
private String NetworkDeviceId(){
    def iphex = convertIPtoHex(settings.piIP).toUpperCase()
    def porthex = convertPortToHex(settings.piPort)
    return "$iphex:$porthex" 
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    //log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    //log.debug hexport
    return hexport
}
 
