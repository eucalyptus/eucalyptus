// image model
//

define([
  './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    namedColumns: ['id', 'image'], 
    sync: function(method, model, options){
      if(method == 'create'){
        var url = "/ec2?Action=RegisterImage";
        var id = this.get('id');
        var parameter = "_xsrf="+$.cookie('_xsrf');
        parameter += "&Name="+this.get('name');
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
          if (mappings['/dev/sda1']) {
            parameter += "&SnapshotId="+mappings['/dev/sda1'].snapshot_id;
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
