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
package com.eucalyptus.cluster.common.broadcast.impl;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;

/**
 *
 */
public class NetworkInfoXmlModule extends JacksonXmlModule {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings( "WeakerAccess" )
  public NetworkInfoXmlModule( ) {
    setDefaultUseWrapper( false );
  }

  @Override
  public void setupModule( final Module.SetupContext context ) {
    super.setupModule( context );
    context.addAbstractTypeResolver( new NetworkInfoTypeResolver( ) );
    context.setNamingStrategy( PropertyNamingStrategy.LOWER_CASE );
  }
}
