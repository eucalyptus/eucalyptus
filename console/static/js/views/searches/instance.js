define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    
    return new Search(images, ['all_text', 'root_device_name', 'state', 
      
      // FIXME - these are guesses based on the wireframe at 
      // https://eucalyptus.atlassian.net/wiki/display/3UD/Manage+Instances+Landing
      //
      // since there are no such fields in the mock data, no idea what they 
      // should actually be
      
      'group_name', 'load_balancer', 'availability_zone', 
      
      'instance_type'], { state : 'Status', group_name : 'Scaling Group' }, null, null, { });
  }
});
