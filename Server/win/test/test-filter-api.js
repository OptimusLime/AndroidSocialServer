
var assert = require('assert');
var should = require('should');
var color = require('colors');
var util = require('util');
var fs = require('fs');
var traverse = require('optimuslime-traverse');
var superagent = require('superagent');

var next = function(range)
{
    return Math.floor((Math.random()*range));
};

var serverLocation = "http://localhost:8000";

var endpoint = {
	recent : serverLocation + "/latest",
	generate : serverLocation + "/upload/generate"
}

var ins = function(obj, val)
{
    return util.inspect(obj, false, val || 10);
}

describe('Testing WIN Filter API -',function(){

    //we need to start up the WIN backend

    before(function(done){

        done();
    });

    beforeEach(function(done){

       done();
    });

    it('Get recent',function(done){

    	superagent
    		.get(endpoint.recent)
    		.query({count: 10})
			.end(function(err, res){

				if(err)
					throw err;
			    //grab the body -- what was in it?
			    console.log(res.body);


    			done();


			});


    });


   

});
