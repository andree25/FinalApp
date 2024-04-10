const functions = require('firebase-functions');
const admin = require('firebase-admin');
const braintree = require('braintree');

admin.initializeApp();

const gateway = new braintree.BraintreeGateway({
  environment: braintree.Environment.Sandbox,
  merchantId: 'dn95q2hkphrtx4rb',
  publicKey: 'mbdd4593wj9tzdkz',
  privateKey: '7002dbbfd4619fc0df6b6ef9ff7a5234'
});

exports.generateClientToken = functions.https.onCall((data, context) => {
  return new Promise((resolve, reject) => {
    gateway.clientToken.generate({}, (err, response) => {
      if (err) {
        reject(new functions.https.HttpsError('internal', 'Error generating client token: ' + err));
      } else {
        resolve({clientToken: response.clientToken});
      }
    });
  });
});



exports.processPayment = functions.https.onCall((data, context) => {
  const nonceFromTheClient = data.paymentMethodNonce;
  const amount = data.amount;

  return new Promise((resolve, reject) => {
    gateway.transaction.sale({
      amount: amount,
      paymentMethodNonce: nonceFromTheClient,
      options: {submitForSettlement: true}
    }, (err, result) => {
      if (err) {
        reject(new functions.https.HttpsError('internal', 'Transaction failed with error: ' + err));
      } else if (result.success) {
        resolve({success: true, transaction: {id: result.transaction.id, amount: result.transaction.amount}});
      } else {
        reject(new functions.https.HttpsError('internal', 'Transaction failed: ' + result.message));
      }
    });
  });
});
