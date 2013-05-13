define(['./blockmap'], function(blockmap) {
  return Backbone.Collection.extend({
    model: blockmap,
    initialize: function() {
    },

    finish: function(outputModel) {
      var outputMappings = this.reduce(function(c, m) {
        return m.get('device_name') == '/dev/sda' ? c.add(m) : c;
      }, new Backbone.Collection());
      outputModel.set('block_device_mappings', JSON.stringify(outputMappings));
    }
  });
});
