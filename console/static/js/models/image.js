// image model
//

define([
  './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    namedColumns: ['id', 'image'], 
    initialize: function() {
      if(!this.get('platform')) {
        this.set('platform', 'linux');
      }
      EucaModel.prototype.initialize.call(this);
    },
    sync: function(method, model, options){
      if(method == 'create'){
        var url = "/ec2?Action=RegisterImage";
        var parameter = "_xsrf="+$.cookie('_xsrf');
        parameter += "&Name="+encodeURIComponent(this.get('name'));
        if (this.get('description')) {
          parameter += "&Description="+toBase64($.trim(this.get('description')));
        }
        // either instance store
        if (this.get('location')) {
          parameter += "&ImageLocation="+this.get('location');
        }
        // or ebs backed
        else {
          var mappings = this.get('block_device_mapping');
          if (mappings['/dev/sda']) {
            parameter += "&BlockDeviceMapping.1.DeviceName=/dev/sda";
            parameter += "&BlockDeviceMapping.1.Ebs.SnapshotId="+mappings['/dev/sda'].snapshot_id;
            if (mappings['/dev/sda'].delete_on_termination) {
              parameter += "&BlockDeviceMapping.1.Ebs.DeleteOnTermination="+mappings['/dev/sda'].delete_on_termination;
            }
          }
        }
        if(this.get('platform') == 'windows'){
          parameter += "&KernelId=windows";
        }
        this.makeAjaxCall(url, parameter, options);
      }
      else if(method == 'delete'){
        var url = "/ec2?Action=DeregisterImage";
        var id = this.get('id');
        var parameter = "_xsrf="+$.cookie('_xsrf')+"&ImageId="+id;
        this.makeAjaxCall(url, parameter, options);
      }
    },
  });
  return model;
});
