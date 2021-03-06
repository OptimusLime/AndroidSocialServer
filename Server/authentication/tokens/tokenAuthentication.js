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

	//what user info are we storing? Also, this will get tagged with the expiration date as well
	var userData = {user_id: user_id};

	//now we must store it in our initializiation db
	db.setKeyWithData(tokenAndHash.tokenHash, userData, function(err, stored)
	{
		//errors end this real quick
		if(err) {
			cb(err);
			return;
		}
	
		//if this for some reason didn't work, there would be an empty stored object
		if(!stored){cb(new Error("Storage of token in Redis DB failed, no redis error though."));return;}

		//we also need to store the inverse - user_id -> access tokens
		//first we fetch our tokens
		exports.fetchUserAPITokens(db, user_id, function(err, tokens)
		{
			if(err){cb(err);return;}

			//we now have the tokens, we append our new token
			tokens.push(tokenAndHash.tokenHash);

			//now we save with the new tokens -- no expiration time
			db.setKeyWithData(user_id, {tokens: tokens}, 0, function(err, stored)
			{
				if(stored)
				{
					//let us know about the token, that's the API access key!
					cb(undefined, tokenAndHash.token, userData._expiration);
				}
				else
				{
					cb(new Error("Storage of api tokens in user_id key in Redis DB failed, no redis error though."));
				}
			});

		});		
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

exports.fetchUserAPITokens = function(db, user_id, cb)
{
	db.getDataByKey(user_id, function(err, tokenData)
	{
		if(err)
		{
			cb(err);
			return;
		}

		//if we have user data, we can verify that information
		if(tokenData)
			cb(undefined, tokenData.tokens);
		else
			//no error, but no token data found!
			cb(undefined, []);

	});
}

exports.verifySignupToken = function(clienttoken, cb)
{
	var apiToken = exports.hashToken(clienttoken);

	signupRedisDB.getDataByKey(apiToken, function(err, data)
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


exports.verifyAPIToken = function(clienttoken, cb)
{
	var apiToken = exports.hashToken(clienttoken);

	apiTokenRedisDB.getDataByKey(apiToken, function(err, data)
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

exports.expireSignupTokens = function(user_id, cb)
{
	//remove all signup keys for a particular user 
	signupRedisDB.expireTokensByUserID(user_id, cb);
}

exports.removeAPIAccessByUserID = function(user_id, cb)
{
	//remove all current api tokens and access for a user based on their user_id
	apiTokenRedisDB.expireTokensByUserID(user_id, cb);
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

