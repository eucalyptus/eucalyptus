/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.objectstorage.providers.walrus

import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.apache.log4j.Logger;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/**
 * Based on CompositeHelper
 * Converts messages from ObjectStorageRequestType to another message type. Both must
 * have BaseMessage as the base class.
 *
 */
public class MessageProxy<T extends BaseMessage> {
	private static final Logger LOG = Logger.getLogger( MessageProxy.class );

	private static final def List<String> baseMsgProps = BaseMessage.metaClass.properties.collect{ MetaProperty p -> p.name };

	/**
	 * Clones the source to dest on a property-name basis.
	 * Requires that both source and dest are not null. Will not
	 * set values to null in destination that are null in source
	 * @param source
	 * @param dest
	 * @return
	 */
	public static <O extends BaseMessage, I extends BaseMessage> O mapExcludeNulls( I source, O dest ) {
		def props = dest.metaClass.properties.collect{ MetaProperty p -> p.name };
		
		source.metaClass.properties.findAll{ MetaProperty it -> it.name != "metaClass" && it.name != "class" && it.name != "correlationId" && !baseMsgProps.contains(it.name) && props.contains(it.name) && source[it.name] != null }.each{ MetaProperty sourceField ->
			LOG.trace("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
			try {
				dest[sourceField.name]=source[sourceField.name];
			} catch(GroovyCastException e) {
				LOG.trace("Cannot cast class. ", e);
			} catch(ReadOnlyPropertyException e) {
				LOG.trace("Cannot set readonly property.",e);
			}
		}
		return dest;
	}

	/**
	 * Clones the source to dest on a property-name basis.
	 * Requires that both source and dest are not null. Will
	 * include null values in the mapping.
	 * @param source
	 * @param dest
	 * @return
	 */	
	public static <O extends BaseMessage, I extends BaseMessage> O mapWithNulls( I source, O dest ) {
		def props = dest.metaClass.properties.collect{ MetaProperty p -> p.name };
		source.metaClass.properties.findAll{ MetaProperty it -> it.name != "metaClass" && it.name != "class" && it.name != "correlationId" && !baseMsgProps.contains(it.name) && props.contains(it.name) }.each{ MetaProperty sourceField ->
			LOG.trace("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
			try {
				dest[sourceField.name]=source[sourceField.name];
			} catch(GroovyCastException e) {
				LOG.trace("Cannot cast class. ", e);
			} catch(ReadOnlyPropertyException e) {
				LOG.trace("Cannot set readonly property.",e);
			}
		}
		return dest;
	}


}
