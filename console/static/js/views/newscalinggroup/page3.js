define([
  'backbone',
  'rivets',
  'text!./page3.html',
], function(Backbone, rivets, template) {
        return Backbone.View.extend({
          title: 'Policies', 

          initialize: function() {
            var scope = new Backbone.Model({
                policies: new Backbone.Collection()
            });
            $(this.el).html(template);
            this.rview = rivets.bind(this.$el, scope);
          },

          render: function() {
            this.rview.sync();
          }
        });
});
