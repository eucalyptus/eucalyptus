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
package com.eucalyptus.walrus.msgs;

import java.util.Date;

public class GetObjectExtendedType extends WalrusDataGetRequestType {

  private Boolean getData;
  private Boolean getMetaData;
  private Boolean inlineData;
  private Long byteRangeStart;
  private Long byteRangeEnd;
  private Date ifModifiedSince;
  private Date ifUnmodifiedSince;
  private String ifMatch;
  private String ifNoneMatch;
  private Boolean returnCompleteObjectOnConditionFailure;

  public Boolean getGetData( ) {
    return getData;
  }

  public void setGetData( Boolean getData ) {
    this.getData = getData;
  }

  public Boolean getGetMetaData( ) {
    return getMetaData;
  }

  public void setGetMetaData( Boolean getMetaData ) {
    this.getMetaData = getMetaData;
  }

  public Boolean getInlineData( ) {
    return inlineData;
  }

  public void setInlineData( Boolean inlineData ) {
    this.inlineData = inlineData;
  }

  public Long getByteRangeStart( ) {
    return byteRangeStart;
  }

  public void setByteRangeStart( Long byteRangeStart ) {
    this.byteRangeStart = byteRangeStart;
  }

  public Long getByteRangeEnd( ) {
    return byteRangeEnd;
  }

  public void setByteRangeEnd( Long byteRangeEnd ) {
    this.byteRangeEnd = byteRangeEnd;
  }

  public Date getIfModifiedSince( ) {
    return ifModifiedSince;
  }

  public void setIfModifiedSince( Date ifModifiedSince ) {
    this.ifModifiedSince = ifModifiedSince;
  }

  public Date getIfUnmodifiedSince( ) {
    return ifUnmodifiedSince;
  }

  public void setIfUnmodifiedSince( Date ifUnmodifiedSince ) {
    this.ifUnmodifiedSince = ifUnmodifiedSince;
  }

  public String getIfMatch( ) {
    return ifMatch;
  }

  public void setIfMatch( String ifMatch ) {
    this.ifMatch = ifMatch;
  }

  public String getIfNoneMatch( ) {
    return ifNoneMatch;
  }

  public void setIfNoneMatch( String ifNoneMatch ) {
    this.ifNoneMatch = ifNoneMatch;
  }

  public Boolean getReturnCompleteObjectOnConditionFailure( ) {
    return returnCompleteObjectOnConditionFailure;
  }

  public void setReturnCompleteObjectOnConditionFailure( Boolean returnCompleteObjectOnConditionFailure ) {
    this.returnCompleteObjectOnConditionFailure = returnCompleteObjectOnConditionFailure;
  }
}
