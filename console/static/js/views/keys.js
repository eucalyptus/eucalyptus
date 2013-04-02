define([
	'dataholder',
    'views/baseview',
	'text!views/templates/keys.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
	    name: 'keys',
        template: template,
        sortKeys:  ['', 'name', 'fingerprint'],
       
	});
});
