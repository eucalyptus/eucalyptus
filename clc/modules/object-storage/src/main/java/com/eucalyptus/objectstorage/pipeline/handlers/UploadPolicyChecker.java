/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eucalyptus.objectstorage.exceptions.s3.InvalidPolicyDocumentException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.http.MappingHttpRequest;

public class UploadPolicyChecker {
	private static Logger LOG = Logger.getLogger( UploadPolicyChecker.class );

	public static void checkPolicy(Map<String, String> formFields) throws S3Exception {
		if(formFields.containsKey(ObjectStorageProperties.FormField.Policy.toString())) {
			String policy = new String(Base64.decode(formFields.get(ObjectStorageProperties.FormField.Policy.toString())));
			String policyData;
			try {
				policyData = new String(Base64.encode(policy.getBytes()));
			} catch (Exception ex) {
				LOG.warn("Denying POST upload due to inability to decode/read required upload Policy from request", ex);
				throw new InvalidPolicyDocumentException(policy, "Invalid policy content");
			}
			//parse policy
			try {
				JsonSlurper jsonSlurper = new JsonSlurper();
				JSONObject policyObject = (JSONObject)jsonSlurper.parseText(policy);
				String expiration = (String) policyObject.get(ObjectStorageProperties.PolicyHeaders.expiration.toString());
				if(expiration != null) {
					Date expirationDate = DateUtils.parseIso8601DateTimeOrDate(expiration);
					if((new Date()).getTime() > expirationDate.getTime()) {
						LOG.warn("Denying POST upload because included policy has expired.");
						throw new InvalidPolicyDocumentException(expiration, "Expired policy: " + expiration);
					}
				}
				List<String> policyItemNames = new ArrayList<String>();

				JSONArray conditions = (JSONArray) policyObject.get(ObjectStorageProperties.PolicyHeaders.conditions.toString());
				for (int i = 0 ; i < conditions.size() ; ++i) {
					Object policyItem = conditions.get(i);
					if(policyItem instanceof JSONObject) {
						JSONObject jsonObject = (JSONObject) policyItem;
						if(!exactMatch(jsonObject, formFields, policyItemNames)) {
                            LOG.warn("Denying POST upload because Policy verification failed due to mismatch of conditions");
                            throw new InvalidPolicyDocumentException(jsonObject.toString(), "Policy conditions not met");
						}
					} else if(policyItem instanceof  JSONArray) {
						JSONArray jsonArray = (JSONArray) policyItem;
						if(!partialMatch(jsonArray, formFields, policyItemNames)) {
                            LOG.warn("Denying POST upload because Policy verification failed due to mismatch of conditions");
                            throw new InvalidPolicyDocumentException(jsonArray.toString(), "Policy conditions not met");
						}
					}
				}

				Set<String> formFieldsKeys = formFields.keySet();
				for(String formKey : formFieldsKeys) {
					if(formKey.startsWith(ObjectStorageProperties.IGNORE_PREFIX))
						continue;
					boolean fieldOkay = false;
					for(ObjectStorageProperties.IgnoredFields field : ObjectStorageProperties.IgnoredFields.values()) {
						if(formKey.equals(field.toString())) {
							fieldOkay = true;
							break;
						}
					}
					if(fieldOkay)
						continue;
					if(policyItemNames.contains(formKey))
						continue;

                    if(ObjectStorageProperties.FormField.isHttpField(formKey)) {
                        //Allow the http header fields if in the form but not in policy conditions. The S3 spec is ambiguous on this
                        // but the behavior indicates content-type, in particular, is not required in the policy conditions.
                        continue;
                    }
					LOG.warn("Denying POST upload due to: All fields except those marked with x-ignore- should be in policy. Form Key: " + formKey);
					throw new InvalidPolicyDocumentException(formKey, "All fields except those marked with x-ignore- should be in policy.");
				}
            } catch(S3Exception e) {
                //pass thru
                throw e;
			} catch(Exception ex) {
				//rethrow
				LOG.error("Denying POST upload due to: Unexpected exception during POST policy checks", ex);
				throw new InvalidPolicyDocumentException(policy, "Error processing the policy");
			}
        }
	}

	private static boolean exactMatch(JSONObject jsonObject, Map<String, String> formFields, List<String> policyItemNames) throws S3Exception {
		Iterator<String> iterator = jsonObject.keys();
		String key = null;
		boolean returnValue = false;
		while(iterator.hasNext()) {
			key = iterator.next();
			key = key.replaceAll("\\$", "");
			policyItemNames.add(key);
			try {
				if(jsonObject.get(key).equals(formFields.get(key).trim()))
					returnValue = true;
				else
					returnValue = false;
			} catch(Exception ex) {
				LOG.error("Unexpected error evaluating the policy for an exact match", ex);
				return false;
			}
		}
		if(!returnValue)
			LOG.trace("POST upload policy exact match on " + key + " failed");
		return returnValue;
	}

	private static boolean partialMatch(JSONArray jsonArray, Map<String, String> formFields, List<String> policyItemNames) throws S3Exception {
		boolean returnValue = false;
		String key;
		if(jsonArray.size() != 3)
			return false;
		try {
			String condition = (String) jsonArray.get(0);
			key = (String) jsonArray.get(1);
			key = key.replaceAll("\\$", "");
			policyItemNames.add(key);
			String value = (String) jsonArray.get(2);
			if(condition.contains("eq")) {
				if(value.equals(formFields.get(key).trim()))
					returnValue = true;
			} else if(condition.contains("starts-with")) {
				if(!formFields.containsKey(key))
					return false;
				if(formFields.get(key).trim().startsWith(value))
					returnValue = true;
			} else if(condition.equals("content-length-range")) {
                long lower, upper, size;
                String[] rangeValues = value.split(",");
                if(rangeValues.length != 2) {
                    throw new InvalidPolicyDocumentException(value, "content-length-range value could not be parsed properly");
                }
                lower = Long.valueOf(rangeValues[0]);
                upper = Long.valueOf(rangeValues[1]);
                size = Long.valueOf(formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()));
                returnValue = (lower <= size && size <= upper);
            }
		} catch(Exception ex) {
			LOG.error("Unexpected error evaluating the policy for a partial match", ex);
			return false;
		}
		if(!returnValue)
			LOG.trace("POST upload policy partial match on " + key + " failed");
		return returnValue;
	}
}
