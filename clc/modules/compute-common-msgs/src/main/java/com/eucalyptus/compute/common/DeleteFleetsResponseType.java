/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;


public class DeleteFleetsResponseType extends ComputeMessage {

  private DeleteFleetSuccessSet successfulFleetDeletions;
  private DeleteFleetErrorSet unsuccessfulFleetDeletions;

  public DeleteFleetSuccessSet getSuccessfulFleetDeletions( ) {
    return successfulFleetDeletions;
  }

  public void setSuccessfulFleetDeletions( final DeleteFleetSuccessSet successfulFleetDeletions ) {
    this.successfulFleetDeletions = successfulFleetDeletions;
  }

  public DeleteFleetErrorSet getUnsuccessfulFleetDeletions( ) {
    return unsuccessfulFleetDeletions;
  }

  public void setUnsuccessfulFleetDeletions( final DeleteFleetErrorSet unsuccessfulFleetDeletions ) {
    this.unsuccessfulFleetDeletions = unsuccessfulFleetDeletions;
  }

}
