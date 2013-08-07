define([
    'underscore',
    'backbone',
    'sharedtags',
    'models/scalinggrps',
	'models/scalinginsts',
	'models/scalingpolicys',
	'models/volumes',
	'models/images',
	'models/launchconfigs',
	'models/instances',
	'models/eips',
	'models/keypairs',
	'models/sgroups',
	'models/snapshots',
	'models/balancers',
	'models/insthealths',
	'models/summarys',
	'models/buckets',
	'models/alarms',
	'models/metrics',
    'models/availabilityzones',
    'models/loadbalancers'
	], 
function(_, Backbone, tags) {
    var self = this;
    var sconfs = [
    ['scalinggrp', 'scalinggroup', 'scalingGroup', 'scalingGroups'],
	['scalinginst', 'scalinginsts'],
	['scalingpolicy', 'scalingpolicys'],
	['volume', 'volumes'],
	['image', 'images'],
	['launchconfig', 'launchconfigs', 'launchConfigs'],
	['instance', 'instances'],
	['eip'],
	['keypair', 'keypairs'],
	['sgroup', 'sgroups'],
	['snapshot', 'snapshots'],
	['balancer'],
	['insthealth', 'instHealths'],
	['summary'],
	['bucket'],
	['alarm', 'alarms'],
	['metrics'],
	['availabilityzone'],
	['loadbalancer', 'loadbalancers']
    ];

    var shared = {};
    var args = arguments;
    var srcs = _.map(_.range(3, args.length), function(n) { 
        return args[n]; 
    });

    _.each(srcs, function(src, index) {
       var clz = srcs[index];
       var obj = new clz();
       _.each(sconfs[index], function(name) {
           shared[name] = obj;
       });
    });

    shared.tags = tags;
    shared.tag = tags;

	return shared;
});
