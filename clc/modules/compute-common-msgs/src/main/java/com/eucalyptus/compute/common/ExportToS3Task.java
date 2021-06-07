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
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ExportToS3Task extends EucalyptusData {

  private String diskImageFormat;
  private String containerFormat;
  private String s3Bucket;
  private String s3Prefix;

  public void ExportToS3TaskType( ) {
  }

  public String getDiskImageFormat( ) {
    return diskImageFormat;
  }

  public void setDiskImageFormat( String diskImageFormat ) {
    this.diskImageFormat = diskImageFormat;
  }

  public String getContainerFormat( ) {
    return containerFormat;
  }

  public void setContainerFormat( String containerFormat ) {
    this.containerFormat = containerFormat;
  }

  public String getS3Bucket( ) {
    return s3Bucket;
  }

  public void setS3Bucket( String s3Bucket ) {
    this.s3Bucket = s3Bucket;
  }

  public String getS3Prefix( ) {
    return s3Prefix;
  }

  public void setS3Prefix( String s3Prefix ) {
    this.s3Prefix = s3Prefix;
  }
}
