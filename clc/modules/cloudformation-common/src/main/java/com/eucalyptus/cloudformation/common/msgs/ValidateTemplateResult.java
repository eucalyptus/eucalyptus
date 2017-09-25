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
package com.eucalyptus.cloudformation.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ValidateTemplateResult extends EucalyptusData {

  private ResourceList capabilities;
  private String capabilitiesReason;
  private String description;
  private TemplateParameters parameters;

  public ResourceList getCapabilities( ) {
    return capabilities;
  }

  public void setCapabilities( ResourceList capabilities ) {
    this.capabilities = capabilities;
  }

  public String getCapabilitiesReason( ) {
    return capabilitiesReason;
  }

  public void setCapabilitiesReason( String capabilitiesReason ) {
    this.capabilitiesReason = capabilitiesReason;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public TemplateParameters getParameters( ) {
    return parameters;
  }

  public void setParameters( TemplateParameters parameters ) {
    this.parameters = parameters;
  }
}
