define([
	'dataholder',
    'views/baseview',
	'text!views/templates/images.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
	    name: 'images', 
        template: template,
        sortKeys:  ['name', 'id', 'architecture', 'description', 'root_device'],
        
	});
});
