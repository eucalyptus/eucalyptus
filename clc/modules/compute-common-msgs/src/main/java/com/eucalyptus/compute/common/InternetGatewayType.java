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
package com.eucalyptus.compute.common;

import com.eucalyptus.util.CompatFunction;
import com.google.common.base.Strings;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InternetGatewayType extends EucalyptusData implements VpcTagged {

  private String internetGatewayId;
  private InternetGatewayAttachmentSetType attachmentSet = new InternetGatewayAttachmentSetType( );
  private ResourceTagSetType tagSet;

  public InternetGatewayType( ) {
  }

  public InternetGatewayType( String internetGatewayId, String attachedVpcId ) {
    this.internetGatewayId = internetGatewayId;
    if ( !Strings.isNullOrEmpty( attachedVpcId ) ) {
      attachmentSet.getItem( ).add( new InternetGatewayAttachmentType( attachedVpcId, "available" ) );
    }
  }

  public static CompatFunction<InternetGatewayType, String> id( ) {
    return new CompatFunction<InternetGatewayType, String>( ) {
      @Override
      public String apply( final InternetGatewayType internetGatewayType ) {
        return internetGatewayType.getInternetGatewayId( );
      }
    };
  }

  public String getInternetGatewayId( ) {
    return internetGatewayId;
  }

  public void setInternetGatewayId( String internetGatewayId ) {
    this.internetGatewayId = internetGatewayId;
  }

  public InternetGatewayAttachmentSetType getAttachmentSet( ) {
    return attachmentSet;
  }

  public void setAttachmentSet( InternetGatewayAttachmentSetType attachmentSet ) {
    this.attachmentSet = attachmentSet;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
