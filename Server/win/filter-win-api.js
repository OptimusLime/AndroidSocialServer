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
        {prepend: prepend, append: "/filterThumbnail"},
        {prepend: prepend, append: "/filterFull"},
        {prepend: prepend, append: "/imageFull"},
        {prepend: prepend, append: "/imageThumbnail"}
    ];
}

var dbModifier = "winfilter";
var dbLocation = 'mongodb://localhost/win' + dbModifier;

var winAPIObject;
var s3Storage;

var metaProps = {
    user: "string",
    timeofcreation: "Number",
    session: "String"
}

var winConfiguration = {
    mainArtifactType : "FilterArtifact",
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


var customS3Match = {schemaName: "S3_connection", schemaJSON: {wid: "String", s3Key: "String"}};

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

        		var metaInfo = {user: user, timeofcreation: Date.now(), session: "funtimesession"};
        		console.log('Confirming upload for uuid: ', uuidUpload);

			  	s3Storage.asyncConfirmUploadComplete(uuidUpload)
			  		.then(function(isCompleted)
			  		{	
			  			//must match the upload key with the filter artifacts -- this way we know where to get the s3 uploads
			  			for(var key in filterArtifacts)
			  				filterArtifacts[key].s3Key = uuidUpload;

			  			console.log('Checked upload: ', isCompleted);
			  			console.log('Artifacts: ', filterArtifacts);
			  			console.log('metainfo', metaInfo);
			  			//doofus say waaaaa -- send back the storage location, problem solved!
		  				//res.json(storageLocations).end();
		  				if(isCompleted.success)
		  				{
		  					//great success!
		  					//what ever shall we do?!?!?	
		  					return winAPIObject.dataAccess.publishWINArtifacts(filterArtifacts, metaInfo);
		  				}
		  				else
		  					throw new Error("Upload to S3 cannot be confirmed.");

			  		})
			  		.then(function()
			  		{
			  			var toSaveConnection = {};
			  			var wid = Object.keys(filterArtifacts)[0];
			  			toSaveConnection[wid] = {s3Key: uuidUpload, wid: wid};

			  			console.log("To save connection: ", toSaveConnection);

			  			//now lets keep track of our s3 connection in a separate database object
			  			return winAPIObject.dataAccess.saveDatabaseObjects(customS3Match.schemaName, toSaveConnection);			  			
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


			//now we setup our app on the desired port
			app.listen(port, function()
			{
				console.log("Now running on port " + port);	
			});
		
		});

}

launchExpress();



