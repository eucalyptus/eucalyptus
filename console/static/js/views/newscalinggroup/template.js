console.log('WIZARD:start');
define([
  'rivets',
  'dataholder',
  'text!./template.html',
], function(rivets, dh, template) {
        return Backbone.View.extend({
          initialize: function() {
            $(this.el).html(template)
            this.rview = rivets.bind(this.$el, this);
          },

          render: function() {
            this.rview.sync();
          }
        });
});
