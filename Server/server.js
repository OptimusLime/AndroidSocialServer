//standard express setup
var express = require('express');
var app = express();

//get our authentication router
var AuthRouting = require("./authentication/authentication.js");
var tokenAuth = require('./authentication/tokens/tokenAuthentication.js');
var googleAuth = require("./authentication/tokens/googleVerification.js");

//what port to operate on?
var port = process.env.PORT || 8000; 		// set our port

//what mongodb to connect to
var mongoDBLocation = 'mongodb://localhost/social';

var mongoose = require('mongoose');

var db = mongoose.connection;

db.on('error', function(e)
{
	console.log("Error connecting to mongodb, is it running?");
	console.error.apply(this, arguments);		
});

var redisDBLocation = {
	signupDB : 2,
	apiTokenDB : 3
};

var redisDefaultTTL = {
	//1/2 hour for signup
	signupDB : 60*30,
	//go for 20 days
	apiTokenDB : 60*60*24*20
};

var clientKeys = require("./clientKeys.js");

db.once('open', function() {

	console.log("MongoDB Connection opened");

	// Our database connection is open, we can now do our router setup and handle model creation 
	tokenAuth.initializeRedisConnections(redisDBLocation, redisDefaultTTL, function(err)
	{

		//catch any errors from redis before starting
		if(err) throw err;

		console.log("Redis Connections initialized.");

		googleAuth.initializeGoogleVerification(clientKeys, function(err)
		{
			if(err) throw err;

			console.log("Google Auth Initialized");

			// a route for handling authentication -- just send it in with our db connections and some params [later]
			app.use('/auth', new AuthRouting(tokenAuth, googleAuth, db, {keys: clientKeys}));

			//now we setup our app on the desired port
			app.listen(port, function()
			{
				console.log("Now running on port " + port);	
			});

		});
	});

});

mongoose.connect(mongoDBLocation);




