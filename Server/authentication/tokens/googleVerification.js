//This module manages all of the token/authentication procedures for API checking
var crypto = require('crypto')
var base64URL = require("base64-url");
var jwt = require("jsonwebtoken");
var superagent = require('superagent');


// verify audience
var googleCerts;  // get public key
var certExpiration;
var googleClientIDs;

//30 minutes before preemptive fetching
var refreshBuffer =  30*60*1000;
var fetchHappening = false;

exports.initializeGoogleVerification = function(googleIDs, cb)
{
	googleClientIDs = googleIDs;

	//fetch the certs and set it up for us
	exports.fetchGoogleCerts(cb);
}

exports.fetchGoogleCerts = function(cb)
{
	if(fetchHappening)
		return;

	fetchHappening = true;

	var request = superagent.agent();
	  request
		.get('https://www.googleapis.com/oauth2/v1/certs')
		.accept('application/json')
		.end(function(err, gRes){
			//done fetching, failure or not
			fetchHappening = false;

			//uh oh, request error
			if(err){cb(err);return;}

			// console.log("Cert body: ", gRes.body);
			// console.log("Cert headers: ", gRes.headers);

			//grab our expiration for this 
			certExpiration = new Date(gRes.headers.expires).getTime();
			googleCerts = gRes.body;

			console.log("Cert header expiration: ", certExpiration, " Actual date: ", new Date(gRes.headers.expires));
			console.log("Now: ", Date.now(), " expired? ", (Date.now() - certExpiration > 0 ? "Yes" : "No"));
			console.log("Certificates Keys: ", Object.keys(googleCerts));

			//need to check for expiration
			cb();

		});
}		

exports.finishVerifyToken = function(api_token, cb)
{
	//TODO: fix cert -- which one to use from google
	var kidHolder = JSON.parse(base64URL.decode(api_token.split(".")[0]));

	jwt.verify(api_token, googleCerts[kidHolder.kid], { audience: googleClientIDs.web }, function(err, decoded) {
	  // if audience mismatch, err == invalid audience

	  console.log("JWT Decode finished---");
	  console.log("Err: ",err);
	  console.log("Decoded: ", decoded);

	});
}

/*
* Verifies proved JWT google token is actually valid and signed by Google with the appropriate audience
*/
exports.verifyGoogleToken = function(api_token, cb) {

	//if you're expired -- we must call now
	if(!googleCerts || Date.now() > certExpiration)
	{
		fetchGoogleCerts(function()
			{
				finishVerifyToken(api_token, cb);
			});
	}
	//otherwise, we're good to verify now
	else
	{
		//we do verification immediately, then we fetch from google
		finishVerifyToken(api_token, cb);
	}

};
