define([], function() {
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
          msg: 'A keypair selection is required.'
        }
    },

    finish: function(outputModel) {
      outputModel.set('key_name', this.get('name'));
    }
  });
});
