
//Control all the win module! Need emitter for basic usage. 
var Emitter = (typeof process != "undefined" ?  require('component-emitter') : require('emitter'));
var Q = require('q');
//

module.exports = winBB;

function winBB(homeDirectory)
{
	//
	var self = this;

	//we're an emitter! but we also mean extra business, so we override some calls later
	Emitter(self);

	//pull the inner versions, we'll overwrite self versions later
	var innerEmit = self.emit;
	var innerHasListeners = self.hasListeners;


	//cache the shift function
	var shift = [].shift;

	self.log = function()
	{
		throw new Error("Backbone doesn't use log directly anymore. Call backbone.getLogger(moduleObject) instead. ");
	}
	self.log.logLevel = function()
	{
		throw new Error("Backbone doesn't use log.loglevel anymore. Call backbone.logLevel directly instead. ");
	}

	var prependText = function(winFunction)
	{
		return !winFunction ? "" :  "    [" + winFunction + "]: ";
	}
	self.silenceBackbone = false;
	self.logLevel = 1;
	self.nologging = -1;
	self.warning = 0;
	self.normal = 1;
	self.verbose = 2;
	self.testing = 3;

	var muted = {};
	var modIDs = 0;
	var propID = "_backboneID";
	var propIDToName = {};
	var allLoggers = [];

	//we assign every module a log identification
	function nextModID() {return modIDs++;}

	//backbone handles the most basic logging for now, filtering by logLevel at the time
	//no stored history -- this will require a separate module
	//the practice of logging through the backbone should be standard though
	self.getLogger = function(moduleObject)
	{
		var winFunction = moduleObject.winFunction;
		var prepend = prependText(winFunction);

		var mid = addLogger(moduleObject);
		//otherwise ... 
		//already have an mid -- assigned by the loader

		if(typeof process != "undefined")//&& "".cyan != undefined)
		{
			prepend = '\x1B[36m' + prepend + '\x1B[39m';
		}

		var logFunction = function()
		{
			var logCategory;
			if(typeof arguments[0] == "number")
			{
				logCategory = [].shift.call(arguments);
			}
			else //otherwise, assume it's just a verbose message by default -- why would you log otherwise?
				logCategory = logFunction.verbose;

			if(!logCategory)
				throw new Error("Log category must be defined.");

			[].splice.call(arguments, 0,0, prepend)

			//needs to be lower than both our individual level, and our global level -- can't flood the log as any module
			if(logCategory <= logFunction.logLevel && logCategory <= self.logLevel && !muted[mid])
				console.log.apply(console, arguments);
		}

		//assign id to our logger!
		logFunction[propID] = mid;

		logFunction.log = logFunction;
		logFunction.logLevel = self.logLevel;
		logFunction.nologging = self.nologging;
		logFunction.warning = self.warning;
		logFunction.normal = self.normal;
		logFunction.verbose = self.verbose;
		logFunction.testing = self.testing;

		return logFunction;
	}
	//hold our logger propID
	var internalLog = self.getLogger({});

	//set the backbone logger to this internal object prop ID assigned by logger
	addNameToMID("backbone", internalLog[propID]);

	internalLog.logLevel = internalLog.testing;

	//none modules so far
	self.moduleCount = 0;

	//we need to have all calls on record
	var callerEvents = {};
	var requiredEvents = {};
	var optionalEvents = {};
	var moduleObjects = {};

	var mutingAll = false;

	function addLogger(moduleObject)
	{
		//tada
		var mid = moduleObject[propID];

		//if we haven't already gotten an mid assigned to this object
		if(mid == undefined)
		{
			//we need to assign an mid 
			mid = nextModID();

			//all we can do is assign it to this winfunction
			moduleObject[propID] = mid;
		}

		//grab the mid -- later we can do other things if necessary
		allLoggers.push(mid);

		//please respect the silence
		if(mutingAll)
			muted[mid] = true;

		return mid;
	}

	//can mute/unmute
	self.mute = function(name)
	{
		var mid = propIDToName[name];

		if(mid != undefined)
			muted[mid] = true;
	}
	self.unmute = function(name)
	{
		var mid = propIDToName[name];
		delete muted[mid];
	}
	self.muteAll = function()
	{
		mutingAll = true;
		for(var i=0; i < allLoggers.length; i++)
			muted[allLoggers[i]] = true;
	}
	self.unmuteAll = function(){
		muted = {};
		mutingAll = false;
	}
	self.muteLogger = function(logObject)
	{
		muted[logObject[propID]] = true;
	}
	
	self.unmuteLogger = function(logObject)
	{
		delete muted[logObject[propID]];
	}


	function addNameToMID(name, id)
	{
		//don't want duplicates
		if(propIDToName[name] != undefined)
			throw new Error("Duplicate prop ID being sent in, likely named another module 'backbone'");

			//for silencing by name
		propIDToName[name] = id;

		if(mutingAll)
			muted[id] = true;
	}

	//helpful getters for the module objects
	self.getModules = function(moduleNames){

		//empty? jsut send the whole module object back -- pretty dangerous -- ill advised
		if(!moduleNames)
			return moduleObjects;

		//otherwise, we build a map for the name
		var mReturn = {};

		//you can send an array of names, an object indexed by names, or a simple string
		var nameList = moduleNames;

		if(typeof moduleNames == "string")
			moduleNames = [moduleNames];
		else if(typeof moduleNames == "object")
			nameList = Object.keys(moduleNames);
		else if(!Array.isArray(moduleNames))
			throw new Error("Improper module names submitted: must be a string, an array, or a map of the module names");

		//loop through, grab the stuff
		for(var i=0; i < nameList.length; i++)
		{
			var name = nameList[i];
			mReturn[name] = moduleObjects[name];
		}

		//send it back, simple
		return mReturn;

	};
	self.getModuleCount = function(){return self.moduleCount;};
	self.getModuleNameList = function(){return Object.keys(moduleObjects);};


	var parseEventName = function(fullEvent)
	{
		var splitEvent = fullEvent.split(':');

		//if there is no ":", then this is improperly formatted
		if(splitEvent.length <= 1)
			throw new Error("Improper event name format, winFunction:eventName, instead looks like: " + fullEvent);

		return {winFunction: splitEvent[0], eventName: splitEvent[1]}
	}

	self.loadModules = function(inputNameOrObject, allConfiguration, localConfiguration)
	{
		var globalConfiguration;
		if(typeof localConfiguration == "undefined")
		{
			//we handle the case where potentially we have a global object and a bunch of local objects
			allConfiguration = allConfiguration || {};
			globalConfiguration = allConfiguration.global || {};
			localConfiguration = allConfiguration;
		}
		//both are defined -- one assumed to be global, other local
		else if(allConfiguration && localConfiguration)
		{
			globalConfiguration = allConfiguration;
			localConfiguration = localConfiguration;
		}
		else if(localConfiguration)
		{
			//allconfiguration is undefined-- this is weird -- maybe they made a mistake
			//try to pull global from local
			allConfiguration = localConfiguration;
			globalConfiguration = localConfiguration.global || {};
		}
		else
		{
			//just cover the basics, both undefined
			allConfiguration  = allConfiguration || {};
			globalConfiguration = allconfiguration.global || {};
			localConfiguration = localConfiguration || {};
		}
		
		//we have sent in a full object, or just a reference for a text file to load
		var jsonModules = inputNameOrObject;
		if(typeof inputNameOrObject == "string")
		{
			var fs = require('fs');
			var fBuffer = fs.readFileSync(inputNameOrObject);
			jsonModules = JSON.parse(fBuffer);
		}

		//otherwise, json modules is the json module information
		var mCount = 0;
		for(var key in jsonModules)
		{
			//perhaps there is some relative adjustments that need to be made for this to work?

			var locationNameOrObject = jsonModules[key];
			//if you're a function or object, we just leave you alone (the function will be instantiated at the end)
			//makes it easier to test things
			if(typeof locationNameOrObject == "object" || typeof locationNameOrObject == "function")
			{
				moduleObjects[key] = locationNameOrObject;
			}
			else if(locationNameOrObject.indexOf('/') != -1)
			{
				//locations relative to the home directory of the app
				moduleObjects[key] = require(homeDirectory + locationNameOrObject);
			}
			else
				moduleObjects[key] = require(locationNameOrObject);

			//if it's a function, we create a new object
			// if(typeof moduleObjects[key] != "function")
				// throw new Error("WIN Modules need to be functions for creating objects (that accept win backbone as first argument)")
			
			//create the object passing the backbone
			if(typeof moduleObjects[key] == "function") // then pass on teh configuration, both inputs are guaranteed to exist
				moduleObjects[key] = new moduleObjects[key](self, globalConfiguration, localConfiguration[key] || {});


			//if they were not assign an mid by a logger, then I don't need to worry -- yet
			var mid = moduleObjects[key][propID];
			if(mid == undefined)
			{
				mid = nextModID();
				moduleObjects[key][propID] = mid;
			}

			//go ahead and register this name for muting purposes
			addNameToMID(key, mid);

			mCount++;
		}

		self.moduleCount = mCount;

		//now we register our winFunctions for these modules
		for(var key in moduleObjects)
		{
			var wFun = moduleObjects[key].winFunction;
			if(!wFun || wFun == "" || typeof wFun != "string")
			{
				internalLog('Module does not implement winFunction properly-- must be non-empty string unlike: ' +  wFun);
				throw new Error("Improper win function");
			}

			//instead we do this later

			// if(!callerEvents[wFun])
			// {
			// 	//duplicate behaviors now allowed in backbone -- multiple objects claiming some events or functionality
			// 	callerEvents[wFun] = {};
			// 	requiredEvents[wFun] = {};
			// 	optionalEvents[wFun] = {};
			// }

		}

		//now we register our callback functions for all the events
		for(var key in moduleObjects)
		{
			var mod = moduleObjects[key];

			// if(!mod.eventCallbacks)
			// {
			// 	throw new Error("No callback function inside module: " +  mod.winFunction +  " full module: " +  mod);

			// }

			//event callbacks are option -- should cut down on module bloat for simple modules to do stuff
			if(!mod.eventCallbacks){
				internalLog("WARNING, loaded module doesn't provide any callback events inside: ", mod.winFunction, " - with key - ", key);

				//skip!
				continue;
			}

			//grab the event callbacks
			var mCallbacks = mod.eventCallbacks();

			for(var fullEventName in mCallbacks)
			{
				//
				if(typeof fullEventName != "string")
				{
					throw new Error("Event callback keys must be strings: " +  fullEventName);
				}

				var cb = mCallbacks[fullEventName];
				if(!cb || typeof cb != "function")
				{
					throw new Error("Event callback must be non-null function: " +  cb);
				}

				if(self.moduleHasListeners(fullEventName))
				{
					internalLog("Backbone doesn't allow duplicate callbacks for the same event: " + fullEventName);
					throw new Error("Same event answered more than once: " + fullEventName);
				}

				//now we register inside of the backbone
				//we override what was there before
				self.off(fullEventName);
				
				//sole callback for this event -- always overwriting
				self.on(fullEventName, cb);

				//throws error for improper formatting
				var parsed = parseEventName(fullEventName);

				var callObject = callerEvents[parsed.winFunction];
				
				if(!callObject){
					callObject = {};
					callerEvents[parsed.winFunction] = callObject;
				}

				callObject[parsed.eventName] = fullEventName;
			}
		}

		//now we grab all the required functionality for the mods
		for(var key in moduleObjects)
		{
			//call the mod for the events
			var mod = moduleObjects[key];

			//guaranteed to exist from callbacks above
			var fun = mod.winFunction;

			if(!mod.requiredEvents){
				internalLog("WARNING, loaded module doesn't require any events inside: ", fun, " - with key - ", key);

				//skip!
				continue;
			}

			// if(!mod.requiredEvents)
			// {
			// 	throw new Error("Required events function not written in module: " +  fun);
			// }

			var reqs = mod.requiredEvents();

			if(!reqs)
			{
				throw new Error("requiredEvents must return non-null array full of required events.");
			}

			//make sure we have all these events
			for(var i=0; i < reqs.length; i++)
			{
				if(!self.moduleHasListeners(reqs[i]))
					throw new Error("Missing a required listener: " +  reqs[i]);

				var parsed = parseEventName(reqs[i]);

				//lets keep track of who needs what. 
				var required = requiredEvents[fun];
				if(!required)
				{
					required = {};
					requiredEvents[fun] = required;
				}

				//then index into win function
				if(!required[parsed.winFunction])
				{
					required[parsed.winFunction] = {};
				}

				//and again to pared event name
				if(!required[parsed.winFunction][parsed.eventName])
				{
					required[parsed.winFunction][parsed.eventName] = reqs[i];
				}

			}

			//of course any mod can make optional events
			//these are events that you can optionally call, but aren't necessarily satisfied by any module
			//you should check the backbone for listeners before making an optional call -- use at your own risk!
			if(mod.optionalEvents)
			{
				var opts = mod.optionalEvents();

				for(var i=0; i < opts.length; i++)
				{
					var parsed = parseEventName(opts[i]);

					//lets keep track of who needs what. 
					var optional = optionalEvents[fun];

					//if we haven't seen this function requiring stuff before, create our object!
					if(!optional){
						optional = {};
						optionalEvents[fun] = optional;
					}

					//same for win function, have we seen before?
					if(!optional[parsed.winFunction])
					{
						optional[parsed.winFunction] = {};
					}

					//then the full on event name
					if(!optional[parsed.winFunction][parsed.eventName])
					{
						optional[parsed.winFunction][parsed.eventName] = opts[i];
					}
				}
			}

		}
	}



	//build a custom emitter for our module
	self.getEmitter = function(module)
	{
		if(!module.winFunction)
		{
			throw new Error("Can't generate module call function for module that doesn't have a winFunction!");
		}
		//emitter implicitly knows who is calling through closure
		var moduleFunction = module.winFunction;

		var emitter = function()
		{
			[].splice.call(arguments, 0, 0, moduleFunction);
			return self.moduleEmit.apply(self, arguments);
		}

		//pass the function through
		emitter.emit = emitter;

		//pass in the emitter to create a q calling function
		emitter.qCall = createQCallback(emitter);

		//use the qcalls to chain multiple calls together using Q.all and Q.allSettled
		emitter.qConcurrent = qAllCallback(emitter.qCall);

		//this makes it more convenient to check for listeners 
		//you don't need a backbone object AND an emitter. The emitter tells you both info 
		//-- while being aware of who is making requests
		emitter.hasListeners = function()
		{
			//has listeners is aware, so we can tap in and see who is checking for listeners 
			return self.moduleHasListeners.apply(self, arguments);
		}

		return emitter;
	}

		//this is for given a module a promise based callback method -- no need to define for every module
	//requires the Q library -- a worthy addition for cleaning up callback logic
	function createQCallback(bbEmit)
	{
		return function()
		{
			//defer -- resolve later
		    var defer = Q.defer();

		    //first add our own function type
		    var augmentArgs = arguments;

		    //make some assumptions about the returning call
		    var callback = function(err)
		    {
		        if(err)
		        {
		            defer.reject(err);
		        }
		        else
		        {
		            //remove the error object, send the info onwards
		            [].shift.call(arguments);

		            //now we have to do something funky here
		            //if you expect more than one argument, we have to send in the argument object 
		            //and you pick out the appropriate arguments
		            //if it's just one, we send the one argument like normal

		            //this is the behavior chosen
		            if(arguments.length > 1)
		                defer.resolve(arguments);
		            else
		                defer.resolve.apply(defer, arguments);
		        }
		    };

		    //then we add our callback to the end of our function -- which will get resolved here with whatever arguments are passed back
		    [].push.call(augmentArgs, callback);

		    //make the call, we'll catch it inside the callback!
		    bbEmit.apply(bbEmit, augmentArgs);

		    return defer.promise;
		}
	}

	function qAllCallback(qCall)
	{
		return function()
		{
			var defer = Q.defer();

			//send in all the events you want called by win-backbone
			var eventCalls = [].shift.call(arguments);

			var options = [].shift.call(arguments) || {};

			//these are all the things you want to call
			var allCalls = [];

			//either we call the all function (wish fails at the first error)
			var qfunc = Q.allSettled;

			//or optionally, we wait till they all fail or succeed
			if(options.endOnError)
				qfunc = Q.all;
			
			//create a bunch of promises that will be potentially resolved
			for(var i=0; i < eventCalls.length; i++)
				allCalls.push(qCall.apply(qCall, eventCalls[i]));

			//here we go!
			qfunc.call(qfunc, allCalls)
				.then(function(results)
				{
					//we got back stuff back
					//it's easy for Q.all
					//it would have caused an error, and been rejected inside fail
					if(options.endOnError){
						defer.resolve(results);
					}
					else
					{
						var finalValues = {length:0};
						var errors = [];
						var errored = false;

						for(var i=0; i < results.length; i++)
						{
							var result = results[i];

							//we know the outcome
					        if (result.state === "fulfilled") {
					            finalValues[i] = result.value;
					            finalValues.length++;
					            errors.push(undefined);
					        } else {
					            var reason = result.reason;
					            errors.push(reason);
					            errored = true;
					        }
						}

						//let the errors be known
						//we always reject with an array to be consistent
						if(errored)
							defer.reject(errors);
						else //otherwise, all good -- on we go
							defer.resolve(finalValues);
					}
				})
				.fail(function(err)
				{
					//end on error -- we only have one error to return
					//we always return arrays
					defer.reject([err]);
				});

			return defer.promise;
		}
	}

	//backwards compat, but more consistent with getters
	self.getModuleRequirements =
	self.moduleRequirements =  function()
	{
		return JSON.parse(JSON.stringify(requiredEvents));
	};
	//backwards compat, but more consistent with getters
	self.getRegisteredEvents =
	self.registeredEvents = function()
	{	
		//return a deep copy so it can't be messed with
		return JSON.parse(JSON.stringify(callerEvents));
	}

	self.initializeModules = function(done)
	{	
		//call each module for initialization

		var totalCallbacks = self.moduleCount;
		var errors;

		var finishCallback = function(err)
		{
			if(err)
			{
				//we encountered an error, we should send that back
				if(!errors)
					errors = [];
				errors.push(err);
			}

			//no matter what happens, we've finished a callback
			totalCallbacks--;

			if(totalCallbacks == 0)
			{
				//we've finished all the callbacks, we're done with initialization
				//send back errors if we have them
				done(errors);
			}
		}
		var wrapMod = function(mod)
		{
			return function()
			{
				mod.initialize(function(err)
				{
					finishCallback(err);
				});
			}
		}

		var hasInit = false;

		//order of initialization might matter -- perhaps this is part of how objects are arranged in the json file?
		for(var key in moduleObjects)
		{
			var mod = moduleObjects[key];
			//make sure not to accidentally forget this
			if(!mod.initialize)
				totalCallbacks--;
			else {
				hasInit = true;
				//seems goofy, but we dont want any poorly configured modules returning during this for loop -- awkward race condition!
				setTimeout(wrapMod(mod), 0)
			}
		}
		//nobody has an initialize function
		if(!hasInit)
		{
			//call done async
			setTimeout(done, 0);
		}
	}


	self.hasListeners = function()
	{
		throw new Error("Backbone doesn't pass listeners through itself any more, it uses the emitter.hasListeners. You must call backbone.getEmitter(moduleObject) to get an emitter.");
	}

	self.emit = function()
	{
		throw new Error("Backbone doesn't pass messages through emit any more. You must call backbone.getEmitter(moduleObject) -- passing the object.");
	}

	self.moduleHasListeners = function()
	{
		//pass request through module here!
		return innerHasListeners.apply(self, arguments);
	}

	self.moduleEmit = function()
	{
		//there are more than two 
		// internalLog('Emit: ', arguments);
		if(arguments.length < 2 || typeof arguments[0] != "string" || typeof arguments[1] != "string")
		{
			throw new Error("Cannot emit with less than two arguments, each of which must be strings: " + JSON.stringify(arguments));
		}
		//take the first argument from the array -- this is the caller
		var caller = shift.apply(arguments);
		//pull out the function and event name arguments to verify the callback
		var parsed = parseEventName(arguments[0]);
		var wFunction = parsed.winFunction;
		var eventName = parsed.eventName;

		internalLog("[" + caller + "]", "calling", "[" + parsed.winFunction + "]->" + eventName);

		//now we check if this caller declared intentions 
		if(!self.verifyEmit(caller, wFunction, eventName))
		{
			throw new Error("[" + caller + "] didn't require event [" + parsed.winFunction + "]->" + parsed.eventName);
		}

		//otherwise, normal emit will work! We've already peeled off the "caller", so it's just the event + arguments being passed
		innerEmit.apply(self, arguments);

	}

	self.verifyEmit = function(caller, winFunction, eventName)
	{
		//did this caller register for this event?
		if((!requiredEvents[caller] || !requiredEvents[caller][winFunction] || !requiredEvents[caller][winFunction][eventName])
			&& (!optionalEvents[caller] || !optionalEvents[caller][winFunction] || !optionalEvents[caller][winFunction][eventName]))
			return false;


		return true;
	}

	return self;
}



