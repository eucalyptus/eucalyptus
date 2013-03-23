define([
	'dataholder',
	//'text!./image_static.html!strip',
  'text!./image.html!strip',
  'rivets'
	], function( dataholder, template, rivets ) {
	return Backbone.View.extend({
		initialize : function() {
			this.view = this;
			this.images = dataholder.images;
      this.jerry = "POMMER";
			this.render();
		},
		doit : function() {
			console.log('DOIT:', arguments);
			this.test.set({value: 'pressed the clicker'});
		},
		render : function() {
      this.$el.html(template);
			//this.$el.html(_.template(template, {collection: this.collection}));
			rivets.bind(this.$el, {images: this.images});
			//$(':input[data-value]', this.$el).keyup(function() { $(this).trigger('change'); });
			return this;
		}
	});
});
