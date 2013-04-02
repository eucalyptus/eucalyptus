// launchconfig model
//

define([
  './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    idAttribute: 'name',

    validation: {

      // ====================
      // API Reference: http://docs.aws.amazon.com/AWSSDKforPHP/latest/index.html#m=AmazonAS/create_launch_configuration
      // ====================         

      name: {
        rangeLength: [1, 255],
        required: true
      },
      image_id: {
        rangeLength: [1, 255],
        required: true
      },
      instance_type: {
        rangeLength: [1, 255],
        required: true
      },
      key_name: {
        rangeLength: [1, 255],
        required: false
      },
      security_groups: {
        required: false
      },
      user_data: {
        rangeLength: [0, 21847],
        required: false
      },
      kernel_id: {
        rangeLength: [1, 255],
        required: false
      },
      ramdisk_id: {
        rangeLength: [1, 255],
        required: false
      },
      block_device_mappings: {
        required: false
      },
      instance_monitoring: {
        required: false
      },
      spot_price: {
        pattern: /\d+\.\d+/,
        required: false
      },
      iam_instance_profile: {
        rangeLength: [1, 1600],
        required: false
      },
      ebs_optimized: {
        required: false
      },
      curlopts: {
        required: false
      },
      return_curl_handle: {
        oneOf: [ 'true', 'false' ],
        required: false
      },
    },

    sync: function(method, model, options) {
      var collection = this;
        if (method == 'create') {
          var name = model.get('name');
          var data = "_xsrf="+$.cookie('_xsrf');
          data += "&LaunchConfigurationName="+name;
          if (model.get('image_id') != undefined)
            data += "&ImageId="+model.get('image_id');
          if (model.get('key_name') != undefined)
            data += "&KeyName="+model.get('key_name');
          if (model.get('user_data') != undefined)
            data += "&UserData="+model.get('user_data');
          if (model.get('instance_type') != undefined)
            data += "&InstanceType="+model.get('instance_type');
          if (model.get('kernel_id') != undefined)
            data += "&KernelId="+model.get('kernel_id');
          if (model.get('ramdisk_id') != undefined)
            data += "&RamdiskId="+model.get('ramdisk_id');
          //TODO: block device mapping
          if (model.get('instance_monitoring') != undefined)
            data += "&InstanceMonitoring="+model.get('instance_monitoring');
          if (model.get('spot_price') != undefined)
            data += "&SpotPrice="+model.get('spot_price');
          if (model.get('instance_profile_name') != undefined)
            data += "&IamInstanceProfile="+model.get('instance_profile_name');
          $.ajax({
            type:"POST",
            url:"/autoscaling?Action=CreateLaunchConfiguration",
            data:data,
            dataType:"json",
            async:true,
            success:
              function(data, textStatus, jqXHR){
                if ( data.results ) {
                  notifySuccess(null, $.i18n.prop('create_launch_config_run_success', DefaultEncoder().encodeForHTML(volumeId), DefaultEncoder().encodeForHTML(instanceId)));
                  thisObj.tableWrapper.eucatable('refreshTable');
                } else {
                  notifyError($.i18n.prop('create_launch_config_run_error', DefaultEncoder().encodeForHTML(model.name), DefaultEncoder().encodeForHTML(model.name)), undefined_error);
                }
              },
            error:
              function(jqXHR, textStatus, errorThrown){
                notifyError($.i18n.prop('create_launch_config_run_error', DefaultEncoder().encodeForHTML(model.name), DefaultEncoder().encodeForHTML(model.name)), getErrorMessage(jqXHR));
              }
          });
        }
        else if (method == 'delete') {
          var name = model.get('name');
          $.ajax({
            type:"POST",
            url:"/autoscaling?Action=DeleteLaunchConfiguration",
            data:"_xsrf="+$.cookie('_xsrf')+"&LaunchConfigurationName="+name,
            dataType:"json",
            async:true,
            success:
              function(data, textStatus, jqXHR){
                if ( data.results ) {
                  notifySuccess(null, $.i18n.prop('delete_launch_config_success', DefaultEncoder().encodeForHTML(name)));
                } else {
                  notifyError($.i18n.prop('delete_launch_config_error', DefaultEncoder().encodeForHTML(name)), undefined_error);
                }
              },
            error:
              function(jqXHR, textStatus, errorThrown){
                notifyError($.i18n.prop('delete_launch_config_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
              }
          });
        }
      }
  });
  return model;
});
