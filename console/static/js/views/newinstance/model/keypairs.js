define(['./keypair'], function(keypair) {
  return Backbone.Collection.extend({
    model: keypair,
    initialize: function() {

    }
  });
});
