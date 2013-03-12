define([
	'dataholder',
    'views/baseview',
	'text!views/templates/volumes.html!strip',
	], function( dataholder, BaseView, template ) {
	return BaseView.extend({
        name: 'volumes',
        template: template,
        sortKeys:  ['', 'id', 'status', 'size', 'instance_id', 'snapshot_id', 'zone', 'create_time']
	});
});
