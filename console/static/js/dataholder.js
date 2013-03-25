define([
        'backbone',
	'models/scalinggrps',
	'models/volumes', 
	'models/images',
	'models/launchconfigs'
], 
function(Backbone, ScalingGroups,Volumes,Images,LaunchConfigs) {
	var shared = {
		launchConfigs: new LaunchConfigs(),
		scalingGroups: new ScalingGroups(),
		volumes: new Volumes(),
		images: new Images(),
		loadBalancers: new Backbone.Collection([
		  new Backbone.Model({name: "LB A"}),
		  new Backbone.Model({name: "LB B"}),
		  new Backbone.Model({name: "LB C"}),
		  new Backbone.Model({name: "LB D"}),
		])
	};

	shared.launchConfigs.fetch();
	shared.scalingGroups.fetch();
	shared.volumes.fetch();
	shared.images.fetch();

	return shared;
});
