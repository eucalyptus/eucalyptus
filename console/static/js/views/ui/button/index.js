define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
			console.log('UI: Button');
			this.$el.html(template);
			$('div', this.$el).html(args.innerHtml);
			this.rview = rivets.bind(this.$el, this);
		},
		clicked : function() {
			if (this.model.click) {
				this.model.click();
			} else {
				console.log('no bound click event');
			}
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
