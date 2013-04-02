define([
	'text!./template.html!strip',
    'rivets',
    'visualsearch'
	], function( template, rivets, VS ) {
	return Backbone.View.extend({
		initialize : function(args) {
			this.$el.html(template);
            VS.init({
                container : this.$el,
                showFacets : true,
                query     : this.model.query,
                callbacks : {
                    search       : this.model.search,
                    facetMatches : this.model.facetMatches,
                    valueMatches : this.model.valueMatches
                }
            });

			this.rview = rivets.bind(this.$el, args.model);
		},
		render : function() {
			this.rview.sync();
			return this;
		}
	});
});
