define(['app'], function(app) {
  return Backbone.Model.extend({
     device_name: "/dev/sda1",
     status: null, 
     __obj_name__: "BlockDeviceType", 
     attach_time: null, 
     no_device: false, 
     volume_id: null, 
     snapshot_id: null, 
     volume_size: null, 
     ebs: "", 
     delete_on_termination: false, 
     ephemeral_name: null,
    
    initialize: function() {
    },

    validation: {
      volume_size: function(value) {
        if (this.get('snapshot_id') != undefined) {
          if (value.length <= 0 || (parseInt(value)<=0 || isNaN(value))){
            return app.msg("launch_instance_error_volume_size_number");
          }
        }
        /*
        var snapshotSize = -1;
          if(volume==='ebs'){
          //find the size of the chosen snapshot;
            var result = describe('snapshot');
            for (i in result){
              var s = result[i]; 
              if(s.id === snapshot){
                snapshotSize = s.volume_size;
                break;
              }
            }
          }else if (emi){ //root volume
            var image = describe('image', emi);
            if(image['block_device_mapping'] && image['block_device_mapping']['/dev/sda']) 
             snapshotSize = parseInt(image['block_device_mapping']['/dev/sda']['size']);
          }
          if(snapshotSize > size){
            thisObj.element.find('.field-error').remove();
            $($cells[3]).append($('<div>').addClass('field-error').html(launch_instance_advanced_error_dev_size));
            return false;
          }
          */
      },
      snapshot_id: function(value) {
        if (this.get('volume_size') != undefined) {
        }
      }
    },

    finish: function(outputModel) {
      outputModel.set('block_device_mappings', this.toJSON());
    }
  });
});
