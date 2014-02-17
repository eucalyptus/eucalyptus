/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.service.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.service.config.ComputeConfiguration;
import com.eucalyptus.ws.server.SoapPipeline;
import com.google.common.collect.Sets;

/**
 *
 */
@ComponentPart( Compute.class )
public class ComputeSoapPipeline extends SoapPipeline {

  public ComputeSoapPipeline( ) {
    super(
        "compute-soap",
        Compute.class,
        Sets.newHashSet( ComputeConfiguration.SERVICE_PATH, "/services/Eucalyptus" ),
        ComputeQueryBinding.COMPUTE_DEFAULT_NAMESPACE,
        "http://ec2.amazonaws.com/doc/\\d\\d\\d\\d-\\d\\d-\\d\\d/" );
  }
}
