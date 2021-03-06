/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *   
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *   
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 *******************************************************************************/
package org.eclipse.vorto.codegen.ios.templates

import org.eclipse.vorto.codegen.api.IFileTemplate
import org.eclipse.vorto.codegen.api.InvocationContext
import org.eclipse.vorto.core.api.model.functionblock.ReturnObjectType
import org.eclipse.vorto.core.api.model.functionblock.ReturnPrimitiveType
import org.eclipse.vorto.core.api.model.informationmodel.InformationModel

/**
 * @author Alexander Edelmann - Robert Bosch (SEA) Pte. Ltd.
 */
class DeviceServiceTemplate implements IFileTemplate<InformationModel> {

	override getFileName(InformationModel context) {
		return context.name+"Device.swift"
	}
	
	override getPath(InformationModel context) {
		return ""
	}
	
	override getContent(InformationModel context,InvocationContext invocationContext) {
'''
//Generated by Vorto

import Foundation
import CoreBluetooth

let deviceName = "«context.displayname»"

let «context.name»InfoServiceUUID = CBUUID(string: "add uuid here") //TODO
«FOR fbProperty : context.properties»
let «fbProperty.type.name»ServiceUUID = CBUUID(string: "add uuid of «fbProperty.name» here") //TODO
«ENDFOR»

// Characteristic UUIDs
let «context.name»InfoSystemIDUUID  = CBUUID(string: "add uuid here") //TODO
«FOR fbProperty : context.properties»
let «fbProperty.type.name»DataUUID = CBUUID(string: "add uuid of «fbProperty.name» for data here")
«IF fbProperty.type.functionblock.configuration != null»
let «fbProperty.type.name»ConfigUUID = CBUUID(string: "add uuid of «fbProperty.name» for configuration here")
«ENDIF»
«ENDFOR»

class «context.name»Device {
    
    // Check name of device from advertisement data
    class func found (advertisementData: [NSObject : AnyObject]!) -> Bool {
        let nameOfDeviceFound = (advertisementData as NSDictionary).objectForKey(CBAdvertisementDataLocalNameKey) as? NSString
        
        return (nameOfDeviceFound == deviceName)
    }
    
    // Check if the service has a valid UUID
    class func validService (service : CBService) -> Bool {
        if service.UUID == «context.name»InfoServiceUUID
        	«FOR fbProperty : context.properties»
        	|| service.UUID == «fbProperty.type.name»ServiceUUID
        	«ENDFOR» {
                return true
        }
        else {
            return false
        }
    }
    
    // Check if the characteristic has a valid data UUID
    class func validDataCharacteristic (characteristic : CBCharacteristic) -> Bool {
        if service.UUID == «context.name»InfoServiceUUID
        	«FOR fbProperty : context.properties»
        	|| service.UUID == «fbProperty.type.name»ServiceUUID
        	«ENDFOR» {
                return true
        }
        else {
            return false
        }
    }
    
    «FOR fbProperty : context.properties»
    	«FOR operation : fbProperty.type.functionblock.operations»
    	«IF operation.returnType != null && operation.returnType instanceof ReturnObjectType»
    	class func «operation.name»(value : NSData) -> «(operation.returnType as ReturnObjectType).returnType.name» {
    		var result = «(operation.returnType as ReturnObjectType).returnType.name»()
    		//TODO convert and map value to response type
    		return result
    	}
    	«ENDIF»
    	«IF operation.returnType != null && operation.returnType instanceof ReturnPrimitiveType»
    	class func «operation.name»(value : NSData) -> «MappingUtils.mapSimpleDatatype((operation.returnType as ReturnPrimitiveType).returnType)» {
    		//TODO convert and map value to response type
    		return result
    	}
    	«ENDIF»
    	«ENDFOR»
    «ENDFOR»
    
'''
	}
}