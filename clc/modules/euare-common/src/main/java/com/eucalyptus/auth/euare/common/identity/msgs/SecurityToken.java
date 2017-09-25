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
package com.eucalyptus.auth.euare.common.identity.msgs;

import java.util.ArrayList;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SecurityToken extends EucalyptusData {

  private String originatingAccessKeyId;
  private String originatingUserId;
  private String originatingRoleId;
  private String nonce;
  private Long created;
  private Long expires;
  private ArrayList<SecurityTokenAttribute> attributes = Lists.newArrayList( );

  public String getOriginatingAccessKeyId( ) {
    return originatingAccessKeyId;
  }

  public void setOriginatingAccessKeyId( String originatingAccessKeyId ) {
    this.originatingAccessKeyId = originatingAccessKeyId;
  }

  public String getOriginatingUserId( ) {
    return originatingUserId;
  }

  public void setOriginatingUserId( String originatingUserId ) {
    this.originatingUserId = originatingUserId;
  }

  public String getOriginatingRoleId( ) {
    return originatingRoleId;
  }

  public void setOriginatingRoleId( String originatingRoleId ) {
    this.originatingRoleId = originatingRoleId;
  }

  public String getNonce( ) {
    return nonce;
  }

  public void setNonce( String nonce ) {
    this.nonce = nonce;
  }

  public Long getCreated( ) {
    return created;
  }

  public void setCreated( Long created ) {
    this.created = created;
  }

  public Long getExpires( ) {
    return expires;
  }

  public void setExpires( Long expires ) {
    this.expires = expires;
  }

  public ArrayList<SecurityTokenAttribute> getAttributes( ) {
    return attributes;
  }

  public void setAttributes( ArrayList<SecurityTokenAttribute> attributes ) {
    this.attributes = attributes;
  }
}
