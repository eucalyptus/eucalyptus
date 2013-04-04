define([
    'underscore',
    'backbone',
    'sharedtags',
    'models/scalinggrps',
	'models/scalinginsts',
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
	'models/zones',
	'models/buckets'
	], 
function(_, Backbone, tags) {
    var self = this;
    var sconfs = [
    ['scalinggrp', 'scalinggroup', 'scalingGroup', 'scalingGroups'],
	['scalinginst', 'scalinginsts'],
	['volume', 'volumes'],
	['image', 'images'],
	['launchconfig', 'launchconfigs'],
	['instance'],
	['eip'],
	['keypair'],
	['sgroup'],
	['snapshot'],
	['balancer'],
	['insthealth', 'instHealths'],
	['summary'],
	['zone'],
	['bucket'],
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
       obj.fetch();
    });

    shared.tags = tags;
    shared.tag = tags;

	return shared;
});
