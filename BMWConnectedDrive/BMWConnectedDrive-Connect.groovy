/**
 *  BMW ConnectedDrive (Connect)
 *
 *  Copyright 2018 (Tom Beech)
 */
definition(
		name: "BMW ConnectedDrive (Connect)",
		namespace: "ibeech",
		author: "Tom Beech",
		description: "Connect your BMW to SmartThings",
		iconUrl: "https://is5-ssl.mzstatic.com/image/thumb/Purple128/v4/f8/d0/0a/f8d00a6b-6d65-df1e-d42e-b4f003301c00/AppIcon-1x_U007emarketing-0-0-GLES2_U002c0-512MB-sRGB-0-0-0-85-220-0-0-0-3.png/246x0w.jpg",
		iconX2Url: "https://is5-ssl.mzstatic.com/image/thumb/Purple128/v4/f8/d0/0a/f8d00a6b-6d65-df1e-d42e-b4f003301c00/AppIcon-1x_U007emarketing-0-0-GLES2_U002c0-512MB-sRGB-0-0-0-85-220-0-0-0-3.png/246x0w.jpg",
    singleInstance: false
)

preferences {
	//startPage
	page(name: "startPage")

	//Connect Pages
	page(name:"mainPage", title:"BMW ConnectedDrive Device Setup", content:"mainPage", install: true)
	page(name: "loginPAGE")
}

def connectedDriveRemoteServicesUK(vin = '/') 			 { return "https://www.bmw-connecteddrive.co.uk/api/vehicle/remoteservices/v1/${vin}" }
def connectedDriveMapUpdateUK(vin = '/') 			     { return "https://www.bmw-connecteddrive.co.uk/api/me/service/mapupdate/download/v1/${vin}" }
def connectedDriveCarDetailsUK(vin = '/') 			     { return "https://www.bmw-connecteddrive.co.uk/api/vehicle/dynamic/v1/${vin}?offset=-60" }
def connectedDriveCarImageUK(vin = '/') 			     { return "https://www.bmw-connecteddrive.de/api/vehicle/image/v1/${vin}?startAngle=10&stepAngle=10&width=300" }

def startPage() {
	if (parent) {
		atomicState?.isParent = false
		tmaConfigurePAGE()
	} else {
		atomicState?.isParent = true
		mainPage()
	}
}

//BMW ConnectedDrive Connect App Pages

def mainPage() {
	log.debug "mainPage"
	if (username == null || username == '' || password == null || password == '') {
		return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
			section {            	
				headerSECTION()
            }
            section {
            	label title: "Name this SmartApp:", required: true, description: "My BMW ConnectedDrive"
				href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter BMW ConnectedDrive credentials", state: authenticated())
			}
		}
	} else {
		log.debug "next phase"
		return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
			section {
				headerSECTION()
			}	
            section { 
            	label title: "Name this Switch configuration:", required: true
            	href("loginPAGE", title: "Authenticated as", description: authenticated() ? username : "Tap to enter BMW ConnectedDrive credentials", state: authenticated())
			}
			if (!stateTokenPresent()) {				
				section {
					paragraph "There was a problem connecting to BMW ConnectedDrive. Check your user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
				}
			}
		}
	}
}

def headerSECTION() {
	return paragraph (image: "https://is5-ssl.mzstatic.com/image/thumb/Purple128/v4/f8/d0/0a/f8d00a6b-6d65-df1e-d42e-b4f003301c00/AppIcon-1x_U007emarketing-0-0-GLES2_U002c0-512MB-sRGB-0-0-0-85-220-0-0-0-3.png/246x0w.jpg",
                  "BMW ConnectedDrive (Connect)\nVersion: 0.05\nDate: 190318")
}

def stateTokenPresent() {
	return state.connectedDriveAccessToken != null && state.connectedDriveAccessToken != ''
}

def authenticated() {
	return (state.connectedDriveAccessToken != null && state.connectedDriveAccessToken != '') ? "complete" : null
}

def loginPAGE() {
	if (username == null || username == '' || password == null || password == '') {
		return dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
			section { headerSECTION() }
			section { paragraph "Enter your BMW ConnectedDrive credentials below to enable SmartThings and BMW ConnectedDrive integration." }
			section("BMW ConnectedDrive Credentials:") {
				input("username", "text", title: "Username", description: "Your BMW ConnectedDrive username (usually an email address)", required: true)
				input("password", "password", title: "Password", description: "Your BMW ConnectedDrive password", required: true, submitOnChange: true)
			}
		}
	} else {
		getconnectedDriveAccessToken()
		dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
			section { headerSECTION() }
			section { paragraph "Enter your BMW ConnectedDrive credentials below to enable SmartThings and BMW ConnectedDrive integration." }
			section("BMW ConnectedDrive Credentials:") {
				input("username", "text", title: "Username", description: "Your BMW ConnectedDrive username (usually an email address)", required: true)
				input("password", "password", title: "Password", description: "Your BMW ConnectedDrive password", required: true, submitOnChange: true)
			}
			if (stateTokenPresent()) {
				section {
					paragraph "You have successfully connected to BMW ConnectedDrive. Tap 'Done' to continue"
				}
			} else {
				section {
					paragraph "There was a problem connecting to BMW ConnectedDrive. Check your user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
				}
			}
		}
	}
}

// App lifecycle hooks
def installed() {
	if(parent) { installedChild() } // This will handle all of the install functions when the child app is installed
	else { installedParent() } // This will handle all of the install functions when the parent app is installed
}

def updated() {
	if(parent) { updatedChild() } // This will handle all of the install functions when the child app is updated
	else { updatedParent() } // This will handle all of the install functions when the parent app is updated
}

def uninstalled() {
	if(parent) { } // This will handle all of the install functions when the child app is uninstalled
	else { uninstalledParent() } // This will handle all of the install functions when the parent app is uninstalled
}

def installedParent() {
	log.debug "installed"
	initialize()
	// Check for new devices every 3 hours
	runEvery3Hours('updateDevices')
	// execute handlerMethod every 10 minutes.
	runEvery10Minutes('refreshDevices')
}

// called after settings are changed
def updatedParent() {
	log.debug "updated"
	unsubscribe()
	initialize()
	unschedule('refreshDevices')
	runEvery10Minutes('refreshDevices')
}

def uninstalledParent() {
	log.info("Uninstalling, removing child devices...")
	unschedule()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

// called after Done is hit after selecting a Location
def initialize() {
	if (parent) { }
    else {
		log.debug "initialize"
		
        state.cars = getRegisteredCars()
                
        state.cars.each { car ->
        
         	def childDevice = getChildDevice(car.vin);
            def carName = "${car.brand} ${car.basicType} ${wagonToTouring(car.bodyType)}"
            
         	if (!childDevice) {
				log.debug "Found new car: ${carName}. VIN: ${car.vin}"
                
                def data = [
                    name: "${carName}",
                    label: "${carName}"
                ]

                childDevice = addChildDevice(app.namespace, "BMW ConnectedDrive Car", car.vin, null, data)
                childDevice.refresh()
            } else {
            	  log.debug "Found existing car ${carName}"         
            }
        }
  	}
}

def wagonToTouring(input) {
	if(input == "Sports Wagon") {
    	return "Msport Touring"
    } else {
    	return input
    }    
}

def generateNotification(msg) {
	if (settings.sendSMS != null) {
		sendSms(sendSMS, msg)
	}
	if (settings.sendPush == true) {
		sendPush(msg)
	}
}

def refreshDevices() {
	log.info("Refreshing all devices...")
	getChildDevices().each { device ->
		device.refresh()
	}
}

def getRegisteredCars() {
           
	httpGet(uri: "https://www.bmw-connecteddrive.co.uk/api/me/vehicles/v2", contentType: 'application/json', headers: apiRequestHeaders()) {response ->
		 return response.data;   
	}    
}

def getCarServicenfo(vin) {

	def params = [
		uri: connectedDriveCarDetailsUK(vin),
		headers: apiRequestHeaders()
	]	
    
    httpGet(params) {response ->    
        log.debug "Mileage: ${response.data.attributesMap.mileage} ${response.data.attributesMap.unitOfLength}"
        
        outputServiceInfo(response.data.vehicleMessages.cbsMessages[0])
        outputServiceInfo(response.data.vehicleMessages.cbsMessages[1])
        outputServiceInfo(response.data.vehicleMessages.cbsMessages[2])
        outputServiceInfo(response.data.vehicleMessages.cbsMessages[3])
		return response.data
	}

}

def getCarImageUrl(vin){
	def params = [
		uri: connectedDriveCarImageUK(vin),
		headers: apiRequestHeaders()
	]	
    
    httpGet(params) {response ->    
        log.debug "${response.data.angleUrls[2].url}"
        
		return response.data.angleUrls[2].url
	}
}

def flashLights(vin) {

	def params = [
			uri: "${connectedDriveRemoteServicesUK(vin)}/RLF",
            contentType: 'application/xml',
        	headers: apiRequestHeaders()   	
        ]

    httpPostJson(params) {response ->
		log.debug "(Remote Light Flash) Request response, $response.status"        
    }
}

def ventilate(vin) {

	def params = [
			uri: "${connectedDriveRemoteServicesUK(vin)}/RCN",
            contentType: 'application/xml',
        	headers: apiRequestHeaders()   	
        ]

    httpPostJson(params) {response ->
		log.debug "(Remote Climate Now) Request response, $response.status"        
    }
}

def lockDoors(vin) {

	def params = [
			uri: "${connectedDriveRemoteServicesUK(vin)}/RDL",
            contentType: 'application/xml',
        	headers: apiRequestHeaders()   	
        ]

    httpPostJson(params) {response ->
		log.debug "(Remote Door Lock) Request response, $response.status"        
    }
}
def unlockDoors(vin) {

	def params = [
			uri: "${connectedDriveRemoteServicesUK(vin)}/RDU",
            contentType: 'application/xml',
        	headers: apiRequestHeaders()   	
        ]

    httpPostJson(params) {response ->
		log.debug "(Remote Door Unlock) Request response, $response.status"        
    }
}

def updateMaps(vin) {

    def params = [
		uri: connectedDriveMapUpdateUK(vin),
		headers: apiRequestHeaders()
	]	
    
    httpGet(params) {response ->
    
    if(response.status == 404) return true
    
        log.debug "Map activation code: ${response.data.activationCode}"
        log.debug "Maps name ${response.data.update[0].name}"
        log.debug "Map update available: ${response.data.update[0].newer}"
		return response.data
	}
}

def apiRequestHeaders() {
    
    if(!isLoggedIn()) {
        getconnectedDriveAccessToken()
    }

	return [
        'authorization': "${state.token_type} ${state.connectedDriveAccessToken}"
    ]
}

def outputServiceInfo(msg){
	              
	if(msg == null) return;
    
    if(msg.unitOfLengthRemaining != "") {
    	log.info "${msg.text} ==${msg.status}== Due ${msg.date} or ${msg.unitOfLengthRemaining}"
        } else {
        	log.info "${msg.text} ==${msg.status}== Due ${msg.date}"
    }
}

def getconnectedDriveAccessToken() {
	try {
           
     	def params = [
			uri: 'https://customer.bmwgroup.com/gcdm/oauth/authenticate',
        	headers: [
              'Content-Type': 'application/x-www-form-urlencoded',
              'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0'
        	],
        	body: 'username=' + settings.username + '&password=' + settings.password + '&redirect_uri=https://www.bmw-connecteddrive.com/app/default/static/external-dispatch.html&response_type=token&scope=authenticate_user fupo&state=eyJtYXJrZXQiOiJkZSIsImxhbmd1YWdlIjoiZGUiLCJkZXN0aW5hdGlvbiI6ImxhbmRpbmdQYWdlIn0&locale=DE-de&client_id=dbf0a542-ebd1-4ff0-a9a7-55172fbfce35'
        ]
        
		state.cookie = ''

		httpPostJson(params) {response ->
			log.debug "Request was successful, $response.status"
            
            if(response.headers['Location'] == 'https://www.bmw-connecteddrive.com/app/default/static/external-dispatch.html?error=access_denied'){
            	
                state.connectedDriveAccessToken = null
        		state.connectedDriveAccessToken_expires_at = null
        		state.loginerrors = "Error: Access Denied. Check your username and password then try again"
        		logResponse(e)
        		return e
                
            } else {

				def result = response.headers['Location'] =~ /.*access_token=([\w\d]+).*token_type=(\w+).*expires_in=(\d+).*/
				state.connectedDriveAccessToken = result[0][1]
				state.token_type = result[0][2]
                state.connectedDriveAccessToken_expires_at = new Date().getTime() + result[0][3].toInteger()
               
                log.debug "Access token '${state.token_type} ${state.connectedDriveAccessToken}' expires in ${result[0][3]} seconds"               
                
                state.loginerrors = null
            }
		}
    } catch (groovyx.net.http.HttpResponseException e) {
    	state.connectedDriveAccessToken = null
        state.connectedDriveAccessToken_expires_at = null
   		state.loginerrors = "Error: ${e.response.status}: ${e.response.data}"
    	logResponse(e.response)
		return e.response
    } catch(Exception ex) {
    	state.connectedDriveAccessToken = null
        state.connectedDriveAccessToken_expires_at = null
        state.loginerrors = "Error: ${ex}"
        log.error ex
        return e
    }
}

def isLoggedIn() {
	log.debug "Calling isLoggedIn()"
	log.debug "isLoggedIn state $state.connectedDriveAccessToken"
	
    // Check if the access token exists
    if(state.connectedDriveAccessToken) {
    	// Check if the access token has expired
    	def now = new Date().getTime()
        log.debug state.connectedDriveAccessToken_expires_at
    	return state.connectedDriveAccessToken_expires_at > now	
	}

	// The access token does not exist
	log.debug "No state.connectedDriveAccessToken"
	return false
}

def logResponse(response) {
	log.info("Status: ${response.status}")
	log.info("Body: ${response.data}")
}
