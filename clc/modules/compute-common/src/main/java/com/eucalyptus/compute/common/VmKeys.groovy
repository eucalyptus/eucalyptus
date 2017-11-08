/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType
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
