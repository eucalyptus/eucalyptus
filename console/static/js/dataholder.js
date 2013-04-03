define([
    'backbone',
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
    'sharedtags'
], 
function(Backbone, ScalingGroups, ScalingInstances, Volumes, Images, LaunchConfigs, Instances, Eips, KeyPairs, SecurityGroups, Snapshots, Balancers, InstHealths, tags) {
	var shared = {
		launchConfigs: new LaunchConfigs(),
		scalingGroups: new ScalingGroups(),
		scalinginsts: new ScalingInstances(),
		volumes: new Volumes(),
		images: new Images(),
		instance: new Instances(),
		loadBalancers: new Balancers(),
		instHealths: new InstHealths(),
        eip : new Eips(),
        keypair : new KeyPairs(),
        sgroup : new SecurityGroups(),
        snapshot : new Snapshots(),
	};

	shared.tags = tags;

    shared.image = shared.images;
    shared.volume = shared.volumes;
    shared.launchconfig = shared.launchConfigs;
    shared.scalingGroup = shared.scalingGroups;
    shared.scalinggroup = shared.scalingGroups;
    shared.scalinggrp = shared.scalingGroups;

	shared.launchConfigs.fetch();
	shared.scalingGroups.fetch();
	shared.scalinginsts.fetch();
	shared.volumes.fetch();
	shared.images.fetch();
	shared.instance.fetch();
    shared.loadBalancers.fetch();
    shared.instHealths.fetch();
    shared.eip.fetch();
    shared.keypair.fetch();
    shared.sgroup.fetch();
    shared.snapshot.fetch();

	return shared;
});
