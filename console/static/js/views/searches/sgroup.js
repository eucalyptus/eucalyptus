define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    
    var facets = ['all_text', 'launch_config_name', 
      'health_check_type', 'availability_zones', 
    ];
    // XXX want actual localization
    var localization = {
      launch_config_name : 'Launch Configuration', health_check_type : 'Health Check' 
    }
    
    return new Search(images, facets, 
      localization, 
      null);
  }
});
