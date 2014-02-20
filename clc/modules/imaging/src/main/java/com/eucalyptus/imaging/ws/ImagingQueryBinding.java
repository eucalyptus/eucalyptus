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
package com.eucalyptus.imaging.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.imaging.Imaging;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 * @author Sang-Min Park
 *
 */
@ComponentPart(Imaging.class)
public class ImagingQueryBinding extends BaseQueryBinding<OperationParameter> {
  static final String IMAGING_NAMESPACE_PATTERN = "http://www.eucalyptus.com/ns/imaging/%s/";
  static final String IMAGING_DEFAULT_VERSION = "2014-02-14";
  static final String IMAGING_DEFAULT_NAMESPACE = String.format( IMAGING_NAMESPACE_PATTERN, IMAGING_DEFAULT_VERSION );

  
  public ImagingQueryBinding( ) {
    super( IMAGING_NAMESPACE_PATTERN, IMAGING_DEFAULT_VERSION, UnknownParameterStrategy.ERROR, OperationParameter.Action, OperationParameter.Operation );
  }
}
