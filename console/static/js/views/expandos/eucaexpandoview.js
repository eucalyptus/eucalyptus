define([
  'backbone',
  'rivets',
], function(Backbone, rivets) {
  return Backbone.View.extend({
    _do_init : function() {
      var self = this;
      this.$el.html(this.template);
      this.rivetsView = rivets.bind(this.$el, this.scope);
      this.render();
      rivets.formatters.date = function(value){
        return moment(value).format(TIME_FORMAT)
      };
    },
    render : function() {
      this.rivetsView.sync();
      return this;
    },
  });
});
