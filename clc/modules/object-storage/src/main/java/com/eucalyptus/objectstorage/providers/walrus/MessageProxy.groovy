/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
