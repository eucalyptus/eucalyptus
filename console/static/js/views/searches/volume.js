define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    return new Search(images, ['all_text', 'attach_data'], { attach_data : 'Attachment' }, null);
  }
});
