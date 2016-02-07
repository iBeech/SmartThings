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
 
metadata {
	definition (name: "Virtual Pi Relay", namespace: "ibeech", author: "ibeech") {
		capability "Switch"
        capability "Refresh"
		capability "Polling"
        
        command "changeSwitchState", ["string"]
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
		details (["switch", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Virtual siwtch parsing '${description}'"
}

def poll() {
	log.debug "Executing 'poll'"   
        
        def lastState = device.currentValue("switch")
    	sendEvent(name: "switch", value: device.deviceNetworkId + ".refresh")
        sendEvent(name: "switch", value: lastState);
}

def refresh() {
	log.debug "Executing 'refresh'"
    
	poll();
}

def on() {
	log.debug "Executing 'on'"	     
    
    sendEvent(name: "switch", value: device.deviceNetworkId + ".on");    
    sendEvent(name: "switch", value: "on");    
}

def off() {
	log.debug "Executing 'off'"
	    
	sendEvent(name: "switch", value: device.deviceNetworkId + ".off");     
    sendEvent(name: "switch", value: "off");
}

def changeSwitchState(newState) {

	log.trace "Received update that this switch is now $newState"
	switch(newState) {
    	case 1:
			sendEvent(name: "switch", value: "on")
            break;
    	case 0:
        	sendEvent(name: "switch", value: "off")
            break;
    }
}
