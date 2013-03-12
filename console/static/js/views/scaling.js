define([
	'dataholder',
    'views/baseview',
	'text!views/templates/scaling.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
	    name: 'scaling',
        template: template,
        sortKeys:  ['', 'name', 'launch_config_name', 'activity', '', 'instances', 'desired_capacity', 'health'],
       
	});
});
