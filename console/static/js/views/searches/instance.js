define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(instances) {
    var config = {
      facets: ['state', 'root_device_name', 'group_name',
        'placement', 'instance_type']
      , localize: {
        group_name: 'Scaling Group',
        placement: 'Availability Zone',
        state: 'Status'
      }
    };
    return new Search(instances, new TagSearch(config, instances));
  }
});
