define([
	'text!./template.html!strip',
        'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
			console.log('UI: Button');
			this.$el.html(template);
			$('.buttoncontent', this.$el).html(args.innerHtml);
			this.rview = rivets.bind(this.$el, args.model);
		},
		click : function() {
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
