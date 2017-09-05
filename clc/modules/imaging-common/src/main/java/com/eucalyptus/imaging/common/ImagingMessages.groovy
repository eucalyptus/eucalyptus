/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
@GroovyAddClassUUID
package com.eucalyptus.imaging.common

import com.eucalyptus.ws.WebServiceError;

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.component.annotation.ComponentMessage
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

@ComponentMessage(Imaging.class)
public class ImagingMessage extends BaseMessage implements Cloneable, Serializable {
  
  public ImagingMessage( ) {
    super( );
  }
  
  public ImagingMessage( BaseMessage msg ) {
    super( msg );
  }
  
  public ImagingMessage( String userId ) {
    super( userId );
  }
}

/** *******************************************************************************/
public class ImportImageType extends ImagingMessage {
  String description;
  
  ImportDiskImage image;
  public ImportImage() {}
}

public class ImportImageResponseType extends ImagingMessage {
  DiskImageConversionTask conversionTask;
  public ImportImageResponse() {}
}

public class ImportDiskImage extends EucalyptusData {
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "ImportDiskImageDetail")
  ArrayList<ImportDiskImageDetail> diskImageSet = new ArrayList<ImportDiskImageDetail>();

  ConvertedImageDetail convertedImage;
  String description;
  String accessKey;
  String uploadPolicy;
  String uploadPolicySignature;
  
  public ImportDiskImage() {}

  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("description", description);
    for(ImportDiskImageDetail disk:diskImageSet)
      obj.accumulate("diskImageSet", disk.toJSON());
    if (convertedImage != null)
      obj.put("convertedImage", convertedImage.toJSON());
    obj.put("accessKey", accessKey);
    obj.put("uploadPolicy", uploadPolicy);
    obj.put("uploadPolicySignature", uploadPolicySignature);
    return obj;
  }

  public ImportDiskImage(JSONObject obj) {
    if (obj != null) {
      JSONArray arr = obj.optJSONArray("diskImageSet");
      if (arr != null) {
        for(int i=0;i<arr.size(); i++)
          diskImageSet.add(new ImportDiskImageDetail(arr.getJSONObject(i)));
      } else {
        JSONObject disk = obj.optJSONObject("diskImageSet");
        if (disk!=null)
          diskImageSet.add(new ImportDiskImageDetail(disk));
      }

      description = obj.optString("description", null);
      JSONObject convertObj  = obj.optJSONObject("convertedImage");
      if (convertObj != null)
        convertedImage = new ConvertedImageDetail(convertObj);
      accessKey = obj.optString("accessKey", null);
      uploadPolicy = obj.optString("uploadPolicy", null);
      uploadPolicySignature = obj.optString("uploadPolicySignature", null);
    }
  }
}

public class ConvertedImageDetail extends EucalyptusData {
  String bucket;
  String prefix;
  String architecture;
  String imageId;
  public ConvertedImageDetail() {}
  
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("bucket", bucket);
    obj.put("prefix", prefix);
    obj.put("architecture", architecture);
    obj.put("imageId", imageId);
    return obj;
  }
  
  public ConvertedImageDetail(JSONObject obj){
    if(obj!=null){
      bucket = obj.optString("bucket", null);
      prefix = obj.optString("prefix", null);
      architecture = obj.optString("architecture", null);
      imageId = obj.optString("imageId", null);
    }
  }
}

public class ImportDiskImageDetail extends EucalyptusData {
  String id
  String format;
  Long bytes;
  String downloadManifestUrl;
  public ImportDiskImageDetail() {}
  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("id", id);
    obj.put("format", format);
    obj.put("bytes", bytes);
    obj.put("downloadManifestUrl", downloadManifestUrl)
    return obj;
    }
  
  public ImportDiskImageDetail(JSONObject obj){
    if(obj!=null){
      id = obj.optString("id", null);
      format = obj.optString("format", null);
      bytes = obj.optLong("bytes", 0L);
      downloadManifestUrl=obj.optString("downloadManifestUrl", null);
    }
  }
}

public class DiskImageConversionTask extends EucalyptusData {
  String conversionTaskId;
  String expirationTime;
  String state;
  String statusMessage;
  ImportDiskImage importDisk;
  
  public DiskImageConversionTask() {}
  public DiskImageConversionTask(JSONObject obj){
    if(obj!=null){
      conversionTaskId = obj.optString("conversionTaskId");
      expirationTime = obj.optString("expirationTime");
      JSONObject diskDetail = obj.optJSONObject("importDisk");
      if (diskDetail != null)
        importDisk = new ImportDiskImage(diskDetail);

      state = obj.optString("state", null);
      statusMessage = obj.optString("statusMessage", null);
    }
  }
  
  public JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("conversionTaskId", conversionTaskId);
    obj.put("expirationTime", expirationTime);
    if (importDisk != null)
      obj.put("importDisk", importDisk.toJSON())
    obj.put("state", state);
    obj.put("statusMessage", statusMessage);
    return obj;
  }
}

/*********************************************************************************/
public class DescribeConversionTasksType extends ImagingMessage {
  @HttpParameterMapping (parameter = "ConversionTaskId")
  ArrayList<String> conversionTaskIdSet = new ArrayList<String>();
  public DescribeConversionTasksType() {}
}
public class DescribeConversionTasksResponseType extends ImagingMessage {
  ArrayList<DiskImageConversionTask> conversionTasks = new ArrayList<DiskImageConversionTask>();
  public DescribeConversionTasksResponseType() {}
}

/*********************************************************************************/
public class CancelConversionTaskType extends ImagingMessage {
  String conversionTaskId;
  public CancelConversionTask() {}
}
public class CancelConversionTaskResponseType extends ImagingMessage {
  Boolean _return;
  public CancelConversionTaskResponse() {}
}
/*********************************************************************************/

public class PutInstanceImportTaskStatusResponseType extends ImagingMessage {
  Boolean cancelled
}

public class PutInstanceImportTaskStatusType extends ImagingMessage {
  String instanceId
  String importTaskId
  String status
  String errorCode;
  String volumeId
  String message
  Long bytesConverted
}

public class GetInstanceImportTaskResponseType extends ImagingMessage {
  String importTaskId
  String importTaskType
  VolumeTask volumeTask
  InstanceStoreTask instanceStoreTask
  public GetInstanceImportTaskResponse() {}
}

public class VolumeTask extends EucalyptusData {
  String volumeId

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "ImageManifest")
  ArrayList<ImageManifest> imageManifestSet = new ArrayList<ImageManifest>();
  
  public VolumeTask() {}
}

public class InstanceStoreTask extends EucalyptusData {
  String accountId;
  String accessKey;
  String uploadPolicy;
  String uploadPolicySignature;
  String s3Url;
  String serviceCertArn;
  
  @HttpEmbedded(multiple = true)
  @HttpParameterMapping (parameter = "ImportImage")
  ArrayList<ImportDiskImageDetail> importImageSet = new ArrayList<ImportDiskImageDetail>();
  ConvertedImageDetail convertedImage;
  public InstanceStoreTask() {}
}

public class ImageManifest extends EucalyptusData {
  String manifestUrl
  String format
  public ImageManifest() {}
}

public class GetInstanceImportTaskType extends ImagingMessage {
  String instanceId;
}

public class Error extends EucalyptusData {
  String type
  String code
  String message
  public Error() {  }
  ErrorDetail detail = new ErrorDetail()
}

public class ErrorResponse extends ImagingMessage implements WebServiceError {
  String requestId
  ArrayList<Error> error = new ArrayList<Error>( )

  ErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    error?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    error?.getAt(0)?.message
  }
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
