define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    
    var facets = ['all_text', 'owner_id', 
      // FIXME:  there is no mock data for platform or architecture,
      // so no idea what properties are intended to be referenced by the
      // wireframe at 
      // https://eucalyptus.atlassian.net/wiki/display/3UD/Manage+Security+Groups+Landing
      'platform', 'architecture', 
      
      'root_device'
    ];
    // PENDING:  tag facets
    
    return new Search(images, facets, {}, null);
  }
});
