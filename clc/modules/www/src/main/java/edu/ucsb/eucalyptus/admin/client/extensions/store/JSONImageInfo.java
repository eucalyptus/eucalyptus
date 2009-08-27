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

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;


public class JSONImageInfo implements ImageInfo {

    private JSONObject object;

    private JSONImageInfo(JSONObject object) {
        this.object = object;
    }
    
    static public ImageInfo fromString(String data) {
        return fromObject(JSONUtil.parseObject(data));
    }

    static public ImageInfo fromObject(JSONObject object) {
        return new JSONImageInfo(object);
    }

    static public List<ImageInfo> fromObjectArray(JSONValue array) {
        return JSONUtil.adaptArray(array, new JSONUtil.ObjectAdapter<ImageInfo>() {
            public ImageInfo adaptObject(JSONObject object) {
                return fromObject(object);
            }
        });
    }
    
    public String getUri() {
        return JSONUtil.asString(object.get("uri"));
    }

    public String getIconUri() {
        return JSONUtil.asString(object.get("icon-uri"));
    }

    public String getTitle() {
        return JSONUtil.asString(object.get("title"));
    }

    public String getSummary() {
        return JSONUtil.asString(object.get("summary"));
    }

    public String getDescriptionHtml() {
        return JSONUtil.asString(object.get("description-html"));
    }

    public String getVersion() {
        return JSONUtil.asString(object.get("version"));
    }

    public Integer getSizeInMB() {
        return JSONUtil.asInteger(object.get("size-in-mb"));
    }

    public List<String> getTags() {
        ArrayList<String> tags = new ArrayList<String>();
        JSONValue value = object.get("tags");
        if (value != null) {
            JSONArray array = value.isArray();
            if (array != null) {
                for (int i = 0; i != array.size(); i++) {
                    String item = JSONUtil.asString(array.get(i));
                    if (item != null) {
                        tags.add(item);
                    }
                }
            }
        }
        return tags;
    }

    public String getProviderTitle() {
        JSONValue providerValue = object.get("provider");
        if (providerValue != null) {
            JSONObject providerObject = providerValue.isObject();
            if (providerObject != null) {
                return JSONUtil.asString(providerObject.get("title"));
            }
        }
        return null;
    }

    public String getProviderUri() {
        JSONValue providerValue = object.get("provider");
        if (providerValue != null) {
            JSONObject providerObject = providerValue.isObject();
            if (providerObject != null) {
                return JSONUtil.asString(providerObject.get("uri"));
            }
        }
        return null;
    }

}
