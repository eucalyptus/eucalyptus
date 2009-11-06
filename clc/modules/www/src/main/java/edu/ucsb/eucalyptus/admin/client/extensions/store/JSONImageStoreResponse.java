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


public class JSONImageStoreResponse implements ImageStoreResponse {

    private final boolean hasImageInfos;
    private final boolean hasImageStates;
    private final boolean hasImageSections;

    private final List<ImageInfo> imageInfos;
    private final List<ImageState> imageStates;
    private final List<ImageSection> imageSections;

    private String errorMessage;

    private JSONImageStoreResponse(JSONObject object) {
        final JSONValue imagesValue = object.get("images");
        hasImageInfos = (imagesValue != null);
        imageInfos = JSONImageInfo.fromObjectArray(imagesValue);

        final JSONValue statesValue = object.get("states");
        hasImageStates = (statesValue != null);
        imageStates = JSONImageState.fromObjectArray(statesValue);

        if (object.containsKey("state") && object.isObject() != null) {
            // When we access a state URI or perform an action, we get
            // back an individual state, but we handle it the same way
            // in the UI.
            JSONObject stateObject = object.get("state").isObject();
            imageStates.add(JSONImageState.fromObject(stateObject));
        }

        final JSONValue sectionsValue = object.get("sections");
        hasImageSections = (sectionsValue != null);
        imageSections = JSONImageSection.fromObjectArray(sectionsValue);

        errorMessage = JSONUtil.asString(object.get("error-message"));
    }

    static public ImageStoreResponse fromString(String data) {
        return fromObject(JSONUtil.parseObject(data));
    }

    static public ImageStoreResponse fromObject(JSONObject object) {
        return new JSONImageStoreResponse(object);
    }

    public boolean hasImageInfos() {
        return hasImageInfos;
    }

    public boolean hasImageStates() {
        return hasImageStates;
    }

    public boolean hasImageSections() {
        return hasImageSections;
    }

    public List<ImageInfo> getImageInfos() {
        return imageInfos;
    }

    public List<ImageState> getImageStates() {
        return imageStates;
    }

    public List<ImageSection> getImageSections() {
        return imageSections;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
