// snapshot model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        namedColumns: ['id', 'volume_id'],
        validation: {

            // ====================
            // APT Reference:
            // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-CreateSnapshot.html
            // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DeleteSnapshot.html
            // ====================

            snapshot_id: {
              required: false
            },
            volume_id: {
              required: false
            },
            status: {
              oneOf: ['pending','completed', 'error'],
              required: false
            },
            start_time: {
              pattern: /^\d{4}-\d{2}-\d{2}T\d{2}\:\d{2}\:\d{2}\.\w+/,
              required: false
            },
            progress: {
              required: false
            },
            owner_id: {
              required: false
            },
            volume_size: {
              min: 1,
              max: 1024,
              required: false
            },
            description: {
              rangeLength: [0, 255],
              required: false
            },
        },
        sync: function(method, model, options){
          if(method == 'create'){
            this.syncMethod_Create(model, options);  
          }else if(method == 'delete'){
            this.syncMethod_Delete(model, options);
          }
        },
        syncMethod_Create: function(model, options){
          var url = "/ec2?Action=CreateSnapshot";
          var volume_id = $.trim(model.get('volume_id'));
          var description = toBase64($.trim(model.get('description')));
          var parameter = "_xsrf="+$.cookie('_xsrf');
          parameter += "&VolumeId="+volume_id+"&Description="+description;
          this.makeAjaxCall(url, parameter, options);
        },
        syncMethod_Delete: function(model, options){
          var url = "/ec2?Action=DeleteSnapshot";
          var id = model.get('id');
          var parameter = "_xsrf="+$.cookie('_xsrf');
          parameter += "&SnapshotId="+id;
          this.makeAjaxCall(url, parameter, options);
        },

        registerSnapshot: function(name, desc, isWindows, options){
          var url = "/ec2?Action=RegisterImage";
          var id = this.get('id');
          var parameter = "_xsrf="+$.cookie('_xsrf');
          parameter += "&SnapshotId="+id+"&Name="+name+"&Description="+toBase64($.trim(desc));
          if(isWindows){
            parameter += "&KernelId=windows";
          }
          this.makeAjaxCall(url, parameter, options);
        },
    
    });
    return model;
});
