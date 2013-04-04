define([], function() {
  return function(config, data) {
    var self = this;

    function getTags() {
      var result = {};
      var jData = data.toJSON();
      for (var i = 0; i < jData.length; i++) {
        var dataItem = jData[i].tags;
        if (dataItem) {
          if (typeof dataItem.toJSON === 'function') {
            dataItem = dataItem.toJSON();
          }
        }
        for (var j = 0; j < dataItem.length; j++) {
          var tag = dataItem[j];
          if (tag && tag.name) {
            var coll = result[tag.name];
            if (!coll) {
              coll = [];
              result[tag.name] = coll;
            }
            coll.push(tag);
          }
        }
        return result;
      }

      data.toJSON().forEach(function(item) {
        if (item.tags) {
          var t = item.tags;
          if (typeof t.toJSON === 'function') {
            t = t.toJSON();
          }
          if (t) {
            t.forEach(function(tag) {
              if (tag.name) {
                var coll = result[tag.name];
                if (!coll) {
                  coll = [];
                  result[tag.name] = coll;
                }
                coll.push(tag);
                nue.push('tag_' + tag.name);
              }
            });
          }
        }
      });
    }

    self.facetsCustomizer = function(add, append) {
      if (config.facetsCustomizer) {
        config.facetsCustomizer.apply(this, add, append);
      }
      var tags = getTags();
      for (var tagName in tags) {
        append(tagName + ' (tag)', tagName + '(tag)');
      }
    };


    self.search = {};
    if (config.search) {
      for (var key in config.search) {
        self.search[key] = config.search[key];
      }
    }

    function searchMatch(search, item) {
      var rex = new RegExp('.*' + search + '.*', 'img');
      for (var key in item) {
        var obj = item[key];
        if (typeof obj !== 'function') {
          if (rex.test(obj + '')) {
            return true;
          }
        }
      }
      return false;
    }

    self.__defineGetter__("search", function() {
      var result = {};
      if (config.search) {
        for (var key in config.search) {
          result[key] = config.search[key];
        }
      }
      var tags = getTags();
      for (var tagName in tags) {
        var searchName = tagName + ' (tag)';
        result[searchName] = function(search, facetSearch, item, itemsFacetValue, hit) {

          // FIXME Whoa, WTF - the search parameter is a *string* like
          // '"Name (tag)" : "foo"'
          var extractSearchText = /".*?":\s?"(.*)"/
          if (extractSearchText.test(search)) {
            var actualSearchTerm = extractSearchText.exec(search)[1];

            var currSet = tags[tagName];
            for (var i = 0; i < currSet.length; i++) {
              var oneItem = currSet[i];
              var theTags = item.tags;
              if (typeof theTags.toJSON === 'function') {
                theTags = theTags.toJSON();
              }
              theTags.forEach(function(oneTag) {
                if (oneItem.id === oneTag.id) {
                  if (searchMatch(actualSearchTerm, oneTag)) {
                    hit();
                  }
                }
              });
            }
          }
          return true;
        };
      }
      return result;
    });

    for (var key in config) {
      if (!self[key]) {
        self[key] = config[key];
      }
    }
  };

});
