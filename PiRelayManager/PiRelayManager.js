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
	input "gpioName2", "text", title: "Relay 2 Name", required:false
	input "gpioName3", "text", title: "Relay 3 Name", required:false
	input "gpioName4", "text", title: "Relay 4 Name", required:false
	input "gpioName7", "text", title: "Relay 7 Name", required:false
	input "gpioName8", "text", title: "Relay 8 Name", required:false
	input "gpioName9", "text", title: "Relay 9 Name", required:false
	input "gpioName10", "text", title: "Relay 10 Name", required:false
    	input "gpioName11", "text", title: "Relay 11 Name", required:false
    	input "gpioName17", "text", title: "Relay 17 Name", required:false
    	input "gpioName18", "text", title: "Relay 18 Name", required:false
    	input "gpioName22", "text", title: "Relay 22 Name", required:false
    	input "gpioName23", "text", title: "Relay 23 Name", required:false
    	input "gpioName24", "text", title: "Relay 24 Name", required:false
    	input "gpioName25", "text", title: "Relay 25 Name", required:false
    	input "gpioName27", "text", title: "Relay 27 Name", required:false
  }
}

def installed() {
	
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])    
   
	setupVirtualRelay("2", gpioName2);
	setupVirtualRelay("3", gpioName3);
 	setupVirtualRelay("4", gpioName4);
	setupVirtualRelay("7", gpioName7);
	setupVirtualRelay("8", gpioName8);
	setupVirtualRelay("9", gpioName9);
	setupVirtualRelay("10", gpioName10);
	setupVirtualRelay("11", gpioName11);
	setupVirtualRelay("17", gpioName17);
	setupVirtualRelay("18", gpioName18);
	setupVirtualRelay("22", gpioName22);
	setupVirtualRelay("23", gpioName23);
	setupVirtualRelay("24", gpioName24);
	setupVirtualRelay("25", gpioName25);
	setupVirtualRelay("27", gpioName27);
}

def updated() {
	log.debug "Updated with settings: ${settings}"
            
    	unsubscribe();
    	
	updateVirtualRelay("2", gpioName2);
	updateVirtualRelay("3", gpioName3);
 	updateVirtualRelay("4", gpioName4);
	updateVirtualRelay("7", gpioName7);
	updateVirtualRelay("8", gpioName8);
	updateVirtualRelay("9", gpioName9);
	updateVirtualRelay("10", gpioName10);
	updateVirtualRelay("11", gpioName11);
	updateVirtualRelay("17", gpioName17);
	updateVirtualRelay("18", gpioName18);
	updateVirtualRelay("22", gpioName22);
	updateVirtualRelay("23", gpioName23);
	updateVirtualRelay("24", gpioName24);
	updateVirtualRelay("25", gpioName25);
	updateVirtualRelay("27", gpioName27);
    
    	subscribe(location, null, response, [filterEvents:false])    
}

def updateVirtualRelay(deviceId, gpioName){

	def children = getChildDevices()
    	def theDeviceNetworkId = "piRelay." + settings.piIP;
    
  	def theSwitch = children.find{ d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }  
    
    	if(theSwitch){ // The switch already exists
    
    		if(!gpioName){ // The user has not filled out data for this device
    			log.debug "Found an existing device, but the user has chosen to delete this particular one"
        		deleteChildDevice(theSwitch.deviceNetworkId);
    		} else {
        
    			log.debug "Found existing switch which we will now update"   
        		theSwitch.deviceNetworkId = theDeviceNetworkId + "." + deviceId
        		theSwitch.label = gpioName
        		theSwitch.name = gpioName
        		
            		subscribe(theSwitch, "switch", switchChange)
            		
        		log.debug "Setting initial state of $gpioName to off"
        		setDeviceState(deviceId, "off");
	    		theSwitch.off();
		}
    	} else { // The switch does not exist
    		if(gpioName){ // The user filled in data about this switch
    			log.debug "This switch does not exist, creating a new one now"
        		setupVirtualRelay(deviceId, gpioName);
       		}
    	}
}

def setupVirtualRelay(deviceId, gpioName){

	if(gpioName){
		log.debug "Create a Virtual Pi Relay named $gpioName"
		def d = addChildDevice("ibeech", "Virtual Pi Relay", "piRelay." + settings.piIP + "." + deviceId, theHub.id, [label:gpioName, name:gpioName])
		subscribe(d, "switch", switchChange)
		    
		log.debug "Setting initial state of $gpioName to off"
		setDeviceState(deviceId, "off");
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
	log.debug "w00t"
	log.debug evt
	log.debug evt.value
}

def switchChange(evt){

	log.debug "Switch event!";
    log.debug evt.value;
    if(evt.value == "on" || evt.value == "off") return;    
	
    
    def parts = evt.value.tokenize('.');
    def deviceId = parts[1];
    def GPIO = parts[5];
    def state = parts[6];
    
    log.debug state;
    
    switch(state){
    	case "refresh":
        // Refresh this switches button
        log.debug "Refreshing the state of GPIO " + GPIO
        executeRequest("/*", "GET", false, null)
        return;        
    }
    
    setDeviceState(GPIO, state);
    
    return;
    
	// This code isnt really needed, but is useful to keep around for reference
	def children = getChildDevices()
	def theDeviceNetworkId = "piRelay." + deviceId + "." + GPIO;
	log.debug theDeviceNetworkId;
    
	def theSwitch = children.find{ d -> d.deviceNetworkId == theDeviceNetworkId }  
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
 
