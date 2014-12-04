var express = require('express');
var bodyParser = require('body-parser');
var superagent = require('superagent');


var util = require('util');

var UserModelClass = require('./models/user.js');
var FBDataModelClass = require('./models/fbdata.js');

//TODO: Error handling in this process. Need to have an official error handler so we can 
//more efficiently pass errors. Better logging platform like Winston as well

module.exports = function(mongoDB, params)
{
	if(!mongoDB){
		throw new Error("MongoDB Connection not passed into authentication router");
	}

	params = params || {};

	var authRouter = express.Router();

	var UserModel = UserModelClass(mongoDB);
	var FBDataModel = FBDataModelClass(mongoDB);

	// configure app to use bodyParser()
	// this will let us get the data from a POST
	authRouter.use(bodyParser.urlencoded({ extended: true }));
	authRouter.use(bodyParser.json());

	function returnUser(res, user)
	{
		//otherwise, we have discovered our user, send them back!
		//we don't need to send the whole user back, but 
		var rUser = {api_token: "", user: UserModelClass.makeClientSafe(user), token_expiration: null};
		
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

	function fetchAndReturnUserFromID(id, res)
	{
		UserModel.findOne({_id: id})
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

				//send them back
				returnUser(res, user);
			});
	}

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
							user_id: savedUser._id,
							email: fbRes.body.email,
							first_name: fbRes.body.first_name,
							last_name: fbRes.body.last_name,
							gender: fbRes.body.gender
						});

						fbData.save(function(err, fbSaved)
						{
							if(errorOccurredCheck(res, err))
								return;

							//we've created our temporary user, lets send them back!
							returnUser(res, savedUser.toObject());
						})
					})

				}
			});

		});


	

	  


	});

	authRouter.post('/signup/facebook', function(req, res, next){
	  console.log("signup request facebook: ", req.body);
	  
	  //all good 
	   res.status(200).json({message: "FB Signup router reached successfully"});
	});

	return authRouter;

}


