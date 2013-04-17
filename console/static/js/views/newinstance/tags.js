define([
    'underscore',
    'app',
	'dataholder',
  'text!./tags.html!strip',
  'rivets',
  ],
  function(_, app, dataholder, template, rivets) {
  return Backbone.View.extend({
    tpl: template,
    title: 'Tags',
    
    initialize: function() {
      var self = this;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('tags', new Backbone.Collection());
    
      var scope =  {
        tags: self.model,
        //model: self.model
      };

      $(this.el).html(this.tpl);
      this.rView = rivets.bind(this.$el, scope);
      this.render();
    },

    render: function() {
      this.rView.sync();
    }
  });
});
