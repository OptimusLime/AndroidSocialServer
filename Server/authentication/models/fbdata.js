var crypto = require('crypto');
var mongoose = require('mongoose');
var Schema = mongoose.Schema;

// User
var FBData = new Schema({
    access_token: {
        type: String, 
        required: true
    },
    fid: {
        type: String,
        required: true
    },
    email: {
        type: String,
        required: true
    },
    first_name: {
        type: String,
        required: true
    },
    last_name: {
        type: String,
        required: true
    },
    gender: {
        type: String,
        required: true
    },
    user_id : {
        type: String,
        required: true
    },
    created: {
        type: Date,
        default: Date.now
    }
});

//send back our User model for access/creation/finding etc
module.exports = function(db)
{
    return db.model('FBData', FBData);
}

