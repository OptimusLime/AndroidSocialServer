var crypto = require('crypto');
var mongoose = require('mongoose');
var Schema = mongoose.Schema;

var Social = new Schema({
    service_name : String,
    service_id: String
});

// User
var User = new Schema({
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

