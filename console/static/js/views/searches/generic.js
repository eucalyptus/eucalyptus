define(['app', 'dataholder'], function(app, dh) {
  var self = this;
  
  function isArray(o) {
    return o && typeof o === 'object' 
            && typeof o.forEach === 'function' 
            && typeof o.length === 'number';
  }
  
  return function(images, allowedFacetNames, localizer, explicitFacets, searchers, propertyMapping) {
    var self = this;
    searchContext = self;
    
    // Make sure if the argument isn't there, that at least
    // something works
    if (!allowedFacetNames) {
      allowedFacetNames = [];
      images.forEach(function(img) {
        for (var key in img) {
          if (!isIgnored(key, img, allowedFacetNames)) {
            allowedFacetNames.push(key);
          }
        }
      })
    }
    
    var sortKeyList = function(list, keyName) {
      return _.chain(list)
              .sort()
              .uniq()
              .value()
    }

    function localize(name) {
      var result = name;
      if (localizer) {
        if (typeof localizer === 'function') {
          var localized = localizer(name);
          if (localized) {
            return localized;
          }
        } else if (localizer[name]) {
          return localizer[name];
        }
      }
      function capitalize(words) {
        for (var i = 0; i < words.length; i++) {
          words[i] = words[i].charAt(0).toUpperCase() + words[i].slice(1);
        }
        return words;
      }
      result = capitalize(name.split(/_/g)).join(' ');
      result = capitalize(result.split(/-/g)).join(' ');
      result = result.replace(/&#x2f;/g, '/')
      result = result.replace(/&#.{0,3};/g, '')
      return result;
    }

    var siftKeyList = function(list, search) {
      console.log(list, search);
      return _.chain(list).filter(function(item) {
        return new RegExp('.*' + search + '.*').test(item);
      }).map(function(item) {
        return item === '' ? {value: item, label: 'None'} : item
      }).value();
    }

    function isIgnored(key, on, done) {
      return /^_.*/.test(key) 
              || typeof on[key] === 'function'
              || done[key];
    }

    function deriveFacets() {
      var derivedFacets = [];
      var done = {};
      images.toJSON().forEach(function(img) {
        for (var i=0; i < allowedFacetNames.length; i++) {
          var key = allowedFacetNames[i];
          if (isIgnored(key, img, done)) {
            continue;
          }
          derivedFacets.push({value: key, label: localize(key)});
          done[key] = true;
        }
      });
      return sortKeyList(derivedFacets, 'label');
    }

    function deriveMatches(facet, searchTerm) {
      if (searchers && searchers[facet]) {
        return searchers[facet].apply(self, [facet, searchTerm]);
      }
      if (explicitFacets && explicitFacets[facet]) {
        return explicitFacets[facet];
      }
      var result = [];
      var found = [];
      images.toJSON().forEach(function(img) {
        var val = img[facet];
        if (val && typeof val !== 'object' && typeof val !== 'function') {
          if (found.indexOf(val) < 0) {
            found.push(val);
            result.push({name: facet, label: localize(val), value: val});
          }
        } else if (isArray(val)) {
            val.forEach(function(item) {
              found.push(item);
              result.push({name: item + '', value : item, label:localize(item + '')})
            });
        } else if (typeof val === 'object') {
          if (propertyMapping && propertyMapping[facet]) {
              var nm = val[propertyMapping[facet]];
              found.push(nm)
              result.push({name: val + '', value : val, label : nm});
          } else {
            console.log("No matching strategy for " + JSON.stringify(img) + " as facet " + facet);
          }
        }
      });
      result = sortKeyList(result, 'label')
      return siftKeyList(result, searchTerm);
    }

    var MAX_RECURSE = 5;
    function drillThrough(obj, rex, depth) {
      if (MAX_RECURSE === depth) {
        return false;
      }
      var result = false;
      switch (typeof obj) {
        case 'string' :
          result = rex.test(obj);
          break;
        case 'number' :
          result = rex.test('' + obj)
          break;
        case 'object' :
          if (isArray(obj)) {
            for (var i = 0; i < obj.length; i++) {
              result = drillThrough(obj[i], rex, depth + 1);
              if (result) {
                break;
              }
            }
          } else {
            for (var key in obj) {
              result = drillThrough(obj[key], rex, depth + 1);
              if (result) {
                break;
              }
            }
          }
      }
      return result;
    }

    this.images = images;
    this.filtered = images.clone();
    this.lastSearch = '';
    this.lastFacets = new Backbone.Model({});
        this.search = function(search, facets) {
            var jfacets = facets.toJSON();
            var results = self.images.filter(function(model) {
        return _.every(jfacets, function(facet) {
          if (searchers && searchers[facet.category]) {
            return searchers[facet.category].apply(self, [facet, search, images]);
          }
          var rex = new RegExp('.*' + facet.value + '.*', 'img');
          if ('all_text' === facet.category) {
            return drillThrough(model, rex, 0);
          } else {
            var curr = model.get(facet.category);
            if (typeof curr === 'object') {
              // Allow for searching inside tags and such
              return drillThrough(curr, rex, 0);
            } else {
              return rex.test(model.get(facet.category));
            }
          }
        });
      }).map(function(model) {
        return model.toJSON();
      });
      console.log(results);
      self.filtered.reset(results);
    }
    this.facetMatches = function(callback) {
      callback(deriveFacets());
    }
    
    this.valueMatches = function(facet, searchTerm, callback) {
      callback(deriveMatches(facet, searchTerm))
    }
    
    function up() {
      self.search(self.lastSearch, self.lastFacets);
    }
    
    images.on('change reset', up);
    up();
  }
});
