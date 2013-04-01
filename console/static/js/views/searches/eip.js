define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    return new Search(images, ['all_text', 'name'], { assigned_to_instance_name : 'Assignment'}, null);
  }
});
