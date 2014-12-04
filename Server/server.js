//standard express setup
var express = require('express');
var app = express();

//get our authentication router
var AuthRouting = require("./authentication/authentication.js");

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

db.once('open', function() {
	// Our database connection is open, we can now do our router setup and handle model creation 

	// a route for handling authentication -- just send it in with our logger and some setup params
	app.use('/auth', new AuthRouting(db));

	//now we setup our app on the desired port
	app.listen(port, function()
	{
		console.log("Now running on port " + port);	
	});

});

mongoose.connect(mongoDBLocation);




