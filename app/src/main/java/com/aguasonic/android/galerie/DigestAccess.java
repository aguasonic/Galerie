/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */

package com.aguasonic.android.galerie;

import android.util.Log;

//- Java code.
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashMap;

//- OkHttp support.
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/*=========================================================================================
From:  https://en.wikipedia.org/wiki/Digest_access_authentication

Digest access authentication was originally specified by RFC 2069
(An Extension to HTTP: Digest Access Authentication). RFC 2069
specifies roughly a traditional digest authentication scheme with security
maintained by a server-generated nonce value. The authentication response
is formed as follows (where HA1 and HA2 are names of string variables):

HA1=MD5(username:realm:password)

HA2=MD5(method:digestURI)

response=MD5(HA1:nonce:HA2)

RFC 2069 was later replaced by RFC 2617 (HTTP Authentication: Basic and
Digest Access Authentication). RFC 2617 introduced a number of optional
security enhancements to digest authentication; "quality of protection
(qop), nonce counter incremented by client, and a client-generated
random nonce. These enhancements are designed to protect against,
for example, chosen-plaintext attack cryptanalysis.

-------------------------------------------------------------------------------------------
If the algorithm directive's value is "MD5" or unspecified, then HA1 is

HA1=MD5(username:realm:password)

-------------------------------------------------------------------------------------------
If the algorithm directive's value is "MD5-sess", then HA1 is

HA1=MD5(MD5(username:realm:password):nonce:cnonce)

-------------------------------------------------------------------------------------------
If the qop directive's value is "auth" or is unspecified, then HA2 is

HA2=MD5(method:digestURI)

-------------------------------------------------------------------------------------------
If the qop directive's value is "auth-int", then HA2 is

HA2=MD5(method:digestURI:MD5(entityBody))

-------------------------------------------------------------------------------------------
If the qop directive's value is "auth" or "auth-int", then compute the

response as follows:

response=MD5(HA1:nonce:nonceCount:clientNonce:qop:HA2)

-------------------------------------------------------------------------------------------
If the qop directive is unspecified, then compute the response as follows:

response=MD5(HA1:nonce:HA2)

The above shows that when qop is not specified, the simpler RFC 2069 standard is followed.

=========================================================================================

Example:

The "response" value is calculated in three steps, as follows. Where values are combined,
 they are delimited by colons.

    The MD5 hash of the combined username, authentication realm and password is calculated.
     The result is referred to as HA1.

    The MD5 hash of the combined method and digest URI is calculated, e.g.
     of "GET" and "/dir/index.html". The result is referred to as HA2.

    The MD5 hash of the combined HA1 result, server nonce (nonce), request counter (nc),
     client nonce (cnonce), quality of protection code (qop) and HA2 result is calculated.
      The result is the "response" value provided by the client.

Since the server has the same information as the client, the response can be checked by
 performing the same calculation. In the example given above the result is formed as
  follows, where MD5() represents a function used to calculate an MD5 hash, backslashes
   represent a continuation and the quotes shown are not used in the calculation.

Completing the example given in RFC 2617 gives the following results for each step.

   HA1 = MD5( "Mufasa:testrealm@host.com:Circle Of Life" )
       = 939e7578ed9e3c518a452acee763bce9

   HA2 = MD5( "GET:/dir/index.html" )
       = 39aff3a2bab6126f332b942af96d3366

   Response = MD5( "939e7578ed9e3c518a452acee763bce9:\
                    dcd98b7102dd2f0e8b11d0f600bfb0c093:\
                    00000001:0a4f113b:auth:\
                    39aff3a2bab6126f332b942af96d3366" )
            = 6629fae49393a05397450978507c4ef1

=========================================================================================*/


/**
 * Created by mark on 8/2/15.
 */
public class DigestAccess {
    private final String LOG_TAG = getClass().getSimpleName();
    //- Could be local, sure. But here to be clear...
    private final String KEY_ALGORITHM = "algorithm";
    private final String KEY_AUTHORIZATION = "Authorization";
    private final String KEY_CHALLENGE = "WWW-Authenticate";
    private final String KEY_NONCE = "nonce";
    private final String KEY_QOP = "qop";
    private final String KEY_REALM = "realm";
    private final String KEY_URL = "url";
    //
    //- These values are fixed.
    private final String STR_SEPARATE = ", ";
    private final String THE_COLON = ":";
    private final String THE_EQUALS = "=";
    private final String THE_LEFT_CURLY = "{";
    private final String THE_METHOD = "GET";
    private final String THE_USER = "galerie";
    private int the_nonce_count;
    //
    //- Rest at run-time.
    final Calendar the_calendar = Calendar.getInstance();
    private final OkHttpClient the_client = new OkHttpClient();

    /*------------------- Private Methods
    **/
    //- Usually MD5
    private String get_hash(final String the_algo, final String string_to_hash) {

        try {
            final MessageDigest the_md = MessageDigest.getInstance(the_algo);

            //- Feed it a byte stream.
            the_md.update(string_to_hash.getBytes());

            final byte[] the_md5 = the_md.digest();

            return (StringSupport.encodeHex(the_md5));

        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        }


        return null;
    }

    private String get_client_nonce() {
        final long the_millis = the_calendar.getTimeInMillis();

        //- TODO have to calculate this.
        return (String.format("%o", the_millis));
    }

    /*------------------------------------------------------------
    //- The MD5 hash of the combined username, authentication realm
    //- and password is calculated. The result is referred to as HA1.
    ------------------------------------------------------------*/
    private String get_hash_one(final String the_algo,
                                final String the_user,
                                final String the_realm,
                                final String the_pass) {
        final String all_together = the_user + THE_COLON + the_realm + THE_COLON + the_pass;

        return (get_hash(the_algo, all_together));
    }

    /*------------------------------------------------------------
    //- The MD5 hash of the combined method and digest URI is calculated,
    //- is calculated, /exemplar gratis/ of "GET" and "/dir/index.html".
    //- The result is referred to as HA2.
    ------------------------------------------------------------*/
    private String get_hash_two(final String the_algo, final String the_method, final String the_URL) {
        final String all_together = the_method + THE_COLON + the_URL;

        return (get_hash(the_algo, all_together));
    }

    /*------------------------------------------------------------
    //- The MD5 hash of the combined HA1 result, server nonce (nonce),
    //- request counter (nc), client nonce (cnonce), quality of
    //- protection code (qop) and HA2 result is calculated.
    //- The result is the "response" value provided by the client.
    ------------------------------------------------------------*/
    private String get_response_hash(final String the_algo,
                                     final String the_HA1,
                                     final String the_nonce,
                                     final String nc_in_hex,
                                     final String the_cnonce,
                                     final String the_qop,
                                     final String the_HA2) {

        final String all_together =
                the_HA1 + THE_COLON + the_nonce + THE_COLON + nc_in_hex +
                        THE_COLON + the_cnonce + THE_COLON + the_qop + THE_COLON + the_HA2;

        return (get_hash(the_algo, all_together));
    }

    /*=========================================================================================
    Digest username="Mufasa",
    realm="testrealm@host.com",
    nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
    uri="/dir/index.html", //- OR the complete url, as in url=http://aguasonic.com/index.html
    qop=auth,
    nc=00000001,   //- IN HEXADECIMAL!
    cnonce="0a4f113b",
    response="6629fae49393a05397450978507c4ef1",
    opaque="5ccc069c403ebaf9f0171e9517f40e41"

    (followed by a blank line, as before).

    =========================================================================================*/
    private String build_authorization_text(
            final String the_algorithm,           //- algorithm to use.
            final String the_username,            //- 1 add quotes
            final String the_realm,               //- 2 add quotes
            final String the_complete_nonce,      //- 3 add quotes
            final String the_URI,                 //- 4 add quotes
            final String the_qop,                 //- 5 NO quotes
            final int the_nc,                     //- 6 NO quotes
            final String the_cnonce,              //- 7 add quotes
            final String the_password) {
        final String STR_CNONCE = "cnonce";
        final String STR_EQUALS = "=";
        final String STR_NC = "nc";
        final String STR_PREFIX = "Digest ";
        final String STR_RESPONSE = "response";
        final String STR_URI = "uri";
        final String STR_USERNAME = "username";
        final String the_HA1 = get_hash_one(the_algorithm, the_username, the_realm, the_password);
        final String the_HA2 = get_hash_two(the_algorithm, THE_METHOD, the_URI);
        //- MUST be 8 digit hexadecimal per the specification!
        final String nc_in_hex = String.format("%08x", the_nc);
        final int the_mark = the_complete_nonce.indexOf(THE_EQUALS);
        //
        //- Probably need to track whether this has been used before.
        //  final String the_nonce_key = the_complete_nonce.substring(0, the_mark);
        //
        final String the_nonce_value = the_complete_nonce.substring(the_mark + 1);
        final String the_response_hash =
                get_response_hash(the_algorithm, the_HA1, the_complete_nonce,
                        nc_in_hex, the_cnonce, the_qop, the_HA2);

        //-- Assembled.
        final String line_1 =
                STR_USERNAME + STR_EQUALS + StringSupport.addDoubleQuotes(the_username) + STR_SEPARATE;
        final String line_2 =
                KEY_REALM + STR_EQUALS + StringSupport.addDoubleQuotes(the_realm) + STR_SEPARATE;
        final String line_3 =
                KEY_NONCE + STR_EQUALS + StringSupport.addDoubleQuotes(the_complete_nonce) + STR_SEPARATE;
        final String line_4 =
                STR_URI + STR_EQUALS + StringSupport.addDoubleQuotes(the_URI) + STR_SEPARATE;
        final String line_5 =
                KEY_QOP + STR_EQUALS + the_qop + STR_SEPARATE;
        final String line_6 =
                STR_NC + STR_EQUALS + nc_in_hex + STR_SEPARATE;
        final String line_7 =
                STR_CNONCE + STR_EQUALS + StringSupport.addDoubleQuotes(the_cnonce) + STR_SEPARATE;
        final String line_8 =
                STR_RESPONSE + STR_EQUALS + StringSupport.addDoubleQuotes(the_response_hash);

        //- Staging.
        final String part_1 = line_1 + line_2 + line_3 + line_4;
        final String part_2 = line_5 + line_6 + line_7 + line_8;

        //- OkHttp _will append the required line-feed/space at the end!
        return (STR_PREFIX + part_1 + part_2);
    }

    /*----------------------------------------------------------------------------------
    GET /dir/index.html HTTP/1.0
    Host: localhost
    Authorization: Digest username="Mufasa",
                         realm="testrealm@host.com",
                         nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
                         uri="/dir/index.html",
                         qop=auth,
                         nc=00000001,
                         cnonce="0a4f113b",
                         response="6629fae49393a05397450978507c4ef1",
                         opaque="5ccc069c403ebaf9f0171e9517f40e41"
    (followed by a blank line, as before).

        //the_response.request().newBuilder().addHeader(the_name, the_value).build();
    */

    private Request build_reply(final Response the_response, final String the_password) {
        final String the_www = the_response.header(KEY_CHALLENGE, null);

        if (the_www != null) {
            final int first_space = the_www.indexOf(" ");
            final String the_rest = the_www.substring(first_space + 1);
            final String[] auth_tokens = the_rest.split(STR_SEPARATE);
            final HashMap<String, String> the_map = new HashMap<>();
            final String resp_to_str = the_response.toString();
            final int the_index_of_left_curly = resp_to_str.indexOf(THE_LEFT_CURLY);
            //- This one ends with a curly, too -- and 'split' thinks it's part of the string...
            final String resp_str = resp_to_str.substring(the_index_of_left_curly + 1, resp_to_str.length() - 1);
            final String[] resp_tokens = resp_str.split(STR_SEPARATE);

            //- Parse the response string and find the requested URL.
            for (final String this_string : resp_tokens) {
                final int the_mark = this_string.indexOf(THE_EQUALS);
                final String the_key = this_string.substring(0, the_mark);
                //- Why does it embed quotes?
                final String the_value = this_string.substring(the_mark + 1);

                if (the_key.equals(KEY_URL)) {
                    //- Put it in our map.
                    the_map.put(the_key, the_value);
                }

            }

            for (final String this_string : auth_tokens) {
                //- Find the first 'equals' symbol.
                final int the_mark = this_string.indexOf(THE_EQUALS);
                final String the_key = this_string.substring(0, the_mark);
                //- Why does it embed quotes?
                final String value_with_quotes = this_string.substring(the_mark + 1);
                final String value_unquoted = StringSupport.stripLeadingAndTrailingQuotes(value_with_quotes);

                //- If the nonce is not already there, it is a new one, so reset the counter.
                if (the_key.equals(KEY_NONCE)) {
                    if (!the_map.containsValue(value_unquoted)) {
                        the_nonce_count = 1;
                    }
                }

                //- Why is everything except 'algorithm' wrapped in quotes?
                the_map.put(the_key, value_unquoted);
            }

            //- Assemble the actual text response from all of these parts.
            final String authorization_text =
                    build_authorization_text(the_map.get(KEY_ALGORITHM),
                            THE_USER, the_map.get(KEY_REALM), the_map.get(KEY_NONCE),
                            the_map.get(KEY_URL), the_map.get(KEY_QOP), the_nonce_count++,
                            get_client_nonce(), the_password);

            return (the_response
                    .request()
                    .newBuilder()
                    .header(KEY_AUTHORIZATION, authorization_text)
                    .build());
        }


        //- Else, it's not what we're looking for.
        return null;
    }

    //- Default implementation.
    public Object processResponseBody(final InputStream the_stream) {

        return null;
    }



    public final Object run(final String requested_URL, final String password_to_use) throws Exception {
        the_client.setAuthenticator(new Authenticator() {
            @Override
            public Request authenticate(final Proxy the_proxy, final Response the_response) {

                //- Null indicates no attempt to authenticate.
                return (build_reply(the_response, password_to_use));
            }

            //- We don't do proxies here.
            @Override
            public Request authenticateProxy(final Proxy the_proxy, final Response the_response) {
                //- Null indicates no attempt to authenticate.
                return null;
            }
        });

        final Request the_request =
                new Request.Builder().url(requested_URL).build();

        final Response the_response = the_client.newCall(the_request).execute();
        //- If we were doing repeated requests of the same URL, we would want this.
        //- private final String KEY_AUTH_INFO = "Authentication-Info";
        //- final String the_info = the_response.header(KEY_AUTH_INFO, null);

        if (!the_response.isSuccessful())
            throw new IOException("Unexpected code " + the_response);

        //- TODO
        //- Would like to be able to handle things differently based on what kind of
        //- data is expected to be returned. I could be finsihed _here_, for example,
        //- and just get the body as a string when I am expecting JSON in return
        //- instead of doing the whole InputStream thing...
        //- TODO
        //- final String the_string = the_response.body().toString();

        //- Send the input stream to someone who cares.
        return (processResponseBody(the_response.body().byteStream()));
    }
}


/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */
