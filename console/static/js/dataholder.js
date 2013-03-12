define(['models/scalinggrps','models/volumes'], function(ScalingGroups,Volumes) {
	var shared = {}
	shared.scalingGroups = new ScalingGroups();
	shared.scalingGroups.fetch();

	shared.volumes = new Volumes();
	shared.volumes.fetch();
	return shared;
});
