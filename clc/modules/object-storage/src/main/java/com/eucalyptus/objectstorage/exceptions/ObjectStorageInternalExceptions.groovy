/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.exceptions

class ObjectStorageInternalException extends RuntimeException {
  public ObjectStorageInternalException() {}

  public ObjectStorageInternalException(String msg) {
    super(msg);
  }

  public ObjectStorageInternalException(Throwable cause) {
    super(cause);
  }

  public ObjectStorageInternalException(String msg, Throwable cause) {
    super(msg, cause);
  }
}

/**
 * A metadata operation could not be completed. Either the db failed
 * or something prevented the update from committing. This is not a conflict or
 * state exception
 */
class MetadataOperationFailureException extends ObjectStorageInternalException {
  public MetadataOperationFailureException() {}

  public MetadataOperationFailureException(String msg) {
    super(msg);
  }

  public MetadataOperationFailureException(Throwable cause) {
    super(cause);
  }

  public MetadataOperationFailureException(String msg, Throwable cause) {
    super(msg, cause);
  }
}

class NoSuchEntityException extends ObjectStorageInternalException {
  public NoSuchEntityException() {}

  public NoSuchEntityException(String msg) {
    super(msg);
  }

  public NoSuchEntityException(Throwable cause) {
    super(cause);
  }

  public NoSuchEntityException(String msg, Throwable cause) {
    super(msg, cause);
  }
}

/**
 * The current state of the resource is not compatible with the requested
 * update. Either a state-machine does not allow the transition or some state
 * of the entity prohibits an update
 */
class IllegalResourceStateException extends ObjectStorageInternalException {
  String expected;
  String found;

  public IllegalResourceStateException() {}

  public IllegalResourceStateException(String msg) {
    super(msg);
  }

  public IllegalResourceStateException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public IllegalResourceStateException(String msg, Throwable cause, String expectedState, String foundState) {
    super(msg, cause);
    expected = expectedState;
    found = foundState;
  }
}
