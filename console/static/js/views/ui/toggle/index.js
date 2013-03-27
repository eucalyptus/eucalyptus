define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.render();
		},
		render : function() {
			this.$el.html(template);
			rivets.bind(this.$el, this);
			return this;
		}
	});
});
