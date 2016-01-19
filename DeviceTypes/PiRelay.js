/**
 *  Pi Relay Control
 *
 *  Copyright 2016 Tom Beech
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
 
 preferences {
	input("ip", "text", title: "IP", description: "The ip of your raspberry pi i.e. 192.168.1.110")
	input("port", "text", title: "Port", description: "The port your WebIOPi service is running on. The default is 8000", default: "8000")
	input("gpio", "text", title: "GPIO#", description: "The GPIO pin your relay is connected to")
} 
 
metadata {
	definition (name: "Pi Relay Control", namespace: "ibeech.pirelaycontrol", author: "Tom Beech") {

		capability "Switch"
        capability "Refresh"
		capability "Polling"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {    
        
		 standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
		}
                
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state("default", label:'refresh', action:"polling.poll", icon:"st.secondary.refresh-icon")
		}

		main "switch"
		details (["switch", "direction", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	//log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
    log.info "Return data: " + msg.header
    
    def response = msg.body;
    
    // The GPIO direction was set
    if(response == "IN" || response == "OUT"){
    	log.debug "GPIO position response";
        data.pinDirection = response;
        log.debug "GPIO $settings.gpio direction is " + data.pinDirection
        
        // Now that we have ensured the GPIO direction is set correctly, update its state
        data.pinDirectionSet = true;
    }
    
    // We need to update the UI with the state
    if(response == "1" || response == "0"){
    	log.debug "GPIO state response"
		setUI(response)
    }
    
}

def poll() {
	log.debug "Executing 'poll'"   
        
    storeNetworkDeviceId()
	//if(data.pinDirectionSet == null){
	//	setPinFunction()
    //} else {
    	updateGpioState()
    //}
}

def refresh() {
	log.debug "Executing 'refresh'"
    
	poll();
}

def on() {
	log.debug "Executing 'on'"	     
    
    setDeviceState('on')   
}

def off() {
	log.debug "Executing 'off'"
	    
    setDeviceState('off')    
}

def setDeviceState(state) {
	log.debug "Executing 'setDeviceState'"
      
    
    def Path = "/GPIO/$settings.gpio/value/";
	Path += (state == "on") ? "1" : "0";
    
    executeRequest(Path, "POST", false);
}

def setUI(response){
	   
    log.debug "Relay current state: " + response       
    
    def switchState = response == "1" ? "on" : "off";
    log.debug "New state is: " + switchState;
    
    sendEvent(name: "switch", value: switchState);     
}

def updateGpioState(){
	
    executeRequest("/GPIO/$settings.gpio/value", "GET", false);   
}

def setPinFunction(){
	
    executeRequest("/GPIO/$settings.gpio/function/OUT", "POST", true);
}

def executeRequest(Path, method, overridePinDirectionCheck) {
		
	log.debug "The " + method + " path is: " + Path;
	
	storeNetworkDeviceId()
    
    def headers = [:] 
    headers.put("HOST", "$settings.ip:$settings.port")
    
    try {    	
        
        def setDirection = new physicalgraph.device.HubAction(
            method: "POST",
            path: "/GPIO/$settings.gpio/function/OUT",
            headers: headers)
        
        def actualAction = new physicalgraph.device.HubAction(
            method: method,
            path: Path,
            headers: headers)
        
        def hubAction = [setDirection, delayAction(100), actualAction]
        
   		return hubAction
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

private delayAction(long time) {
	log.debug "Delaying by: " + time
    
	new physicalgraph.device.HubAction("delay $time")
}

private storeNetworkDeviceId(){
    def iphex = convertIPtoHex(settings.ip).toUpperCase()
    def porthex = convertPortToHex(settings.port)
    device.deviceNetworkId = "$iphex:$porthex" 
    
    //log.debug device.deviceNetworkId
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
