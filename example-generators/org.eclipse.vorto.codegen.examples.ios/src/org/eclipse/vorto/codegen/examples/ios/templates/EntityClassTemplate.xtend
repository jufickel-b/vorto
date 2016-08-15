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
package org.eclipse.vorto.codegen.examples.ios.templates

import org.eclipse.vorto.codegen.api.IFileTemplate
import org.eclipse.vorto.codegen.api.InvocationContext
import org.eclipse.vorto.core.api.model.datatype.Entity
import org.eclipse.vorto.core.api.model.datatype.ObjectPropertyType
import org.eclipse.vorto.core.api.model.datatype.PrimitivePropertyType
import org.eclipse.vorto.core.api.model.datatype.PrimitiveType
import org.eclipse.vorto.core.api.model.datatype.PropertyType

/**
 * @author Alexander Edelmann - Robert Bosch (SEA) Pte. Ltd.
 */
class EntityClassTemplate implements IFileTemplate<Entity> {

	override getFileName(Entity context) {
		return context.name+".swift"
	}
	
	override getPath(Entity context) {
		return ""
	}
	
	override getContent(Entity context,InvocationContext invocationContext) {
'''
//Generated by Vorto

import Foundation

class «context.name» {

	«FOR property : context.properties» 
	var «property.name» : «IF property.multiplicity»[«getType(property.type)»]«ELSE»«getType(property.type)»«ENDIF»
	«ENDFOR»
	
	init() {}
}
'''
	}
	
	def getType(PropertyType propType) {
		if (propType instanceof PrimitivePropertyType) {
			var primitiveProp = propType as PrimitivePropertyType
			switch(primitiveProp.type) {
				case (PrimitiveType.STRING) : return "String"
				case (PrimitiveType.INT) : return "Int"
				case (PrimitiveType.DATETIME) : return "NSDate"
				case (PrimitiveType.DOUBLE) : return "Double"
				case (PrimitiveType.FLOAT) : return "Float"
				default : return "String"
			}
		} else {
			return (propType as ObjectPropertyType).type.name
		}
	}
}