/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class ModifyInstanceCreditSpecificationResponseType extends ComputeMessage {

  private SuccessfulInstanceCreditSpecificationSet successfulInstanceCreditSpecifications;
  private UnsuccessfulInstanceCreditSpecificationSet unsuccessfulInstanceCreditSpecifications;

  public SuccessfulInstanceCreditSpecificationSet getSuccessfulInstanceCreditSpecifications( ) {
    return successfulInstanceCreditSpecifications;
  }

  public void setSuccessfulInstanceCreditSpecifications( final SuccessfulInstanceCreditSpecificationSet successfulInstanceCreditSpecifications ) {
    this.successfulInstanceCreditSpecifications = successfulInstanceCreditSpecifications;
  }

  public UnsuccessfulInstanceCreditSpecificationSet getUnsuccessfulInstanceCreditSpecifications( ) {
    return unsuccessfulInstanceCreditSpecifications;
  }

  public void setUnsuccessfulInstanceCreditSpecifications( final UnsuccessfulInstanceCreditSpecificationSet unsuccessfulInstanceCreditSpecifications ) {
    this.unsuccessfulInstanceCreditSpecifications = unsuccessfulInstanceCreditSpecifications;
  }

}
