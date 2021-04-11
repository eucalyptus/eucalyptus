/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.activities;

/**
 *
 */
public class RdsActivityException extends Exception {

  private static final long serialVersionUID = 1L;

  public RdsActivityException(String message){
    super(message);
  }

  public RdsActivityException(String message, Throwable cause){
    super(message, cause);
  }
}

