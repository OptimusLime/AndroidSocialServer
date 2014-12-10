var crypto = require('crypto');
var mongoose = require('mongoose');
var Schema = mongoose.Schema;

var uuid = require("win-utils").cuid;

var traverse = require('optimuslime-traverse');

var Social = new Schema({
    service_name : String,
    service_id: String
});

// User
var User = new Schema({
    user_id : {
     type: String,
     default: function(){
        //generate a random id that's almost impossible to smash on multiple concurrent machines -- time-based as well
        return uuid();
     },
     unique: true
    },
    username: {
        type: String,
        unique: true
    },
    hashedPassword: {
        type: String
    },
    salt: {
        type: String
    },
    email: {
        type: String,
        unique: true
    },
    //this is the google account that was attached to the request 
    //it's not officially used in any context other than to know if someone from
    //the same google account has made more than 1 account -- which is allowed up to a point (possible spam)
    googleAuthorizedID : {
        type: String
    },
    isInitialized: {
        type: Boolean
    },
    //store all our account info as an internal array
    social_accounts : [Social],
    created: {
        type: Date,
        default: Date.now
    }
});

User.methods.encryptPassword = function(password) {
    // return crypto.createHmac('sha1', this.salt).update(password).digest('hex');
    return crypto.pbkdf2Sync(password, this.salt, 10000, 512);
};

//what propoerties to return 

User.virtual('userId')
    .get(function () {
        return this.id;
    });

User.virtual('password')
    .set(function(password) {
        this._plainPassword = password;
        // this.salt = crypto.randomBytes(32).toString('base64');
        this.salt = crypto.randomBytes(128).toString('base64');
        this.hashedPassword = this.encryptPassword(password);
    })
    .get(function() { return this._plainPassword; });


User.methods.checkPassword = function(password) {
    return this.encryptPassword(password) === this.hashedPassword;
};

//send back our User model for access/creation/finding etc
module.exports = function(db)
{
    var social = db.model('Social', Social);
    return db.model('User', User);
}

module.exports.clientSafeProperties = function()
{
    return ["user_id", "username", "email", "isInitialized", "social_accounts.service_name", "social_accounts.service_id"];
}

module.exports.makeClientSafe = function(user)
{
    //what propoerties can we actually return
    var clientSafeProperties = module.exports.clientSafeProperties();
    
    //create a hash of this property array
    var clientSafeHash = {};
    traverse(clientSafeProperties).forEach(function(val){ clientSafeHash[val] = true; });

    //no ids allowed
    var finalUser = traverse(user).map(function (x) {
        //don't do anything with root
        if(this.isRoot) return;

        //always strip out database IDs and weird objects
        if(this.key == "_id" || this.key == "_bsontype") { 
            //remove AND don't investigate any further
            this.remove(true);
        }
        //strip out high level objects
        //for anything other than single level depth, we need to do a check on the path
        if(this.isLeaf)
        {
            var nodePath = this.path.slice(0);

            //simple leaf, do a quick check and be done
            if(this.path.length == 1)
            {
                if(!clientSafeHash[this.key])
                    this.remove();

                return;
            }

            //otherwise, we might end up with complicated paths that involve arrays
            //what we do is we strip out the numbers inside of the path array, then see if we want to keep this object

            var nonNumbersPath = [];
            //remove anything that is a number
            traverse(nodePath).forEach(function (x) {
                // //if we parse an int from the path, you gots to go
                if(this.isLeaf && isNaN(parseInt(x)))
                {
                   nonNumbersPath.push(x);
                }
            });

            //now we have a bunch of non-numbers -- we join them according to how many we have
            var joined;
            if(nonNumbersPath.length ==1)
                joined = nonNumbersPath[0];
            else if(nonNumbersPath.length > 1)
                joined = nonNumbersPath.join('.');

            //if we're only numbers, that's bogus.
            if(!joined)
                this.remove();

            //if it's not approved, remove it!
            if(!clientSafeHash[joined])
                this.remove();
        }
    });
    //has all the safe properties we like and can be sent back to the user no worries
    return finalUser;
}

