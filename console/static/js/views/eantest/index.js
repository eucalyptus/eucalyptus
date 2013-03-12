console.log('EANTEST:start');
define([
	'dataholder',
	'text!views/eantest/template.html!strip',
	], function( dataholder, template ) {
	return Backbone.View.extend({
		events : {
		},
		initialize : function() {
			var view = this;

			this.collection = dataholder.scalingGroups;
			this.collection.on('reset', function() { view.render() });
			this.collection.on('change', function() { view.render() });
			this.collection.fetch();	
		},
		render : function() {
			this.$el.html(_.template(template)({ groups: this.collection.toJSON() }));
			return this;
		}
	});
});
