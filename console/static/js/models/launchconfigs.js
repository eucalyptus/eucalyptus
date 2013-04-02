define([
    'models/eucacollection',
    'models/launchconfig'
], function(EucaCollection, LaunchConfig) {
    var collection = EucaCollection.extend({
      model: LaunchConfig,
      url: '/autoscaling?Action=DescribeLaunchConfigurations',
    });
    return collection;
});
