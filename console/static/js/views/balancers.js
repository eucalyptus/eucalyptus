define([
	'dataholder',
    'views/baseview',
	'text!views/templates/balancers.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
        name: 'balancers',
		template: template,
        sortKeys:  ['', 'name', 'dns_name', 'most_least_active', 'create_time'],
        
	});
});
