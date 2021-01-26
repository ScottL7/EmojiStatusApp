const functions = require("firebase-functions");

// Create and Deploy Your First Cloud Functions
// https://firebase.google.com/docs/functions/write-firebase-functions

// The Firebase Admin SDK to access Firestore.
const admin = require("firebase-admin");
admin.initializeApp();

exports.helloWorld = functions.https.onRequest((request, response) => {
  functions.logger.info("Hello logs!", {structuredData: true});
  response.send("My favorite emoji is \u{1F43C}");
});

exports.addUserToFirestore = functions.auth.user().onCreate((user) => {
  // This code runs every time a new user is created
  const usersRef = admin.firestore().collection("users");
  return usersRef.doc(user.uid).set({
    displayName: user.displayName,
    emojis: "\u{1F43C}\u{1F33F}\u{2764}", // panda, herb, heart emojis
  });
});
