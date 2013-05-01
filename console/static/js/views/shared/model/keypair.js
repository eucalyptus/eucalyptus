define(['app'], function(app) {
  return Backbone.Model.extend({
    name: '',
    region: [],
    material: null,
    item: '',
    connection: [],
    fingerprint: '',

    validation: {
        name: {
          required: true,
          msg: app.msg("launch_instance_error_keypair_required")
        }
    },

    finish: function(outputModel) {
      outputModel.set('key_name', this.get('name'));
    }
  });
});
