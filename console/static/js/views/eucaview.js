console.log('WIZARD:start');
define([
  'backbone',
  'rivets',
], function(Backbone, rivets) {
        return Backbone.View.extend({
          bind: function() {
            $(this.el).html(this.template)
            this.rview = rivets.bind(this.$el, this);
          }

          render: function() {
            this.rview.sync();
          }
        });
});
