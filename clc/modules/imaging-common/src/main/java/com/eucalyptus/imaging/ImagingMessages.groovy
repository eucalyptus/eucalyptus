/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
@GroovyAddClassUUID
package com.eucalyptus.imaging;

import java.io.Serializable;
import java.util.ArrayList;

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.component.annotation.ComponentMessage
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
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
  
  @HttpEmbedded
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

  @HttpEmbedded
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

public class DiskImageConversionTask {
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
  
  @Override
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
  String ec2Cert;
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

public class ErrorResponse extends ImagingMessage {
  String requestId
  public ErrorResponse() {
  }
  ArrayList<Error> error = new ArrayList<Error>()
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
