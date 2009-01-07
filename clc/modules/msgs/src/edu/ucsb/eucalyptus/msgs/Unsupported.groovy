package edu.ucsb.eucalyptus.msgs
/**
 * The ConfirmProductInstance operation checks whether a running instance is based on an
 * AMI with a given product code; if it is, the operation returns the identifier of the instance's owner.
 * This operation is only useful to EC2 developers who have sold an AMI to a customer with a support
 * agreement and an associated product code. In this circumstance, it allows you to confirm that the instance a
 * customer is running is eligible for support.
 */
public class ConfirmProductInstanceResponseType extends EucalyptusMessage {
  boolean _return;
  String ownerId;
}
public class ConfirmProductInstanceType extends EucalyptusMessage {
  String productCode;
  String instanceId;
}
/*********************************************************************************/
