//base code from: https://github.com/kdelemme/nodejs-token-auth

module.exports = function(redisClient, defaultTTL)
{
	//save object for returning
	var self = this;

	/*
	* Stores a token with user data for a ttl period of time
	* token: String - Token used as the key in redis 
	* data: Object - value stored with the token 
	* ttl: Number - Time to Live in seconds (default: 24Hours)
	* callback: Function
	*/
	self.setKeyWithData = function(token, data, ttl, callback) {
		if (token == null) throw new Error('Token is null');
		if (data != null && typeof data !== 'object') throw new Error('data is not an Object');

		//if we don't send ttl, just use default instead 
		if(typeof ttl == "function")
		{
			callback = ttl;
			ttl = defaultTTL;
		}

		var userData = data || {};
		userData._ts = new Date();

		//if we say ttl = 0, then the key does not expire
		if(ttl == 0)
		{
			//does not expire!
			redisClient.set(token, JSON.stringify(userData), function(err, reply)
			{
				if (err) {callback(err); return;}

				//token was set properly by redis, hooray!
				if (reply) {
					callback(null, true);
				} else {
					//token failed to get set by redis, but no error was thrown -- figure out how to proceed
					callback(null, false);
				}
			});
		}
		else
		{
			//otherwise, we've got set a key with an expiration
			var timeToLive = ttl || defaultTTL;
			if (timeToLive != null && typeof timeToLive !== 'number') throw new Error('TimeToLive is not a Number');

			redisClient.setex(token, timeToLive, JSON.stringify(userData), function(err, reply) {
				if (err) {callback(err); return;}

				//token was set properly by redis, hooray!
				if (reply) {
					callback(null, true);
				} else {
					//token failed to get set by redis, but no error was thrown -- figure out how to proceed
					callback(null, false);
				}
			});
		}

		
		
	};

	/*
	* Gets the associated data of the token.
	* key: String - key in redis
	* callback: Function - returns data
	*/
	self.getDataByKey = function(token, callback) {
		if (token == null) callback(new Error('Token is null'));

		redisClient.get(token, function(err, userData) {
			if (err) {
				callback(err);
				return;
			}

			//if we find the user, parse it and send info back
			if (userData != null) callback(null, JSON.parse(userData));
			//no error, but also no user info, just send back nothing -- errors are for when something went wrong with the DB
			else callback();
		});
	};

	/*
	* Expires a token by deleting the entry in redis
	* callback(null, true) if successfuly deleted
	*/
	self.expireToken = function(token, callback) {
		if (token == null) callback(new Error('Token is null'));

		redisClient.del(token, function(err, reply) {
			if (err) {
				callback(err);
				return;
			}
			//we had an existing object, now its gone
			if (reply) callback(null, true);
			//there was nothing there, it's not really gone
			else callback(null, false);
		});
	};

	self.expireTokensByUserID = function(user_id, callback) {
		
		if (user_id == null) callback(new Error('user_id is null'));

		self.getDataByKey(user_id, function(err, tokenObject)
		{
			if (err) {callback(err); return;}

			//we grab all the tokens associated with the user
			var apiTokens = tokenObject.tokens;

			//this function will be called when redis removes all the tokens
			apiTokens.push(function(err, numDeleted) {
				if (err) {callback(err); return;}

				//then remove any reference to the user_id
				redisClient.del(user_id, function(err, reply) {

					if(reply)
					{
						callback(null, false, "user_id deletion did not return reply object.");
					}
					else
					{
						//all the keys have been removed -- it's alldone!
						if(numDeleted == apiTokens.length -1)
							callback(null, true);
						else 
							//failed to delete every key associated with the user
							callback(null, false);
					}
				});
			});

			//then apply these tokens as the desired remove callback including the 
			redisClient.del.apply(redisClient, apiTokens);




		});
		
	};


	return self;
}