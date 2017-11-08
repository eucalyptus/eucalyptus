/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.event_store;

public class ReportingS3ObjectEventStore extends EventStoreSupport
{
  private static ReportingS3ObjectEventStore instance = new ReportingS3ObjectEventStore();

  public static ReportingS3ObjectEventStore getInstance() {
    return instance;
  }

  protected ReportingS3ObjectEventStore() {
  }

  public void insertS3ObjectCreateEvent(String s3BucketName, String s3ObjectKey, String objectVersion,
		  long size, long timestampMs, String userId)
  {
    persist( new ReportingS3ObjectCreateEvent(s3BucketName, s3ObjectKey, objectVersion, size, timestampMs, userId) );
  }

  public void insertS3ObjectDeleteEvent(String s3BucketName, String s3ObjectKey, String objectVersion,
		  long timestampMs)
  {
    persist( new ReportingS3ObjectDeleteEvent(s3BucketName, s3ObjectKey, objectVersion, timestampMs) );
  }
}
