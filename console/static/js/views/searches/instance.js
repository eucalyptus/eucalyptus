define([
  'views/searches/generic',
], function(Search) {
  return function(instances) {
    var config = {
      facets: ['state', 'root_device_name', 'group_name',
        'placement', 'instance_type']
      , localize: {
        state: 'Status',
        't1.micro': 'Micro',
        'm1.small': 'Standard',
        'c1.medium': 'High Memory',
        'm1.large' : 'High CPU'
      }
    };
    return new Search(instances, config);
  }
});
