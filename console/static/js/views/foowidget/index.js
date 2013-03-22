define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			console.log('FOOTEST', this);
			this.render();
		},
		render : function() {
			this.$el.html(template);
			rivets.bind(this.$el, this);
			return this;
		}
	});
});
