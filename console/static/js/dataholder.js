define(['models/scalinggrps','models/volumes', 'models/images'], function(ScalingGroups,Volumes,Images) {
	var shared = {
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

	shared.scalingGroups.fetch();
	shared.volumes.fetch();
	shared.images.fetch();

	return shared;
});
