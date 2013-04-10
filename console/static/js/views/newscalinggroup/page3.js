console.log('WIZARD:start');
define([
  'rivets',
  'dataholder',
  'text!./page3.html',
], function(rivets, dh, template) {
        return Backbone.View.extend({
          title: 'Policies', 

          initialize: function() {
            $(this.el).html(template)
            this.rview = rivets.bind(this.$el, this.model);
            this.render();
          },

          render: function() {
            this.rview.sync();
          }
        });
});
