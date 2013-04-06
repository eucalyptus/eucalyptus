// rivets.js
// version: 0.4.5
// author: Michael Richards
// license: MIT
(function() {
  var Binding, Rivets_binders, Rivets_config, Rivets_formatters, View, bindEvent, convertToModel, createInputBinder, createSubExpressionBinder, defaultExpressionParser, expressionRegex, findBinder, getAdapter, getInputValue, iterate, loopDeps, rivets, trim, unbindEvent, _map,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __slice = [].slice,
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  trim = function(s) {
    return s.replace(/^\s+|\s+$/g, '');
  };

  _map = function(col, callback, thisArg) {
    var A, O, k, len;
    O = Object(col);
    len = O.length >>> 0;
    A = new Array(len);
    k = 0;
    while (k < len) {
      if (k in O) {
        A[k] = callback.call(thisArg, O[k], k, O);
      }
      k++;
    }
    return A;
  };

  getAdapter = function() {
    return rivets.config.adapter;
  };

  findBinder = function(type, argStore) {
    var args, binder, binders, identifier, regexp, value;
    binders = rivets.binders;
    if (!(binder = binders[type])) {
      binder = binders['*'];
      for (identifier in binders) {
        value = binders[identifier];
        if (identifier !== '*' && identifier.indexOf('*') !== -1) {
          regexp = new RegExp("^" + (identifier.replace('*', '(.+)')) + "$");
          if (regexp.test(type)) {
            binder = value;
            argStore.args = args = regexp.exec(type);
            args.shift();
          }
        }
      }
    }
    if (binder instanceof Function) {
      binder = {
        routine: binder
      };
    }
    return binder;
  };

  Binding = (function() {

    function Binding(el, type, model, keypath, options) {
      this.el = el;
      this.type = type;
      this.model = model;
      this.keypath = keypath;
      //this.unbind = __bind(this.unbind, this);

      //this.bind = __bind(this.bind, this);

      //this.publish = __bind(this.publish, this);

      this.sync = __bind(this.sync, this);

      //this.set = __bind(this.set, this);

      //this.formattedValue = __bind(this.formattedValue, this);

      this.options = (options || (options = {}));
      this.binder = options.binder ? (this.args = options.args, options.binder) : findBinder(type, this);
      this.formatters = options.formatters || [];

      var _this = this;
      this.deconstruct = __bind(this.deconstruct, this);
      var destroyCallback = function() {
          _this.deconstruct();
          jQuery(el).off('destroyed', destroyCallback);
      };
      jQuery(el).on('destroyed', destroyCallback);
    }

    Binding.prototype.deconstruct = function(value) {
      this.unbind();
      this.el = document.createElement('div');
      if (this.options.destroyedHandler) {
        this.options.destroyedHandler(this);
      }
      if (false) for (var key in this) {
        this[key] = null;
        delete this[key];
      }
    };
    Binding.prototype.formattedValue = function(value) {
      var model;
      model = this.model;
      _map(this.formatters, function(formatter) {
        var args, id, m;
        args = formatter.split(/\s+/);
        id = args.shift();
        m = getAdapter().read(model, id);
        formatter = m instanceof Function ? m : rivets.formatters[id];
        value = formatter ? formatter.read instanceof Function ? formatter.read.apply(formatter, [value].concat(__slice.call(args))) : formatter instanceof Function ? formatter.apply(null, [value].concat(__slice.call(args))) : value : value;
      });
      return value;
    };

    Binding.prototype.set = function(value) {
      var binder, _ref;
      binder = this.binder;
      value = this.formattedValue(value instanceof Function && !binder["function"] ? value.call(this.model, this.options.bindContext) : value);
      if ((_ref = binder.routine) != null) {
        _ref.call(this, this.el, value);
      }
    };

    Binding.prototype.sync = function() {
      var keypath, model;
      keypath = this.keypath;
      model = this.model;
      this.set(this.options.bypass ? model[keypath] : getAdapter().read(model, keypath));
    };

    Binding.prototype.publish = function() {
      var value;
      if (this.binder.tokenizes) {
        return;
      }
      value = getInputValue(this.el);
      _map(this.formatters.slice(0).reverse(), function(formatter) {
        var args, f, id;
        args = formatter.split(/\s+/);
        id = args.shift();
        f = rivets.formatters[id];
        if (f && f.publish) {
          value = f.publish.apply(f, [value].concat(__slice.call(args)));
        }
      });
      getAdapter().publish(this.model, this.keypath, value);
    };

    Binding.prototype.bind = function() {
      var adapter, binder, keypath, sync, _ref;
      adapter = getAdapter();
      binder = this.binder;
      if ((_ref = binder.bind) != null) {
        _ref.call(this, this.el);
      }
      sync = this.sync;
      if (this.options.bypass) {
        sync();
      } else {
        keypath = this.keypath;
        if (keypath && !binder.tokenizes) {
          adapter.subscribe(this.model, keypath, sync);
        }
        if (rivets.config.preloadData) {
          sync();
        }
      }
      loopDeps(this, function(model, keypath) {
        adapter.subscribe(model, keypath, sync);
      });
    };

    Binding.prototype.unbind = function() {
      var adapter, binder, keypath, sync, _ref;
      adapter = getAdapter();
      binder = this.binder;
      if ((_ref = binder.unbind) != null) {
        _ref.call(this, this.el);
      }
      sync = this.sync;
      keypath = this.keypath;
      if (!this.options.bypass) {
        if (keypath && !binder.tokenizes) {
          adapter.unsubscribe(this.model, keypath, sync);
        }
      }
      loopDeps(this, function(model, keypath) {
        adapter.unsubscribe(model, keypath, sync);
      });
    };

    return Binding;

  })();

  loopDeps = function(binder, callback) {
    _map(binder.options.dependencies, function(dependency) {
      var keypath, model;
      if (/^\./.test(dependency)) {
        model = binder.model;
        keypath = dependency.substr(1);
      } else {
        dependency = dependency.split('.');
        model = getAdapter().read(binder.view.models(dependency.shift()));
        keypath = dependency.join('.');
      }
      callback(model, keypath);
    });
  };

  expressionRegex = /(.*?)\{\{([^{}]+)\}\}/;

  createSubExpressionBinder = function(outerBinding, values, i) {
    values[i] = null;
    return {
      routine: function(el, value) {
        values[i] = value;
        outerBinding.sync();
      }
    };
  };

  defaultExpressionParser = function(view, node, type, models, value) {
    var adapter, bindMethod, binding, context, dependencies, firstPart, keypath, matches, model, options, parsingSupport, path, pipes, splitPath, subBinding, subs, unbindMethod, values;
    if (expressionRegex.test(value)) {
    //function Binding(el, type, model, keypath, options) {
      binding = new Binding(node, type, models);
      values = [];
      subs = [];
      binding.options.destroyedHandler = function(child) {
        for (var i = 0; i < subs.length; i++) {
          if (subs[i] === child) {
       	    subs[i] = null;
          }
        }
      };
      while (value && expressionRegex.test(value)) {
        matches = expressionRegex.exec(value);
        value = value.substring(matches[0].length);
        if (matches[1]) {
          values.push(matches[1]);
        }
        subs.push(subBinding = defaultExpressionParser(view, null, '*', models, matches[2]));
        subBinding.binder = createSubExpressionBinder(binding, values, values.length);
      }
      if (value) {
        values.push(value);
      }
      bindMethod = binding.bind;
      unbindMethod = binding.unbind;
      binding.sync = function() {
        binding.set(values.join(''));
      };
      binding.publish = function() {};
      binding.bind = function() {
        bindMethod();
        _map(subs, function(sub) {
          sub.bind();
        });
      };
      binding.unbind = function() {
        unbindMethod();
        _map(subs, function(sub) {
          sub.unbind();
        });
      };
      return binding;
    }
    pipes = _map(value.split('|'), trim);
    context = _map(pipes.shift().split('<'), trim);
    path = context.shift();
    options = {
      formatters: pipes,
      bindContext: models
    };
    adapter = getAdapter();
    parsingSupport = adapter.parsingSupport || (options.binder = findBinder(type, options)).tokenizes;
    if (parsingSupport) {
      model = models;
      keypath = path;
    } else {
      if (path.indexOf(':') !== -1) {
        options.bypass = true;
      }
      splitPath = path.split(/\.|:/);
      firstPart = splitPath.shift();
      model = firstPart ? adapter.read(models, firstPart) : models;
      keypath = splitPath.join('.');
    }
    if (model) {
      if (dependencies = context.shift()) {
        options.dependencies = dependencies.split(/\s+/);
      }
      binding = new Binding(node, type, model, keypath, options);
      binding.view = view;
    }
    return binding;
  };

  View = (function() {

    function View(els, models) {
      var binders, bindingRegExp, bindings, parse, skipNodes,
        _this = this;
      this.models = models;
      this.publish = __bind(this.publish, this);

      this.select = __bind(this.select, this);

      _map(['bind', 'unbind', 'sync'], function(method) {
        _this[method] = function() {
          _map(_this.bindings, function(binding) {
            if (binding != null) binding[method].call(binding);
          });
        };
      });
      this.els = els = els.jquery || els instanceof Array ? els : [els];
      bindings = this.bindings = [];
      skipNodes = [];
      bindingRegExp = this.bindingRegExp();
      binders = rivets.binders;
      parse = function(node) {
        var attributes;
        attributes = node.attributes;
        if (__indexOf.call(skipNodes, node) < 0) {
          _map(attributes, function(attribute) {
            var binder, identifier, n, regexp, type, value;
            n = attribute.name;
            if (bindingRegExp.test(n)) {
              type = n.replace(bindingRegExp, '');
              if (!(binder = binders[type])) {
                binder = binders['*'];
                for (identifier in binders) {
                  value = binders[identifier];
                  if (identifier !== '*' && identifier.indexOf('*') !== -1) {
                    regexp = new RegExp("^" + (identifier.replace('*', '.+')) + "$");
                    if (regexp.test(type)) {
                      binder = value;
                    }
                  }
                }
              }
              if (binder.block) {
                _map(node.getElementsByTagName('*'), function(n) {
                  skipNodes.push(n);
                });
                attributes = [attribute];
              }
            }
          });
          _map(attributes, function(attribute) {
            var binding, n, type;
            n = attribute.name;
            if (bindingRegExp.test(n)) {
              type = n.replace(bindingRegExp, '');
              binding = defaultExpressionParser(_this, node, type, models, attribute.value);
              if (binding) {
                binding.options.destroyedHandler = function(child) {
                  var nullCount = 0, foundChild
                  for (var i = 0; i < bindings.length; i++) {
                    if (bindings[i] === child) {
                      bindings[i] = null;
                      foundChild = true;
                    }
                    if (bindings[i] === null) {
                      nullCount++;
                    }
                  }
                  if (foundChild && nullCount == bindings.length) {
                    _this.els = [];
                    _this.models = convertToModel({});
                    //console.log('PURELY NULL VIEW', _this, child);
                  }
                };
                bindings.push(binding);
              }
            }
          });
        }
      };
      _map(els, function(el) {
        parse(el);
        _map(el.getElementsByTagName('*'), parse);
      });
    }

    View.prototype.bindingRegExp = function() {
      var prefix;
      prefix = rivets.config.prefix;
      if (prefix) {
        return new RegExp("^data-" + prefix + "-");
      } else {
        return /^data-/;
      }
    };

    View.prototype.select = function(fn) {
      var binding, _i, _len, _ref, _results;
      _ref = this.bindings;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        binding = _ref[_i];
        if (binding != null && fn(binding)) {
          _results.push(binding);
        }
      }
      return _results;
    };

    View.prototype.publish = function() {
      _map(this.select(function(b) {
        return b.binder.publishes;
      }), function(binding) {
        binding.publish();
      });
    };

    return View;

  })();

  bindEvent = function(el, event, handler, context, bindContext) {
    var fn;
    fn = function(e) {
      return handler.call(context, e, bindContext);
    };
    if (window.jQuery != null) {
      el = jQuery(el);
      if (el.on != null) {
        el.on(event, fn);
      } else {
        el.bind(event, fn);
      }
    } else if (window.addEventListener != null) {
      el.addEventListener(event, fn, false);
    } else {
      event = 'on' + event;
      el.attachEvent(event, fn);
    }
    return fn;
  };

  unbindEvent = function(el, event, fn) {
    if (window.jQuery != null) {
      el = jQuery(el);
      if (el.off != null) {
        el.off(event, fn);
      } else {
        el.unbind(event, fn);
      }
    } else if (window.removeEventListener) {
      el.removeEventListener(event, fn, false);
    } else {
      event = 'on' + event;
      el.detachEvent(event, fn);
    }
  };

  getInputValue = function(el) {
    var o, _i, _len, _results;
    switch (el.type) {
      case 'checkbox':
        return el.checked;
      case 'select-multiple':
        _results = [];
        for (_i = 0, _len = el.length; _i < _len; _i++) {
          o = el[_i];
          if (o.selected) {
            _results.push(o.value);
          }
        }
        return _results;
        break;
      default:
        return el.value;
    }
  };

  iterate = function(collection, callback) {
    var adapter, m, n;
    adapter = getAdapter();
    if (adapter.iterate) {
      adapter.iterate(collection, callback);
    } else if (collection instanceof Array) {
      _map(collection, callback);
    } else {
      for (n in collection) {
        m = collection[n];
        callback(m, n);
      }
    }
  };

  convertToModel = function(data) {
    var adapter;
    adapter = getAdapter();
    if (adapter.convertToModel) {
      return adapter.convertToModel(data);
    } else {
      return data;
    }
  };

  createInputBinder = function(routine) {
    return {
      publishes: true,
      bind: function(el) {
        this.currentListener = bindEvent(el, 'change', this.publish);
      },
      unbind: function(el) {
        unbindEvent(el, 'change', this.currentListener);
      },
      routine: routine
    };
  };

  Rivets_binders = {
    enabled: function(el, value) {
      el.disabled = !value;
    },
    disabled: function(el, value) {
      el.disabled = !!value;
    },
    checked: createInputBinder(function(el, value) {
      el.checked = el.type === 'radio' ? el.value === value : !!value;
    }),
    unchecked: createInputBinder(function(el, value) {
      el.checked = el.type === 'radio' ? el.value !== value : !value;
    }),
    show: function(el, value) {
      el.style.display = value ? '' : 'none';
    },
    hide: function(el, value) {
      el.style.display = value ? 'none' : '';
    },
    html: function(el, value) {
      el.innerHTML = value != null ? value : '';
    },
    value: createInputBinder(function(el, value) {
      if (el.type === 'select-multiple') {
        _map(el, function(o) {
          var _ref;
          if (value != null) {
            o.selected = (_ref = o.value, __indexOf.call(value, _ref) >= 0);
          }
        });
      } else {
        el.value = value != null ? value : '';
      }
    }),
    text: function(el, value) {
      var newValue;
      newValue = value != null ? value : '';
      if (el.innerText != null) {
        el.innerText = newValue;
      } else {
        el.textContent = newValue;
      }
    },
    "on-*": {
      "function": true,
      routine: function(el, value) {
        var currentListener, firstArg;
        firstArg = this.args[0];
        currentListener = this.currentListener;
        if (currentListener) {
          unbindEvent(el, firstArg, currentListener);
        }
        this.currentListener = bindEvent(el, firstArg, value, this.model, this.options.bindContext);
      }
    },
    "each-*": {
      block: true,
      routine: function(el, collection) {
        var iterated, marker, parentNode, previous,
          _this = this;
        _map(this.iterated, function(view) {
          view.unbind();
          _map(view.els, function(e) {
            e.parentNode.removeChild(e);
          });
        });
        marker = this.marker;
        if (!marker) {
          parentNode = el.parentNode;
          marker = this.marker = parentNode.insertBefore(document.createComment(" rivets: " + this.type + " "), el);
          parentNode.removeChild(el);
        }
        this.iterated = iterated = [];
        if (collection) {
          previous = marker.nextSibling;
          iterate(collection, function(item, i) {
            var data, firstArg, newNode;
            data = {};
            firstArg = _this.args[0];
            iterate(_this.view.models, function(item, i) {
              data[i] = item;
            });
            data[firstArg] = item;
            data["" + firstArg + "_index"] = data['rivets_index'] = i;
            newNode = el.cloneNode(true);
            newNode.removeAttribute(['data', rivets.config.prefix, _this.type].join('-').replace('--', '-'));
            iterated.push(rivets.bind(marker.parentNode.insertBefore(newNode, previous), convertToModel(data)));
            previous = iterated[iterated.length - 1].els[0].nextSibling;
          });
        }
      }
    },
    "class-*": function(el, value) {
      var classOrder, firstArg, i;
      classOrder = el.className.split();
      firstArg = this.args[0];
      i = __indexOf.call(classOrder, firstArg) >= 0;
      if (!value === !i) {
        if (value) {
          classOrder.push(firstArg);
        } else {
          classOrder.splice(i, 1);
        }
        el.className = classOrder.join(' ');
      }
    },
    "*": function(el, value) {
      if (value) {
        el.setAttribute(this.type, value);
      } else {
        el.removeAttribute(this.type);
      }
    }
  };

  Rivets_config = {
    preloadData: true
  };

  Rivets_formatters = {};

  rivets = {
    binders: Rivets_binders,
    formatters: Rivets_formatters,
    config: Rivets_config,
    configure: function(options) {
      var property, value;
      options || (options = {});
      for (property in options) {
        value = options[property];
        rivets.config[property] = value;
      }
    },
    bind: function(el, models) {
      var view;
      models || (models = {});
      view = new View(el, models);
      view.bind();
      return view;
    }
  };

  if (typeof module !== "undefined" && module !== null) {
    module.exports = rivets;
  } else {
    this.rivets = rivets;
  }

}).call(this);
