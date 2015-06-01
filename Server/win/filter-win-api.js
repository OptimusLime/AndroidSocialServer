var S3StoreClass = require('s3-image-store');
var winAPIClass = require("win-api");

//standard express setup
var express = require('express');
var bodyParser = require('body-parser');
var app = express();

//what port to operate on?
var port = process.env.PORT || 8000; 		// set our port

var createFiles = function(props)
{
    //we will create 4 files -- pulling info from props
    var user = props.user;

    var prepend = user + "/";

    return [
        {prepend: prepend, append: "/filterThumbnail.png"},
        {prepend: prepend, append: "/filterFull.png"},
        {prepend: prepend, append: "/imageFull.png"},
        {prepend: prepend, append: "/imageThumbnail.png"}
    ];
}

var dbModifier = "winfilter";
var dbLocation = 'mongodb://localhost/win' + dbModifier;

var winAPIObject;
var s3Storage;

var metaProps = {
    user: "string",
    timeofcreation: "Number",
    session: "String",
    s3Key: "String"
}

var winConfiguration = {
    mainArtifactType : "FilterArtifact",
    defaultFeedFetch: 10, 
    maxFeedFetch : 25,
    validator: {
        multipleErrors : true,
        allowAnyObjects : false,
        requireByDefault : true,
        metaProperties: metaProps
    },
    schemaLocation : __dirname + "/schema",
    mongo : {dbLocation : dbLocation},
    redis: {}
};

var configLocation = __dirname + "/../access-credentials.json";

var customS3Match = {schemaName: "S3_connection", schemaJSON: {wid: "String", s3Key: "String", username: "String", date: "Number", photoCaption: "String"}};
var customHashMatch = {schemaName: "Hash_connection", schemaJSON: {wid: "String", s3Key: "String", username: "String", hashtag: "String", date: "Number", photoCaption: "String"}};
var customFavoriteMatch = {schemaName: "favorite_filters", schemaJSON: {wid: "String", username: "String", date: "Number"}};

function initializeServerObjects()
{

	return S3StoreClass.initialize(configLocation, createFiles)
			.then(function(storeResult)
			{
				//we intialized our connection to redis and the s3 loading worked
				s3Storage = storeResult.s3Store;

				//now we need to start up our WIN server
		 		return winAPIClass.asyncLaunch(winConfiguration);
			})
			.then(function(winObject)
			{
				winAPIObject = winObject;

				winAPIObject.dataAccess.syncLoadCustomSchema(customS3Match.schemaName, customS3Match.schemaJSON);
				winAPIObject.dataAccess.syncLoadCustomSchema(customHashMatch.schemaName, customHashMatch.schemaJSON);
				winAPIObject.dataAccess.syncLoadCustomSchema(customFavoriteMatch.schemaName, customFavoriteMatch.schemaJSON);

				var popularityProperty = 'parent';
				var schemaInfo =  winAPIObject.dataModification.createPopularitySchema(popularityProperty, 
					winConfiguration.mainArtifactType);

				//we have to send in the name afterwards for addditional clarity to mongodb
				winAPIObject.dataAccess.syncLoadCustomSchema(schemaInfo.schemaName, schemaInfo.schemaJSON, schemaInfo.schemaName);

			})
			.catch(function(err)
			{
				throw err;	
			});
}


function launchExpress()
{
	initializeServerObjects()
		.catch(function(err)
		{
			throw err;
		})
		.done(function()
		{

			//use body parsing going through her 
			//you may only need body parsing on some routes not all
			app.use(bodyParser.json()); 

			//we're all done with win api objects and s3 storage objects
			//now we need to construct and launch our api for express
			app.get('/upload/generate', function(req, res)
			{
				//lets generate from a list
				console.log("credential check: ", req.user);

				//default user for now, k thx
			  	var user = req.user || "paul";

			  	//this will get passed to the "createFiles" function above
			  	//that will handle how to create the desired file names synchronously
			  	var uploadProps = {user: user};

			  	s3Storage.asyncInitializeUpload(uploadProps)
			  		.then(function(storageLocations)
			  		{	
			  			console.log('url locations and info: '.green, require('util').inspect(storageLocations, false, 10));
			  			//doofus say waaaaa -- send back the storage location, problem solved!
		  				res.json(storageLocations).end();
			  		})
			  		.catch(function(err){

			  			//server error
			  			res.status(500).send('Error initialize upload ' + (err.message || err)).end();
			  		});
			});

			app.put('/check/:uuid/:username/:image', function(req, res)
			{
				// console.log(req);

				console.log(req.headers);

				res.json(JSON.stringify({succes: true})).end();


			})

			app.post('/upload/confirm', function(req, res)
			{
				//lets generate from a list
				console.log("credential check: ", req.user);

				console.log("Body: ", req.body);

				//default user for now, k thx
			  	var user = req.user || "paul";

			  	//grab the uuid for the upload
			  	var uuidUpload = req.body.uuid;

			  	//also grab the wid item as well
			  	var filterArtifacts = req.body.filterArtifacts;
			  	for(var key in filterArtifacts)
			  	{
			  		filterArtifacts[key].parents = filterArtifacts[key].parents || [];
			  		var genomes = filterArtifacts[key].genomeFilters;
			  		for(var i=0; i < genomes.length; i++)
			  		{
			  			var ng = genomes[i];
			  			ng.parents = ng.parents || [];
			  		}
			  	}

        		var metaInfo = {user: user, timeofcreation: Date.now(), session: "funtimesession"};
        		console.log('Confirming upload for uuid: ', uuidUpload);

			  	s3Storage.asyncConfirmUploadComplete(uuidUpload)
			  		.then(function(isCompleted)
			  		{
			  			if(!isCompleted.success)
		  				{
		  					throw new Error("Cannot confirm S3 upload.")
		  				}

			  			var toSaveConnection = {};
			  			var wid = Object.keys(filterArtifacts)[0];
			  			var date = Date.now();
			  			var hashtags = filterArtifacts[wid].hashtags;
			  			toSaveConnection[wid] = {s3Key: uuidUpload, wid: wid, username: user, date: date, photoCaption: filterArtifacts[wid].photoCaption};

			  			console.log("To save connection: ", toSaveConnection);

			  			//now lets keep track of our s3 connection in a separate database object
			  			return winAPIObject.dataAccess.saveDatabaseObjects(customS3Match.schemaName, toSaveConnection);			  			
			  		})
			  		.then(function()
			  		{
			  			var toSaveConnection = {};
			  			var wid = Object.keys(filterArtifacts)[0];
			  			var date = Date.now();
			  			var hashtags = filterArtifacts[wid].hashtags;
			  			for(var i=0; i < hashtags.length; i++)
			  				toSaveConnection[wid + "-" + i] = {s3Key: uuidUpload, wid: wid, 
			  					username: user, hashtag: hashtags[i].toLowerCase(), date: date, photoCaption: filterArtifacts[wid].photoCaption};

			  			//now lets keep track of our hash tags in a separate database object
			  			return winAPIObject.dataAccess.saveDatabaseObjects(customHashMatch.schemaName, toSaveConnection);	
			  		})
			  		.then(function()
			  		{	

			  			metaInfo.s3Key = uuidUpload;

			  			//must match the upload key with the filter artifacts -- this way we know where to get the s3 uploads
			  			for(var key in filterArtifacts){
			  				//save the s3 uplaod key
			  				// filterArtifacts[key].meta = metaInfo;
			  				filterArtifacts[key].s3Key = uuidUpload;

		  					var gf = filterArtifacts[key].genomeFilters;
			  				for(var i=0; i < gf.length; i++){
			  					delete gf[i].nodeLookup;
			  					delete gf[i].connectionLookup;
			  				}
			  			}

			  			// console.log('Checked upload: ', isCompleted);
			  			console.log('Artifacts: ', filterArtifacts);
			  			console.log('metainfo', metaInfo);

			  			//doofus say waaaaa -- send back the storage location, problem solved!
		  				//res.json(storageLocations).end();
	  					//great success!
	  					//what ever shall we do?!?!?	
	  					return winAPIObject.dataAccess.publishWINArtifacts(filterArtifacts, metaInfo);
			  		})
			  		.then(function()
			  		{
			  			console.log('Finished and confirmed upload. 200 status.');

			  			//we've saved the object -- success!
			  			res.json({uuid: uuidUpload}).end();
			  		})
			  		.catch(function(err){
			  			console.log('Error confirming', require('util').inspect(err, false, 10));
			  			//server error
			  			res.status(500).send('Error initialize upload ' + (err.message || err)).end();
			  		});
			});


			app.get("/latest", function(req, res)
			{
				//start searching from a certain time -- or simply start from now
				var afterDate = req.query.after;
				var beforeDate = req.query.before;

				var feedCount = Math.min(winConfiguration.maxFeedFetch, req.query.count || winConfiguration.defaultFeedFetch)

				//look back the most recent s3s
				winAPIObject.dataAccess.loadRecentArtifacts({before: beforeDate, after: afterDate}, feedCount, customS3Match.schemaName)
					.then(function(models)
					{
						console.log('Loaded latest: ', models);

						//send back the models
						res.json(models).end();
					})
					.catch(function(err)
					{
						console.log('Error fetching recent', require('util').inspect(err, false, 10));
			  			//server error
			  			res.status(500).send('Error fetching recent ' + (err.message || err)).end();
					});

			});

			app.get("/hashtag", function(req, res)
			{
				//start searching from a certain time -- or simply start from now
				var afterDate = req.query.after;
				var beforeDate = req.query.before;
				var hashtag = req.query.hashtag;
				var feedCount = Math.min(winConfiguration.maxFeedFetch, req.query.count || winConfiguration.defaultFeedFetch)

				//look back the most recent s3s
				winAPIObject.dataAccess.loadRecentArtifactsByProperties({hashtag: hashtag},
				 customHashMatch.schemaName, {start: {before: beforeDate, after: afterDate}, count: feedCount})
					.then(function(models)
					{
						console.log('Loaded latest: ', models);

						//send back the models
						res.json(models).end();
					})
					.catch(function(err)
					{
						console.log('Error fetching recent', require('util').inspect(err, false, 10));
			  			//server error
			  			res.status(500).send('Error fetching recent ' + (err.message || err)).end();
					});

			});

			app.get("/artifacts/latest", function(req, res)
			{
				//start searching from a certain time -- or simply start from now
				var afterDate = req.query.after;
				var beforeDate = req.query.before;

				var feedCount = Math.min(winConfiguration.maxFeedFetch, req.query.count || winConfiguration.defaultFeedFetch)

				var widToS3Key = {};
				var widToDate = {};
				//look back the most recent schema objects this tells us the wids of the most recent -- 
				//if we have tons and tons of artifacts that are pretty large in size, 
				//this is the fastest read we can do before we simply return all objects matching these ids
				winAPIObject.dataAccess.loadRecentArtifacts({before: beforeDate, after: afterDate}, feedCount, customS3Match.schemaName)
					.then(function(models)
					{
						var wids = [];
						for(var i=0; i < models.length;i++){
							var mod = models[i];
							wids.push(mod.wid);
							widToS3Key[mod.wid] = mod.s3Key;
						}

						//got the models that have the hashtags -- now we need to turn them into artifacts
						return winAPIObject.dataAccess.loadWINArtifacts(wids);		
					})
					.then(function(models)
					{
						// console.log('Loaded latest: ', models);

						for(var i=0; i < models.length; i++)
						{
							var mod = models[i];

							if(!mod.meta.s3Key)
								mod.meta.s3Key = widToS3Key[mod.wid];
						}

						models.sort(function(a,b){return b.date - a.date;})

						//send back the models
						res.json(models).end();
					})
					.catch(function(err)
					{
						console.log('Error fetching recent', require('util').inspect(err, false, 10));
			  			//server error
			  			res.status(500).send('Error fetching recent ' + (err.message || err)).end();
					});

			});

			app.get("/artifacts/hashtag", function(req, res)
			{
				//start searching from a certain time -- or simply start from now
				var afterDate = req.query.after;
				var beforeDate = req.query.before;
				var hashtag = req.query.hashtag;
				var feedCount = Math.min(winConfiguration.maxFeedFetch, req.query.count || winConfiguration.defaultFeedFetch)

				//look back the most recent schema objects --
				//this tells us the wids of the most recent  for this hashtag -- 
				//if we have tons and tons of hashtags, this is the fastest read we could do instead of looking through ALL the genotype array objects
				//then we simply return all objects matching these ids
				winAPIObject.dataAccess.loadRecentArtifactsByProperties({hashtag: hashtag}, 
					customHashMatch.schemaName, {start: {before: beforeDate, after: afterDate}, count: feedCount})
					.then(function(models)
					{
						var wids = [];
						for(var i=0; i < models.length;i++)
							wids.push(models[i].wid);

						//got the models that have the hashtags -- now we need to turn them into artifacts
						return winAPIObject.dataAccess.loadWINArtifacts(wids);		
					})
					.then(function(models)
					{
						console.log('Loaded latest: ', models);

						//send back the models
						res.json(models).end();
					})
					.catch(function(err)
					{
						console.log('Error fetching recent', require('util').inspect(err, false, 10));
			  			//server error
			  			res.status(500).send('Error fetching recent ' + (err.message || err)).end();
					});

			});

			app.get('/artifacts/popular', function(req, res)
			{
				var feedCount = Math.min(winConfiguration.maxFeedFetch, req.query.count || winConfiguration.defaultFeedFetch)
				var start = parseInt(req.query.skip || 0);
				if(isNaN(start))
				{
					console.log('start value isnt a number');
		  			res.status(400).send('Bad request: invalid start location').end();
		  			return;
				}

				var popularityProperty = 'parent';
				var remapIDToWID = 'wid';
				var schemaInfo =  winAPIObject.dataModification.createPopularitySchema(popularityProperty, 
					winConfiguration.mainArtifactType);

				winAPIObject.dataModification.countModelByProperty(popularityProperty, 
					winConfiguration.mainArtifactType)
					.then(function()
					{
						//lets do this thang -- get the model we want!
						return winAPIObject.dataModification.getArtifactsByHighestPropertyCount(remapIDToWID, schemaInfo.schemaName, {skip: start, count: feedCount});
					})
					.then(function(results)
					{	
						var widList = [];
						for(var i=0; i < results.length; i++)
						{
							if(typeof results[i].wid == "string")
								widList.push(results[i].wid);
						}

						// console.log('Highest pop res:', results);
						console.log('Highest pop list:', widList);

						//got the models that are popular -- now we need to turn them into artifacts
						return winAPIObject.dataAccess.loadWINArtifacts(widList);			
					})
					.then(function(results){

						console.log('High pop res: ', results);
						res.json(results).end();
					})
					.catch(function(err)
					{
						console.log('Error fetching popular', require('util').inspect(err, false, 10));
			  			res.status(500).send('Error fetching recent ' + (err.message || err)).end();
					});
			});

			//like something!
			app.post('/artifacts/favorite/:username/:wid', function(req, res)
			{
				//need to check on user logged in for favorites 
				var user = req.params.username;
				var wid = req.params.wid;
				console.log('wid: ' + (typeof wid) + ' user: ' + (typeof user));

				//first we need to check if the wid of the artifact exists -- if so, add the favorite -- or maybe don't bother?
				//if someone is saving invalid wids, who cares?  
				if(typeof wid != "string" || typeof user != "string")
				{
					console.log('WID favorite isnt a string, foul play?');
		  			res.status(400).send('Bad request: invalid wid type').end();
		  			return;
				}

				//TODO: validate user HERE
				var favorite = {wid: wid, username: user, date: Date.now()};

				//save connection
				winAPIObject.dataAccess.asyncCreateOrRemove(customFavoriteMatch.schemaName, ["wid", "username"], favorite)
					.then(function(action)
					{
						if(action.created)
							res.json({liked: true}).end();
						else if(action.removed)
							res.json({unliked: true}).end();
						else
						{
							console.log('Error asyncCreateOrRemove', action);
			  				res.status(500).send('Error asyncCreateOrRemove -- unknown action taken').end();
						}
					})
					.catch(function(err)
					{
						console.log('Error saving favorite', require('util').inspect(err, false, 10));
			  			res.status(500).send('Error fetching recent ' + (err.message || err)).end();
					})
			});

			app.get('/artifacts/favorite/:username', function(req, res)
			{
				var user = req.params.username;
				var start = req.query.start;
				var feedCount = Math.min(winConfiguration.maxFeedFetch, req.query.count || winConfiguration.defaultFeedFetch)

				var sInt = parseInt(start);

				if(isNaN(sInt))
				{
					console.log('start value isnt a number');
		  			res.status(400).send('Bad request: invalid start location').end();
		  			return;
				}

				//TODO: need to verify the user is the one making the requests! 
				//lets pull out al the liked objects for the user plz
				var query = {username: user};
				var options = {sort: {}, skip: sInt, count: feedCount};

				winAPIObject.dataAccess.loadRecentArtifactsByProperties(query, customFavoriteMatch.schemaName, options)
					.then(function(models)
					{
						//we got all of our favorites for this user -- now pull out the wids and load those objects -- if they exist!
						var wids = [];
						for(var i=0; i < models.length;i++)
							wids.push(models[i].wid);

						//got the models that have the hashtags -- now we need to turn them into artifacts
						return winAPIObject.dataAccess.loadWINArtifacts(wids);		
					})
					.then(function(models)
					{
						console.log('Loaded latest: ', models);

						//send back the models
						res.json(models).end();
					})
					.catch(function(err)
					{
						console.log('Error fetching favorite', require('util').inspect(err, false, 10));
			  			res.status(500).send('Error fetching favs ' + (err.message || err)).end();
					});
			});

			//now we setup our app on the desired port
			app.listen(port, function()
			{
				console.log("Now running on port " + port);	
			});
		
		});

}

launchExpress();



