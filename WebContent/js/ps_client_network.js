/*
 * PS NETWORK v2
 *
 *

 *
 */

if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.slice(0, str.length) == str;
  };
}


var Class =
{

	create: function()
	{
	    function klass()
	    {
	        this.initialize.apply(this, arguments);
	    }

		if(arguments[0].hasOwnProperty('toString'))
			klass.prototype['toString'] = arguments[0]['toString'];
		if(arguments[0].hasOwnProperty('valueOf'))
			klass.prototype['valueOf'] = arguments[0]['valueOf']

		for (p in arguments[0])
		{
			klass.prototype[p] = arguments[0][p];
		}
		return klass;
	}
}


var ua = navigator.userAgent;
var IS_ANDROID =  /Android/i.test(ua);
var IS_IPAD =  /iPad/i.test(ua);
var IS_IPHONE = /iPhone/i.test(ua);
var IS_IOS =  IS_IPAD || IS_IPHONE;
var IS_WEBKIT =  IS_IOS || /webkit/i.test(ua);



var Logger = Class.create(
{
	initialize: function()
	{

	}
});


Logger.log = function(o, clear)
	{
		clear = (clear ? clear : false);
		try { console.log(o);} catch(e){}
	};
	/*
	  param o Object
	  param clear Boolean
	  returns void
	*/
Logger.debug = function(o, clear)
	{
		clear = (clear ? clear : false);
		try { console.log(o);} catch(e){}
	};
	/*
	  param o Object
	  param clear Boolean
	  returns void
	*/
Logger.error = function(o, clear)
	{
		clear = (clear ? clear : false);
		try { console.log("ERROR"); console.log(o); } catch(e){}
	};

/*
  Entity
  import com_pagesociety_web_amf_AmfLong
*/

var com_pagesociety_persistence_Entity = Class.create(
{
	initialize: function(type, id)
	{
		this._def/*EntityDefinition*/;
		this._type/*String*/;
		this._id/*AmfLong*/;
		this._attributes = {  }/*Object*/;
		this._dirty_attributes = [  ]/*Array*/;
		this._fieldnames = [  ]/*Array*/;
		this._ps_clazz = "Entity";
		type = (type ? type : null);
		id = (id ? id : -1);
		if (type instanceof String)
		{
			this._type = type;
			this._id = id < 1 ? com_pagesociety_persistence_Entity.UNDEFINED : id;
		}
		else
		{
			this._type = type.type;
			this._id = type.id;
			this._attributes = type.attributes;
		}
		this._attributes.id = this.id;

	},
	/*
	  param name String
	  param attr Object
	  returns void
	*/
	setAttribute: function(name, attr)
	{
		if (name == null)
			throw new Error("NULL name?");
		if (this._dirty_attributes.indexOf(name) == -1)
			this._dirty_attributes.push(name);
		this._attributes[name] = attr;
		if (this._fieldnames.indexOf(name) == -1)
			this._fieldnames.push(name);
	},

	/*
	  param e Entity
	  returns Boolean
	*/
	eq: function(e)
	{
		if (e == null)
			return false;
		return this._type == e.getType() && this._id == e.getId();
	},

	/*
	  returns String
	*/
	toString: function()
	{
		return this._type + " ";
	},

	/*
	  param masked_fields Array
	  returns String
	*/
	toStringLong: function(masked_fields)
	{
		masked_fields = (masked_fields ? masked_fields : null);
		var s = "Entity " + this.getType();
		for (var p in this.getAttributes())


		{
		if (masked_fields != null && masked_fields.indexOf(p) != -1)
				continue;
		var o = this.getAttributes()[p];
		s += "  " + p;
		if (o == null)
			{
				p += "null";
			}
			else
				if (o instanceof Date)
				{
					var d = o/* as Date*/;
					if (isNaN(d./*?Date*/fullYear))
							continue;
					s += (d./*?Date*/month + 1) + "-";
				}
				else
					if (o instanceof com_pagesociety_persistence_Entity)
					{
						s += o./*?Object*/type + ":";
					}
					else
						if (o instanceof Array)
						{
							s += "[" + o./*?Object*/length + "]";
						}
						else
						{
							s += o;
						}
		s += "\n";
		}

		return s;
	},

	/*
	  param seen Object
	  returns Entity
	*/
	clone: function(seen)
	{
		seen = (seen ? seen : null);
		if (seen == null)
			seen = new Object();
		var sid = this.getType() + this.getId();
		if (seen[sid] != null)
			return seen[sid];
		var target = new com_pagesociety_persistence_Entity(this.getType(), this.getId().getLongValue());
		seen[sid] = target;
		var p;
		var i;
		for (p in this.getAttributes())


		{
		var v = this.getAttributes()[p];
		if (v instanceof Array && v./*?Object*/length != 0 && v[0] instanceof com_pagesociety_persistence_Entity)
			{
				target.getAttributes()[p] = new Array(v./*?Object*/length);
				for (i = 0; i < v./*?Object*/length; i++)
						target.getAttributes()[p] = (v[i]/* as com_pagesociety_persistence_Entity*/)./*?(*/clone(seen);
			}
			else
				if (v instanceof Array)
				{
					target.getAttributes()[p] = new Array(v./*?Object*/length);
					for (i = 0; i < v./*?Object*/length; i++)
						{
							target.getAttributes()[p] = v[i];
						}
				}
				else
					if (v instanceof com_pagesociety_persistence_Entity)
					{
						target.getAttributes()[p] = (v/* as com_pagesociety_persistence_Entity*/)./*?(*/clone(seen);
					}
					else
					{
						target.getAttributes()[p] = v;
					}
		}

		return target;
	},

	/*
	  returns Entity
	*/
	cloneShallow: function()
	{
		var target = new com_pagesociety_persistence_Entity(this.getType(), this.getId().getLongValue());
		target.setDirtyAttributes(this.setDirtyAttributes);
		var p;
		var i;
		var e0;
		for (p in this.getAttributes())


		{
		var v = this.getAttributes()[p];
		if (v instanceof Array && v./*?Object*/length != 0 && v[0] instanceof com_pagesociety_persistence_Entity)
			{
				target.getAttributes()[p] = new Array(v./*?Object*/length);
				for (i = 0; i < v./*?Object*/length; i++)
					{
						e0 = v[i]/* as com_pagesociety_persistence_Entity*/;
						target.getAttributes()[p] = new com_pagesociety_persistence_Entity(e0.getType(), e0.getId().getLongValue());
					}
			}
			else
				if (v instanceof Array)
				{
					target.getAttributes()[p] = new Array(v./*?Object*/length);
					for (i = 0; i < v./*?Object*/length; i++)
						{
							target.getAttributes()[p] = v[i];
						}
				}
				else
					if (v instanceof com_pagesociety_persistence_Entity)
					{
						e0 = v/* as com_pagesociety_persistence_Entity*/;
						target.getAttributes()[p] = new com_pagesociety_persistence_Entity(e0.getType(), e0.getId().getLongValue());
					}
					else
					{
						target.getAttributes()[p] = v;
					}
		}

		return target;
	},

	/*
	  returns String
	*/
	getType: function()
	{
		return this._type;
	},

	/*
	  returns AmfLong
	*/
	getId: function()
	{
		return this._id;
	},

	/*
	  returns EntityDefinition
	*/
	getDefinition: function()
	{
		return this._def;
	},

	/*
	  returns Object
	*/
	getAttributes: function()
	{
		return this._attributes;
	},

	/*
	  returns Object
	*/
	get$: function()
	{
		return this._attributes;
	},

	/*
	  returns Array
	*/
	getDirtyAttributes: function()
	{
		return this._dirty_attributes;
	},

	/*
	  returns Object
	*/
	getDirtyValues: function()
	{
		var a = {  };
		for (var i = 0; i < this._dirty_attributes.length; i++)
			a[this._dirty_attributes[i]] = this._attributes[this._dirty_attributes[i]];
		return a;
	},

	/*
	  param type String
	  returns void
	*/
	setType: function(type)
	{
		this._type = type;
		if (com_pagesociety_persistence_Entity.DEF_PROVIDER != null)
		{
			this._def = com_pagesociety_persistence_Entity.DEF_PROVIDER.provideEntityDefinition(this._type);
		}
	},

	/*
	  param id AmfLong
	  returns void
	*/
	setId: function(id)
	{
		this._id = id;
		this._attributes.id = id;
	},

	/*
	  param attr Object
	  returns void
	*/
	setAttributes: function(attr)
	{
		this._attributes = attr;
		this._fieldnames = new Array();
		for (var p in this._attributes)
			this._fieldnames.push(p);
	},

	/*
	  param attr Array
	  returns void
	*/
	setDirtyAttributes: function(attr)
	{
		this._dirty_attributes = attr;
	}

});

com_pagesociety_persistence_Entity.UNDEFINED = -1;
com_pagesociety_persistence_Entity.DEF_PROVIDER;
com_pagesociety_persistence_Entity.INDICES;
	/*
	  param defs Array
	  returns void
	*/
com_pagesociety_persistence_Entity.setDefinitions = function(defs)
	{
		com_pagesociety_persistence_Entity.DEF_PROVIDER = new com.pagesociety.persistence.DefaultEntityProvider(defs);
	};
	/*
	  returns Array
	*/
com_pagesociety_persistence_Entity.getDefinitions = function()
	{
		return com_pagesociety_persistence_Entity.DEF_PROVIDER.provideEntityDefinitions();
	};
	/*
	  param type String
	  returns EntityDefinition
	*/
com_pagesociety_persistence_Entity.getDefinition = function(type)
	{
		return com_pagesociety_persistence_Entity.DEF_PROVIDER.provideEntityDefinition(type);
	};
	/*
	  param indices Object
	  returns void
	*/
com_pagesociety_persistence_Entity.setIndices = function(indices)
	{
		com_pagesociety_persistence_Entity.INDICES = indices;
		var defs = com_pagesociety_persistence_Entity.DEF_PROVIDER.provideEntityDefinitions();
		for (var i = 0; i < defs.length; i++)
		{
			var def = defs[i];
			def.setIndices(indices[def.getName()]);
		}
	};
	/*
	  param e Entity
	  param pf String
	  returns void
	*/
com_pagesociety_persistence_Entity.dump = function(e, pf)
	{
		pf = (pf ? pf : "");
		if (pf.length > 10)
			throw new Error("TOO DEEEEP");
		for (var p in e.getAttributes())
		{
		var v = e.getAttributes()[p];
		if (v instanceof Array && v.length != 0 && v[0] instanceof com_pagesociety_persistence_Entity)
			{
				for (var i = 0; i < v.length; i++)
						com_pagesociety_persistence_Entity.dump(v[i], pf + "  ");
			}
			else
				if (v instanceof com_pagesociety_persistence_Entity)
				{
					com_pagesociety_persistence_Entity.dump(v/* as com_pagesociety_persistence_Entity*/, pf + "  ");
				}
				else
				{
					com.pagesociety.persistence.trace(pf + p);
				}
		}

	};
/*
  ModuleConnection
*/

var com_pagesociety_web_ModuleConnection = Class.create(
{
	initialize: function($super)
	{

	}
});

	/*
	  param mm String
	  param a Array
	  param ok Function
	  param err Function
	  param concurrentOk Boolean
	  returns void
	*/
com_pagesociety_web_ModuleConnection.doModule = function(mm, a, ok, err, concurrentOk)
	{
		concurrentOk = (concurrentOk ? concurrentOk : false);
		if (err == null)
			err = function (e)
			{
				throw new Error("ERROR " + com_pagesociety_web_ModuleRequest.SERVICE_URL + " ");
			};
		com_pagesociety_web_ModuleRequest.doModule(mm, a, ok, err, concurrentOk);
	};
/*
  ModuleRequest
  import com_pagesociety_ux_INetworkEventHandler
  import flash_events_AsyncErrorEvent
  import flash_events_Event
  import flash_events_IOErrorEvent
  import flash_events_NetStatusEvent
  import flash_events_SecurityErrorEvent
  import flash_events_TimerEvent
  import flash_net_NetConnection
  import flash_net_Responder
  import flash_utils_Timer
*/

var com_pagesociety_web_ModuleRequest = Class.create(
{
	initialize: function(module_and_method, args)
	{
//		this._connection/*NetConnection*/;
		this._module_name/*String*/;
		this._method_name/*String*/;
		this._arguments/*Array*/;
		this._result_handlers/*Array*/;
		this._error_handlers/*Array*/;
		module_and_method = (module_and_method ? module_and_method : null);
		args = (args ? args : null);
		this._result_handlers = new Array();
		this._error_handlers = new Array();
		this._arguments = (args == null) ? new Array() : args;
		if (module_and_method != null)
		{
			var s = module_and_method.split("/");
			if (s.length == 2)
				{
					this._module_name = s[0];
					this._method_name = s[1];
				}
		}

	},
	/*
	  param f Function
	  returns void
	*/
	addResultHandler: function(f)
	{
		this._result_handlers.push(f);
	},

	/*
	  param f Function
	  returns void
	*/
	addErrorHandler: function(f)
	{
		this._error_handlers.push(f);
	},

	/*
	  returns void
	*/
	execute: function()
	{
		if (this._concurrent_ok)
			this.connect();
		else
			com_pagesociety_web_ModuleRequest.QUEUE_LOAD(this);
	},

	/*
	  returns void
	*/
	do_execute: function()
	{
		if (com_pagesociety_web_ModuleRequest._timer != null)
		{
			com_pagesociety_web_ModuleRequest._timer.reset();
			com_pagesociety_web_ModuleRequest._timer.start();
		}
		this.connect();
	},




	//var base_url = base_url || "";

	connect : function()
		{
//	Logger.log("MODULE REQUEST: " + this._module_name+"/"+this._method_name+' '+this._arguments.join(','));
			var T = this;
			var module_request_url =   com_pagesociety_web_ModuleRequest.SERVICE_URL +"/"+this._module_name+"/"+this._method_name + "/.json";
			var on_success = function(response){
				var ret = response.value;
				if(ret != null && ret.exception != null )
				{
					//Logger.log("1: fail");
					//Logger.log(ret);
					T.on_error(ret);
				}
				else
				{
					if(ret != null)
					{
						ret = com_pagesociety_web_ModuleRequest.expand_entities(ret);
					}
//					console.log("1: success", response, response.responseJSON);
					T.on_result(ret);
				}

			}


			if (com_pagesociety_web_ModuleRequest.USE_JSONP)
				jsonp(module_request_url, this._arguments, on_success);
			else
			{
				var as = JSON.stringify(this._arguments);
				as = encodeURIComponent(as);
				try {
					AJAX(module_request_url+"?args="+as+"&encode=false", "GET",
							function(r) { on_success(JSON.parse(decodeURIComponent(r))); },
							function(r){console.log(r);});
				} catch(e){}
			}
		},


	/*
	  param o Object
	  returns void
	*/
	on_result: function(o)
	{
		this.close();
		for (var i = 0; i < this._result_handlers.length; i++)
			this._result_handlers[i](o);
		com_pagesociety_web_ModuleRequest.QUEUE_COMPLETE(this, com_pagesociety_web_ModuleRequest.STATUS_OK, o);
	},

	/*
	  param o Object
	  returns void
	*/
	on_error: function(o)
	{
		this.close();
		for (var i = 0; i < this._error_handlers.length; i++)
			this._error_handlers[i]
		(o);
		com_pagesociety_web_ModuleRequest.QUEUE_COMPLETE(this, com_pagesociety_web_ModuleRequest.STATUS_ERR, o);
	},

	/*
	  returns void
	*/
	close: function()
	{
		if (this._connection != null)
		{
			this._connection.close();
			this._connection = null;
		}
	},




	/*
	  returns Boolean
	*/
	getConnected: function()
	{
		return (this._connection != null);
	},

	/*
	  returns String
	*/
	getModule: function()
	{
		return this._module_name;
	},

	/*
	  returns String
	*/
	getMethod: function()
	{
		return this._method_name;
	},

	/*
	  returns Array
	*/
	getArguments: function()
	{
		return this._arguments;
	},

	/*
	  returns Boolean
	*/
	getConcurrentOk: function()
	{
		return this._concurrent_ok;
	},

	/*
	  param name String
	  returns void
	*/
	setModule: function(name)
	{
		this._module_name = name;
	},

	/*
	  param args Array
	  returns void
	*/
	setArguments: function(args)
	{
		this._arguments = args;
	},

	/*
	  param b Boolean
	  returns void
	*/
	setConcurrentOk: function(b)
	{
		this._concurrent_ok = b;
	}

});

com_pagesociety_web_ModuleRequest.SERVICE_URL;
com_pagesociety_web_ModuleRequest.USE_JSONP = false;
com_pagesociety_web_ModuleRequest._concurrent_ok;
com_pagesociety_web_ModuleRequest._networking;
com_pagesociety_web_ModuleRequest._net_event_handler;
com_pagesociety_web_ModuleRequest._timeout = 12000;
com_pagesociety_web_ModuleRequest._timer;
com_pagesociety_web_ModuleRequest._last_event = 0;
com_pagesociety_web_ModuleRequest._QUEUE = [];
com_pagesociety_web_ModuleRequest._QUEUE_LOAD_COUNT = 0;
com_pagesociety_web_ModuleRequest._QUEUE_MAX_SIMULATANEOUS_LOAD = 1;
com_pagesociety_web_ModuleRequest.STATUS_OK = 0;
com_pagesociety_web_ModuleRequest.STATUS_ERR = 1;
	/*
	  param mm String
	  param a Array
	  param ok Function
	  param err Function
	  param concurrentOk Boolean
	  returns void
	*/
com_pagesociety_web_ModuleRequest.doModule = function(mm, a, ok, err, concurrentOk)
	{
		concurrentOk = (concurrentOk ? concurrentOk : false);
		var m = new com_pagesociety_web_ModuleRequest(mm, a);
		m.setConcurrentOk(concurrentOk);
		m.addErrorHandler(err);
		m.addResultHandler(ok);
		m.execute();
	};
	/*
	  param e Event
	  returns void
	*/
com_pagesociety_web_ModuleRequest.on_timeout = function(e)
	{
		e = (e ? e : null);
		if (com_pagesociety_web_ModuleRequest._net_event_handler == null)
			return
		com_pagesociety_web_ModuleRequest._net_event_handler.timeout();
	};
	/*
	  param r ModuleRequest
	  returns void
	*/
com_pagesociety_web_ModuleRequest.QUEUE_LOAD = function(r)
	{
		com_pagesociety_web_ModuleRequest._QUEUE.push(r);
		com_pagesociety_web_ModuleRequest.QUEUE_DO_LOAD();
	};
	/*
	  returns void
	*/
com_pagesociety_web_ModuleRequest.QUEUE_DO_LOAD = function()
	{
		var s = com_pagesociety_web_ModuleRequest._QUEUE_MAX_SIMULATANEOUS_LOAD - com_pagesociety_web_ModuleRequest._QUEUE_LOAD_COUNT;
		/*	_networking = false;
			if (_net_event_handler!=null)
				_net_event_handler.networking = _networking;
			*/;
		if (com_pagesociety_web_ModuleRequest._QUEUE.length == 0)
		{
			return ;
		}
		/*
		if (!_networking && _QUEUE.length!=0)
		{
			_networking = true;
			if (_net_event_handler!=null)
				_net_event_handler.networking = _networking;
		}
		*/;
		for (var i = 0; i < s && com_pagesociety_web_ModuleRequest._QUEUE.length != 0; i++)
		{
			var r = com_pagesociety_web_ModuleRequest._QUEUE.shift();
			r.do_execute();
			if (com_pagesociety_web_ModuleRequest._net_event_handler != null)
					com_pagesociety_web_ModuleRequest._net_event_handler.beginModuleRequest(r);
			com_pagesociety_web_ModuleRequest._QUEUE_LOAD_COUNT++;
		}
	};
	/*
	  param r ModuleRequest
	  param status uint
	  param arg Object
	  returns void
	*/
com_pagesociety_web_ModuleRequest.QUEUE_COMPLETE = function(r, status, arg)
	{
		status = (status ? status : com_pagesociety_web_ModuleRequest.STATUS_OK);
		arg = (arg ? arg : null);
		if (com_pagesociety_web_ModuleRequest._net_event_handler != null)
			com_pagesociety_web_ModuleRequest._net_event_handler.endModuleRequest(r, status, arg);
		com_pagesociety_web_ModuleRequest._QUEUE_LOAD_COUNT--;
		com_pagesociety_web_ModuleRequest.QUEUE_DO_LOAD();
	};
	/*
	  returns int
	*/
com_pagesociety_web_ModuleRequest.getTimeout = function()
	{
		return com_pagesociety_web_ModuleRequest._timeout;
	};
	/*
	  param n INetworkEventHandler
	  returns void
	*/
com_pagesociety_web_ModuleRequest.setNetworkEventHandler = function(n)
	{
		if (com_pagesociety_web_ModuleRequest._timer == null)
		{
			com_pagesociety_web_ModuleRequest._timer = new Timer(com_pagesociety_web_ModuleRequest._timeout);
			com_pagesociety_web_ModuleRequest._timer.addEventListener(TimerEvent.TIMER, com_pagesociety_web_ModuleRequest.on_timeout);
		}
		com_pagesociety_web_ModuleRequest._timer.reset();
		com_pagesociety_web_ModuleRequest._timer.start();
		com_pagesociety_web_ModuleRequest._net_event_handler = n;
	};
	/*
	  param timeout int
	  returns void
	*/
com_pagesociety_web_ModuleRequest.setTimeout = function(timeout)
	{
		com_pagesociety_web_ModuleRequest._timeout = timeout;
	};





com_pagesociety_web_ModuleRequest.entity_pool = {}
com_pagesociety_web_ModuleRequest.expand_entities = function (o)
	{
		var otype = com_pagesociety_web_ModuleRequest.typeOf(o);

		if(otype == 'object')
		{
			if (o._circular_ref)
			{
				var ref = com_pagesociety_web_ModuleRequest.entity_pool[o._object_id];
				if (ref==null)
					throw new Error("com_pagesociety_web_ModuleRequest.expand_entities - no ref to "+o._object_id);
				return ref;
			}
			if(o._ps_clazz == "Entity")
			{
				if(o.attributes == null)
					o.attributes = new Object();
				var e = new com_pagesociety_persistence_Entity(o);
			 	e.setId(o.id);
			 	e.setType(o.type);
				com_pagesociety_web_ModuleRequest.entity_pool[o._object_id] = e;
				delete o._object_id;
				for(k in o.attributes)
			 	{
			 		o.attributes[k] = com_pagesociety_web_ModuleRequest.expand_entities(o.attributes[k]);
			 	}
				return e;
			}
			else
			{
				com_pagesociety_web_ModuleRequest.entity_pool[o._object_id] = o;
				delete o._object_id;
				for(k in o)
			 	{
					o[k] = com_pagesociety_web_ModuleRequest.expand_entities(o[k]);
			 	}
			}
		}
		else if (otype == 'array')
		{
 			for(var i = 0;i < o.length;i++)
 			{
 				o[i] = com_pagesociety_web_ModuleRequest.expand_entities(o[i]);
 			}

		}
		return o;
	};



com_pagesociety_web_ModuleRequest.typeOf = function(value)
	{
	    var s = typeof value;
	    if (s === 'object')
	    {
	        if (value) {
	            if (typeof value.length === 'number' &&
	                    !(value.propertyIsEnumerable('length')) &&
	                    typeof value.splice === 'function') {
	                s = 'array';
	            }
	        } else {
	            s = 'null';
	        }
		}
		return s;
	};




/*
  PathProvider
  import com_pagesociety_persistence_Entity
  import com_pagesociety_util_ObjectUtil
  import com_pagesociety_web_module_Resource
  import flash_system_Security
*/

var com_pagesociety_web_PathProvider = Class.create(
{
	initialize: function(s3_bucket)
	{
		this.base_url/*String*/;
		if (s3_bucket.indexOf("http://") == 0)
		{
			var s = s3_bucket.split("/");
			//flash.system.Security./*?Security*/loadPolicyFile("http://" + s[2]);
			this.base_url = s3_bucket;
		}
		else
		{
			//Security./*?Security*/loadPolicyFile("http://" + s3_bucket);
			this.base_url = "http://" + s3_bucket;
		}
		if (this.base_url.charAt(this.base_url.length - 1) != "/")
			this.base_url += "/";

	},
	/*
	  param resource Entity
	  param width int
	  param height int
	  returns String
	*/
	getPath: function(resource, width, height)
	{
		width = (width ? width : -1);
		height = (height ? height : -1);
		var path_token = resource.get$()['path-token'];
		if (path_token == null)
			return null;
		var preview_name = new String();
		if (width == -1 || height == -1)
		{
			preview_name = path_token;
		}
		else
		{
			var dot_idx = path_token.lastIndexOf('.');
			var ext = "jpg";
			if (dot_idx != -1 && path_token.length - 1 > dot_idx)
				{
					ext = path_token.substring(dot_idx + 1).toLowerCase();
					path_token = path_token.substring(0, dot_idx);
				}
			preview_name += (path_token);
			preview_name += ('_');
			preview_name += (width.toString());
			preview_name += ('x');
			preview_name += (height.toString());
			preview_name += ('.');
			preview_name += ext;
		}
		return this.base_url + preview_name;
	},

	/*
	  param o Object
	  param width int
	  param height int
	  returns void
	*/
	getPreviewUrls: function(o, width, height)
	{
		if (o == null)
		{
			return ;
		}
		var i;
		if (o instanceof Array)
		{
			var results = o/* as Array*/;
			for (i = 0; i < results.length; i++)
					this.fill_e(results[i], width, height);
		}
		else
			if (o instanceof com_pagesociety_persistence_Entity)
			{
				this.fill_e(o/* as com_pagesociety_persistence_Entity*/, width, height);
			}
			else
			{
				throw new Error("Can't fill that kind of thing");
			}
	},

	/*
	  param e Entity
	  param width int
	  param height int
	  returns void
	*/
	fill_e: function(e, width, height)
	{
		if (e == null)
			return
		var i;
		var j;
		if (com_pagesociety_util_ObjectUtil.isResource(e))
		{
			e.get$().url = this.getPath(e, width, height);
		}
		else
		{
			for (var p in e.getAttributes())
			{

				var o = e.get$()[p];
				if (o instanceof com_pagesociety_persistence_Entity)
						this.fill_e(o/* as com_pagesociety_persistence_Entity*/, width, height);
					else
						if (o instanceof Array && o./*?Object*/length != 0 && o[0] instanceof com_pagesociety_persistence_Entity)
							for (j = 0; j < o./*?Object*/length; j++)
								this.fill_e(o[j], width, height);
			}

		}
	}

});

/*
  ResourceModuleProvider
  import com_pagesociety_persistence_Entity
  import com_pagesociety_web_amf_AmfLong
  import com_pagesociety_web_module_User
*/

var com_pagesociety_web_ResourceModuleProvider = Class.create(
{
	initialize: function(module_name, type, root_url)
	{
		this._module_name/*String*/;
		this._type/*String*/;
		this._path_provider/*PathProvider*/;
		this._module_name = module_name;
		this._type = type;
		if (root_url != null)
			this._path_provider = new com_pagesociety_web_PathProvider(root_url);

	},
	/*
	  returns String
	*/
	CreateResource: function()
	{
		return this._module_name + "/CreateResource";
	},

	/*
	  returns String
	*/
	DeleteResource: function()
	{
		return this._module_name + "/DeleteResource";
	},

	/*
	  returns String
	*/
	UpdateResource: function()
	{
		return this._module_name + "/UpdateResource";
	},

	/*
	  returns String
	*/
	CancelUpload: function()
	{
		return this._module_name + "/CancelUpload";
	},

	/*
	  returns String
	*/
	GetResourceUrl: function()
	{
		return this._module_name + "/GetResourceURL";
	},

	/*
	  returns String
	*/
	GetResourceUrlWithDim: function()
	{
		return this._module_name + "/GetResourcePreviewURLWithDim";
	},

	/*
	  returns String
	*/
	GetUploadProgress: function()
	{
		return this._module_name + "/GetUploadProgress";
	},

	/*
	  returns String
	*/
	GetSessionId: function()
	{
		return com_pagesociety_web_module_User.METHOD_GETSESSIONID;
	},

	/*
	  param id AmfLong
	  param on_complete Function
	  param on_error Function
	  returns void
	*/
	getResourceUrl: function(id, on_complete, on_error)
	{
		com_pagesociety_web_ModuleConnection.doModule(this.GetResourceUrl(), [ id ], on_complete, on_error, true);
	},

	/*
	  param id AmfLong
	  param w Number
	  param h Number
	  param on_complete Function
	  param on_error Function
	  returns void
	*/
	getResourceUrlWithDim: function(id, w, h, on_complete, on_error)
	{
		com_pagesociety_web_ModuleConnection.doModule(this.GetResourceUrlWithDim(), [ id, w, h ], on_complete, on_error, true);
	},

	/*
	  param resource Entity
	  param w Number
	  param h Number
	  returns String
	*/
	getPath: function(resource, w, h)
	{
		if (this._path_provider == null)
			throw new Error("NO PATH PROVIDER DEFINED FOR " + resource);
		return this._path_provider.getPath(resource, w, h);
	}

});

/*
  ResourceUtil
  import com_pagesociety_persistence_Entity
  import com_pagesociety_persistence_FieldDefinition
  import com_pagesociety_ux_system_LoadAndSize
*/

var com_pagesociety_web_ResourceUtil = Class.create(
{
	initialize: function()
	{

	}
});

com_pagesociety_web_ResourceUtil.DEBUG = false;
com_pagesociety_web_ResourceUtil.RESOURCE_MAP;
	/*
	  param data Array
	  returns void
	*/
com_pagesociety_web_ResourceUtil.init = function(data)
	{
		if (com_pagesociety_web_ResourceUtil.RESOURCE_MAP != null)
		{
			Logger.log("Resource Map has already been initialized...");
			return ;
		}
		com_pagesociety_web_ResourceUtil.RESOURCE_MAP = {  };
		for (var i = 0; i < data.length; i++)
		{
			var info = data[i];
			var module_name = info.resource_module_name;
			var entity_type = info.resource_entity_name;
			var base_url = info.resource_base_url;
			if (com_pagesociety_web_ResourceUtil.RESOURCE_MAP[entity_type] != null)
					throw new Error("ResourceUtil.init ERROR: Registering entity " + entity_type);
			com_pagesociety_web_ResourceUtil.RESOURCE_MAP[entity_type] = new com_pagesociety_web_ResourceModuleProvider(module_name, entity_type, base_url);
			//Logger.log("Registered ResourceModule " + entity_type);
		}
	};
	/*
	  param field FieldDefinition
	  returns Boolean
	*/
com_pagesociety_web_ResourceUtil.isResource = function(field)
	{
		return com_pagesociety_web_ResourceUtil.RESOURCE_MAP[field.getReferenceType()] != null;
	};
	/*
	  param o Object
	  returns ResourceModuleProvider
	*/
com_pagesociety_web_ResourceUtil.getModuleProvider = function(o)
	{
		var type;
		if (o instanceof String)
			type = o;
		else
			if (o instanceof com_pagesociety_persistence_FieldDefinition)
				type = o.referenceType;
			else
				throw new Error("CANT getModuleProvider for " + o);
		return com_pagesociety_web_ResourceUtil.RESOURCE_MAP[type];
	};
	/*
	  param resource Entity
	  param w Number
	  param h Number
	  returns String
	*/
com_pagesociety_web_ResourceUtil.getPath = function(resource, w, h)
	{
		if (resource==null)
			return null;
		w = (w ? w : -1);
		h = (h ? h : -1);
		if (com_pagesociety_web_ResourceUtil.RESOURCE_MAP == null)
			throw new Error("ResourceUtil.RESOURCE_MAP is not configured");
		var resource_module = com_pagesociety_web_ResourceUtil.RESOURCE_MAP[resource.getType()];
		if (resource_module == null)
			throw new Error("ResourceUtil.RESOURCE_MAP does not contain " + resource.getType());
		return resource_module.getPath(resource, w, h);
	};
	/*
	  param resource Entity
	  param on_complete Function
	  returns void
	*/
com_pagesociety_web_ResourceUtil.getUrl = function(resource, on_complete)
	{
		if (resource == null)
		{
			on_complete(null);
		}
		else
			if (com_pagesociety_web_ResourceUtil.DEBUG)
			{
				on_complete(resource);
			}
			else
			{
				if (com_pagesociety_web_ResourceUtil.RESOURCE_MAP == null)
						throw new Error("ResourceUtil.RESOURCE_MAP is not configured");
				var resource_module = com_pagesociety_web_ResourceUtil.RESOURCE_MAP[resource.getType()];
				if (resource_module == null)
						throw new Error("ResourceUtil.RESOURCE_MAP does not contain " + resource.getType());
				resource_module.getResourceUrl(resource.getId(), on_complete, function (e)
				{
					Logger.log("ERROR");
				});
			}
	};
	/*
	  param resource Entity
	  param w uint
	  param h uint
	  param on_complete Function
	  returns void
	*/
com_pagesociety_web_ResourceUtil.getPreviewUrl = function(resource, w, h, on_complete)
	{
		if (resource == null)
		{
			on_complete(null);
		}
		else
			if (com_pagesociety_web_ResourceUtil.DEBUG)
			{
				new com_pagesociety_ux_system_LoadAndSize(resource.get$()./*?Object*/resource, w, h, on_complete);
			}
			else
			{
				if (com_pagesociety_web_ResourceUtil.RESOURCE_MAP == null)
						throw new Error("ResourceUtil.RESOURCE_MAP is not configured");
				var resource_module = com_pagesociety_web_ResourceUtil.RESOURCE_MAP[resource.getType()];
				if (resource_module == null)
						throw new Error("ResourceUtil.RESOURCE_MAP does not contain " + resource.getType());
				resource_module.getResourceUrlWithDim(resource.getId(), w, h, on_complete, function (e)
				{
					Logger.log("ERROR");
				});
			}
	};
	/*
	  param type String
	  returns Boolean
	*/
com_pagesociety_web_ResourceUtil.hasResourceModuleProvider = function(type)
	{
		return com_pagesociety_web_ResourceUtil.RESOURCE_MAP[type] != null;
	};
/*
  Locker
*/

var com_pagesociety_util_Locker = Class.create(
{
	initialize: function($super, locked)
	{
		this._func/*Function*/;
		this._locked/*Boolean*/;
		locked = (locked ? locked : false);
		this._locked = locked;

	},
	/*
	  returns void
	*/
	lock: function()
	{
		if (this._locked)
			return
		this._locked = true;
	},

	/*
	  returns void
	*/
	unlock: function()
	{
		if (!this._locked)
			return
		if (this._func != null)
		{
			this._func();
			this._func = null;
		}
		this._locked = false;
	},

	/*
	  param func Function
	  returns void
	*/
	wait: function(func)
	{
		if (!this._locked)
		{
			func();
			return ;
		}
		this._func = func;
	}

});

/*
  ObjectUtil
  import com_pagesociety_persistence_Entity
  import com_pagesociety_persistence_EntityDefinition
  import com_pagesociety_persistence_EntityIndex
  import com_pagesociety_persistence_FieldDefinition
  import com_pagesociety_ux_system_ResourceUtil
  import com_pagesociety_web_ErrorMessage
  import com_pagesociety_web_amf_AmfDouble
  import com_pagesociety_web_amf_AmfFloat
  import com_pagesociety_web_amf_AmfLong
  import com_pagesociety_web_upload_UploadProgressInfo
  import flash_net_registerClassAlias
  import flash_utils_ByteArray
  import flash_utils_describeType
  import flash_utils_getDefinitionByName
  import flash_utils_getQualifiedClassName
*/

var com_pagesociety_util_ObjectUtil = Class.create(
{
	initialize: function()
	{

	}
});

com_pagesociety_util_ObjectUtil._eclass = new Object();
com_pagesociety_util_ObjectUtil.default_entities_registered = false;
	/*
	  param target Object
	  param prop_expr String
	  returns Object
	*/
com_pagesociety_util_ObjectUtil.getProperty = function(target, prop_expr)
	{
		var s = prop_expr.split(".");
		var o = target;
		for (var i = 0; i < s.length - 1; i++)
		{
			o = o[s[i]];
			if (o == null)
				{
					com.pagesociety.util.trace("Style error " + prop_expr);
					return null;
				}
		}
		var lf = s[s.length - 1];
		try
		{
			var lo = o[lf];
			return lo;
		}
		catch (e)
		{
		}
		return null;
	};
	/*
	  param target Object
	  param prop_expr String
	  param val *
	  returns void
	*/
com_pagesociety_util_ObjectUtil.setProperty = function(target, prop_expr, val)
	{
		var s = prop_expr.split(".");
		var o = target;
		for (var i = 0; i < s.length - 1; i++)
		{
			o = o[s[i]];
			if (o == null)
				{
					com.pagesociety.util.trace("Style error " + prop_expr);
					return ;
				}
		}
		var lf = s[s.length - 1];
		try
		{
			o[lf] = val;
		}
		catch (e)
		{
		}
	};
	/*
	  param o *
	  returns *
	*/
com_pagesociety_util_ObjectUtil.replaceEntitiesWithObjects = function(o)
	{
		var no = null;
		var p;
		if (o instanceof com_pagesociety_persistence_Entity)
		{
			var e = o/* as com_pagesociety_persistence_Entity*/;
			no = com_pagesociety_util_ObjectUtil.get_instance(e.getType());
			if (no != null)
				{
					no./*?**/id = e.getId();
					no./*?**/type = e.getType();
					for (p in e.getAttributes())


					try
						{
							no[p] = com_pagesociety_util_ObjectUtil.replaceEntitiesWithObjects(e.getAttributes()[p]);
						}
					catch (e)
						{
						}
				}
		}
		if (no == null)
		{
			no = o;
			for (p in o)


			no[p] = com_pagesociety_util_ObjectUtil.replaceEntitiesWithObjects(o[p]);
		}
		return no;
	};
	/*
	  param name String
	  param clazz Class
	  returns void
	*/
com_pagesociety_util_ObjectUtil.registerEntityClass = function(name, clazz)
	{
		com_pagesociety_util_ObjectUtil._eclass[name] = clazz;
	};
	/*
	  param type String
	  returns *
	*/
com_pagesociety_util_ObjectUtil.get_instance = function(type)
	{
		if (com_pagesociety_util_ObjectUtil._eclass[type] == null)
			return null;
		return new com_pagesociety_util_ObjectUtil._eclass();
	};


	/*
	  param e Entity
	  returns Boolean
	*/
com_pagesociety_util_ObjectUtil.isResource = function(e)
	{
		return com_pagesociety_web_ResourceUtil.hasResourceModuleProvider(e.getType());
	};
	/*
	  param source Object
	  returns *
	*/
com_pagesociety_util_ObjectUtil.clone = function(source)
	{
		var copier = new flash.utils.ByteArray();
		copier./*?ByteArray*/writeObject(source);
		copier./*?ByteArray*/position = 0;
		return (copier./*?ByteArray*/readObject());
	};
	/*
	  param sourceObj Object
	  returns *
	*/
com_pagesociety_util_ObjectUtil.newSibling = function(sourceObj)
	{
		if (sourceObj)
		{
			var objSibling;
			try
				{
					var classOfSourceObj = flash.utils.getDefinitionByName(flash.utils.getQualifiedClassName(sourceObj))/* as com.pagesociety.util.Class*/;
					objSibling = new classOfSourceObj();
				}
			catch (e)
				{
				}
			return objSibling;
		}
		return null;
	};
	/*
	  param source Object
	  returns Object
	*/
com_pagesociety_util_ObjectUtil.clone1 = function(source)
	{
		var clone;
		if (source)
		{
			clone = com_pagesociety_util_ObjectUtil.newSibling(source);
			if (clone)
				{
					com_pagesociety_util_ObjectUtil.copyData(source, clone);
				}
		}
		return clone;
	};

	/*
	  param o Object
	  returns String
	*/
com_pagesociety_util_ObjectUtil.print = function(o)
	{
		var s = com_pagesociety_util_ObjectUtil.printo(o, "");
		Logger.log(s);
		return s;
	};
	/*
	  param o Object
	  param pad String
	  returns String
	*/
com_pagesociety_util_ObjectUtil.printo = function(o, pad)
	{
		var s = "";
		if (o instanceof String)
		{
			s += "\"" + o;
		}
		else
			if (o instanceof Number || o instanceof Boolean)
			{
				s += o;
			}
			else
				if (o instanceof Array)
				{
					s += "\n" + pad;
					for (var i = 0; i < o./*?Object*/length; i++)
						{
							s += pad;
							s += com_pagesociety_util_ObjectUtil.printo(o[i], pad + "  ");
							s += ",\n";
						}
					s += pad + "]\n";
				}
				else
					if (o instanceof Object)
					{
						s += "\n" + pad;
						for (var p in o)


													s += pad + p;
							s += com_pagesociety_util_ObjectUtil.printo(o[p], pad + "  ");
							s += ",\n";

						s += pad + "}\n";
					}
		return s;
	};
/*
  Random
*/

var com_pagesociety_util_Random = Class.create(
{
	initialize: function()
	{

	}
});

	/*
	  param max uint
	  returns uint
	*/
com_pagesociety_util_Random.R = function(max)
	{
		return Math.floor(Math.random() * max);
	};
	/*
	  param bottom int
	  param top int
	  returns uint
	*/
com_pagesociety_util_Random.RR = function(bottom, top)
	{
		var r = top - bottom;
		return com_pagesociety_util_Random.R(r) + bottom;
	};
	/*
	  param array Array
	  returns *
	*/
com_pagesociety_util_Random.A = function(array)
	{
		return array[com_pagesociety_util_Random.R(array.length)];
	};

com_pagesociety_util_Random.C = function()
	{
		return com_pagesociety_util_Random.R(2)==0;
	};
/*
  StringBuffer
*/

var com_pagesociety_util_StringBuffer = Class.create(
{
	initialize: function(init_val)
	{
		init_val = (init_val ? init_val : "");
		this.s = init_val;

	},
	/*
	  param a Object
	  returns void
	*/
	append: function(a)
	{
		  this.s += a;

	},

	/*
	  returns String
	*/
	toString: function()
	{

		  return this.s;
	}

});

/*
  StringUtil
*/

var com_pagesociety_util_StringUtil = Class.create(
{
	initialize: function()
	{

	}
});

	/*
	  param str String
	  param oldSubStr String
	  param newSubStr String
	  returns String
	*/
com_pagesociety_util_StringUtil.replace = function(str, oldSubStr, newSubStr)
	{
		return str.split(oldSubStr).join(newSubStr);
	};
	/*
	  param str String
	  returns String
	*/
com_pagesociety_util_StringUtil.trim = function(str)
	{
		str = com_pagesociety_util_StringUtil.trimBack(com_pagesociety_util_StringUtil.trimFront(str, " "), " ");
		str = com_pagesociety_util_StringUtil.trimBack(com_pagesociety_util_StringUtil.trimFront(str, "\t"), "\t");
		str = com_pagesociety_util_StringUtil.trimBack(com_pagesociety_util_StringUtil.trimFront(str, "\n"), "\n");
		str = com_pagesociety_util_StringUtil.trimBack(com_pagesociety_util_StringUtil.trimFront(str, "\r"), "\r");
		return str;
	};
	/*
	  param str String
	  param _char String
	  returns String
	*/
com_pagesociety_util_StringUtil.trimFront = function(str, _char)
	{
		if (str == null)
			return "";
		_char = com_pagesociety_util_StringUtil.stringToCharacter(_char);
		if (str.charAt(0) == _char)
		{
			str = com_pagesociety_util_StringUtil.trimFront(str.substring(1), _char);
		}
		return str;
	};
	/*
	  param str String
	  param _char String
	  returns String
	*/
com_pagesociety_util_StringUtil.trimBack = function(str, _char)
	{
		_char = com_pagesociety_util_StringUtil.stringToCharacter(_char);
		if (str.charAt(str.length - 1) == _char)
		{
			str = com_pagesociety_util_StringUtil.trimBack(str.substring(0, str.length - 1), _char);
		}
		return str;
	};
	/*
	  param s String
	  param max_length uint
	  returns String
	*/
com_pagesociety_util_StringUtil.shorten = function(s, max_length)
	{
		if (s == null)
			return "";
		if (s.length <= max_length)
			return s;
		return s.substr(0, max_length) + "...";
	};

	/*
	  param s String
	  returns String
	*/
com_pagesociety_util_StringUtil.stripTags = function(s)
	{
		if (s == null)
			return "";

		var b = new com_pagesociety_util_StringBuffer();

		var open = false;
		for (var i = 0; i < s.length; i++)
		{
			if (s.charAt(i) == '<')
				{
					open = true;
				}
			if (!open)
				{
					b.append(s.charAt(i));
				}
			if (s.charAt(i) == '>')
				{
					open = false;
					b.append(" ");
				}
		}

		return com_pagesociety_util_StringUtil.trim(b.toString());
	};
	/*
	  param str String
	  returns String
	*/
com_pagesociety_util_StringUtil.stringToCharacter = function(str)
	{
		if (str.length == 1)
		{
			return str;
		}
		return str.slice(0, 1);
	};
	/*
	  param i uint
	  returns String
	*/
com_pagesociety_util_StringUtil.twoDigitNumber = function(i)
	{
		if (i < 10)
			return "0" + i;
		else
			return i.toString();
	};
	/*
	  param n Number
	  param add_currency_symbol Boolean
	  returns String
	*/
com_pagesociety_util_StringUtil.formatPrice = function(n, add_currency_symbol)
	{
		add_currency_symbol = (add_currency_symbol ? add_currency_symbol : true);
		var cs = add_currency_symbol ? "$" : "";
		var ni = n;
		var nd = String(Math.floor((n - ni) * 100));
		if (nd.length == 1)
			nd = nd + "0";
		if (ni == 0)
			return cs + ".";
		else
			return cs + ni;
	};
	/*
	  param timeMillis uint
	  returns String
	*/
com_pagesociety_util_StringUtil.formatTime = function(timeMillis)
	{
		var time = Math.floor(timeMillis / 1000);
		var seconds = String(time % 60);
		var minutes = String(Math.floor((time % 3600) / 60));
		var hours = String(Math.floor(time / 3600));
		for (var i = 0; i < 2; i++)
		{
			if (seconds.length < 2)
					seconds = "0" + seconds;
			if (minutes.length < 2)
					minutes = "0" + minutes;
			if (hours.length < 2)
					hours = "0" + hours;
		}
		if (hours == "00")
			return minutes + ":";
		else
			return hours + ":";
	};
	/*
	  param d Date
	  returns String
	*/
com_pagesociety_util_StringUtil.formatDate = function(d)
	{
		if (d == null)
			return "";
		return d./*?Date*/fullYear + ".";
	};
	/*
	  param d Date
	  returns String
	*/
com_pagesociety_util_StringUtil.formatDateNoTime = function(d)
	{
		if (d == null)
			return "";
		return d./*?Date*/fullYear + ".";
	};
	/*
	  param d Date
	  returns String
	*/
com_pagesociety_util_StringUtil.formatTimeNoDate = function(d)
	{
		return com_pagesociety_util_StringUtil.twoDigitNumber(d./*?Date*/hours) + ":";
	};
	/*
	  param input String
	  param prefix String
	  returns Boolean
	*/
com_pagesociety_util_StringUtil.startsWith = function(input, prefix)
	{
		return input.length >= prefix.length && input.substr(0, prefix.length) == prefix;
	};
	/*
	  param input String
	  param prefix String
	  returns Boolean
	*/
com_pagesociety_util_StringUtil.beginsWith = function(input, prefix)
	{
		return com_pagesociety_util_StringUtil.startsWith(input, prefix);
	};
	/*
	  param input String
	  param suffix String
	  returns Boolean
	*/
com_pagesociety_util_StringUtil.endsWith = function(input, suffix)
	{
		return input.indexOf(suffix) == (input.length - suffix.length);
	};
	/*
	  param s String
	  returns String
	*/
com_pagesociety_util_StringUtil.getExtension = function(s)
	{
		var lid = s./*?String*/lastIndexOf(".");
		if (lid == -1)
			return "";
		return s.substr(lid + 1)./*?substr*/toLowerCase();
	};
	/*
	  param s String
	  returns String
	*/
com_pagesociety_util_StringUtil.trimExtension = function(s)
	{
		var lid = s./*?String*/lastIndexOf(".");
		if (lid == -1)
			return s;
		return s.substr(0, lid);
	};
	/*
	  param s String
	  returns String
	*/
com_pagesociety_util_StringUtil.getFileName = function(s)
	{
		var lid = s./*?String*/lastIndexOf(".");
		if (lid == -1)
			return s;
		return s.substr(0, lid);
	};









var loading_msg = function(msg)
{
	var r = document.getElementById("root");
	if (r!=null)
		r.innerHTML = msg;

}

var include = function(url,f_onload)
{
	loading_msg("Loading "+url+"...")
	var rurl = url;
	if (com_postera_system_code_root!=null && url.indexOf("http")!=0)
	{
		rurl = com_postera_system_code_root+'/'+url;
	}
	if (IS_IOS)
	{
		if (rurl.indexOf("?")==-1)
			rurl += "?";
		else
			rurl += "&";
		rurl += "_____m="+Math.random();
	}
	if (com_pagesociety_util_StringUtil.endsWith(url,"css"))
	{
		_INCLUDE_CSS(rurl,f_onload);
	}
	else
	{
		_INCLUDE_JS(rurl,f_onload);
	}
}

var _INCLUDE_JS = function(url,f_onload)
{
   var headID = document.getElementsByTagName("head")[0];
   var newScript = document.createElement('script');
   newScript.type = 'text/javascript';
   newScript.onload=f_onload;
   /*IE*/
   newScript.onreadystatechange = function ()
   {
        if (newScript.readyState == 'loaded' || newScript.readyState == 'complete')
        {
        f_onload();
       }
   }
   newScript.src = url;
   headID.appendChild(newScript);
};


var _INCLUDE_CSS = function(url,f_onload)
{

	AJAX(url, 'GET',function(responseText){
		var newCss = document.createElement('style');
	   var headID = document.getElementsByTagName("head")[0];
	   //var newCss = document.createElement('link');
	  // newCss.rel  ="stylesheet";
	   newCss.type = "text/css";
	   if(newCss.styleSheet)
	   {// IE
			newCss.styleSheet.cssText = responseText;
		}
		else
			newCss.innerHTML = responseText;
	  // newCss.href = url;
	   headID.appendChild(newCss);
	   f_onload();

	});


}


var AJAX = function(url,method,on_success,on_failure)
{

    var xmlHttpReq = false;
    var self = this;
    // Mozilla/Safari
    if (window.XMLHttpRequest) {
        self.xmlHttpReq = new XMLHttpRequest();
    }
    // IE
    else if (window.ActiveXObject) {
        self.xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
    }
    self.xmlHttpReq.open(method, url, true);
    self.xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    self.xmlHttpReq.onreadystatechange = function() {
        if (self.xmlHttpReq.readyState == 4) {
            if(self.xmlHttpReq.status == 200)
        		on_success(self.xmlHttpReq.responseText);
            else
            	on_failure(self.xmlHttpReq.statusText);
        }
    }
    self.xmlHttpReq.send(/*optional properly encoded query string for request body*/);
}



