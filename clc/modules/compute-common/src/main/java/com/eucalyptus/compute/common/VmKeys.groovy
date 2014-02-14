/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.PolicyResourceType
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@PolicyResourceType( "keypair" )//GRZE:WHINE: this needs to be a string in order to avoid build-deps cycle
public class VmKeyPairMessage extends ComputeMessage {
  
  public VmKeyPairMessage( ) {
    super( );
  }
  
  public VmKeyPairMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public VmKeyPairMessage( String userId ) {
    super( userId );
  }
  
}
/** *******************************************************************************/
public class CreateKeyPairResponseType extends VmKeyPairMessage {
  String keyName;
  String keyFingerprint;
  String keyMaterial;
}
public class CreateKeyPairType extends VmKeyPairMessage {
  String keyName;
}
/** *******************************************************************************/
public class DeleteKeyPairResponseType extends VmKeyPairMessage {
}
public class DeleteKeyPairType extends VmKeyPairMessage {
  String keyName;
}
/** *******************************************************************************/
public class DescribeKeyPairsResponseType extends VmKeyPairMessage {
  ArrayList<DescribeKeyPairsResponseItemType> keySet = new ArrayList<DescribeKeyPairsResponseItemType>();
}
public class DescribeKeyPairsType extends VmKeyPairMessage {
  @HttpParameterMapping (parameter = "KeyName")
  ArrayList<String> keySet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeKeyPairsResponseItemType extends EucalyptusData {
  String keyName;
  String keyFingerprint;
  
  def DescribeKeyPairsResponseItemType() {
  }
  
  def DescribeKeyPairsResponseItemType(final String keyName, final String keyFingerprint) {
    this.keyName = keyName;
    this.keyFingerprint = keyFingerprint;
  }
}
public class ImportKeyPairResponseType extends VmKeyPairMessage {
  String keyName;
  String keyFingerprint;
  public ImportKeyPairResponseType() {
  }
}
public class ImportKeyPairType extends VmKeyPairMessage {
  String keyName;
  String publicKeyMaterial;
  public ImportKeyPairType() {  }
}
