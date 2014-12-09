var express = require('express');
var bodyParser = require('body-parser');
var superagent = require('superagent');

var util = require('util');

var UserModelClass = require('./models/user.js');
var FBDataModelClass = require('./models/fbdata.js');

//TODO: Error handling in this process. Need to have an official error handler so we can 
//more efficiently pass errors. Better logging platform like Winston as well

module.exports = function(tokenAuth, mongoDB, params)
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

	function returnUser(res, user, apiToken)
	{
		//otherwise, we have discovered our user, send them back!
		//we don't need to send the whole user back, but 
		var rUser = {api_token: apiToken, user: UserModelClass.makeClientSafe(user)};
		
		console.log("Returning user: ", rUser);
		res.json(rUser);
		//all done!

	}

	function errorOccurredCheck(res, err)
	{
		if(err)
		{
			console.error(err);
			res.status(500);
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
					tokenAuth.createAPIToken(user.user_id, function(err, apiToken)
					{
						console.log("ApiToken generated: " + apiToken)
						//we've created an api token, return with user
						returnUser(res, user, apiToken);
					});
				}
				else
				{
					//otherwise, this is an uninitiated user, we create a temporary signup token for the user
					//there is only 1 temp signup token per user
					tokenAuth.createTemporarySignupToken(user.user_id, function(err, apiToken)
					{
						console.log("Temp signup token: " + apiToken);
						//send them back with the token
						returnUser(res, user, apiToken);
					});
				}				

			});
		
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

						fbData.save(function(err, fbSaved)
						{
							if(errorOccurredCheck(res, err))
								return;

							//finally, we create our initialization API token
							//this is a short lived token for creating a new user and updating their information
							//during signup -- this does not give you access to other parts of the api
							tokenAuth.createTemporarySignupToken(newUser.user_id, function(err, apiToken)
							{
								if(errorOccurredCheck(res, err))
									return;

								//we've created our temporary user and our api authentications, lets send them back!
								returnUser(res, savedUser.toObject(), apiToken);

							})


						})
					});
				}
			});

		});

	});

	authRouter.post('/signup/facebook', function(req, res, next){
	  console.log("signup request facebook: ", req.body);
	  
	  //need to change user information, first verify the signup api token is correct
	  var signupToken = req.body.api_token;

	  tokenAuth.verifySignupToken(signupToken, function(err, user_id)
	  {
	  	//check for error finding signup token
	  	if(errorOccurredCheck(res,err))
	  		return;

	  	//no error! It was found! 
	  	if(!user_id)
		{
			//TODO: this would send a not authorized callback -- the authorization does not exist anymore
			//either is expired, or it was removed
			errorOccurredCheck(res, new Error("Verify signup token returned empty user_id. This is wrong."));
			return;	
		}

		//we have a user id! go fetch our user, then update their info, finally sending back the information
	  	UserModel.findOne({user_id: user_id})
			.exec(function(err, user) {
					
				//uh oh, mongodb error
	 			if(errorOccurredCheck(res, err))
						return;
				
				console.log("Found user: ", user.toObject());

				if(!user)
				{
					//there is no existing user with this id, this is a weird error
					console.error(new Error("Weird error: cannot find user during signup despire having known user_id. Malicious?"));
					res.status(500);
					return;
				}

				if(user.isInitialized)
				{
					//send them back with the already initialized user
					returnUser(res, savedUser.toObject());

					//importantly, expire all tokens associated with this user
					tokenAuth.expireSignupTokens(savedUser.user_id, function()
					{
						//we don't care if this succeeds or not -- the tokens will eventually expire anyways
					});

					return;
				}


				//we know the user exists, so we create a nice little token for them
				tokenAuth.createAPIToken(user.user_id, function(err, apiToken)
				{
					//oitherwise, user is not initialised, so we take the user requests and apply them 
					//to the account with this signup mumbo jumbo! We are verified so no worries
					console.log("Unsafe user overwrite occurring, must check email and verify username exists");
					
					//verify the email is not in use before writing it to the user
					user.email = req.body.email || user.email;

					//TODO: verify the username exists before giving it away -- lol
					user.username = req.body.username || user.username;

					//this will actually encrypt the password and save the hash
					user.password = req.body.password;

					console.log("Official api token: " + apiToken);

					//successfully initilaized our user! Save this sexy user to the DB
					user.isInitialized = true;

					user.save(function(err, savedUser)
					{
						if(errorOccurredCheck(res, err))
							return;

						//send them back with the token and the updated user
						returnUser(res, savedUser.toObject(), apiToken);


					});

				});

			});

	  });

	  //all good 
	   // res.status(200).json({message: "FB Signup router reached successfully"});
	});

	authRouter.post('/signup/email', function(req, res, next){
	  console.log("signup request email: ", req.body);

	  // console.log("Google web token: " + req.body.api_token);

	  var splitToken = req.body.api_token.split(".");

	  for(var i=0; i < splitToken.length; i++)
	  {
	  	var toDecode = splitToken[i];
		var buf = new Buffer(toDecode, 'base64'); // Ta-da
		// console.log("Part " + i + ": " + buf.toString());
	  }

	  //certs at: https://www.googleapis.com/oauth2/v1/certs

	  var crypto = require('crypto');
	  var base64URL = require("base64-url");
	  var decodeHeader = new Buffer(splitToken[0], 'base64').toString();
	  var decodePayload = new Buffer(splitToken[1], 'base64').toString();

		var unescapesig = base64URL.unescape(splitToken[2]);
		// console.log("unescape: "  + unescapesig)
	  var verified = crypto.createVerify("RSA-SHA256")
  							.update(splitToken[0] + "." + splitToken[1])
  							.verify("-----BEGIN CERTIFICATE-----\nMIICITCCAYqgAwIBAgIIYArhtPfhtHQwDQYJKoZIhvcNAQEFBQAwNjE0MDIGA1UE\nAxMrZmVkZXJhdGVkLXNpZ25vbi5zeXN0ZW0uZ3NlcnZpY2VhY2NvdW50LmNvbTAe\nFw0xNDEyMDgwOTEzMzRaFw0xNDEyMDkyMjEzMzRaMDYxNDAyBgNVBAMTK2ZlZGVy\nYXRlZC1zaWdub24uc3lzdGVtLmdzZXJ2aWNlYWNjb3VudC5jb20wgZ8wDQYJKoZI\nhvcNAQEBBQADgY0AMIGJAoGBALSRZguUq3Uv6dDsMV7zj/Y7jyyUcLMotqz8uSfY\nNBaJW7R2zbYJ9uzQkHMxcdxQx/BuLHSwyrAWBNDMHiPqi1iQplcTGg/EwwkeVpP3\ntg+ZWNovtc8HpJHITId3gz975wCbhUuA96YiEzoVwvowqt2WNtTWe6+RpWTufbNr\ndGO5AgMBAAGjODA2MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMBYGA1Ud\nJQEB/wQMMAoGCCsGAQUFBwMCMA0GCSqGSIb3DQEBBQUAA4GBAGN+LzexrGDhL+Rf\n+YNRhiu6alt3afMIAJm/JwKR4ommwbP+2cW1fRlcOiEIt/CtOwz9sgwMKrU01I9F\nr/yH38SVdrY7/LDZg1YSZziVzDOYURcwbj0tVKzREVT7g+xwwTR/h8YiYH1IBYN6\nf03y3YGzpdieQWZXkw/PCFMZeLYm\n-----END CERTIFICATE-----\n"
  								, unescapesig, 'base64');

  								// "_wZcyFHf2JhbExJRPZj_38dT", unescapesig, 'base64');


	  // console.log("Payload:" + decodePayload);
	  console.log(jsonPayload, "","");


	  console.log("Verified signed?" + verified);

	  var jsonPayload = JSON.parse(decodePayload);

	  console.log("Verified Web? " + (googleAuthKeys.web == jsonPayload.aud ? "yes, web matches. " : "no, web doesn't match. "));
	  console.log("Verified Android? " + (googleAuthKeys.android == jsonPayload.azp ? "yes, android matches. " : "no, android doesn't match. "));


	 });

	return authRouter;

}


