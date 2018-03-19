/**
 *  BMW ConnectedDrive Car
 *
 *  Copyright 2018 Tom Beech
 *
 */
 
metadata {
	definition (name: "BMW ConnectedDrive Car", namespace: "ibeech", author: "ibeech") {
		capability "Lock"
        capability "Image Capture"
        capability "Refresh"
        capability "Polling"
        capability "Thermostat Fan Mode"
        capability "Light"
        
        command "startVentalation"
        command "flashLights"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        
    	tiles {

            carouselTile("carImage", "device.image", action: "Image Capture.take", width: 3, height: 2) { }
			     
           	valueTile("carDetails", "device.carDetails", width: 3, height: 2) {
        		  state "val", label:'${currentValue}', defaultState: true
    		}
            
            valueTile("carService", "device.carService", width: 6, height: 2) {
        		  state "val", label:'${currentValue}', defaultState: true
    		}
            
            standardTile("lockCar", "lock", width: 2, height: 2) {
				state "default", label:'Lock', action:"lock", icon:"st.Home.home3", backgroundColor:"#79b821"
			}
            
            standardTile("unlockCar", "unlock", width: 2, height: 2) {
				state "default", label:'Unlock', action:"unlock", icon:"st.Home.home3", backgroundColor:"#79b821"
			}
            
            standardTile("flashLightButton", "flashLights", width: 2, height: 2) {
				state "default", label:'Flash', action:"flashLights", icon:"st.Lighting.light11", backgroundColor:"#79b821"
			}      
            
            standardTile("startVentalationButton", "startVentalation", width: 2, height: 2) {
				state "default", label:'Ventilate', action:"startVentalation", icon:"st.Home.home1", backgroundColor:"#79b821"
			}            
                            
        	standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
				state("default", label:'refresh', action:"refresh", icon:"st.secondary.refresh-icon")
			}

            main "startVentalationButton"
            details([ "carImage", "carDetails", "carService", "lockCar", "unlockCar", "flashLightButton", "startVentalationButton", "refresh"])
        }        
    }
}

def refresh() {

	def serviceInfo = parent.getCarServicenfo(device.deviceNetworkId)
        
    def string = device.displayName;
    string += "\r\rMiles: ${serviceInfo.attributesMap.mileage} ${serviceInfo.attributesMap.unitOfLength}"    
	sendEvent(name: "carDetails", value: string);    
    
    string = "Location: ${getCarLocation(serviceInfo.attributesMap.gps_lat, serviceInfo.attributesMap.gps_lng)}"
    serviceInfo.vehicleMessages.cbsMessages.each{msg -> 
        def info1 = formatServiceInfo(msg);
        if(info1 != "") string += "\r\n${info1}";
    }
    sendEvent(name: "carService", value: string);    
    
    captureCarImage()
}

def getCarLocation(lat, lon) {

	def params = [
		uri: "https://maps.googleapis.com/maps/api/geocode/json?latlng=${lat},${lon}&key=AIzaSyD-nz_0qxBlvGj0WZjpbHfnA-rQG4anMtM"
	]	
    log.debug params
    
    httpGet(params) {response ->    
        log.debug "${response.data}"
        
		return response.data.results.formatted_address[0]
	}
}

def formatServiceInfo(msg){
	              
	if(msg == null) return "";
    
    if(msg.unitOfLengthRemaining != "") {
    	return "${msg.text}: ${msg.status}. Due ${msg.date} or ${msg.unitOfLengthRemaining}"
        } else {
        	return "${msg.text}: ${msg.status}. Due ${msg.date}"
    }
}

def poll() {
	refresh()
}

def lock() {
	log.debug "Locking"
    parent.lockDoors(device.deviceNetworkId)
}

def unlock() {
	log.debug "Unlocking"
    parent.unlockDoors(device.deviceNetworkId)
}

def startVentalation() {
	log.debug "Starting Ventilation"
    parent.ventilate(device.deviceNetworkId)
}

def fanOn() {
	log.debug "fanOn hit"
	startVentalation();
}
def fanCirculate() {
	log.debug "fanCirculate hit"
	startVentalation();
}
def fanAuto() {
	log.debug "fanAuto hit"
	startVentalation();
}

def flashLights() {
	log.debug "Flashing lights ${device.deviceNetworkId}"
    parent.flashLights(device.deviceNetworkId)
}
def on() {
	log.debug "Light.on hit"
    flashLights()    
}
def off() {
	log.debug "Light.off hit. Unable to turn lights off as they only flash"
}

def getImageName() {
    return java.util.UUID.randomUUID().toString().replaceAll('-','')
}

def captureCarImage() {
    if(state.PicTaken) {
    	// Pic was already taken, all done.    
    } else {
        log.debug "Aquring image of car"
        def params = [
            uri: parent.getCarImageUrl(device.deviceNetworkId)
        ]

        try {
            httpGet(params) { response ->
                // we expect a content type of "image/jpeg" from the third party in this case
                if (response.status == 200 && response.headers.'Content-Type'.contains("image/png")) {
                    def imageBytes = response.data
                    if (imageBytes) {
                        def name = getImageName()
                        try {
                            storeImage(name, imageBytes)
                            state.PicTaken = true
                        } catch (e) {
                            log.error "Error storing image ${name}: ${e}"
                        }

                    }
                } else {
                    log.error "Image response not successful or not a jpeg response"
                }
            }
        } catch (err) {
            log.debug "Error making request: $err"
        }
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Virtual siwtch parsing '${description}'"
}
