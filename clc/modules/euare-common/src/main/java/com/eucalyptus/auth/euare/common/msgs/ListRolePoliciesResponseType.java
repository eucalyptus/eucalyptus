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
package com.eucalyptus.auth.euare.common.msgs;

public class ListRolePoliciesResponseType extends EuareMessage {

  private ListRolePoliciesResult listRolePoliciesResult = new ListRolePoliciesResult( );
  private ResponseMetadataType responseMetadata = new ResponseMetadataType( );

  public ListRolePoliciesResult getListRolePoliciesResult( ) {
    return listRolePoliciesResult;
  }

  public void setListRolePoliciesResult( ListRolePoliciesResult listRolePoliciesResult ) {
    this.listRolePoliciesResult = listRolePoliciesResult;
  }

  public ResponseMetadataType getResponseMetadata( ) {
    return responseMetadata;
  }

  public void setResponseMetadata( ResponseMetadataType responseMetadata ) {
    this.responseMetadata = responseMetadata;
  }
}
