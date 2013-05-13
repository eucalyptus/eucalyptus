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
      volume_size: {
        required: false,
        pattern: 'number',
        min: 1,
        max: 1000,
        msg: app.msg("launch_instance_error_volume_size_number")
      }
    },

    finish: function(outputModel) {
      outputModel.set('block_device_mappings', this.toJSON());
    }
  });
});
