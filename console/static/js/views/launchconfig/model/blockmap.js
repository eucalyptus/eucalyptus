define([], function() {
  return Backbone.Model.extend({
     map_name: "/dev/sda1",
     status: null, 
     __obj_name__: "BlockDeviceType", 
     attach_time: null, 
     no_device: false, 
     volume_id: null, 
     connection: null, 
     snapshot_id: null, 
     size: 0, 
     ebs: "", 
     delete_on_termination: false, 
     ephemeral_name: null,
    
    initialize: function() {

    },

    validation: {
      size: {
        required: true,
        pattern: 'number',
        min: 1,
        msg: 'The volume size must be a positive number.'
      }
    },

    finish: function(outputModel) {
      outputModel.set('block_device_mappings', this.toJSON());
    }
  });
});
