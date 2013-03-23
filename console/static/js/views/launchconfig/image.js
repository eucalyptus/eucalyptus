define([
	'dataholder',
	'text!./image_static.html!strip',
  'rivets'
	], function( dataholder, template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.view = this;
			this.images = dataholder.images;
			this.render();
		},
		doit : function() {
			console.log('DOIT:', arguments);
			this.test.set({value: 'pressed the clicker'});
		},
		render : function() {
			this.$el.html(template);
			rivets.bind(this.$el, this);
			//$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			return this;
		}
	});
});
