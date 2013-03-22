define(['models/scalinggrps','models/volumes', 'models/images'], function(ScalingGroups,Volumes,Images) {
	var shared = {}
	shared.scalingGroups = new ScalingGroups();
	shared.scalingGroups.fetch();

	shared.volumes = new Volumes();
	shared.volumes.fetch();

  shared.images = new Images();
  shared.images.fetch();
	return shared;
});
