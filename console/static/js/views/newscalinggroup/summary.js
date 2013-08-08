define([
  'text!./summary.html',
  'rivets'
], function(template, rivets) {
  return Backbone.View.extend({
    tpl: template,
    initialize: function() {
      var self = this;

      var scope = {
        view: this,
        model: this.model,
        title: 'Summary',
        summary: this.model,
      };

      scope.model.get('scalingGroup').on('change:launchConfig', function() {
        self.render();
      });

      this.$el.html(template);
      this.riv = rivets.bind(this.$el, scope);
      this.render();
    },

    render: function() {
      this.riv.sync();
    },
  });
});
