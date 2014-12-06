//This module manages all of the token/authentication procedures for API checking
var crypto = require('crypto')
var uuid = require('node-uuid');
var redis = require('redis');

var RedisHelperClass = require("./redisHelper.js");

var signupRedisDB;
var apiTokenRedisDB;

/*
* Create a 32 bytes token with node-uuid, v4 compliant
* Then hash that object with sha256 to create the token to be stored
*/
exports.createToken = function(callback) {
	//create random 128 bit token
	var clientToken = uuid.v4();
	//then send back the token and it's hash
	return {token: clientToken, tokenHash: exports.hashToken(clientToken)};
};

exports.hashToken = function(clientToken)
{
	// return crypto.createHash('sha256').update(clientToken).digest('hex');
	return crypto.createHash('md5').update(clientToken).digest('hex');
}

function createSignupTokenInDB(db, user_id, cb)
{
	//first create our signup
	var tokenAndHash = exports.createToken();

	console.log("Token created before DB entry: ", tokenAndHash);

	//now we must store it in our initializiation db
	db.setTokenWithData(tokenAndHash.tokenHash, {user_id: user_id}, function(err, stored)
	{
		if(err) {
			cb(err);
			return;
		}

		if(stored)
		{
			//let us know about the token, that's the API access key!
			cb(undefined, tokenAndHash.token);
		}
		else
		{
			cb(new Error("Storage of token in Redis DB failed, no redis error though."));
		}
	});
}

exports.createTemporarySignupToken = function(user_id, cb)
{
	//setup the token, send it back -- make sure to store in the signup specific db
	createSignupTokenInDB(signupRedisDB, user_id, cb);
}
exports.createAPIToken = function(user_id, cb)
{
	//setup the token in  the apitoken redis db, send it back
	createSignupTokenInDB(apiTokenRedisDB, user_id, cb);
}

exports.verifySignupToken = function(clienttoken, cb)
{
	var apiToken = exports.hashToken(clienttoken);

	signupRedisDB.getDataByToken(apiToken, function(err, data)
	{
		if(err)
		{
			cb(err);
			return;
		}

		//if we have user data, we can verify that information
		if(data)
			cb(undefined, data.user_id);
		else
			//no error, but no user data found -- not verified!
			cb();

	});
}

exports.initializeRedisConnections = function(databaseValues, databaseTTL, cb)
{
	//signup db connection
	var signupDBConnection = redis.createClient();

	//now let's get the redis database selected properly for our helper
	signupDBConnection.select(databaseValues.signupDB || 0, function(err, res)
	{
		if(err) {
			cb(err);
			return;
		}

		//create a connection for our helper class and the default TTL for keys
		signupRedisDB = new RedisHelperClass(signupDBConnection, databaseTTL.signupDB);

		var apiDBConnection = redis.createClient();

		//now let's get the redis database selected properly for our helper
		apiDBConnection.select(databaseValues.apiTokenDB || 1, function(err, res)
		{
			if(err) {
				cb(err);
				return;
			}

			//create a connection for our helper class and the default TTL for keys
			apiTokenRedisDB = new RedisHelperClass(apiDBConnection, databaseTTL.apiTokenDB);

			console.log("Both redisDB initialized successfully");
			cb(null);
		});
	})
}

