// volume model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
          idAttribute: 'name',
          namedColumns: ['id','snapshot_id'],
          validation: {

            // ====================
            // API Reference: 
            // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-CreateVolume.html
            // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DeleteVolume.html
            // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-AttachVolume.html
            // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DetachVolume.html
            // ====================

            volume_id: {
              required: false
            },
            status: {
              oneOf: ['attaching','attached', 'detaching', 'detached'],
              required: false
            },
            size: {
              min: 1,
              max: 1024,
              required: false
            },
            instance_id: {
              required: false
            },
            device: {
              required: false
            },
            attach_time: {
              pattern: /^\d{4}-\d{2}-\d{2}T\d{2}\:\d{2}\:\d{2}\.\w+/,
              required: false
            },
            force: {
              oneOf: ['true', 'false'],
              required: false
            },
            availablity_zone: {
              required: false
            },
            create_time: {
              pattern: /^\d{4}-\d{2}-\d{2}T\d{2}\:\d{2}\:\d{2}\.\w+/,
              required: false
            },
            volume_type: {
              oneOf: ['standard', 'io1'],
              required: false
            },
            iops: {
              min: 100,
              max: 2000,
              required: false
            },
            snapshot_id: {
              required: false
            },
          },
          makeAjaxCall: function(url, param, options){
            $.ajax({
                type: "POST",
                url: url,
                data: param,
                dataType: "json",
                async: true,
                success: options.success,
                error: options.error
            });
          },
          sync: function(method, model, options){
            if(method == 'create'){
              this.syncMethod_Create(model, options);
            }else if(method == 'delete'){
              this.syncMethod_Delete(model, options);
            }else if(method == 'attach'){
              this.syncMethod_Attach(model, options);
            }else if(method == 'detach'){
              this.syncMethod_Detach(model, options);
            }
          },
          syncMethod_Create: function(model, options){
            var url = "/ec2?Action=CreateVolume";
            var availability_zone = model.get('availablity_zone');
            var snapshot_id = model.get('snapshot_id');
            var parameter = "_xsrf="+$.cookie('_xsrf');
            parameter += "&Size="+size+"&AvailabilityZone="+availability_zone;
            if(snapshot_id != undefined){
              parameter += "&SnapshotId="+snapshot_id;
            }
            this.makeAjaxCall(url, parameter, options);
          },
          syncMethod_Delete: function(model, options){
            var url = "/ec2?Action=DeleteVolume";
            var id = model.get('id');
            var parameter = "_xsrf="+$.cookie('_xsrf');
            parameter += "&VolumeId="+id;
            this.makeAjaxCall(url, parameter, options);
          },
          syncMethod_Attach: function(model, options){
            var url = "/ec2?Action=AttachVolume";
            var volume_id = model.get('volume_id');
            var instance_id = model.get('instance_id');
            var device = model.get('device');
            var parameter = "_xsrf="+$.cookie('_xsrf');
            parameter += "&VolumeId="+volume_id+"&InstanceId="+instance_id+"&Device="+device;
            this.makeAjaxCall(url, parameter, options);
          },
          syncMethod_Detach: function(model, options){
            var url = "/ec2?Action=DetachVolume";
            var volume_id = model.get('id');             // Need consistency in ID label  -- Kyo 040813
            var parameter = "_xsrf="+$.cookie('_xsrf');
            parameter += "&VolumeId="+volume_id;
            this.makeAjaxCall(url, parameter, options);
          },

    });
    return model;
});
