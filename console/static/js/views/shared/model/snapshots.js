define(['./snapshot'], function(snapshot) {
  return Backbone.Collection.extend({
    model: snapshot,
    initialize: function() {

    }
  });
});
