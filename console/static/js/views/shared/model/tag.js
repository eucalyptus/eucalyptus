define([], function() {
  return Backbone.Model.extend({
    tags: new Backbone.Collection(),

    finish: function(outputModel) {
      var tgs = this.get('tags').clone();
      outputModel.set('tags', tgs);
    }
  });
});
