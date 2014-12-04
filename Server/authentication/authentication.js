var express = require('express');
var bodyParser = require('body-parser');

module.exports = function(params)
{

  params = params || {};

var authRouter = express.Router();

// configure app to use bodyParser()
// this will let us get the data from a POST
authRouter.use(bodyParser.urlencoded({ extended: true }));
authRouter.use(bodyParser.json());

// will handle any request that ends in /events
// depends on where the router is "use()'d"
authRouter.post('/login/facebook', function(req, res, next) {
   console.log("Login request facebook: ", req.body);

   res.status(200).json({message: "FB Login router reached successfully"});
});

authRouter.post('/signup/facebook', function(req, res, next){
  console.log("signup request facebook: ", req.body);
  
  //all good 
   res.status(200).json({message: "FB Signup router reached successfully"});
});

return authRouter;

}


