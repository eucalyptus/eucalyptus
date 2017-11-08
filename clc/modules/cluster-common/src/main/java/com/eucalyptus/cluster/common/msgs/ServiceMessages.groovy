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
import com.eucalyptus.empyrean.StartServiceResponseType
import com.eucalyptus.empyrean.StartServiceType
import com.eucalyptus.empyrean.StopServiceResponseType
import com.eucalyptus.empyrean.StopServiceType
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage( ClusterController )
interface ClusterServiceMessage extends BaseMessageMarker { }

class ClusterDescribeServicesResponseType extends DescribeServicesResponseType implements ClusterServiceMessage { }
class ClusterDescribeServicesType extends DescribeServicesType implements ClusterServiceMessage { }
class ClusterDisableServiceResponseType extends DisableServiceResponseType implements ClusterServiceMessage { }
class ClusterDisableServiceType extends DisableServiceType implements ClusterServiceMessage { }
class ClusterEnableServiceResponseType extends EnableServiceResponseType implements ClusterServiceMessage { }
class ClusterEnableServiceType extends EnableServiceType implements ClusterServiceMessage { }
class ClusterStartServiceResponseType extends StartServiceResponseType implements ClusterServiceMessage { }
class ClusterStartServiceType extends StartServiceType implements ClusterServiceMessage { }
class ClusterStopServiceResponseType extends StopServiceResponseType implements ClusterServiceMessage { }
class ClusterStopServiceType extends StopServiceType implements ClusterServiceMessage { }