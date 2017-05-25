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
package com.eucalyptus.cluster.service;

import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesType;
import com.eucalyptus.cluster.common.msgs.ClusterDisableServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDisableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterStartServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStartServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStopServiceType;

/**
 *
 */
public interface ClusterEmpyreanService {

  ClusterDescribeServicesResponseType describeServices( ClusterDescribeServicesType request );
  ClusterDisableServiceResponseType disableService( ClusterDisableServiceType request );
  ClusterEnableServiceResponseType enableService( ClusterEnableServiceType request );
  ClusterStartServiceResponseType startService( ClusterStartServiceType request );
  ClusterStopServiceResponseType stopService( ClusterStopServiceType request );
}
