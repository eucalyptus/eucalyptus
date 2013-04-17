define([], function() {
  return Backbone.Model.extend({
    tags: new Backbone.Collection(),

    finish: function(outputModel) {
      outputModel.set('tags', this.get('tags'));
    }
  });
});
