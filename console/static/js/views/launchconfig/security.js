define([
	'dataholder',
  'text!./security.html!strip',
  'rivets',
	], function( dataholder, template, rivets ) {
	return Backbone.View.extend({
    title: 'Security',

		initialize : function() {

      var scope = {

      };
      $(this.el).html(template)
      this.rView = rivets.bind(this.$el, scope);
      this.render();
		},

    render: function() {
      this.rView.sync();
    }
});
});
