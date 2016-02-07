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
 * 
 * 	=======INSTRUCTIONS======
 	1) For UK go to: https://graph-eu01-euwest1.api.smartthings.com3
	2) For US go to: https://graph.api.smartthings.com1
	3) Click 'My Device Handlers'
	4) Click 'New Device Handler' in the top right
	5) Click the 'From Code' tab
	6) Paste in the code from: https://github.com/iBeech/SmartThings/blob/master/PlexManager/PlexHomeTheatre.groovy
	7) Click 'Create'
	8) Click 'Publish -> For Me'
 * 
 */
 
metadata {
	definition (name: "Plex Home Theatre", namespace: "ibeech", author: "ibeech") {
    	capability "Switch"
		capability "musicPlayer"
        
        command "scanNewClients"
        command "setPlaybackIcon", ["string"]
        command "setPlaybackTitle", ["string"]
        command "setVolumeLevel", ["number"]        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        
        multiAttributeTile(name:"status", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
            attributeState "playing", label:'Playing', action:"music Player.pause", icon:"st.Electronics.electronics16", nextState:"paused", backgroundColor:"#79b821"
            attributeState "stopped", label:'Stopped', action:"music Player.play", icon:"st.Electronics.electronics16", backgroundColor:"#ffffff"
            attributeState "paused", label:'Paused', action:"music Player.play", icon:"st.Electronics.electronics16", nextState:"playing", backgroundColor:"#FFA500"
        }        
            tileAttribute ("device.trackDescription", key: "SECONDARY_CONTROL") {
                attributeState "trackDescription", label:'${currentValue}'
            }

            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action: "setVolumeLevel"
            }
        }
        
        standardTile("next", "device.status", width: 2, height: 2, decoration: "flat") {
        	state "next", label:'', action:"music Player.nextTrack", icon:"st.sonos.next-btn", backgroundColor:"#ffffff"
        }
        
        standardTile("previous", "device.status", width: 2, height: 2, decoration: "flat") {
        	state "previous", label:'', action:"music Player.previousTrack", icon:"st.sonos.previous-btn", backgroundColor:"#ffffff"
        }	
        
        standardTile("fillerTile", "device.status", width: 5, height: 1, decoration: "flat") {
        	state "default", label:'', action:"", icon:"", backgroundColor:"#ffffff"
        }
        
        standardTile("stop", "device.status", width: 2, height: 2, decoration: "flat") {
            state "default", label:'', action:"music Player.stop", icon:"st.sonos.stop-btn", backgroundColor:"#ffffff"
            state "grouped", label:'', action:"music Player.stop", icon:"st.sonos.stop-btn", backgroundColor:"#ffffff"
        }
        
        standardTile("scanNewClients", "device.status", width: 5, height: 1, decoration: "flat") {
            state "default", label:'Scan New Clients', action:"scanNewClients", icon:"state.icon", backgroundColor:"#ffffff"
            state "grouped", label:'', action:"scanNewClients", icon:"state.icon", backgroundColor:"#ffffff"
        }
        
        main "status"
        details (["status", "previous", "stop", "next", "currentSong"])
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
    setPlaybackTitle("Stopped");
}

def previousTrack() {
	log.debug "Executing 'previous': "
    
    setPlaybackTitle("Skipping previous");
    sendCommand("previous");    
}

def nextTrack() {
	log.debug "Executing 'next'"

	setPlaybackTitle("Skipping next");
	sendCommand("next");
}

def scanNewClients() {
	log.debug "Executing 'scanNewClients'"        
    sendCommand("scanNewClients");
}

def setVolumeLevel(level) {
	log.debug "Executing 'setVolumeLevel(" + level + ")'"
    sendEvent(name: "level", value: level);
    sendCommand("setVolume." + level);
}

def sendCommand(command) {
	
    def lastState = device.currentState('switch').getValue();
    sendEvent(name: "switch", value: device.deviceNetworkId + "." + command);
    sendEvent(name: "switch", value: lastState);
}

def setPlaybackState(state) {

	log.debug "Executing 'setPlaybackState'"
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

def setPlaybackTitle(text) {
	log.debug "Executing 'setPlaybackTitle'"
    
    sendEvent(name: "trackDescription", value: text)
}

def setPlaybackIcon(iconUrl) {
	log.debug "Executing 'setPlaybackIcon'"
    
    state.icon = iconUrl;
    
    //sendEvent(name: "scanNewClients", icon: iconUrl)
    //sendEvent(name: "scanNewClients", icon: iconUrl)
    
    log.debug "Icon set to " + state.icon
}
