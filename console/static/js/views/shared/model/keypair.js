define(['models/keypair'], function(keypair) {
  return keypair.extend({
    __obj_name__: "KeyPair",
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
