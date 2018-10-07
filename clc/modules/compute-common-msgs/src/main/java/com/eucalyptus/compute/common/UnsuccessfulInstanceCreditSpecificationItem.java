/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class UnsuccessfulInstanceCreditSpecificationItem extends EucalyptusData {

  private UnsuccessfulInstanceCreditSpecificationItemError error;
  private String instanceId;

  public UnsuccessfulInstanceCreditSpecificationItemError getError( ) {
    return error;
  }

  public void setError( final UnsuccessfulInstanceCreditSpecificationItemError error ) {
    this.error = error;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

}
