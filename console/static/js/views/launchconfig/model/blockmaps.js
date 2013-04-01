define(['./blockmap'], function(blockmap) {
  return Backbone.Collection.extend({
    model: blockmap,
    initialize: function() {
    }
  });
});
