define([
  'text!./summary.html',
  'rivets',
  '../shared/summary'
], function(template, rivets, Summary) {
  return Summary.extend({
    tpl: template,
  });
});
