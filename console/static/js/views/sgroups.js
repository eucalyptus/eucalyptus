define([
	'dataholder',
    'views/baseview',
	'text!views/templates/sgroups.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
	    name: 'sgroups',
        template: template,
        sortKeys:  ['', 'name', 'description'],
        
	});
});
