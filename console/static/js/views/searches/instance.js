define([
  'views/searches/generic',
  'views/searches/tagsearch'
], function(Search, TagSearch) {
  return function(instances) {
    var config = {
      facets: ['state', 'root_device_name', 'group_name',
        'placement', 'instance_type']
      , localize: {
        state: 'Status',
        't1.micro': 'T1 Micro',
        'm1.small': 'M1 Small',
        'c1.medium': 'C1 Medium',
        'm1.large' : 'M1 Large'
      }
    };
    return new Search(instances, new TagSearch(config, instances));
  }
});
