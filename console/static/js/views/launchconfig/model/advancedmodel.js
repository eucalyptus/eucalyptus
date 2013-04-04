define([], function() {
  return Backbone.Model.extend({
    kernel_id: null,
    ramdisk_id: null,
    user_data: null,
    instance_monitoring: true,
    block_device_mappings: [],

    initialize: function() {

    },

    finish: function(outputModel) {
      outputModel.set(this.toJSON());
    }
  });
});
