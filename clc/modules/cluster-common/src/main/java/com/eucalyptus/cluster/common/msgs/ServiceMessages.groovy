/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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