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
		res.json({user_exists: user.isInitialized, api_token: "", user: {username: user.username}, token_expiration: null});
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
		UserModel.findOne({_id: id}, function(err, user) {
					
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

	   //the very first thing we do is we check for any facebook data with these access token particulars
	   FBDataModel.findOne({access_token: req.body.access_token}, 'user_id email', function(err, existingToken)
	   {
	   		//we have an existing access_token, we can use this to discover our user
	   		if(errorOccurredCheck(res, err))
	   			return;

			if(existingToken)
			{
			 	fetchAndReturnUserFromID(existingToken.user_id, res);
			 	return;
			}
			else
			{
				  //now we must request information from facebook that will help us identify our user
				   //this will also verify the access token is functional 
				   //create our super agent for the request to facebook
				   var request = superagent.agent();


			      request
					.get('https://graph.facebook.com/me?access_token=' + req.body.access_token)
					.accept('application/json')
					.end(function(err, fbRes){
						// Do something
						//uh oh, request error
						if(errorOccurredCheck(res, err))
							return;
						else if(!fbRes.body || !fbRes.body.email)
						{
							errorOccurredCheck(new Error("FB Response body does not exist, or there is no e-mail. Are the access_token permissions set right?"));
							return;
						}

						//what did facebook tell us about the user?
						//now we need to determine if we know this particular user or not
						FBDataModel.findOne({fid: fbRes.body.id}, 'user_id email', function(err, associatedUser)
						{
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
								var completedSaves = 0;

								//step 1, create our user and save first, then save FBData for user we can set up flow control later to do this in parallel
								
								//very simple, empty object -- marked as not initialized 
								//we do however associate the facebook account with this new user
								var newUser = new UserModel({
									isInitialized : false,
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
										returnUser(res, savedUser);
									})
								})

							}
						});

					});


			}
	   });


	 

	

	  


	});

	authRouter.post('/signup/facebook', function(req, res, next){
	  console.log("signup request facebook: ", req.body);
	  
	  //all good 
	   res.status(200).json({message: "FB Signup router reached successfully"});
	});

	return authRouter;

}


