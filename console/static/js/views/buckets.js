define([
	'dataholder',
    'views/baseview',
	'text!views/templates/buckets.html!strip',
	], function( dataholder, BaseView, template ) {
        return BaseView.extend({
    	    name: 'buckets',
            template: templates,
            sortKeys:  ['', 'id', 'status', 'volume_size', 'volume_id', 'description', 'start_time'],
        
	});
});
