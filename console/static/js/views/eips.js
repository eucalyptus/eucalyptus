define([
	'dataholder',
    'views/baseview',
	'text!views/templates/eips.html!strip',
	], function( dataholder, template ) {
	return BaseView.extend({
	    name: 'eips',
        template: template,
        sortKeys:  ['', 'public_ip', 'instance_ip'],
       
	});
});
