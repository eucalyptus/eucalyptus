package edu.ucsb.eucalyptus.admin.client.ImageStore;

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
