define([
        'backbone',
        'rivets',
	'text!./template.html!strip',
	], function( Backbone, rivets, template ) {
	return Backbone.View.extend({
		initialize : function(args) {
			this.$el.html(template);
			$('.ui-content', this.$el).html(args.innerHtml);
                        args.model.label = $(args.innerHtml).filter('label').text();
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
