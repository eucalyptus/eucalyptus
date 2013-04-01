define([], function() {
  return Backbone.Model.extend({
    __obj_name__: "KeyPair",
    name: '',
    region: [],
    material: null,
    item: '',
    connection: [],
    fingerprint: ''
  });
});
