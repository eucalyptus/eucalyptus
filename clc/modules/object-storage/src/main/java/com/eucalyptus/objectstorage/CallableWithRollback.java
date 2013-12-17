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

import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;

/**
 * An wrapper for an operation with two phases:
 * call()
 * rollback()
 * 
 * To allow passing of operations (like Callable) with
 * another rollback option if necessary.
 * @author zhill
 *
 */
public interface CallableWithRollback<T,R> {
	/**
	 * Do the operation
	 * @return
	 * @throws Exception
	 */
	public abstract T call() throws S3Exception, Exception;
	
	/**
	 * Rollback the previous call.
	 * @return
	 * @throws Exception
	 */
	public abstract R rollback(T arg) throws S3Exception, Exception;
}
