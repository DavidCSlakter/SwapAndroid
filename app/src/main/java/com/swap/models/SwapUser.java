package com.swap.models;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapperConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMappingException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.AttributeType;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException;
import com.amazonaws.services.cognitoidentityprovider.model.ListUsersRequest;
import com.amazonaws.services.cognitoidentityprovider.model.ListUsersResult;
import com.amazonaws.services.cognitoidentityprovider.model.UserType;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OneSignal;
import com.pinterest.android.pdk.PDKCallback;
import com.pinterest.android.pdk.PDKClient;
import com.pinterest.android.pdk.PDKException;
import com.pinterest.android.pdk.PDKResponse;
import com.swap.R;
import com.swap.Twitter.MyTwitterApiClient;
import com.swap.retrofitApi.APIService;
import com.swap.retrofitApi.ApiUtils;
import com.swap.utilities.AddSubscription;
import com.swap.utilities.AmazonClientManager;
import com.swap.utilities.Constants;
import com.swap.utilities.CreateContact;
import com.swap.utilities.Preferences;
import com.swap.utilities.Utils;
import com.twitter.sdk.android.core.TwitterCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Result;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.swap.models.SwapUser.FollowSpotifyUser_Async.getNewAccessToken;
import static com.swap.utilities.AppHelper.userPoolId;
import static com.swap.utilities.Constants.GITHUB_FOLLOW_USER_URL;
import static com.swap.utilities.Constants.TABLE_USERS;
import static com.swap.utilities.Preferences.GITHUB_ACCESS_TOKEN;

public class SwapUser {

    private static Handler handler;

    public static Users getUser(Context context, String username) {
        try {
            AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
            if (ddb != null) {
                DynamoDBMapper mapper = new DynamoDBMapper(ddb);
                Users objUsers = mapper.load(Users.class, username, null, new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.ConsistentReads.CONSISTENT));
                Log.d("user name ", objUsers.getUsername());
                return objUsers;
            }
        } catch (Exception ex) {
        }
        return null;
    }


    /*
     * Updates one attribute/value pair for the specified user.
     */
    public static boolean updateUsers(Context context, Users updateUser) {
        boolean status = false;

        Log.d("function", "Starting..");
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        Log.d("ddb", "Starting..");

        if (updateUser != null) {
            Log.d("updateUser", "done");
            if (ddb != null) {
                Log.d("ddb", "done");
                DynamoDBMapper mapper = new DynamoDBMapper(ddb);
                try {
                    mapper.save(updateUser);
                    Log.d("save", "done");
                    status = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("ddb", "notDone");
            }
        } else {
            Log.d("updateUser", "notDone");
        }

        return status;

    }

    public static void performActionOnSwapRequestFromUser(Context context, String userName, boolean doAccept) {
        SwapRequest objSwapRequest = new SwapRequest();
        objSwapRequest.setSender(userName);
        objSwapRequest.setRequested(Preferences.get(context, Preferences.USERNAME));
        objSwapRequest.setSent_at((double) System.currentTimeMillis());
        objSwapRequest.setStatus(doAccept); // Not accepted yet

        objSwapRequest.setRequested_user_has_responded_to_request(true);
        if (!doAccept) {
            // Sender rejected the Swap Request. So now, set senderConfirmed = true so that it no longer appears on their pending Swap Requests
            objSwapRequest.setSender_confirmed_acceptance(true);
        }
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);
            try {
                mapper.save(objSwapRequest, new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES));
            } catch (AmazonServiceException ex) {
                /*UserPreferenceDemoActivity.clientManager
                        .wipeCredentialsOnAuthError(ex);*/
            }
        }
    }

    public static void sendSwapRequest(Context context, Users swapUserName) {
        SwapRequest objSwapRequest = new SwapRequest();
        objSwapRequest.setSender(Preferences.get(context, Preferences.USERNAME));
        objSwapRequest.setRequested(swapUserName.getUsername());
        objSwapRequest.setSent_at((double) System.currentTimeMillis());
        objSwapRequest.setStatus(false); // Not accepted yet
        objSwapRequest.setSender_confirmed_acceptance(false); // Sender has not confirmed yet
        objSwapRequest.setRequested_user_has_responded_to_request(false);

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);
            try {
                mapper.save(objSwapRequest, new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES));
                sendNotification(swapUserName);
            } catch (AmazonServiceException ex) {
                /*UserPreferenceDemoActivity.clientManager
                        .wipeCredentialsOnAuthError(ex);*/
            }
        }
    }

    /*public static ArrayList<SwapRequest> getSwaps(Context context, String username) {
        ArrayList<SwapRequest> resultList = new ArrayList<>();
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            // Create our map of values
            Map keyConditions = new HashMap();

            Condition hashKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ.toString())
                    .withAttributeValueList(new AttributeValue().withS(username));
            keyConditions.put("requested", hashKeyCondition);

            Map lastEvaluatedKey = null;
            String sender = "";
            String requested = "";
            String sender_confirmed_acceptance = "";
            String sent_at = "";
            String status = "";
            String requested_user_has_responded_to_request = "";
            do {
                QueryRequest queryRequest = new QueryRequest()
                        .withTableName(Constants.TABLE_SWAP_REQUEST)
                        .withKeyConditions(keyConditions)
                        .withExclusiveStartKey(lastEvaluatedKey)
                        .withIndexName("requested");
                try {
                    QueryResult queryResult = ddb.query(queryRequest);


                    for (Map item : queryResult.getItems()) {
                        // name is a string, so it's stored value will be in the S field
                        Log.i("swap", "record id = '" + item.get("sent_at") + "'");

                        sender = getSubSrting(item.get("sender").toString(), false);
                        requested = getSubSrting(item.get("requested").toString(), false);
                        sender_confirmed_acceptance = getSubSrting(item.get("sender_confirmed_acceptance").toString(), true);
                        sent_at = getSubSrting(item.get("sent_at").toString(), false);
                        status = getSubSrting(item.get("status").toString(), true);
                        requested_user_has_responded_to_request = getSubSrting(item.get("requested_user_has_responded_to_request").toString(), true);
                        if (requested_user_has_responded_to_request.equals("false")) {
                            SwapRequest swapRequest = new SwapRequest();
                            swapRequest.setSender(sender);
                            swapRequest.setRequested(requested);
                            swapRequest.setSender_confirmed_acceptance(Boolean.parseBoolean(sender_confirmed_acceptance));
                            swapRequest.setSent_at(Double.parseDouble(sent_at));
                            swapRequest.setStatus(Boolean.parseBoolean(status));
                            swapRequest.setRequested_user_has_responded_to_request(Boolean.parseBoolean(requested_user_has_responded_to_request));
                            resultList.add(swapRequest);
                        }
                    }
                    // If the response lastEvaluatedKey has contents, that means there are more results
                    lastEvaluatedKey = queryResult.getLastEvaluatedKey();
                } catch (Exception e) {
                }
            } while (lastEvaluatedKey != null);
        }
        return resultList;
    }*/

    /*private static String getSubSrting(String str, boolean isBoolValue) {
        String requiredString;
        if (isBoolValue)
            requiredString = str.substring(str.indexOf(":") + 2, str.indexOf("}"));
        else
            requiredString = str.substring(str.indexOf(":") + 2, str.indexOf(","));
        return requiredString;
    }*/

    public static ArrayList<SwapRequest> getRequestedSwaps(Context context, String requestedUser) {

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);

            Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
            eav.put(":val1", new AttributeValue().withS(requestedUser));
            eav.put(":val2", new AttributeValue().withBOOL(false));

            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                    .withFilterExpression("requested = :val1 and requested_user_has_responded_to_request = :val2").withExpressionAttributeValues(eav);

            List<SwapRequest> scanResult = mapper.scan(SwapRequest.class, scanExpression);

            ArrayList<SwapRequest> resultList = new ArrayList<>();
            for (SwapRequest up : scanResult) {
                resultList.add(up);
            }

            return resultList;
        }
        return null;
    }

    public static ArrayList<SwapRequest> getPendingSentSwapRequests(Context context, String userName) {

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);

            SwapRequest swapRequest = new SwapRequest();
            swapRequest.setSender(userName);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
            expressionAttributeValues.put(":val1", new AttributeValue().withBOOL(false));

            DynamoDBQueryExpression<SwapRequest> queryExpression = new DynamoDBQueryExpression<SwapRequest>()

                    .withHashKeyValues(swapRequest)
                    .withFilterExpression("sender_confirmed_acceptance = :val1")
                    .withExpressionAttributeValues(expressionAttributeValues);

            PaginatedQueryList<SwapRequest> result = mapper.query(SwapRequest.class, queryExpression);
            Log.d("swap request", result.size() + "");
            ArrayList<SwapRequest> resultList = new ArrayList<>();
            for (SwapRequest up : result) {
                resultList.add(up);
            }
            return resultList;
        }
        return null;
    }

    public static boolean swap(Context context, String withUserName, boolean overridePendingAccount) {

        boolean status = false;
        if (withUserName.equals(Preferences.get(context, Preferences.USERNAME))) {
            status = false;
        } else {
            Users user = getUser(context, withUserName);
            if (user != null) {
                boolean userIsPrivate = user.isPrivate();
                boolean shouldSendSwapRequest = overridePendingAccount ? false : userIsPrivate;
                if (shouldSendSwapRequest) {
                    sendSwapRequest(context, user);
                    status = true;
                } else {
                    // Follow social medias.
                    String currentUser = Preferences.get(context, Preferences.USERNAME);
                    String otherUser = user.getUsername();

                    SwapUserHistory objSwapUserHistory = new SwapUserHistory(currentUser, otherUser);
                    objSwapUserHistory.didShare(context, new SwapHistory());

                    try {
                        startSwapAction(context, user, getUser(context, currentUser));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    status = true;
                }
            }
        }
        return status;
    }

    private static void startSwapAction(Context context, Users receiverUser, Users senderUser) {

        try {
            sendSwappedNotification(senderUser, receiverUser);
            if (receiverUser.getWillSharePhone() && receiverUser.getWillShareEmail()) {
                CreateContact.WritePhoneContact(receiverUser.getFirstname() + receiverUser.getLastname(), receiverUser.getPhonenumber(), receiverUser.getEmail(), context);
            }
            if (receiverUser.getWillSharePhone())
                CreateContact.WritePhoneContact(receiverUser.getFirstname() + receiverUser.getLastname(), receiverUser.getPhonenumber(), "", context);
            if (receiverUser.getWillShareEmail())
                CreateContact.WritePhoneContact(receiverUser.getFirstname() + receiverUser.getLastname(), "", receiverUser.getEmail(), context);
            if (receiverUser.getWillShareInstagram() && receiverUser.getInstagram_ID() != null && !receiverUser.getInstagram_ID().isEmpty()) {
                followInstagram(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareSpotify() && receiverUser.getSpotify_ID() != null && !receiverUser.getSpotify_ID().isEmpty()) {
                followSpotify(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareSoundCloud() && receiverUser.getSoundcloud_ID() != null && !receiverUser.getSoundcloud_ID().isEmpty()) {
                followSoundCloud(context, receiverUser, senderUser);
            } else if (receiverUser.getWillSharePinterest() && receiverUser.getPinterest_ID() != null && !receiverUser.getPinterest_ID().isEmpty()) {
                followPinterest(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                followYoutube(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                //getRedditAccessToken(context, receiverUser, senderUser);
                subscribeReddit(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                followGitHub(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                followVimeo(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                followTwitterUser(context, receiverUser, senderUser);
            }

        } catch (Exception e) {
        }
    }

    private static void followTwitterUser(final Context context, Users receiverUser, Users senderUser) {
        MyTwitterApiClient client = new MyTwitterApiClient(TwitterCore.getInstance().getSessionManager().getActiveSession());
        client.getCreateFriendshipService().followTwitterUser("https://api.twitter.com/1.1/friendships/create.json", true, receiverUser.getTwitterUsername(), receiverUser.getTwitter_ID()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.e("Response : ", "Response success");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Response : ", "Response failure");
            }
        });
    }

    private static void followYoutube(Context context, Users receiverUser, Users senderUser) {
        if (Utils.isNetworkConnected(context)) {
            new suscibeAsync(receiverUser, senderUser, context).execute();
        } else {
            getToastMessege(context);
        }
    }


    private static void getToastMessege(final Context context) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
            }
        });

    }

    private static void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    private static class suscibeAsync extends AsyncTask<Object, Object, String> {
        Users receiverUser;
        Users senderUser;
        Context mContext;

        public suscibeAsync(Users receiverUser, Users senderUser, Context context) {
            this.receiverUser = receiverUser;
            this.senderUser = senderUser;
            this.mContext = context;
        }

        @Override
        protected String doInBackground(Object... voids) {
            //suscribeYoutube();
            String value = AddSubscription.main(receiverUser.getYoutube_ID(), mContext);
            return value;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null && !s.isEmpty()) {
                Log.d("result subs: ", s);
            } else {
                Log.d("result subs: ", "null");
            }
            if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                //getRedditAccessToken(mContext, receiverUser, senderUser);
                subscribeReddit(mContext, receiverUser, senderUser);
            } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                followGitHub(mContext, receiverUser, senderUser);
            } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                followVimeo(mContext, receiverUser, senderUser);
            } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                followTwitterUser(mContext, receiverUser, senderUser);
            }
        }
    }

    private static void followVimeo(final Context context, final Users receiverUser, final Users senderUser) {
        if (Utils.isNetworkConnected(context)) {
            try {
                String vimeoFollowUrl = "https://api.vimeo.com/users/" + senderUser.getVimeoID() + "/following/" + receiverUser.getVimeoID() + "?access_token=" + Preferences.get(context, Preferences.VIMEO_ACCESS_TOKEN);
                APIService mAPIService = ApiUtils.getAPIService();
                mAPIService.followVimeoUser(vimeoFollowUrl).enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                        if (response.isSuccessful()) {
                        } else {
                        }
                        if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                            followTwitterUser(context, receiverUser, senderUser);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                            followTwitterUser(context, receiverUser, senderUser);
                        }
                    }
                });
            } catch (Exception e) {

            }
        } else {
            getToastMessege(context);
            // Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
        }

    }

    public static void getRedditAccessToken(final Context context, final Users receiverUser, final Users senderUser) {


        if (Utils.isNetworkConnected(context)) {
            OkHttpClient client = new OkHttpClient();

            String authString = Constants.REDDIT_CLIENT_ID + ":";
            String encodedAuthString = Base64.encodeToString(authString.getBytes(),
                    Base64.NO_WRAP);

            Request request = new Request.Builder()
                    .addHeader("User-Agent", "Swap")
                    .addHeader("Authorization", "Basic " + encodedAuthString)
                    .url(Constants.REDDIT_ACCESS_TOKEN_URL)
                    .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                            "grant_type=authorization_code&code=" + Preferences.get(context, Preferences.REDDIT_CODE) +
                                    "&redirect_uri=" + Constants.REDDIT_REDIRECT_URI))
                    .build();


            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("ERROR: ", String.valueOf(e));
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    String json = response.body().string();

                    JSONObject data = null;
                    try {
                        data = new JSONObject(json);
                        String accessToken = data.optString("access_token");
                        String refreshToken = data.optString("refresh_token");
                        if (accessToken != null) {
                            Preferences.save(context, Preferences.REDDIT_ACCESS_TOKEN, accessToken);
                            subscribeReddit(context, receiverUser, senderUser);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        } else {
            getToastMessege(context);
            // Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
        }


    }

    private static void subscribeReddit(final Context context, final Users receiverUser, final Users senderUser) {

        JSONObject friend = new JSONObject();
        try {
            friend.put("name", receiverUser.getRedditID());
            friend.put("notes", "Hello");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkHttpClient client = new OkHttpClient();
        client.connectTimeoutMillis();
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + Preferences.get(context, Preferences.REDDIT_ACCESS_TOKEN))
                .url(Constants.REDDIT_FRIENDS_URL + receiverUser.getRedditID())
                .put(RequestBody.create(MediaType.parse("application/json"), String.valueOf(friend)))
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("ERROR: ", String.valueOf(e));

                if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                    followGitHub(context, receiverUser, senderUser);
                } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                    followVimeo(context, receiverUser, senderUser);
                } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                    followTwitterUser(context, receiverUser, senderUser);
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String json = response.body().string();
                JSONObject data = null;
                try {
                    data = new JSONObject(json);
                    Log.d("REDDIT = ", String.valueOf(data));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                    followGitHub(context, receiverUser, senderUser);
                } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                    followVimeo(context, receiverUser, senderUser);
                } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                    followTwitterUser(context, receiverUser, senderUser);
                }
            }
        });
    }

    private static void followGitHub(final Context context, final Users otherUser, final Users loggedInUser) {
        if (Utils.isNetworkConnected(context)) {
            APIService mAPIService = ApiUtils.getAPIService();
            String followUrl = GITHUB_FOLLOW_USER_URL + otherUser.getGithubID() + "?access_token=" + Preferences.get(context, GITHUB_ACCESS_TOKEN);
            mAPIService.followGitHub(followUrl).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Log.d("Github follow", "done");
                    } else {
                        Log.d("Github follow", "error");
                    }
                    if (otherUser.getWillShareVimeo() && otherUser.getVimeoID() != null && !otherUser.getVimeoID().isEmpty()) {
                        followVimeo(context, otherUser, loggedInUser);
                    } else if (otherUser.getWillShareTwitter() && otherUser.getTwitter_ID() != null && !otherUser.getTwitter_ID().isEmpty()) {
                        followTwitterUser(context, otherUser, loggedInUser);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("Github follow", "failure");
                    if (otherUser.getWillShareVimeo() && otherUser.getVimeoID() != null && !otherUser.getVimeoID().isEmpty()) {
                        followVimeo(context, otherUser, loggedInUser);
                    } else if (otherUser.getWillShareTwitter() && otherUser.getTwitter_ID() != null && !otherUser.getTwitter_ID().isEmpty()) {
                        followTwitterUser(context, otherUser, loggedInUser);
                    }
                }
            });
        } else {
            getToastMessege(context);
            //  Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
        }
    }

    private static void followPinterest(final Context context, final Users receiverUser, final Users senderUser) {

        if (Utils.isNetworkConnected(context)) {
            String path = "me/following/users/";
            HashMap<String, String> param = new HashMap<String, String>();
            param.put("user", receiverUser.getPinterest_ID());
            PDKClient.getInstance().postPath(path, param, new PDKCallback() {
                @Override
                public void onSuccess(PDKResponse response) {
                    Log.d(getClass().getName(), "Response: " + response.getData().toString());
                    if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                        followYoutube(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                        //getRedditAccessToken(context, receiverUser, senderUser);
                        subscribeReddit(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                        followGitHub(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                        followVimeo(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                        followTwitterUser(context, receiverUser, senderUser);
                    }
                }

                @Override
                public void onFailure(PDKException exception) {
                    Log.e(getClass().getName(), "error: " + exception.getDetailMessage());
                    if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                        followYoutube(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                        //getRedditAccessToken(context, receiverUser, senderUser);
                        subscribeReddit(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                        followGitHub(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                        followVimeo(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                        followTwitterUser(context, receiverUser, senderUser);
                    }
                }
            });
        } else {
            getToastMessege(context);
            // Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
        }

    }

    private static void followSoundCloud(final Context context, final Users receiverUser, final Users senderUser) {
        if (Utils.isNetworkConnected(context)) {
            //String pratitisipl = "65459351";
            //String micheal = "52970802";
            APIService mAPIService = ApiUtils.getAPIService();
            //String soundCloudFollowUrl = "https://api.soundcloud.com/users/" + senderUser.getSoundcloud_ID() + "/followings/" + receiverUser.getSoundcloud_ID() + "?access_token=" + Preferences.get(context, Preferences.SOUND_CLOUD_ACCESS_TOKEN);
            String soundCloudFollowUrl = "https://api.soundcloud.com/me/followings/" + receiverUser.getSoundcloud_ID() + ".json?oauth_token=" + Preferences.get(context, Preferences.SOUND_CLOUD_ACCESS_TOKEN) + "&client_id=" + Constants.SOUNDCLOUD_CLIENT_ID;
            mAPIService.followSoundCloudUser(soundCloudFollowUrl).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                    } else {
                    }
                    if (receiverUser.getWillSharePinterest() && receiverUser.getPinterest_ID() != null && !receiverUser.getPinterest_ID().isEmpty()) {
                        followPinterest(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                        followYoutube(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                        //getRedditAccessToken(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                        followGitHub(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                        followVimeo(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                        followTwitterUser(context, receiverUser, senderUser);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (receiverUser.getWillSharePinterest() && receiverUser.getPinterest_ID() != null && !receiverUser.getPinterest_ID().isEmpty()) {
                        followPinterest(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                        followYoutube(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                       // getRedditAccessToken(context, receiverUser, senderUser);
                        subscribeReddit(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                        followGitHub(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                        followVimeo(context, receiverUser, senderUser);
                    } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                        followTwitterUser(context, receiverUser, senderUser);
                    }
                }
            });
        } else {
            getToastMessege(context);
            // Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
        }
    }

    private static void followSpotify(Context context, Users receiverUser, Users senderUser) {
        if (Utils.isNetworkConnected(context)) {
            String spotifyFollowID = receiverUser.getSpotify_ID();
            long accessTokenTime = Preferences.getLong(context, Preferences.SPOTIFY_ACCESS_TOKEN_TIME);
            int expiryDuration = Preferences.getInt(context, Preferences.SPOTIFY_EXPIRES_IN_DURATION);
            if (System.currentTimeMillis() - accessTokenTime >= expiryDuration) {
                //Access Token Expired
                getNewAccessToken(context, spotifyFollowID);
            } else {
                //Access Token Valid
                new FollowSpotifyUser_Async(context, receiverUser, senderUser).execute();
            }
        } else {
            getToastMessege(context);
            // Toast.makeText(context, R.string.pleaseCheckInternetConnection, Toast.LENGTH_LONG).show();
        }
    }

    public static class FollowSpotifyUser_Async extends AsyncTask<Object, Object, Result> {

        private static final String TAG = "FollowSpotifyUser";
        Context context;
        //String spotifyFollowID;
        static Users receiverUser;
        static Users senderUser;

        public FollowSpotifyUser_Async(Context context, Users receiverUser, Users senderUser) {
            this.context = context;
            this.receiverUser = receiverUser;
            this.senderUser = senderUser;
        }


        @Override
        protected Result doInBackground(Object... voids) {
            SpotifyApi spotifyApi = new SpotifyApi();
            spotifyApi.setAccessToken(Preferences.get(context, Preferences.SPOTIFY_ACCESS_TOKEN));
            SpotifyService spotify = spotifyApi.getService();
            //Following user will work like

            //use the above line to follow users, and you can get response in onPostExecute like you're getting for user's public profile,
            // you only need to pass a spotify user Id.I'll share that with you
            Result result = spotify.followUsers(receiverUser.getSpotify_ID());

            return result;
        }

        public static void getNewAccessToken(final Context context, final String spotifyFollowID) {
            String requestUrl = "https://accounts.spotify.com/api/token";
            APIService mAPIService = ApiUtils.getAPIService();

            mAPIService.renewSpotifyAccessToken(requestUrl, "authorization_code", Preferences.get(context, Preferences.SPOTIFY_REFRESH_TOKEN), Constants.SPOTIFY_CLIENT_ID, Constants.SPOTIFY_CLIENT_SECRET).enqueue(new retrofit2.Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    JSONObject jsonObject = null;
                    try {
                        if (response.body() != null) {
                            String json = response.body().string();
                            if (json != null)
                                jsonObject = new JSONObject(json);

                            if (jsonObject.has("access_token")) {
                                String accessToken = jsonObject.getString("access_token");
                                int expiresIn = Integer.parseInt(jsonObject.getString("expires_in"));

                                if (accessToken != null) {
                                    //Save to preferences
                                    Preferences.save(context, Preferences.SPOTIFY_ACCESS_TOKEN, accessToken);
                                    Preferences.saveInt(context, Preferences.SPOTIFY_EXPIRES_IN_DURATION, expiresIn);

                                    new FollowSpotifyUser_Async(context, receiverUser, senderUser).execute();
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "ERROR: " + "ERROR");
                }
            });
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (receiverUser.getWillShareSoundCloud() && receiverUser.getSoundcloud_ID() != null && !receiverUser.getSoundcloud_ID().isEmpty()) {
                followSoundCloud(context, receiverUser, senderUser);
            } else if (receiverUser.getWillSharePinterest() && receiverUser.getPinterest_ID() != null && !receiverUser.getPinterest_ID().isEmpty()) {
                followPinterest(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                followYoutube(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                //getRedditAccessToken(context, receiverUser, senderUser);
                subscribeReddit(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                followGitHub(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                followVimeo(context, receiverUser, senderUser);
            } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                followTwitterUser(context, receiverUser, senderUser);
            }
        }

    }

    public static void followInstagram(final Context context, final Users receiverUser, final Users senderUser) {

        if (Utils.isNetworkConnected(context)) {
            try {
                String instaFollowUrl = "https://api.instagram.com/v1/users/" + receiverUser.getInstagram_ID() + "/relationship?access_token=" + Preferences.get(context, Preferences.INSTAGRAM_ACCESS_TOKEN);
                APIService mAPIService = ApiUtils.getAPIService();
                mAPIService.followInstagramUser(instaFollowUrl, "follow").enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                        } else {
                        }

                        if (receiverUser.getWillShareSpotify() && receiverUser.getSpotify_ID() != null && !receiverUser.getSpotify_ID().isEmpty()) {
                            followSpotify(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareSoundCloud() && receiverUser.getSoundcloud_ID() != null && !receiverUser.getSoundcloud_ID().isEmpty()) {
                            followSoundCloud(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillSharePinterest() && receiverUser.getPinterest_ID() != null && !receiverUser.getPinterest_ID().isEmpty()) {
                            followPinterest(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                            followYoutube(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                            //getRedditAccessToken(context, receiverUser, senderUser);
                            subscribeReddit(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                            followGitHub(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                            followVimeo(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                            followTwitterUser(context, receiverUser, senderUser);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        if (receiverUser.getWillShareSpotify() && receiverUser.getSpotify_ID() != null && !receiverUser.getSpotify_ID().isEmpty()) {
                            followSpotify(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareSoundCloud() && receiverUser.getSoundcloud_ID() != null && !receiverUser.getSoundcloud_ID().isEmpty()) {
                            followSoundCloud(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillSharePinterest() && receiverUser.getPinterest_ID() != null && !receiverUser.getPinterest_ID().isEmpty()) {
                            followPinterest(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareYouTube() && receiverUser.getYoutube_ID() != null && !receiverUser.getYoutube_ID().isEmpty()) {
                            followYoutube(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareReddit() && receiverUser.getRedditID() != null && !receiverUser.getRedditID().isEmpty()) {
                           // getRedditAccessToken(context, receiverUser, senderUser);
                            subscribeReddit(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareGitHub() && receiverUser.getGithubID() != null && !receiverUser.getGithubID().isEmpty()) {
                            followGitHub(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareVimeo() && receiverUser.getVimeoID() != null && !receiverUser.getVimeoID().isEmpty()) {
                            followVimeo(context, receiverUser, senderUser);
                        } else if (receiverUser.getWillShareTwitter() && receiverUser.getTwitter_ID() != null && !receiverUser.getTwitter_ID().isEmpty()) {
                            followTwitterUser(context, receiverUser, senderUser);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            getToastMessege(context);
        }

    }

    public static ArrayList<SwapHistory> getSwapHistory(Context context, String userName) {

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);
            SwapHistory swapHistory = new SwapHistory();
            swapHistory.setSwap(Preferences.get(context, Preferences.USERNAME));

            DynamoDBQueryExpression<SwapHistory> queryExpression = new DynamoDBQueryExpression<SwapHistory>()
                    .withHashKeyValues(swapHistory);
            ArrayList<SwapHistory> resultList = null;
            try {
                PaginatedQueryList<SwapHistory> result = mapper.query(SwapHistory.class, queryExpression, new DynamoDBMapperConfig(DynamoDBMapperConfig.ConsistentReads.CONSISTENT));

                resultList = new ArrayList<>();
                for (SwapHistory up : result) {
                    resultList.add(up);
                }
            } catch (DynamoDBMappingException e) {
                e.printStackTrace();
            }

            return resultList;
        }
        return null;
    }

    public static void confirmSwapRequestToUser(Context context, String withUserName) {
        SwapRequest swapRequest = new SwapRequest();
        swapRequest.setSender(Preferences.get(context, Preferences.USERNAME));
        swapRequest.setRequested(withUserName);
        swapRequest.setSender_confirmed_acceptance(true);
        swapRequest.setRequested_user_has_responded_to_request(true);
        swapRequest.setSent_at((double) System.currentTimeMillis());

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);
            try {
                mapper.save(swapRequest, new DynamoDBMapperConfig(
                        DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES));
            } catch (AmazonServiceException ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void giveSwapPointsToUsersWhoSwapped(Context context, Users swap, Users swapped) {
        boolean shouldGivePoints = shouldGivePointsSwapAndSwappedBetweenUsers(context, swap, swapped);
        if (shouldGivePoints) {
            incrementSwaps(context, swap);
            incrementSwapped(context, swapped);
            incrementPoints(context, swap, 5);
            incrementPoints(context, swapped, 5);
            SwapUserHistory objSwapUserHistory = new SwapUserHistory(swap.getUsername(), swapped.getUsername());
            SwapHistory swapHistory = new SwapHistory();
            swapHistory.setDidGiveSwapPointsFromSwap(true);
            objSwapUserHistory.didShare(context, swapHistory);
        }
    }

    private static void incrementSwapped(Context context, Users swapped) {
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();

        if (ddb != null) {

            String userName = swapped.getUsername();
            int incrementByValue = 1;
            java.util.Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put("username", new AttributeValue().withS(userName));

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
            expressionAttributeValues.put(":val1", new AttributeValue().withN(String.valueOf(incrementByValue)));

            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(TABLE_USERS)
                    .withKey(key)
                    .withUpdateExpression("SET swapped = swapped + :val1")
                    .withExpressionAttributeValues(expressionAttributeValues);
            ddb.updateItem(updateItemRequest);
        }
    }

    public static void incrementSwaps(Context context, Users swap) {
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();

        if (ddb != null) {
            String userName = swap.getUsername();
            int incrementByValue = 1;
            java.util.Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put("username", new AttributeValue().withS(userName));

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
            expressionAttributeValues.put(":val1", new AttributeValue().withN(String.valueOf(incrementByValue)));

            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(TABLE_USERS)
                    .withKey(key)
                    .withUpdateExpression("SET swaps = swaps + :val1")
                    .withExpressionAttributeValues(expressionAttributeValues);
            ddb.updateItem(updateItemRequest);
        }
    }

    public static void incrementPoints(Context context, Users updateUser, int value) {

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();

        if (ddb != null) {
            String userName = updateUser.getUsername();

            java.util.Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put("username", new AttributeValue().withS(userName));

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
            expressionAttributeValues.put(":val1", new AttributeValue().withN(String.valueOf(value)));

            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(TABLE_USERS)
                    .withKey(key)
                    .withUpdateExpression("SET points = points + :val1")
                    .withExpressionAttributeValues(expressionAttributeValues);
            ddb.updateItem(updateItemRequest);
        }
    }

    public static boolean shouldGivePointsSwapAndSwappedBetweenUsers(Context context, Users swap, Users swapped) {
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();

        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);
            try {
                SwapHistory objSwapHistory = mapper.load(SwapHistory.class, swap.getUsername(), swapped.getUsername());
                if (objSwapHistory == null) {
                    return true;
                } else {
                    boolean didGivePoints = objSwapHistory.getDidGiveSwapPointsFromSwap();
                    return !didGivePoints;
                }

            } catch (AmazonServiceException ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }

    public static ArrayList<SwapHistory> getSwappedHistory(Context context, String userName) {

        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            DynamoDBMapper mapper = new DynamoDBMapper(ddb);

            Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
            eav.put(":val1", new AttributeValue().withS(userName));

            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                    .withFilterExpression("swapped = :val1").withExpressionAttributeValues(eav);
            List<SwapHistory> scanResult = null;
            ArrayList<SwapHistory> resultList = null;
            try {
                scanResult = mapper.scan(SwapHistory.class, scanExpression);
                resultList = new ArrayList<>();
                for (SwapHistory up : scanResult) {
                    resultList.add(up);
                }
            } catch (DynamoDBMappingException e) {
                e.printStackTrace();
            }

            return resultList;
        }
        return null;
    }

    /*public static ArrayList<SwapHistory> getSwappedHistory(Context context, String userName) {

        ArrayList<SwapHistory> resultList = new ArrayList<>();
        AmazonDynamoDBClient ddb = AmazonClientManager.getInstance(context).ddb();
        if (ddb != null) {
            // Create our map of values
            Map keyConditions = new HashMap();

            Condition hashKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ.toString())
                    .withAttributeValueList(new AttributeValue().withS(userName));
            keyConditions.put("swapped", hashKeyCondition);

            Map lastEvaluatedKey = null;
            String swap;
            String swapped;
            String didShareBirthday = "";
            String didShareEmail = "";
            String didShareInstagram = "";
            String didSharePhonenumber = "";
            String didSharePinterest = "";
            String didShareReddit = "";
            String didShareSoundCloud = "";
            String didShareSpotify = "";
            String didShareTwitter = "";
            String didShareVine = "";
            String didShareYouTube = "";
            String didShareGitHub = "";
            String didShareVimeo = "";
            String location = "";
            String method = "";
            String time = "";
            String didGiveSwapPointsFromSwap = "";
            String latitude = "";
            String longitude = "";
            do {
                QueryRequest queryRequest = new QueryRequest()
                        .withTableName(Constants.TABLE_SWAP_HISTORY)
                        .withKeyConditions(keyConditions)
                        .withExclusiveStartKey(lastEvaluatedKey)
                        .withIndexName("swapped");
                try {
                    QueryResult queryResult = ddb.query(queryRequest);

                    for (Map item : queryResult.getItems()) {


                        swap = getSubSrting(item.get("swap").toString(), false);
                        swapped = getSubSrting(item.get("swapped").toString(), false);
                        if (item.get("didShareBirthday") != null)
                            didShareBirthday = getSubSrting(item.get("didShareBirthday").toString(), true);
                        if (item.get("didShareEmail") != null)
                            didShareEmail = getSubSrting(item.get("didShareEmail").toString(), true);
                        if (item.get("didShareInstagram") != null)
                            didShareInstagram = getSubSrting(item.get("didShareInstagram").toString(), true);
                        if (item.get("didSharePhonenumber") != null)
                            didSharePhonenumber = getSubSrting(item.get("didSharePhonenumber").toString(), true);
                        if (item.get("didSharePinterest") != null)
                            didSharePinterest = getSubSrting(item.get("didSharePinterest").toString(), true);
                        if (item.get("didShareReddit") != null)
                            didShareReddit = getSubSrting(item.get("didShareReddit").toString(), true);
                        if (item.get("didShareSoundCloud") != null)
                            didShareSoundCloud = getSubSrting(item.get("didShareSoundCloud").toString(), true);
                        if (item.get("didShareSpotify") != null)
                            didShareSpotify = getSubSrting(item.get("didShareSpotify").toString(), true);
                        if (item.get("didShareTwitter") != null)
                            didShareTwitter = getSubSrting(item.get("didShareTwitter").toString(), true);
                        if (item.get("didShareVine") != null)
                            didShareVine = getSubSrting(item.get("didShareVine").toString(), true);
                        if (item.get("didShareYouTube") != null)
                            didShareYouTube = getSubSrting(item.get("didShareYouTube").toString(), true);
                        if (item.get("didShareGitHub") != null)
                            didShareGitHub = getSubSrting(item.get("didShareGitHub").toString(), true);
                        if (item.get("didShareVimeo") != null)
                            didShareVimeo = getSubSrting(item.get("didShareVimeo").toString(), true);
                        if (item.get("location") != null)
                            location = getSubSrting(item.get("location").toString(), false);
                        if (item.get("method") != null)
                            method = getSubSrting(item.get("method").toString(), false);
                        if (item.get("time") != null)
                            time = getSubSrting(item.get("time").toString(), false);
                        if (item.get("didGiveSwapPointsFromSwap") != null)
                            didGiveSwapPointsFromSwap = getSubSrting(item.get("didGiveSwapPointsFromSwap").toString(), true);
                        if (item.get("latitude") != null)
                            latitude = getSubSrting(item.get("latitude").toString(), false);
                        if (item.get("longitude") != null)
                            longitude = getSubSrting(item.get("longitude").toString(), false);
                        SwapHistory swapHistory = new SwapHistory();
                        swapHistory.setSwap(swap);
                        swapHistory.setSwapped(swapped);

                        swapHistory.setDidShareBirthday(Boolean.parseBoolean(didShareBirthday));
                        swapHistory.setDidShareEmail(Boolean.parseBoolean(didShareEmail));
                        swapHistory.setDidShareInstagram(Boolean.parseBoolean(didShareInstagram));
                        swapHistory.setDidSharePhonenumber(Boolean.parseBoolean(didSharePhonenumber));
                        swapHistory.setDidSharePinterest(Boolean.parseBoolean(didSharePinterest));
                        swapHistory.setDidShareReddit(Boolean.parseBoolean(didShareReddit));
                        swapHistory.setDidShareSoundCloud(Boolean.parseBoolean(didShareSoundCloud));
                        swapHistory.setDidShareTwitter(Boolean.parseBoolean(didShareTwitter));
                        swapHistory.setDidShareSpotify(Boolean.parseBoolean(didShareSpotify));
                        swapHistory.setDidShareVine(Boolean.parseBoolean(didShareVine));
                        swapHistory.setDidShareYouTube(Boolean.parseBoolean(didShareYouTube));
                        swapHistory.setDidShareGitHub(Boolean.parseBoolean(didShareGitHub));
                        swapHistory.setDidShareVimeo(Boolean.parseBoolean(didShareVimeo));
                        swapHistory.setDidGiveSwapPointsFromSwap(Boolean.parseBoolean(didGiveSwapPointsFromSwap));
                        swapHistory.setLocation(location);
                        swapHistory.setMethod(method);
                        swapHistory.setTime(Double.parseDouble(time));
                        if (!latitude.equals(""))
                            swapHistory.setLatitude(Double.parseDouble(latitude));
                        if (!longitude.equals(""))
                            swapHistory.setLongitude(Double.parseDouble(longitude));
                        resultList.add(swapHistory);
                    }
                    // If the response lastEvaluatedKey has contents, that means there are more results
                    lastEvaluatedKey = queryResult.getLastEvaluatedKey();
                } catch (Exception e) {
                    Log.d("get swapped", e.getMessage());
                }
            } while (lastEvaluatedKey != null);
        }
        return resultList;
    }*/

    public static void sendNotification(Users users) {
        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userId = users.getNotification_id_one_signal();
        String pushToken = status.getSubscriptionStatus().getPushToken();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();
        String sendingMessage = users.getFirstname() + " " + users.getLastname() + " (@" + users.getUsername() + ") requested to Swap™ you.";
        if (isSubscribed) {
            try {
                OneSignal.postNotification(new JSONObject("{'contents': {'en': '" + sendingMessage + "'}," +
                                "'include_player_ids': ['" + userId + "'], " +
                                "content_available: true," +
                                "ios_badgeType: Increase," +
                                "ios_badgeCount: 1," +
                                "'headings': {'en': 'Swap'}, " +
                                "'data': {'username': '" + users.getUsername() + "'}}"),
                        new OneSignal.PostNotificationResponseHandler() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.i("OneSignalExample", "postNotification Success: " + response.toString());
                            }

                            @Override
                            public void onFailure(JSONObject response) {
                                Log.e("OneSignalExample", "postNotification Failure: " + response.toString());
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendSwappedNotification(Users loggeidInUser, Users otherUser) {
        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userId = otherUser.getNotification_id_one_signal();
        String pushToken = status.getSubscriptionStatus().getPushToken();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();
        String sendingMessage = loggeidInUser.getFirstname() + " " + loggeidInUser.getLastname() + " (@" + loggeidInUser.getUsername() + ") has Swapped™ you.";
        if (isSubscribed) {
            try {
                OneSignal.postNotification(new JSONObject("{'contents': {'en': '" + sendingMessage + "'}," +
                                "'include_player_ids': ['" + userId + "'], " +
                                "content_available: true," +
                                "ios_badgeType: Increase," +
                                "ios_badgeCount: 1," +
                                "'headings': {'en': 'Swap'}}"),
                        new OneSignal.PostNotificationResponseHandler() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.i("OneSignalExample", "postNotification Success: " + response.toString());
                            }

                            @Override
                            public void onFailure(JSONObject response) {
                                Log.e("OneSignalExample", "postNotification Failure: " + response.toString());
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendNotifcationOfSwapRequestAcceptanceToUser(Users users) {

        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userId = users.getNotification_id_one_signal();
        String pushToken = status.getSubscriptionStatus().getPushToken();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();


        String sendingMessage = users.getFirstname() + " " + users.getLastname() + " (@" + users.getUsername() + ") has accepted your Swap™ Request.";
        if (isSubscribed) {
            try {
                OneSignal.postNotification(new JSONObject("{'contents': {'en': '" + sendingMessage + "'}," +
                                "'include_player_ids': ['" + userId + "'], " +
                                "content_available: true," +
                                "ios_badgeType: Increase," +
                                "ios_badgeCount: 1," +
                                "'headings': {'en': 'Swap'}}"),
                        new OneSignal.PostNotificationResponseHandler() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.i("OneSignalExample", "postNotification Success: " + response.toString());
                            }

                            @Override
                            public void onFailure(JSONObject response) {
                                Log.e("OneSignalExample", "postNotification Failure: " + response.toString());
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public static List<UserType> getCognitoUserList(String username) {

        AmazonCognitoIdentityProviderClient identityUserPoolProviderClient = new AmazonCognitoIdentityProviderClient(new BasicAWSCredentials(Constants.accessKey, Constants.secretKey));
        // omitted stuff...
        // initialize the Cognito Provider client.  This is used to talk to the user pool
        identityUserPoolProviderClient.setRegion(RegionUtils.getRegion("us-east-1")); // var loaded
        // ...some code omitted
        ListUsersRequest listUsersRequest = new ListUsersRequest();
        listUsersRequest.withUserPoolId(userPoolId); // id of the userpool, look this up in Cognito console
        listUsersRequest.withFilter("username ^= \"" + username.toLowerCase() + "\"");  // i THINK this is how the Filter works... the documentation is terribad
        //listUsersRequest.withFilter("username = \"praveenios\"");  // i THINK this is how the Filter works... the documentation is terribad
        listUsersRequest.withLimit(10);
        // get the results
        List<UserType> userTypeList = null;
        try {
        ListUsersResult result = identityUserPoolProviderClient.listUsers(listUsersRequest);
        userTypeList = result.getUsers();
        // loop through them
        for (UserType userType : userTypeList) {
            List<AttributeType> attributeList = userType.getAttributes();
            for (AttributeType attribute : attributeList) {
                String attName = attribute.getName();
                String attValue = attribute.getValue();
                System.out.println(attName + ": " + attValue);
            }
        }
        } catch (InvalidParameterException ipe) {
            ipe.printStackTrace();
        }
        return userTypeList;
    }


}
