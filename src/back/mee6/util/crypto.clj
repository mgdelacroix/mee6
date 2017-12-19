(ns mee6.util.crypto
  "Monitoring engine namespace."
  (:refer-clojure :exclude [derive])
  (:require [cuerdas.core :as str]
            [mee6.util.transit :as t])
  (:import java.security.MessageDigest
           java.security.SecureRandom
           java.security.spec.KeySpec
           javax.crypto.spec.PBEKeySpec
           javax.crypto.SecretKeyFactory
           org.apache.commons.codec.binary.Base64))

(defn base64-encode
  [^bytes data]
  (Base64/encodeBase64URLSafeString data))

(defn base64-decode
  [^String data]
  (Base64/decodeBase64 data))

(defn digest-data
  "Given the check essential data, calculates the sha256 hash of
  its msgpack representation."
  [data]
  (let [data (t/encode data {:type :msgpack})
        dgst (MessageDigest/getInstance "SHA-256")]
    (.update dgst data 0 (count data))
    (base64-encode (.digest dgst))))

(defn random-bytes
  "Generate a byte array of scpecified length with random
  bytes taken from secure random number generator.
  This method should be used to generate a random
  iv/salt or arbitrary length."
  ([^long numbytes]
   (random-bytes numbytes (SecureRandom.)))
  ([^long numbytes ^SecureRandom sr]
   (let [buffer (byte-array numbytes)]
     (.nextBytes sr buffer)
     buffer)))

(defn random-token
  "Generates a random token with 256 bits of random data."
  []
  (-> (random-bytes 32)
      (base64-encode)))

(defn- equals?
  "Test whether two sequences of characters or bytes are equal in a
  way that protects against timing attacks. Note that this does not
  prevent an attacker from discovering the *length* of the data being
  compared."
  [a b]
  (let [a (map int a), b (map int b)]
    (if (and a b (= (count a) (count b)))
      (zero? (reduce bit-or 0 (map bit-xor a b)))
      false)))

(defn- derive
  "A low level method that derives the password from user provided
  password."
  [password salt]
  (let [password (.toCharArray password)
        keyspec (PBEKeySpec. password salt 10000 256)
        keyfact (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256")]
    (-> (.generateSecret keyfact keyspec)
        (.getEncoded))))

(defn derive-password
  ([password]
   (let [salt (random-bytes 16)]
     (derive-password password salt)))
  ([password salt]
   (let [pwdata (derive password salt)]
     (str (base64-encode salt) "$"
          (base64-encode pwdata)))))

(defn verify-password
  [pwhash password]
  (let [[salt pwdata :as pwhash] (str/split pwhash  "$" 2)]
    (when (and salt pwdata)
      (let [salt (base64-decode salt)
            pwdata (base64-decode pwdata)
            pwdata' (derive password salt)]
        (equals? pwdata' pwdata)))))
