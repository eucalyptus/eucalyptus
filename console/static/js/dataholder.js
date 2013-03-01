define(['models/scalinggrps'], function(ScalingGroups) {
	var shared = {}
	shared.scalingGroups = new ScalingGroups();
	shared.scalingGroups.fetch();
	return shared;
});
