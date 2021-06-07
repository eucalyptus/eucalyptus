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
package com.eucalyptus.imaging.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import net.sf.json.JSONObject;

public class DiskImageConversionTask extends EucalyptusData {

  private String conversionTaskId;
  private String expirationTime;
  private String state;
  private String statusMessage;
  private ImportDiskImage importDisk;

  public DiskImageConversionTask( ) {
  }

  public DiskImageConversionTask( JSONObject obj ) {
    if ( obj != null ) {
      conversionTaskId = obj.optString( "conversionTaskId" );
      expirationTime = obj.optString( "expirationTime" );
      JSONObject diskDetail = obj.optJSONObject( "importDisk" );
      if ( diskDetail != null ) importDisk = new ImportDiskImage( diskDetail );

      state = obj.optString( "state", null );
      statusMessage = obj.optString( "statusMessage", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "conversionTaskId", conversionTaskId );
    obj.put( "expirationTime", expirationTime );
    if ( importDisk != null ) obj.put( "importDisk", importDisk.toJSON( ) );
    obj.put( "state", state );
    obj.put( "statusMessage", statusMessage );
    return obj;
  }

  public String getConversionTaskId( ) {
    return conversionTaskId;
  }

  public void setConversionTaskId( String conversionTaskId ) {
    this.conversionTaskId = conversionTaskId;
  }

  public String getExpirationTime( ) {
    return expirationTime;
  }

  public void setExpirationTime( String expirationTime ) {
    this.expirationTime = expirationTime;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public ImportDiskImage getImportDisk( ) {
    return importDisk;
  }

  public void setImportDisk( ImportDiskImage importDisk ) {
    this.importDisk = importDisk;
  }
}
