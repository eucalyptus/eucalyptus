/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class CreateFpgaImageType extends ComputeMessage {

  private String clientToken;
  private String description;
  private Boolean dryRun;
  @Nonnull
  private StorageLocation inputStorageLocation;
  private StorageLocation logsStorageLocation;
  private String name;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public Boolean getDryRun( ) {
    return dryRun;
  }

  public void setDryRun( final Boolean dryRun ) {
    this.dryRun = dryRun;
  }

  public StorageLocation getInputStorageLocation( ) {
    return inputStorageLocation;
  }

  public void setInputStorageLocation( final StorageLocation inputStorageLocation ) {
    this.inputStorageLocation = inputStorageLocation;
  }

  public StorageLocation getLogsStorageLocation( ) {
    return logsStorageLocation;
  }

  public void setLogsStorageLocation( final StorageLocation logsStorageLocation ) {
    this.logsStorageLocation = logsStorageLocation;
  }

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

}
