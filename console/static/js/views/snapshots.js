define([
	'dataholder',
    'views/baseview',
	'text!views/templates/snapshots.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
	    name: 'snapshots',
        template: template,
        sortKeys:  ['', 'id', 'status', 'volume_size', 'volume_id', 'description', 'start_time'],
    
	});
});
