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
          sync: function(method, model, options){
            var collection = this;
            if(method == 'create'){
              var availability_zone = model.get('availablity_zone');
              var snapshot_id = model.get('snapshot_id');
              var parameter = "_xsrf="+$.cookie('_xsrf');
              parameter += "&Size="+size+"&AvailabilityZone="+availability_zone;
              if(snapshot_id != undefined){
                parameter += "&SnapshotId="+snapshot_id;
              }
              $.ajax({
                type: "POST",
                url: "/ec2?Action=CreateVolume",
                data: parameter,
                dataType: "json",
                async: true,
                success:
                  function(data, textStatus, jqXHR){
                    if(data.results){
                      var volId = data.results.id;
                      notifySuccess(null, $.i18n.prop('volume_create_success', DefaultEncoder().encodeForHTML(volId)));
                    }else{
                      notifyError($.i18n.prop('volume_create_error'), undefined_error);
                    }
                  },
                error:
                  function(jqXHR, textStatus, errorThrown){
                    notifyError($.i18n.prop('volume_create_error'), getErrorMessage(jqXHR));
                  }
                });
              } 
          }

    });
    return model;
});
