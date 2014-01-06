/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
	 * Returns the last object in the listing, Can be either String or type T.
	 * Used to populate the "next" reference
	 * @return
	 */
	public Object getLastEntry() {
		return this.lastEntry;		
	}
	
	public void setLastEntry(Object last) {
		this.lastEntry = last;
	}
}