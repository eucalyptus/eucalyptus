define([
	'text!./template.html!strip',
    'rivets'
	], function( template, rivets ) {
	return Backbone.View.extend({
		initialize : function(args) {
            template = template.replace(/%%KEYPATH%%/g, args.keypath);
			this.$el.html(template);
            console.log('CHECKBOX', this.model);
			this.rview = rivets.bind(this.$el, args.parentmodel);
			this.render(this.model);
		},
		render : function(newValue) {
		this.model = newValue;
            this.rview.sync();
			return this;
		}
	});
});
