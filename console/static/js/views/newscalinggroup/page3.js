console.log('WIZARD:start');
define([
  'rivets',
  'dataholder',
  'text!./page3.html',
], function(rivets, dh, template) {
        return Backbone.View.extend({
          title: 'Policies', 

          initialize: function() {
            this.render();
          },

          render: function() {
            $(this.el).html(template)
            rivets.bind(this.$el, this);
          }
        });
});
