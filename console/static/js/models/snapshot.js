// snapshot model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        idAttribute: 'name',
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
            var collection = this;
            if(method == 'create'){
              var volume_id = $.trim(model.get('volume_id'));
              var description = toBase64($.trim(model.get('description')));
              var parameter = "_xsrf="+$.cookie('_xsrf');
              parameter += "&VolumeId="+volume_id+"&Description="+description;
              console.log("Model Snapshot Sync Create: " + parameter);
              $.ajax({
                type: "POST",
                url: "/ec2?Action=CreateSnapshot",
                data: parameter,
                dataType: "json",
                async: true,
                success: options.success,
                error: options.error
                });
              }
          }    
    });
    return model;
});
