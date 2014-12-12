var express = require('express');
var bodyParser = require('body-parser');
var superagent = require('superagent');

var util = require('util');

var UserModelClass = require('./models/user.js');
var FBDataModelClass = require('./models/fbdata.js');

//TODO: Error handling in this process. Need to have an official error handler so we can 
//more efficiently pass errors. Better logging platform like Winston as well

module.exports = function(tokenAuth, googleAuth, mongoDB, params)
{
	if(!mongoDB){
		throw new Error("MongoDB Connection not passed into authentication router");
	}

	params = params || {};

	var googleAuthKeys = params.keys;

	var authRouter = express.Router();

	var UserModel = UserModelClass(mongoDB);
	var FBDataModel = FBDataModelClass(mongoDB);

	// configure app to use bodyParser()
	// this will let us get the data from a POST
	authRouter.use(bodyParser.urlencoded({ extended: true }));
	authRouter.use(bodyParser.json());

	function returnUser(res, user, apiToken, expiration)
	{
		//otherwise, we have discovered our user, send them back!
		//we don't need to send the whole user back, but 
		var rUser = {api_token: apiToken, user: UserModelClass.makeClientSafe(user), expiration: expiration};
		
		console.log("Returning user: ", rUser);
		res.json(rUser);
		//all done!

	}

	function errorOccurredCheck(res, err)
	{
		if(err)
		{
			console.error(err);
			res.status(500).end();
			return true;
		}

		return false;
	}

	function fetchAndReturnUserFromID(user_id, res)
	{
		UserModel.findOne({user_id: user_id})
			.select(UserModelClass.clientSafeProperties().join(' '))
			.lean()
			.exec(function(err, user) {
					
				//uh oh, mongodb error
	 			if(errorOccurredCheck(res, err))
						return;
				
				console.log("Found user: ", user);

				if(!user)
				{
					//there is no existing user with this id, this is a weird error
					console.error(new Error("Weird error: cannot find user despite having known user_id. Malicious?"));
					res.status(500);
					return;
				}

				//we know the user exists, but are they initialized already?
				if(user.isInitialized)
				{
					//we simply create a token for this user - new or otherwise
					tokenAuth.createAPIToken(user.user_id, function(err, apiToken, expiration)
					{
						console.log("ApiToken generated: " + apiToken)
						//we've created an api token, return with user
						returnUser(res, user, apiToken, expiration);
					});
				}
				else
				{
					//otherwise, this is an uninitiated user, we create a temporary signup token for the user
					//there is only 1 temp signup token per user
					tokenAuth.createTemporarySignupToken(user.user_id, function(err, apiToken, expiration)
					{
						console.log("Temp signup token: " + apiToken);
						//send them back with the token
						returnUser(res, user, apiToken, expiration);
					});
				}				

			});
		
	}

	function asyncValidateEmail(email, cb)
	{
		//this is actual email validation
		UserModel.findOne({email: email}).lean().exec(function(err, user)
		{
			if(err){cb(err); return;}
			if(user)
				cb(null, false);
			else
				cb(null, true);
		});
	}
	function validatePassword(pw)
	{
		//this is all junk for now
		console.log("Fake validate password happened");
		return pw.length >= 6;
	}
	function asyncValidateUsername(username, cb)
	{
		//this is actual email validation
		UserModel.findOne({username: username}).lean().exec(function(err, user)
		{
			if(err){cb(err); return;}
			if(user)
				cb(null, false);
			else
				cb(null, true);
		});
	}

	function validateAndSaveUserInformation(user, userJSON, cb)
	{
		//oitherwise, user is not initialised, so we take the user requests and apply them 
		//to the account with this signup mumbo jumbo! We are verified so no worries
		console.log("Unsafe user overwrite occurring, must check email and verify username exists");
		
		//verify the email is not in use before writing it to the user
		user.email = userJSON.email || user.email;

		//TODO: verify the username exists before giving it away -- lol
		user.username = userJSON.username || user.username;

		//this will actually encrypt the password and save the hash
		user.password = userJSON.password;

		//is our user authorized by some google account? Make sure to keep it
		user.googleAuthorizedID = userJSON.googleAuthorizedID;

		//successfully initilaized our user! Save this sexy user to the DB
		user.isInitialized = true;


		if(!validatePassword(user.password))
			cb(new Error("Password validation failed."));
		else
		{
			asyncValidateUsername(user.username, function(err, isValid)
			{
				if(err){cb(err); return;}

				if(isValid)
				{
					asyncValidateEmail(user.email, function(err, isValid)
					{
						if(err){cb(err); return;}

						if(isValid)
							user.save(cb);
						else
							cb(new Error("Email already in use."));
					})
				}
				else
					cb(new Error("Username already taken."));
			});
		}
	}

	//temporary route to verify api access
	authRouter.get('/verify', function(req, res, next){
	  	console.log("credential check: ", req.query.api_token);

	  	var api_token = req.query.api_token;

  		tokenAuth.verifyAPIToken(api_token, function(err, user_id)
  		{
  			if(!user_id)
  			{
  				res.status(401).json({message: "user_id not found from access token"});
  				return;
  			}
  			UserModel.findOne({user_id: user_id})
			.select(UserModelClass.clientSafeProperties().join(' '))
			.lean()
			.exec(function(err, user) {
				if(errorOccurredCheck(res,err))
					return;

				//send it on back
				returnUser(res, user, api_token);
			});
  			
  		});
	});


	authRouter.get('/signup/:username', function(req, res, next){
	  	console.log("username check: ", req.params.username);

  		UserModel.findOne({username: req.params.username}).select('user_id').lean().exec(function(err, existingUser)
  		{
  			if(errorOccurredCheck(res, err))
  				return;

  			var isAvailable = existingUser ? false : true;
  			// console.log("Is '"+ req.params.username + "' available: " + isAvailable)
  			
  			//we must let it be known whether the name is available or not
			res.status(200).json({username: req.params.username, isAvailable: isAvailable});
  		});	
	});

	// will handle any request that ends in /events
	// depends on where the router is "use()'d"
	authRouter.post('/login/facebook', function(req, res, next) {
	   
	   console.log("Login request facebook: ", req.body);

	   if(!req.body.access_token)
	   {
	   		errorOccurredCheck(res, new Error("Improper facebook login request formatting"));
	  		return;
	   }

	   //we have a request to login, and we only have an access token, that means locally on the device we don't
	   //have any user info/api info. Let's ask facebook for the user info, then check if we know that user

	  //now we must request information from facebook that will help us identify our user
	   //this will also verify the access token is functional 
	   //create our super agent for the request to facebook
	   var request = superagent.agent();


	   //contact the graph api -- only one call necessary and pass the token
      request
		.get('https://graph.facebook.com/me?access_token=' + req.body.access_token)
		.accept('application/json')
		.end(function(err, fbRes){
			//uh oh, request error
			if(errorOccurredCheck(res, err))
				return;
			else if(!fbRes.body || !fbRes.body.email)
			{
				errorOccurredCheck(res, new Error("FB Response body does not exist, or there is no e-mail. Are the access_token permissions set right?"));
				return;
			}

			//what did facebook tell us about the user?
			//now we need to determine if we know this particular user or not
			FBDataModel.findOne({fid: fbRes.body.id}, 'user_id email', function(err, associatedUser)
			{
				//we have a mongodb error outside of a null result
				if(errorOccurredCheck(res, err))
					return;

				//if we have an associated user, then we're done, we fetch user info and return it
				if(associatedUser)
				{
					//use our user_id to return the associated user!
					console.log("Found associated user from facebook response, fetching user info to send back");
					fetchAndReturnUserFromID(associatedUser.user_id, res);
 					return;
				}
				else
				{
					//we don't have any associated user, we need to do two things -- store the facebook data and create the user
					//we then pass this temporary unitialized data back to the client -- they'll then use that
					//id to initialize the user info with an email/username/password
					var completedSaves = 0;

					//step 1, create our user and save first, then save FBData for user we can set up flow control later to do this in parallel
					
					//very simple, empty object -- marked as not initialized 
					//we do however associate the facebook account with this new user
					var newUser = new UserModel({
						isInitialized : false,
						email: fbRes.body.email,
						social_accounts: [
							{service_name: 'facebook', service_id: fbRes.body.id}
						]
					});
					//TODO: what if we already have this user -- that is the process failed on saving the FB Data
					//we need to do a check for an existing user before we can save them here (potential duplicate)

					//now we save the user! 
					newUser.save(function(err, savedUser)
					{
						if(errorOccurredCheck(res, err))
							return;

						//user has been saved, lets make our fbdataobject for storage
						var fbData = new FBDataModel({
							fid: fbRes.body.id,
							access_token: req.body.access_token,
							user_id: savedUser.user_id,
							email: fbRes.body.email,
							first_name: fbRes.body.first_name,
							last_name: fbRes.body.last_name,
							gender: fbRes.body.gender
						});

						//save this FB data please
						fbData.save(function(err, fbSaved)
						{
							if(errorOccurredCheck(res, err))
								return;

							//we've created our temporary user but they lack any api authentication
							//they'll need this info to confirm their identity during signup later
							returnUser(res, savedUser.toObject());

						})
					});
				}
			});
		});
	});

	authRouter.post('/signup/facebook', function(req, res, next){
	  console.log("signup request facebook: ", req.body);
	  
	  if(!req.body.api_token || !req.body.user_id || !req.body.email || !req.body.password || !req.body.username)
	  {
	  	errorOccurredCheck(res, new Error("Improper facebook signup request formatting"));
	  	return;
	  }

	  //need to change user information, first verify the api token is correct
	  var fbAccessToken = req.body.api_token;
	  var sent_user_id = req.body.user_id;

	  //the api token is actually the access token for facebook
	  //we request the user info from facebook, then take that information and verify the user_id matches
	   //this will also verify the access token is functional 

	   //create our super agent for the request to facebook
	   var request = superagent.agent();

	   //contact the graph api -- only one call necessary and pass the token
      request
		.get('https://graph.facebook.com/me?access_token=' + fbAccessToken)
		.accept('application/json')
		.end(function(err, fbRes){
			//uh oh, request error
			if(errorOccurredCheck(res, err))
				return;
			//no body or fbid 
			else if(!fbRes.body || !fbRes.body.id)
			{
				errorOccurredCheck(res, new Error("FB Response body does not exist, or there is no fb id. Are the access_token permissions set right?"));
				return;
			}

			//what did facebook tell us about the user?
			//now we need to determine if we know this particular user or not
			FBDataModel.findOne({fid: fbRes.body.id}, 'user_id email', function(err, associatedUser)
			{
				//we have a mongodb error outside of a null result
				if(errorOccurredCheck(res, err))
					return;

				//if we have an associated user, almost done now -- just a few checks
				if(associatedUser)
				{
					//the user ids need to match to know this is an authentic request --
					//facebook agrees they are who they say they are
					if(associatedUser.user_id != sent_user_id)
					{
						errorOccurredCheck(res, new Error("UserID sent does not match access token of user"));
						return;
					}

					//we have a user id and it matches the sent ID! 
					//go fetch our user, then update their info, finally sending back the information
				  	UserModel.findOne({user_id: associatedUser.user_id})
						.exec(function(err, user) {
								
							//uh oh, mongodb error
				 			if(errorOccurredCheck(res, err))
									return;
							
							console.log("Found user: ", user.toObject());

							if(!user)
							{
								//there is no existing user with this id, this is a weird error
								errorOccurredCheck(res, new Error("Weird error: cannot find user during signup despire having known user_id. Malicious?"));
								return;
							}

							//we know the user exists, so we create a nice little token for them
							tokenAuth.createAPIToken(user.user_id, function(err, apiToken, expiration)
							{
								//if we're already initialized -- we send back the object and the token
								//they're already initialized -- they may be maliciously attempting to adjust something
								//that time is over!
								if(user.isInitialized)
								{
									//send them back with the already initialized user
									returnUser(res, savedUser.toObject(), apiToken, expiration);

									//importantly, expire all tokens associated with this user
									tokenAuth.expireSignupTokens(savedUser.user_id, function()
									{
										//we don't care if this succeeds or not -- the tokens will eventually expire anyways
									});

									//all done!
									return;
								}
							
								//Create and save our user info now
								validateAndSaveUserInformation(user, {
									email: req.body.email,
									username: req.body.username,
									password: req.body.password
								}, function(err, savedUser)
								{
									if(errorOccurredCheck(res, err))
										return;

									//send them back with the token and the updated user
									returnUser(res, savedUser.toObject(), apiToken, expiration);
								})

							});

						});

				}
				else
				{
					errorOccurredCheck(res, new Error("Invalid FB Access Token, no associated user found. "));
				}
			});
		});


	
	  //all good 
	   // res.status(200).json({message: "FB Signup router reached successfully"});
	});

	authRouter.post('/login', function(req, res, next) {

		//we login with a post to our login path
		var username = req.body.username;
		var password = req.body.password;

		//should not be hard coded for 6... 
		if(!username || !password || !validatePassword(password))
		{
			errorOccurredCheck(res, new Error("Invalid password format"));
			return;
		}

		//now we do the real deed of searching the user, and confirming
		UserModel.findOne({username: username}, function(err, user)
		{
			if(errorOccurredCheck(res,err))
				return;

			if(!user || !user.isInitialized)
			{
				//no user? wrong username I guess
				errorOccurredCheck(res, new Error("Invalid username/password"))
				return;
			}

			if(user.checkPassword(password))
			{
				//success! they can be logged in, ya hear?
				//now we get them an api token

				//we simply create a token for this user - new or otherwise
				tokenAuth.createAPIToken(user.user_id, function(err, apiToken, expiration)
				{
					if(errorOccurredCheck(res,err))
						return;

					//successful api token, send back user info
					returnUser(res, user.toObject(), apiToken, expiration);
				});
			}
			else
				errorOccurredCheck(res, new Error("Invalid username/password"))
		})



	});

	authRouter.post('/signup/email', function(req, res, next){
	  console.log("signup request email: ", req.body);

	  // console.log("Google web token: " + req.body.api_token);

	  googleAuth.verifyGoogleToken(req.body.api_token, function(isVerified, decoded)
	  {
	  		//double check verified
  			console.log("User is verified? " + isVerified);
	  		
	  		//we have potentially verified the signup with this new information
	  		if(isVerified)
	  		{	  			
	  			//user is google authenticated, as far as we can tell
				var newUser = new UserModel({
					isInitialized : true,
					social_accounts: []
				});

				//we simply create a token for this user - new or otherwise
				tokenAuth.createAPIToken(newUser.user_id, function(err, apiToken, expiration)
				{
					//created token
					console.log("ApiToken generated for verified email user: " + apiToken)
					
					//validate our user info before saving, and we're good to go
	  				//we associate the google account with this new user so we can track if 
	  				//many of the same devices/people create a bunch of different accounts -- possible spam
					validateAndSaveUserInformation(newUser, {
						email: req.body.email,
						username: req.body.username,
						googleAuthorizedID: decoded.email,
						password: req.body.password
					}, function(err, savedUser)
					{
						//check any errors
						if(errorOccurredCheck(res, err))
							return;

						//our user has been verified and saved,
						//return with user
						returnUser(res, savedUser.toObject(), apiToken, expiration);
					})

				});
	  		}
	  		else
	  		{
	  			errorOccurredCheck(res, new Error("Google API Token invalid, cannot determine device identity."));
	  			return;
	  		}


	  });


	 //  var splitToken = req.body.api_token.split(".");

	 //  for(var i=0; i < splitToken.length; i++)
	 //  {
	 //  	var toDecode = splitToken[i];
		// var buf = new Buffer(toDecode, 'base64'); // Ta-da
		// // console.log("Part " + i + ": " + buf.toString());
	 //  }

	  //certs at: https://www.googleapis.com/oauth2/v1/certs

	 //  var crypto = require('crypto');
	 //  var base64URL = require("base64-url");
	 //  var decodeHeader = new Buffer(splitToken[0], 'base64').toString();
	 //  var decodePayload = new Buffer(splitToken[1], 'base64').toString();

		// var unescapesig = base64URL.unescape(splitToken[2]);
		// // console.log("unescape: "  + unescapesig)
	 //  var verified = crypto.createVerify("RSA-SHA256")
  // 							.update(splitToken[0] + "." + splitToken[1])
  // 							.verify("-----BEGIN CERTIFICATE-----\nMIICITCCAYqgAwIBAgIIYArhtPfhtHQwDQYJKoZIhvcNAQEFBQAwNjE0MDIGA1UE\nAxMrZmVkZXJhdGVkLXNpZ25vbi5zeXN0ZW0uZ3NlcnZpY2VhY2NvdW50LmNvbTAe\nFw0xNDEyMDgwOTEzMzRaFw0xNDEyMDkyMjEzMzRaMDYxNDAyBgNVBAMTK2ZlZGVy\nYXRlZC1zaWdub24uc3lzdGVtLmdzZXJ2aWNlYWNjb3VudC5jb20wgZ8wDQYJKoZI\nhvcNAQEBBQADgY0AMIGJAoGBALSRZguUq3Uv6dDsMV7zj/Y7jyyUcLMotqz8uSfY\nNBaJW7R2zbYJ9uzQkHMxcdxQx/BuLHSwyrAWBNDMHiPqi1iQplcTGg/EwwkeVpP3\ntg+ZWNovtc8HpJHITId3gz975wCbhUuA96YiEzoVwvowqt2WNtTWe6+RpWTufbNr\ndGO5AgMBAAGjODA2MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMBYGA1Ud\nJQEB/wQMMAoGCCsGAQUFBwMCMA0GCSqGSIb3DQEBBQUAA4GBAGN+LzexrGDhL+Rf\n+YNRhiu6alt3afMIAJm/JwKR4ommwbP+2cW1fRlcOiEIt/CtOwz9sgwMKrU01I9F\nr/yH38SVdrY7/LDZg1YSZziVzDOYURcwbj0tVKzREVT7g+xwwTR/h8YiYH1IBYN6\nf03y3YGzpdieQWZXkw/PCFMZeLYm\n-----END CERTIFICATE-----\n"
  // 								, unescapesig, 'base64');

  // 								// "_wZcyFHf2JhbExJRPZj_38dT", unescapesig, 'base64');


	 //  // console.log("Payload:" + decodePayload);
	 //  console.log(jsonPayload, "","");


	 //  console.log("Verified signed?" + verified);

	 //  var jsonPayload = JSON.parse(decodePayload);

	  // console.log("Verified Web? " + (googleAuthKeys.web == jsonPayload.aud ? "yes, web matches. " : "no, web doesn't match. "));
	  // console.log("Verified Android? " + (googleAuthKeys.android == jsonPayload.azp ? "yes, android matches. " : "no, android doesn't match. "));


	 });

	return authRouter;

}


