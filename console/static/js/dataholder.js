define([
    'backbone',
	'models/scalinggrps',
	'models/volumes', 
	'models/images',
	'models/launchconfigs',
	'models/instances',
	'models/eips',
	'models/keypairs',
	'models/sgroups',
	'models/snapshots'
], 
function(Backbone, ScalingGroups,Volumes,Images,LaunchConfigs,Instances, Eips, KeyPairs, SecurityGroups, Snapshots) {
	var shared = {
		launchConfigs: new LaunchConfigs(),
		scalingGroups: new ScalingGroups(),
		volumes: new Volumes(),
		images: new Images(),
		instance: new Instances(),
		loadBalancers: new Backbone.Collection([
		  new Backbone.Model({name: "LB A"}),
		  new Backbone.Model({name: "LB B"}),
		  new Backbone.Model({name: "LB C"}),
		  new Backbone.Model({name: "LB D"}),
		]),
                eip : new Eips(),
                keypair : new KeyPairs(),
                sgroup : new SecurityGroups(),
                snapshot : new Snapshots()
	};

    shared.image = shared.images;
    shared.volume = shared.volumes;
    shared.launchConfig = shared.launchConfigs;
    shared.scalingGroup = shared.scalingGroups;

	shared.launchConfigs.fetch();
	shared.scalingGroups.fetch();
	shared.volumes.fetch();
	shared.images.fetch();
	shared.instance.fetch();

	return shared;
});
