/**
 *  Pi Temperature
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
	definition (name: "Virtual Pi Temperature", namespace: "ibeech", author: "ibeech") {
		capability "Temperature Measurement"
        
        command "setTemperature", ["number", "string"]
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2)  {    
        
		 multiAttributeTile(name: "thermostat", width: 6, height: 2, type:"thermostat") {
			tileAttribute("device.temperature", key:"PRIMARY_CONTROL", canChangeBackground: true){
				attributeState "default", label: '${currentValue}°', unit:"C", backgroundColors: [
				// Celsius Color Range
				[value: 0, color: "#50b5dd"],
				[value: 18, color: "#43a575"],
				[value: 20, color: "#c5d11b"],
				[value: 24, color: "#f4961a"],
				[value: 27, color: "#e75928"],
				[value: 30, color: "#d9372b"],
				[value: 32, color: "#b9203b"]
			]}
			tileAttribute ("zoneName", key: "SECONDARY_CONTROL") {
				attributeState "zoneName", label:'${currentValue}'
			}
		}

		main "thermostat"
		details (["thermostat"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Virtual temperature parsing '${description}'"
}

def setTemperature(val, zoneName) {
    sendEvent(name: 'temperature', value: val, unit: "C")
    sendEvent(name: 'zoneName', value: zoneName)
}
