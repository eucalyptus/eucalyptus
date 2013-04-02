define([
    'views/searches/generic',
], function(Search) {
    return function(instances) {
      return new Search(instances, ['state', 'root_device_name', 'group_name', 'placement', 'instance_type'], {}, null);
    }
});
