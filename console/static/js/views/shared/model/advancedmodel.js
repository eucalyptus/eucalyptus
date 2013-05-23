define([], function() {
  return Backbone.Model.extend({
    kernel_id: null,
    ramdisk_id: null,
    user_data: null,
    instance_monitoring: true,
    private_network: false,

    initialize: function() {

    },

    finish: function(outputModel) {
      this.set('monitoring_enabled', this.get('instance_monitoring'));
      this.set('addressing_type', (this.get('private_network')) ? 'private' : 'public');
      this.set('placement', {availability_zone: this.get('zone'), group_name: null, tenancy: null});
      outputModel.set(this.toJSON());
    }
  });
});
