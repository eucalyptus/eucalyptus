/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
@GroovyAddClassUUID
package com.eucalyptus.cluster.common.msgs

import com.eucalyptus.cluster.common.ClusterController
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.empyrean.DescribeServicesResponseType
import com.eucalyptus.empyrean.DescribeServicesType
import com.eucalyptus.empyrean.DisableServiceResponseType
import com.eucalyptus.empyrean.DisableServiceType
import com.eucalyptus.empyrean.EnableServiceResponseType
import com.eucalyptus.empyrean.EnableServiceType
import com.eucalyptus.empyrean.ServiceId
import com.eucalyptus.empyrean.StartServiceResponseType
import com.eucalyptus.empyrean.StartServiceType
import com.eucalyptus.empyrean.StopServiceResponseType
import com.eucalyptus.empyrean.StopServiceType
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import org.jibx.runtime.IMarshallable
import org.jibx.runtime.IMarshallingContext
import org.jibx.runtime.IUnmarshallable
import org.jibx.runtime.IUnmarshallingContext
import org.jibx.runtime.JiBXException

@ComponentMessage( ClusterController )
interface ClusterServiceMessage extends BaseMessageMarker {
  String getCorrelationId( )
  void setCorrelationId( String correlationId )
  String getUserId( )
  void setUserId( String userId )
  String getStatusMessage( )
  void setStatusMessage( String statusMessage )
  Boolean get_return( )
  void set_return( Boolean returnValue )
  Integer get_epoch( )
  void set_epoch( Integer epoch )
  ArrayList<ServiceId> get_services( )
  void set_services( ArrayList<ServiceId> services )
  ArrayList<ServiceId> get_disabledServices( )
  void set_disabledServices( ArrayList<ServiceId> services )
  ArrayList<ServiceId> get_notreadyServices( )
  void set_notreadyServices( ArrayList<ServiceId> services )
}

interface ClusterServiceTransitionMessage extends ClusterServiceMessage {
  ArrayList<ServiceId> getServices( )
  void setServices( ArrayList<ServiceId> services )
}

class ClusterServiceMessageJibxMixin implements IMarshallable, IUnmarshallable {
  @Override String JiBX_className( ) { null }
  @Override String JiBX_getName( ) { null }
  @Override void marshal(final IMarshallingContext ctx) throws JiBXException { }
  @Override void unmarshal(final IUnmarshallingContext ctx) throws JiBXException { }
}

class ClusterDescribeServicesResponseType extends DescribeServicesResponseType implements ClusterServiceMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterDescribeServicesType extends DescribeServicesType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterDisableServiceResponseType extends DisableServiceResponseType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterDisableServiceType extends DisableServiceType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterEnableServiceResponseType extends EnableServiceResponseType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterEnableServiceType extends EnableServiceType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterStartServiceResponseType extends StartServiceResponseType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterStartServiceType extends StartServiceType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterStopServiceResponseType extends StopServiceResponseType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}
class ClusterStopServiceType extends StopServiceType implements ClusterServiceTransitionMessage {
  @Delegate ClusterServiceMessageJibxMixin jibxMixin = new ClusterServiceMessageJibxMixin();
}