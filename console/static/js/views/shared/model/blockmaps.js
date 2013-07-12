define(['./blockmap'], function(blockmap) {
  return Backbone.Collection.extend({
    model: blockmap,
    initialize: function() {
    },

    finish: function(outputModel) {
        outputModel.set('block_device_mappings', this.toJSON());
    }
  });
});
