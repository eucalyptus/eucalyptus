define([
	'dataholder',
    'views/baseview',
	//'text!views/templates/volumes.html!strip',
	], function( dataholder, BaseView ) {
	return BaseView.extend({
        name: 'volumes',
        header: 'Manage Volumes',
        tableHeader: ['', 'ID', 'Status', 'Size', 'Attached to Instance', 'Snapshot ID', 'Availability Zone', 'Creation Time'],
        filterOptions: [
            {label: 'All Volumes', value: 'all'}, 
            {label: 'Attached Volumes', value: 'attached'}, 
            {label: 'Detached Volumes', value: 'detached'}
        ],
        searchText: 'Search Volumes',
        buttonLabel: 'Create new volume',
        sortKeys:  ['', 'id', 'status', 'size', 'instance_id', 'snapshot_id', 'zone', 'create_time']
	});
});
