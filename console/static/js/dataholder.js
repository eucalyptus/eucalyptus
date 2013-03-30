define([
    'backbone',
	'models/scalinggrps',
	'models/volumes', 
	'models/images',
	'models/launchconfigs',
	'models/instances',
  'models/sgroups',
  'models/keypairs'
], 
function(Backbone, ScalingGroups,Volumes,Images,LaunchConfigs,Instances, SecurityGroups, KeyPairs) {
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
    securityGroups: new SecurityGroups(),
    keypairs: new KeyPairs()
	};

    shared.image = shared.images;
    shared.volume = shared.images;
    shared.launchConfig = shared.launchConfigs;
    shared.scalingGroup = shared.scalingGroups;
    shared.securityGroup = shared.securityGroups;
    shared.keypair = shared.keypairs;

	shared.launchConfigs.fetch();
	shared.scalingGroups.fetch();
	shared.volumes.fetch();
	shared.images.fetch();
	shared.instance.fetch();
  shared.securityGroups.fetch();
  shared.keypairs.fetch();

	return shared;
});
