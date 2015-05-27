
var assert = require('assert');
var should = require('should');
var color = require('colors');
var util = require('util');
var fs = require('fs');
var traverse = require('optimuslime-traverse');
var superagent = require('superagent');
var Q = require('q');

var next = function(range)
{
    return Math.floor((Math.random()*range));
};

var serverLocation = "http://localhost:8000";

var endpoint = {
    recentArtifacts : serverLocation + "/artifacts/latest",
    hashtagArtifacts : serverLocation + "/artifacts/hashtag",
    hashtagFeed : serverLocation + "/hashtag",
	recent : serverLocation + "/latest",
	generate : serverLocation + "/upload/generate",
    popularArtifacts: serverLocation + '/artifacts/popular',
    favoriteArtifacts: serverLocation + '/artifacts/favorite'
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

    it('Get recent feed items',function(done){

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


    it('Get recent artifacts',function(done){

        superagent
            .get(endpoint.recentArtifacts)
            .query({count: 10})
            .end(function(err, res){

                if(err)
                    throw err;
                //grab the body -- what was in it?
                console.log(res.body);


                done();


            });


    });

     it('Get feed items by hashtag',function(done){

        superagent
            .get(endpoint.hashtagFeed)
            .query({count: 10})
            .query({hashtag: "#save"})
            .end(function(err, res){

                if(err)
                    throw err;
                //grab the body -- what was in it?
                console.log(res.body);


                done();


            });


    });

    it('Get artifacts by hashtag',function(done){

        superagent
            .get(endpoint.hashtagArtifacts)
            .query({count: 10})
            .query({hashtag: "#save"})
            .end(function(err, res){

                if(err)
                    throw err;
                //grab the body -- what was in it?
                console.log(res.body);

                done();
            });
    });

    it('Get artifacts by popularity',function(done){

        superagent
            .get(endpoint.popularArtifacts)
            .query({property: '_id'})
            .query({count: 10})
            .end(function(err, res){

                if(err)
                    throw err;
                //grab the body -- what was in it?
                console.log(res.body);


                done();
            });
    });

    it('Post a favorite artifact',function(done){

        var user = "paul";
        var wid = "testfavorite";

        superagent
            .post(endpoint.favoriteArtifacts + '/' + user + '/' + wid)
            .end(function(err, res){

                if(err)
                    throw err;

                //grab the body -- what was in it?
                console.log(res.body);

                (res.body.liked || res.body.unliked || false).should.equal(true);

                if(res.body.liked)
                {
                    asyncToggleFavorite(wid, user)
                    .then(function(val)
                    {
                        should.exist(val.unliked);
                        val.unliked.should.equal(true);

                        done();
                    })
                    .catch(function(err)
                    {
                        throw err;
                    })
                }
                else
                    done();


            });
    });
    
    function getFavorites(user, done)
    {
        superagent
            .get(endpoint.favoriteArtifacts + "/" + user)
            .query({count: 10})
            .query({start: 0})
            .end(function(err, res){

                if(err)
                    throw err;

                //grab the body -- what was in it?
                console.log('Artifacts found that much be unliked: ', res.body);

                var artifacts = res.body;

                var promiseToUnlike = [];

                for(var i=0; i < artifacts.length; i++)
                {
                    //we need to unlike everything for the reset!
                    promiseToUnlike.push(asyncToggleFavorite(artifacts[i].wid, user));
                }

                Q.allSettled(promiseToUnlike)
                    .then(function(vals)
                    {
                        console.log('All done unliking (not gauranteed): ', vals);
                        // console.log('All done potentially unliking -- not gauranteed to work');
                        done();
                    });
            });

    }

    function asyncToggleFavorite(wid, username)
    {
        var defer = Q.defer();

        var unlikeUrl = endpoint.favoriteArtifacts + "/" + username + "/" + wid;
        console.log('Attempting unlike: ', unlikeUrl);
        superagent
            .post(unlikeUrl)
             .end(function(err, res){
             
                if(err)
                    defer.reject(err);
                else
                {
                    if(res.status == 200)
                        defer.resolve(res.body);
                    else
                        defer.reject(new Error("Invalid request: " + res.status));
                }
             });

        return defer.promise;
    }
    


    it('Get favorite artifacts',function(done){
        
        superagent
            .get(endpoint.recentArtifacts)
            .query({count: 10})
            .end(function(err, res){

                if(err)
                    throw err;
                //grab the body -- what was in it?
                console.log(res.body);

                var artifacts = res.body; 

                var rSelect = Math.floor(Math.random()*artifacts.length);

                var user = 'testuser';
                var finished = false;
                asyncToggleFavorite(artifacts[rSelect].wid, user)
                    .then(function(rBody)
                    {
                        if(rBody.unliked)
                        {
                            return asyncToggleFavorite(artifacts[rSelect].wid, user);
                        }
                        else
                        {
                            finished = true;
                            getFavorites(user, done);
                        }
                    })
                    .then(function(rBody)
                    {
                        if(finished)
                            return;

                        if(rBody.liked)
                        {
                            getFavorites(user, done);
                        }
                        else
                        {
                            throw new Error('Cannot like artifact for some reason');
                        }

                    })
                    .catch(function(err)
                    {
                        throw err;
                    })


            });        
    });   

});
