define([
  'text!./summary.html',
  'rivets'
], function(template, rivets) {
  return Backbone.View.extend({
    initialize: function() {

      var scope = {
        view: this,
        model: this.model,
        title: 'Summary',
        summary: this.model,
      };

      this.$el.html(template);
      this.riv = rivets.bind(this.$el, scope);
      this.render();
    },

    render: function() {
      this.riv.sync();
    },
  });
});
