package edu.ucsb.eucalyptus.msgs

import edu.ucsb.eucalyptus.annotation.HttpParameterMapping;

public class VmKeyPairMessage extends EucalyptusMessage {}
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
  boolean _return;
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
