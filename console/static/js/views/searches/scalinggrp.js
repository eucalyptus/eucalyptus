define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    
    var facets = ['all_text', 'launch_config_name', 'health_check_type', 'availability_zones', 'tags' ];
    
    // FIXME - according to https://eucalyptus.atlassian.net/wiki/display/3UD/Manage+Scaling+Groups+Landing
    // tags should be searchable;  however, the mock data is very inconsistent
    
    return new Search(images, facets, {
      launch_config_name : 'Launch Configuration',
      availability_zones : 'Availability Zone'
    }, null);
  }
});
