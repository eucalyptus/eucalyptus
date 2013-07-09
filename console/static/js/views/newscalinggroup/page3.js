define([
  'app',
  'backbone',
  'rivets',
  'text!./page3.html',
], function(app, Backbone, rivets, template) {
        return Backbone.View.extend({
          title: 'Policies', 

          initialize: function() {
            var self = this;

            var scope = new Backbone.Model({
                policies: new Backbone.Model({
                    available: new Backbone.Collection(),
                    selected: new Backbone.Collection(),
                    })
            });
            $(this.el).html(template);
            this.rview = rivets.bind(this.$el, scope);
          },

          render: function() {
            this.rview.sync();
          }
        });
});
