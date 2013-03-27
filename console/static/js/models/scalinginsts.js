define([
    'models/eucacollection',
    'models/scalinginst'
], function(EucaCollection, ScalingInst) {
    return EucaCollection.extend({
	model: ScalingInst,
	url: '/autoscaling?Action=DescribeAutoScalingInstances'
    });
});
