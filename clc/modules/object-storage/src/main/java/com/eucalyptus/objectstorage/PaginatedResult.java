/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple type for describing paginated listings
 *
 * @param <T>
 */
public class PaginatedResult<T> {
  protected List<T> entityList;
  protected List<String> commonPrefixes;
  protected boolean isTruncated;
  protected Object lastEntry;

  public PaginatedResult() {
    this.entityList = new ArrayList<T>();
    this.commonPrefixes = new ArrayList<String>();
    this.isTruncated = false;
  }

  public PaginatedResult(List<T> entries, List<String> commonPrefixes, boolean truncated) {
    this.entityList = entries;
    this.commonPrefixes = commonPrefixes;
    this.isTruncated = truncated;
  }

  public List<T> getEntityList() {
    return entityList;
  }

  public void setEntityList(List<T> entityList) {
    this.entityList = entityList;
  }

  public List<String> getCommonPrefixes() {
    return commonPrefixes;
  }

  public void setCommonPrefixes(List<String> commonPrefixes) {
    this.commonPrefixes = commonPrefixes;
  }

  public boolean getIsTruncated() {
    return this.isTruncated;
  }

  public void setIsTruncated(boolean t) {
    this.isTruncated = t;
  }

  /**
   * Returns the last object in the listing, Can be either String or type T. Used to populate the "next" reference
   * 
   * @return
   */
  public Object getLastEntry() {
    return this.lastEntry;
  }

  public void setLastEntry(Object last) {
    this.lastEntry = last;
  }
}
