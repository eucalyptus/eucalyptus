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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
