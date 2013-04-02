// rivets.js
// version: 0.4.5
// author: Michael Richards
// license: MIT
(function() {
  var Rivets, bindEvent, convertToModel, createInputBinder, createSubExpressionBinder, defaultExpressionParser, expressionRegex, findBinder, getInputValue, iterate, loopDeps, rivets, unbindEvent,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __slice = [].slice,
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  Rivets = {};

  if (!String.prototype.trim) {
    String.prototype.trim = function() {
      return this.replace(/^\s+|\s+$/g, '');
    };
  }

  findBinder = function(type) {
    var args, binder, identifier, regexp, value, _ref;
    if (!(binder = Rivets.binders[type])) {
      binder = Rivets.binders['*'];
      _ref = Rivets.binders;
      for (identifier in _ref) {
        value = _ref[identifier];
        if (identifier !== '*' && identifier.indexOf('*') !== -1) {
          regexp = new RegExp("^" + (identifier.replace('*', '(.+)')) + "$");
          if (regexp.test(type)) {
            binder = value;
            args = regexp.exec(type);
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
    return [binder, args];
  };

  Rivets.Binding = (function() {

    function Binding(el, type, model, keypath, options) {
      var _ref;
      this.el = el;
      this.type = type;
      this.model = model;
      this.keypath = keypath;
      this.unbind = __bind(this.unbind, this);

      this.bind = __bind(this.bind, this);

      this.publish = __bind(this.publish, this);

      this.sync = __bind(this.sync, this);

      this.set = __bind(this.set, this);

      this.formattedValue = __bind(this.formattedValue, this);

      this.options = (options || (options = {}));
      _ref = options.binder ? [options.binder, options.args] : findBinder(type), this.binder = _ref[0], this.args = _ref[1];
      this.formatters = options.formatters || [];
    }

    Binding.prototype.formattedValue = function(value) {
      var args, formatter, id, m, model, _i, _len, _ref;
      model = this.model;
      _ref = this.formatters;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        formatter = _ref[_i];
        args = formatter.split(/\s+/);
        id = args.shift();
        m = Rivets.config.adapter.read(model, id);
        formatter = m instanceof Function ? m : Rivets.formatters[id];
        if ((formatter != null ? formatter.read : void 0) instanceof Function) {
          value = formatter.read.apply(formatter, [value].concat(__slice.call(args)));
        } else if (formatter instanceof Function) {
          value = formatter.apply(null, [value].concat(__slice.call(args)));
        }
      }
      return value;
    };

    Binding.prototype.set = function(value) {
      var binder, _ref;
      binder = this.binder;
      value = this.formattedValue(value instanceof Function && !binder["function"] ? value.call(this.model, this.options.bindContext) : value);
      return (_ref = binder.routine) != null ? _ref.call(this, this.el, value) : void 0;
    };

    Binding.prototype.sync = function() {
      var keypath, model;
      keypath = this.keypath;
      model = this.model;
      return this.set(this.options.bypass ? model[keypath] : Rivets.config.adapter.read(model, keypath));
    };

    Binding.prototype.publish = function() {
      var args, f, formatter, id, value, _i, _len, _ref;
      if (this.binder.tokenizes) {
        return;
      }
      value = getInputValue(this.el);
      _ref = this.formatters.slice(0).reverse();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        formatter = _ref[_i];
        args = formatter.split(/\s+/);
        id = args.shift();
        f = Rivets.formatters[id];
        if (f != null ? f.publish : void 0) {
          value = f.publish.apply(f, [value].concat(__slice.call(args)));
        }
      }
      return Rivets.config.adapter.publish(this.model, this.keypath, value);
    };

    Binding.prototype.bind = function() {
      var _ref,
        _this = this;
      if ((_ref = this.binder.bind) != null) {
        _ref.call(this, this.el);
      }
      if (this.options.bypass) {
        this.sync();
      } else {
        if (this.keypath && !this.binder.tokenizes) {
          Rivets.config.adapter.subscribe(this.model, this.keypath, this.sync);
        }
        if (Rivets.config.preloadData) {
          this.sync();
        }
      }
      return loopDeps(this, function(model, keypath) {
        return Rivets.config.adapter.subscribe(model, keypath, _this.sync);
      });
    };

    Binding.prototype.unbind = function() {
      var _ref,
        _this = this;
      if ((_ref = this.binder.unbind) != null) {
        _ref.call(this, this.el);
      }
      if (!(this.options.bypass || !this.binder.tokenizes)) {
        if (this.keypath) {
          Rivets.config.adapter.unsubscribe(this.model, this.keypath, this.sync);
        }
      }
      return loopDeps(this, function(model, keypath) {
        return Rivets.config.adapter.unsubscribe(model, keypath, _this.sync);
      });
    };

    return Binding;

  })();

  loopDeps = function(binder, callback) {
    var dependency, keypath, model, _i, _len, _ref, _ref1, _results;
    if ((_ref = binder.options.dependencies) != null ? _ref.length : void 0) {
      _ref1 = binder.options.dependencies;
      _results = [];
      for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
        dependency = _ref1[_i];
        if (/^\./.test(dependency)) {
          model = binder.model;
          keypath = dependency.substr(1);
        } else {
          dependency = dependency.split('.');
          model = Rivets.config.adapter.read(binder.view.models(dependency.shift()));
          keypath = dependency.join('.');
        }
        _results.push(callback(model, keypath));
      }
      return _results;
    }
  };

  expressionRegex = /(.*?)\{\{([^{}]+)\}\}/;

  createSubExpressionBinder = function(outerBinding, values, i) {
    values[i] = null;
    return {
      routine: function(el, value) {
        values[i] = value;
        return outerBinding.sync();
      }
    };
  };

  defaultExpressionParser = function(view, node, type, models, value) {
    var bindMethod, binder, binderTokenizes, binding, context, ctx, dependencies, firstPart, keypath, matches, model, options, parsingSupport, path, pipe, pipes, splitPath, subBinding, subs, unbindMethod, values, _ref;
    if (expressionRegex.test(value)) {
      binding = new Rivets.Binding(node, type, models);
      values = [];
      subs = [];
      while (value && expressionRegex.test(value)) {
        matches = expressionRegex.exec(value);
        value = value.substring(matches[0].length);
        if (matches[1]) {
          values[values.length] = matches[1];
        }
        subs[subs.length] = subBinding = defaultExpressionParser(view, null, '*', models, matches[2]);
        subBinding.binder = createSubExpressionBinder(binding, values, values.length);
      }
      if (value) {
        values[values.length] = value;
      }
      bindMethod = binding.bind;
      unbindMethod = binding.unbind;
      binding.sync = function() {
        return binding.set(values.join(''));
      };
      binding.publish = function() {};
      binding.bind = function() {
        var sub, _i, _len, _results;
        bindMethod();
        _results = [];
        for (_i = 0, _len = subs.length; _i < _len; _i++) {
          sub = subs[_i];
          _results.push(sub.bind());
        }
        return _results;
      };
      binding.unbind = function() {
        var sub, _i, _len, _results;
        unbindMethod();
        _results = [];
        for (_i = 0, _len = subs.length; _i < _len; _i++) {
          sub = subs[_i];
          _results.push(sub.unbind());
        }
        return _results;
      };
      return binding;
    }
    pipes = (function() {
      var _i, _len, _ref, _results;
      _ref = value.split('|');
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        pipe = _ref[_i];
        _results.push(pipe.trim());
      }
      return _results;
    })();
    context = (function() {
      var _i, _len, _ref, _results;
      _ref = pipes.shift().split('<');
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        ctx = _ref[_i];
        _results.push(ctx.trim());
      }
      return _results;
    })();
    path = context.shift();
    splitPath = path.split(/\.|:/);
    options = {
      formatters: pipes,
      bypass: path.indexOf(':') !== -1,
      bindContext: models
    };
    parsingSupport = Rivets.config.adapter.parsingSupport;
    _ref = findBinder(type), binder = _ref[0], options.args = _ref[1];
    binderTokenizes = binder.tokenizes;
    options.binder = binder;
    firstPart = parsingSupport || binderTokenizes ? splitPath[0] : splitPath.shift();
    model = firstPart || !binderTokenizes ? Rivets.config.adapter.read(models, firstPart) : models;
    keypath = splitPath.join('.');
    if (model || binderTokenizes) {
      if (dependencies = context.shift()) {
        options.dependencies = dependencies.split(/\s+/);
      }
      binding = new Rivets.Binding(node, type, (parsingSupport ? models : model), keypath, options);
      binding.view = view;
    }
    return binding;
  };

  Rivets.View = (function() {

    function View(els, models) {
      this.models = models;
      this.publish = __bind(this.publish, this);

      this.sync = __bind(this.sync, this);

      this.unbind = __bind(this.unbind, this);

      this.bind = __bind(this.bind, this);

      this.select = __bind(this.select, this);

      this.build = __bind(this.build, this);

      this.bindingRegExp = __bind(this.bindingRegExp, this);

      this.els = els.jquery || els instanceof Array ? els : [els];
      this.build();
    }

    View.prototype.bindingRegExp = function() {
      var prefix;
      prefix = Rivets.config.prefix;
      if (prefix) {
        return new RegExp("^data-" + prefix + "-");
      } else {
        return /^data-/;
      }
    };

    View.prototype.build = function() {
      var bindingRegExp, bindings, el, node, parse, skipNodes, _i, _j, _len, _len1, _ref, _ref1,
        _this = this;
      bindings = this.bindings = [];
      skipNodes = [];
      bindingRegExp = this.bindingRegExp();
      parse = function(node) {
        var attribute, attributes, binder, binding, identifier, n, regexp, type, value, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2, _ref3;
        if (__indexOf.call(skipNodes, node) < 0) {
          _ref = node.attributes;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            attribute = _ref[_i];
            if (bindingRegExp.test(attribute.name)) {
              type = attribute.name.replace(bindingRegExp, '');
              if (!(binder = Rivets.binders[type])) {
                _ref1 = Rivets.binders;
                for (identifier in _ref1) {
                  value = _ref1[identifier];
                  if (identifier !== '*' && identifier.indexOf('*') !== -1) {
                    regexp = new RegExp("^" + (identifier.replace('*', '.+')) + "$");
                    if (regexp.test(type)) {
                      binder = value;
                    }
                  }
                }
              }
              binder || (binder = Rivets.binders['*']);
              if (binder.block) {
                _ref2 = node.getElementsByTagName('*');
                for (_j = 0, _len1 = _ref2.length; _j < _len1; _j++) {
                  n = _ref2[_j];
                  skipNodes.push(n);
                }
                attributes = [attribute];
              }
            }
          }
          _ref3 = attributes || node.attributes;
          for (_k = 0, _len2 = _ref3.length; _k < _len2; _k++) {
            attribute = _ref3[_k];
            if (bindingRegExp.test(attribute.name)) {
              type = attribute.name.replace(bindingRegExp, '');
              binding = defaultExpressionParser(_this, node, type, _this.models, attribute.value);
              if (binding) {
                bindings.push(binding);
              }
            }
          }
          if (attributes) {
            attributes = null;
          }
        }
      };
      _ref = this.els;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        el = _ref[_i];
        if (el.attributes != null) {
          parse(el);
        }
        _ref1 = el.getElementsByTagName('*');
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          node = _ref1[_j];
          if (node.attributes != null) {
            parse(node);
          }
        }
      }
    };

    View.prototype.select = function(fn) {
      var binding, _i, _len, _ref, _results;
      _ref = this.bindings;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        binding = _ref[_i];
        if (fn(binding)) {
          _results.push(binding);
        }
      }
      return _results;
    };

    View.prototype.bind = function() {
      return this.bindings.map(function(binding) {
        return binding.bind();
      });
    };

    View.prototype.unbind = function() {
      return this.bindings.map(function(binding) {
        return binding.unbind();
      });
    };

    View.prototype.sync = function() {
      return this.bindings.map(function(binding) {
        return binding.sync();
      });
    };

    View.prototype.publish = function() {
      return (this.select(function(b) {
        return b.binder.publishes;
      })).map(function(binding) {
        return binding.publish();
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
        return el.off(event, fn);
      } else {
        return el.unbind(event, fn);
      }
    } else if (window.removeEventListener) {
      return el.removeEventListener(event, fn, false);
    } else {
      event = 'on' + event;
      return el.detachEvent(event, fn);
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
    var i, item, m, n, _i, _len, _results, _results1;
    if (Rivets.config.adapter.iterate) {
      return Rivets.config.adapter.iterate(collection, callback);
    } else if (collection instanceof Array) {
      _results = [];
      for (i = _i = 0, _len = collection.length; _i < _len; i = ++_i) {
        item = collection[i];
        _results.push(callback(item, i));
      }
      return _results;
    } else {
      _results1 = [];
      for (n in collection) {
        m = collection[n];
        _results1.push(callback(m, n));
      }
      return _results1;
    }
  };

  convertToModel = function(data) {
    if (Rivets.config.adapter.convertToModel) {
      return Rivets.config.adapter.convertToModel(data);
    } else {
      return data;
    }
  };

  createInputBinder = function(routine) {
    return {
      publishes: true,
      bind: function(el) {
        return this.currentListener = bindEvent(el, 'change', this.publish);
      },
      unbind: function(el) {
        return unbindEvent(el, 'change', this.currentListener);
      },
      routine: routine
    };
  };

  Rivets.binders = {
    enabled: function(el, value) {
      return el.disabled = !value;
    },
    disabled: function(el, value) {
      return el.disabled = !!value;
    },
    checked: createInputBinder(function(el, value) {
      return el.checked = el.type === 'radio' ? el.value === value : !!value;
    }),
    unchecked: createInputBinder(function(el, value) {
      return el.checked = el.type === 'radio' ? el.value !== value : !value;
    }),
    show: function(el, value) {
      return el.style.display = value ? '' : 'none';
    },
    hide: function(el, value) {
      return el.style.display = value ? 'none' : '';
    },
    html: function(el, value) {
      return el.innerHTML = value != null ? value : '';
    },
    value: createInputBinder(function(el, value) {
      var o, _i, _len, _ref, _results;
      if (el.type === 'select-multiple') {
        if (value != null) {
          _results = [];
          for (_i = 0, _len = el.length; _i < _len; _i++) {
            o = el[_i];
            _results.push(o.selected = (_ref = o.value, __indexOf.call(value, _ref) >= 0));
          }
          return _results;
        }
      } else {
        return el.value = value != null ? value : '';
      }
    }),
    text: function(el, value) {
      var newValue;
      newValue = value != null ? value : '';
      if (el.innerText != null) {
        return el.innerText = newValue;
      } else {
        return el.textContent = newValue;
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
        return this.currentListener = bindEvent(el, firstArg, value, this.model, this.options.bindContext);
      }
    },
    "each-*": {
      block: true,
      bind: function(el, collection) {
        return el.removeAttribute(['data', rivets.config.prefix, this.type].join('-').replace('--', '-'));
      },
      routine: function(el, collection) {
        var e, iterated, marker, parentNode, view, _i, _j, _len, _len1, _ref,
          _this = this;
        iterated = this.iterated;
        if (iterated != null) {
          for (_i = 0, _len = iterated.length; _i < _len; _i++) {
            view = iterated[_i];
            view.unbind();
            _ref = view.els;
            for (_j = 0, _len1 = _ref.length; _j < _len1; _j++) {
              e = _ref[_j];
              e.parentNode.removeChild(e);
            }
          }
        } else {
          marker = this.marker = document.createComment(" rivets: " + this.type + " ");
          parentNode = el.parentNode;
          parentNode.insertBefore(marker, el);
          parentNode.removeChild(el);
        }
        this.iterated = iterated = [];
        if (collection) {
          marker = this.marker;
          return iterate(collection, function(item, i) {
            var data, itemEl, previous, _ref1;
            data = {};
            iterate(_this.view.models, function(item, i) {
              return data[i] = item;
            });
            data[_this.args[0]] = item;
            data["" + _this.args[0] + "_index"] = data['rivets_index'] = i;
            data = convertToModel(data);
            itemEl = el.cloneNode(true);
            previous = iterated.length > 0 ? iterated[iterated.length - 1].els[0] : marker;
            marker.parentNode.insertBefore(itemEl, (_ref1 = previous.nextSibling) != null ? _ref1 : null);
            return iterated.push(rivets.bind(itemEl, data));
          });
        }
      }
    },
    "class-*": function(el, value) {
      var elClass;
      elClass = " " + el.className + " ";
      if (!value === (elClass.indexOf(" " + this.args[0] + " ") !== -1)) {
        return el.className = value ? "" + el.className + " " + this.args[0] : elClass.replace(" " + this.args[0] + " ", ' ').trim();
      }
    },
    "*": function(el, value) {
      if (value) {
        return el.setAttribute(this.type, value);
      } else {
        return el.removeAttribute(this.type);
      }
    }
  };

  Rivets.config = {
    preloadData: true
  };

  Rivets.formatters = {};

  rivets = {
    binders: Rivets.binders,
    formatters: Rivets.formatters,
    config: Rivets.config,
    configure: function(options) {
      var property, value;
      options || (options = {});
      for (property in options) {
        value = options[property];
        Rivets.config[property] = value;
      }
    },
    bind: function(el, models) {
      var view;
      models || (models = {});
      view = new Rivets.View(el, models);
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
