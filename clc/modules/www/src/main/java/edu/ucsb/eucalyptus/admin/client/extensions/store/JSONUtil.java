/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
package edu.ucsb.eucalyptus.admin.client.extensions.store;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.core.client.GWT;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONArray;


public class JSONUtil {

    static public JSONObject parseObject(String data) {
        JSONObject jsonObject = null;
        try {
            JSONValue jsonValue = JSONParser.parse(data);
            jsonObject = jsonValue.isObject();
        } catch (JSONException e) {
            GWT.log("Tried to parse bad JSON data", e);
            JSONValue jsonValue = JSONParser.parse(
                "{\"error-message\": \"Received bad JSON data.\"}");
            jsonObject = jsonValue.isObject();
        }
        return jsonObject;
    }


    static String asString(JSONValue value) {
        if (value != null) {
            JSONString string = value.isString();
            if (string != null) {
                return string.stringValue();
            }
        }
        return null;
    }

    static Integer asInteger(JSONValue value) {
        if (value != null) {
            JSONNumber number = value.isNumber();
            if (number != null) {
                return (int)number.doubleValue();
            }
        }
        return null;
    }

    static int asInt(JSONValue value, int defaultResult) {
        if (value != null) {
            JSONNumber number = value.isNumber();
            if (number != null) {
                return (int)number.doubleValue();
            }
        }
        return defaultResult;
    }

    static boolean asBoolean(JSONValue value, boolean defaultResult) {
        if (value != null) {
            JSONBoolean bool = value.isBoolean();
            if (bool != null) {
                return bool.booleanValue();
            }
        }
        return defaultResult;
    }


    public static interface ObjectAdapter<T> {
        T adaptObject(JSONObject object);
    }

    public static <T> List<T> adaptArray(JSONValue arrayValue,
                                         ObjectAdapter<T> adapter) {
        List<T> result = new ArrayList<T>();
        if (arrayValue != null && arrayValue.isArray() != null) {
            JSONArray array = arrayValue.isArray();
            for (int i = 0; i != array.size(); i++) {
                JSONObject object = array.get(i).isObject();
                if (object != null) {
                    result.add(adapter.adaptObject(object));
                }
            }
        }
        return result;
    }

}
