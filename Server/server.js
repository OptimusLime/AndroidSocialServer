//standard express setup
var express = require('express');
var app = express();

//get our authentication router
var AuthRouting = require("./authentication/authentication.js");

//what port to operate on?
var port = process.env.PORT || 8000; 		// set our port

// a route for handling authentication -- just send it in with our logger and some setup params
app.use('/auth', new AuthRouting());

//now we setup our app on the desired port
app.listen(port, function()
{
	console.log("Now running on port " + port);	
});



