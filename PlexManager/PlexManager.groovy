/**
 *  Plex Manager
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
    name: "Plex Manager",
    namespace: "ibeech",
    author: "ibeech",
    description: "Add and Plex Home Theatre endpoints",
    category: "Safety & Security",
    iconUrl: "http://download.easyicon.net/png/1126483/64/",
    iconX2Url: "http://download.easyicon.net/png/1126483/128/",
    iconX3Url: "http://download.easyicon.net/png/1126483/128/")


preferences {

  section("Plex Media Server"){
  		input "plexServerIP", "text", "title": "Server IP", multiple: false, required: true
    	input "plexUserName", "text", "title": "Plex Username", multiple: false, required: true
    	input "plexPassword", "password", "title": "Plex Password", multiple: false, required: true
    	input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }
}

def installed() {
	
	log.debug "Installed with settings: ${settings}"
	initialize()
    
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])    
   	getAuthenticationToken();
    getClients();   
    state.poll = true;
    regularPolling();
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
    state.authenticationToken = null;
    state.tokenUserName = null;
    state.poll = false;
    
    getAuthenticationToken();
    getClients();
    
    if(!state.poll){
    	state.poll = true;
    	regularPolling();
    } 
}

def uninstalled() {

	state.poll = false;
	unsubscribe();
    
	def delete = getChildDevices()
    unsubscribe();    
	delete.each {
		deleteChildDevice(it.deviceNetworkId)
	}   
}

def response(evt) {	 
    
    def msg = parseLanMessage(evt.description);
    if(msg && msg.body && msg.body.startsWith("<?xml")){
    	
    	def statusrsp = new XmlSlurper().parseText(msg.body)
                
        log.debug "Parsing /clients"
        statusrsp.Server.each { thing ->        	
            log.trace thing.@name.text()
            log.trace thing.@address.text()            

            updatePHT(thing.@name.text(), thing.@address.text())
        }
        
        log.debug "Parsing /status/sessions"
        getChildDevices().each { pht ->
         
         	// Convert the devices full network id to just the IP address of the device
        	def address = deviceNetworkId_ToPHTAddress(pht.deviceNetworkId);
            
            // Look at all the current content playing, and determine if anything is playing on this device
            def currentPlayback = statusrsp.Video.find { d -> d.Player.@address.text() == address }
            
            // If there is no content playing on this device, then the state is stopped
            def playbackState = "stopped";            
            
            // If we found active content on this device, look up its current state (i.e. playing or paused)
            if(currentPlayback) {
                        	
            	playbackState = currentPlayback.Player.@state.text();
            }            
                        
            log.trace "Determined that PHT " + address + " playback state is: " + playbackState
                        
            pht.setPlaybackState(playbackState);
            pht.setPlaybackTitle(currentPlayback.@grandparentTitle.text() + ": " + currentPlayback.@title.text());
            
            def iconUrl = "http://" + settings.plexServerIP + ":32400" + currentPlayback.@thumb + "?X-Plex-Token=" + state.authenticationToken
            log.debug iconUrl;
            pht.setPlaybackIcon(iconUrl);
         }
            
	}
}

def String deviceNetworkId_ToPHTAddress(id) {

	def parts = id.tokenize('.');
	return parts[5] + "." + parts[6] + "." + parts[7] + "." + parts[8];
}

def updatePHT(phtName, phtIP){

	log.info "Updating PHT: " + phtName + " with IP: " + phtIP

	def children = getChildDevices()
    
  	def pht = children.find{ d -> d.deviceNetworkId == childDeviceID(phtIP) }  
    
    if(!pht){ // The PHT does not exist, create it

        log.debug "This PHT does not exist, creating a new one now"
		pht = addChildDevice("ibeech", "Plex Home Theatre", childDeviceID(phtIP), theHub.id, [label:phtName, name:phtName])		
    }
    
    // Renew the subscription
    subscribe(pht, "switch", switchChange)
}

def String childDeviceID(phtIP) {

	return "pht." + settings.plexServerIP + "." + phtIP
}

def switchChange(evt) {
    
    // We are only interested in event data which contains 
    if(evt.value == "on" || evt.value == "off") return;   
    
	log.debug "Plex Home Theatre event received: " + evt.value;
    
    def parts = evt.value.tokenize('.');
    
    // Parse out the PHT IP address from the event data
    def phtIP = deviceNetworkId_ToPHTAddress(evt.value);
    
    // Parse out the new switch state from the event data
    def state = parts[9]
    
    //log.debug "phtIP: " + phtIP
    log.debug "state: " + state
    
    switch(state) {
    	case "next":
        	log.debug "Sending command 'next' to " + phtIP
            next(phtIP);
        break;
        
        case "previous":
        	log.debug "Sending command 'previous' to " + phtIP
            previous(phtIP);
        break;
        
        case "play":
        case "pause":
        case "stop":
            // Toggle the play / pause button for this PHT
    		playpause(phtIP);
        break;
        
        case "scanNewClients":
        	getClients();
            
        case "setVolume":
        	setVolume(phtIP, parts[10]);
        break;
    }
    
    return;
}

def setVolume(phtIP, level) {
	log.debug "Executing 'setVolume'"
	
	executeRequest("/system/players/" + phtIP + "/playback/setParameters?volume=" + level, "GET");
}

def regularPolling() { 

	if(!state.poll) return;
    
    log.debug "Polling for PHT state"
    
    if(state.authenticationToken) {
        updateClientStatus();
    }
    
    runIn(10, regularPolling);
}

def getClients() {

	log.debug "Executing 'getClients'"
    
    executeRequest("/clients", "GET");
}

def updateClientStatus(){
	log.debug "Executing 'updateClientStatus'"
    
	executeRequest("/status/sessions", "GET")
}

def playpause(phtIP) {
	log.debug "Executing 'playpause'"
	
	executeRequest("/system/players/" + phtIP + "/playback/play", "GET");
}

def next(phtIP) {
	log.debug "Executing 'next'"
	
	executeRequest("/system/players/" + phtIP + "/playback/skipNext", "GET");
}

def previous(phtIP) {
	log.debug "Executing 'next'"
	
	executeRequest("/system/players/" + phtIP + "/playback/skipPrevious", "GET");
}

def executeRequest(Path, method) {
		   
	log.debug "The " + method + " path is: " + Path;
     
    // We don't have an authentication token
    if(!state.authenticationToken) {
    	getAuthenticationToken()
    }
	    
	def headers = [:] 
	headers.put("HOST", "$settings.plexServerIP:32400")
    headers.put("X-Plex-Token", state.authenticationToken)
	
	try {    
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

def getAuthenticationToken() {
	      
    log.debug "Getting authentication token for Plex Server " + settings.plexServerIP      

    def params = [
    	uri: "https://plex.tv/users/sign_in.json?user%5Blogin%5D=" + settings.plexUserName + "&user%5Bpassword%5D=" + settings.plexPassword,
        headers: [
            'X-Plex-Client-Identifier': 'Plex',
			'X-Plex-Product': 'Device',
			'X-Plex-Version': '1.0'
        ]
   	]
    
	try {    
		httpPostJson(params) { resp ->
        	state.tokenUserName = settings.plexUserName            
        	state.authenticationToken = resp.data.user.authentication_token;
        	log.debug "Token is: " + state.authenticationToken
        }
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $params"
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
