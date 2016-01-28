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
	definition (name: "Plex Home Theatre", namespace: "ibeech", author: "ibeech") {
    	capability "Switch"
		capability "Music Player"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {    
        
		standardTile("main", "device.status", width: 1, height: 1, canChangeIcon: true) {
			state "playing", label:'Playing', action:"music Player.pause", icon:"st.Electronics.electronics16", nextState:"paused", backgroundColor:"#79b821"
			state "stopped", label:'Stopped', action:"music Player.play", icon:"st.Electronics.electronics16", backgroundColor:"#ffffff"
			state "paused", label:'Paused', action:"music Player.play", icon:"st.Electronics.electronics16", nextState:"playing", backgroundColor:"#FFA500"
        }
        
        standardTile("stop", "device.status", width: 1, height: 1, decoration: "flat") {
			state "default", label:'', action:"music Player.stop", icon:"st.sonos.stop-btn", backgroundColor:"#ffffff"
			state "grouped", label:'', action:"music Player.stop", icon:"st.sonos.stop-btn", backgroundColor:"#ffffff"
		}

		main "main"
		details (["main", "stop"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Virtual siwtch parsing '${description}'"
}

def play() {
	log.debug "Executing 'on'"	     
    
    sendEvent(name: "switch", value: device.deviceNetworkId + ".play");    
    sendEvent(name: "switch", value: "on");    
    sendEvent(name: "status", value: "playing");
}

def pause() {
	log.debug "Executing 'pause'"
	    
	sendEvent(name: "switch", value: device.deviceNetworkId + ".pause");     
    sendEvent(name: "switch", value: "off");
    sendEvent(name: "status", value: "paused");
}

def stop() {
	log.debug "Executing 'off'"
	    
	sendEvent(name: "switch", value: device.deviceNetworkId + ".stop");     
    sendEvent(name: "switch", value: "off");
    sendEvent(name: "status", value: "stopped");
}

def setPlaybackState(state) {

    switch(state) {
        case "stopped":
        sendEvent(name: "switch", value: "off");
        sendEvent(name: "status", value: "stopped");
        break;

        case "playing":
        sendEvent(name: "switch", value: "on");
        sendEvent(name: "status", value: "playing");
        break;

        case "paused":
        sendEvent(name: "switch", value: "off");
        sendEvent(name: "status", value: "paused");
    }
}
