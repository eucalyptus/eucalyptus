/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
