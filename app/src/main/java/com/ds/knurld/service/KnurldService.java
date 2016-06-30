package com.ds.knurld.service;

import com.ds.knurld.Config;
import com.ds.knurld.model.AppModel;
import com.ds.knurld.model.ConsumerModel;
import com.ds.knurld.model.EnrollmentModel;
import com.ds.knurld.model.VerificationModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andyshear on 2/15/16.
 */
public class KnurldService {

    // Knurld token, Getter/Setter
    private static String CLIENT_TOKEN = null;
    public String getClientToken() {
        return CLIENT_TOKEN;
    }

    // Models
    private AppModel appModel;
    private ConsumerModel consumerModel;
    private EnrollmentModel enrollmentModel;

    // Model getters/setters
    public AppModel getAppModel() {
        return appModel;
    }
    public void setAppModel(AppModel appModel) {
        this.appModel = appModel;
    }
    public ConsumerModel getConsumerModel() {
        return consumerModel;
    }
    public void setConsumerModel(ConsumerModel consumerModel) {
        this.consumerModel = consumerModel;
    }
    public EnrollmentModel getEnrollmentModel() {
        return enrollmentModel;
    }
    public void setEnrollmentModel(EnrollmentModel enrollmentModel) {
        this.enrollmentModel = enrollmentModel;
    }

    // Empty constructor for MainActivity
    public KnurldService() {
        CLIENT_TOKEN = requestToken();
        KnurldModelService.setClientToken(CLIENT_TOKEN);
        setupExistingKnurldUser(Config.APP_MODEL_ID, Config.CONSUMER_ID, null);
    }

    // Start knurld service by getting token, or
    // Start knurld service with existing token, pass in model Id's if they exist
    public KnurldService(String token, String enrollmentModelId) {
        CLIENT_TOKEN = token == null ? requestToken() : token;
        KnurldModelService.setClientToken(CLIENT_TOKEN);
        setupExistingKnurldUser(Config.APP_MODEL_ID, Config.CONSUMER_ID, enrollmentModelId);
    }

    // Start thread to request token
    public String requestToken() {
        final String[] token = {null};
        Thread tokenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                token[0] = getAccessToken();
            }
        });
        tokenThread.start();

        try {
            tokenThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return token[0];
    }

    // Get knurld access token
    public String getAccessToken(){
        KnurldTokenService knurldTokenService = new KnurldTokenService();
        return CLIENT_TOKEN = knurldTokenService.getToken();
    }

    // Set up an existing knurld user with ID's, or each model will be built from index call
    public void setupExistingKnurldUser(String appModelId, String consumerModelId, String enrollmentModelId) {
        AppModel appModel = new AppModel();
        ConsumerModel consumerModel = new ConsumerModel();
        EnrollmentModel enrollmentModel = new EnrollmentModel();

        appModel.buildFromId(appModelId);
        consumerModel.buildFromId(consumerModelId);
        enrollmentModel.buildFromId(enrollmentModelId);

        setAppModel(appModel);
        setConsumerModel(consumerModel);
        setEnrollmentModel(enrollmentModel);
    }

    // Get existing appModel and consumerModel, then create an enrollment
    public void startEnrollment() {
        AppModel appModel = getAppModel();
        ConsumerModel consumerModel = getConsumerModel();

        JSONObject body = new JSONObject();
        try {
            body.put("consumer", consumerModel.getHref());
            body.put("application", appModel.getHref());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        EnrollmentModel enrollmentModel = new EnrollmentModel();
        enrollmentModel.buildFromResponse(enrollmentModel.create(body.toString()));
        enrollmentModel.buildFromResponse(enrollmentModel.show(enrollmentModel.resourceId));
        setEnrollmentModel(enrollmentModel);
    }

    // Set up a knurld user who has not yet created an enrollment
    public boolean enroll() {
        AppModel appModel = getAppModel();

        final EnrollmentModel enrollmentModel = getEnrollmentModel();

        // Create analysis endpoint
        int words = appModel.getVocabulary().length();
        final JSONObject body = new JSONObject();
        try {
            body.put("filedata", "enrollment.wav");
            body.put("words", words);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Get phrase intervals from analysis service
        final JSONArray intervals = runAnalysis(body);

        // Add phrases to analysis intervals
        JSONObject analysisObj = prepareAnalysisJSON(intervals, getAppModel().getVocabulary(), getAppModel().getEnrollmentRepeats(), getAppModel().getVocabulary().length());

        // Return false if there is a bad analysis, re-record enrollment and try again
        if (analysisObj == null) {
            return false;
        }

        // Update enrollment with valid intervals from analysis, then set enrollment
        enrollmentModel.buildFromResponse(enrollmentModel.update(enrollmentModel.resourceId, analysisObj.toString()));

        // Get updated verification, if it is still processing, poll until complete/failed
        enrollmentModel.buildFromResponse(enrollmentModel.show(enrollmentModel.resourceId));
        while (!enrollmentModel.completed && !enrollmentModel.failed) {
            enrollmentModel.buildFromResponse(enrollmentModel.show(enrollmentModel.resourceId));
        }

        setEnrollmentModel(enrollmentModel);
        return enrollmentModel.completed;
    }

    public String[] startVerification() {
        AppModel appModel = getAppModel();
        ConsumerModel consumerModel = getConsumerModel();

        JSONObject body = new JSONObject();
        try {
            body.put("consumer", consumerModel.getHref());
            body.put("application", appModel.getHref());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VerificationModel verificationModel = new VerificationModel();
        verificationModel.buildFromResponse(verificationModel.create(body.toString()));
        verificationModel.buildFromResponse(verificationModel.show(verificationModel.resourceId));
        return new String[]{verificationModel.phrases, verificationModel.resourceId, verificationModel.phrasesArray.toString()};
    }

    // Set up and run a knurld verification
    public boolean verify(String verificationId, String vocab) {
        AppModel appModel = getAppModel();

        final VerificationModel verificationModel = new VerificationModel();
        verificationModel.buildFromId(verificationId);

        // Create analysis endpoint
        int words = appModel.getVerificationLength();
        final JSONObject body = new JSONObject();
        try {
            body.put("filedata", "verification.wav");
            body.put("words", words);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Get phrase intervals from analysis service
        JSONArray intervals = runAnalysis(body);

        // Add phrases to analysis intervals
        try {
            JSONArray vocabArray = new JSONArray(vocab);
            JSONObject analysisObj = prepareAnalysisJSON(intervals, vocabArray, 1, vocabArray.length());

            // Return false if there is a bad analysis, re-record enrollment and try again
            if (analysisObj == null) {
                return false;
            }

            // Update verification with valid intervals from analysis, then set verification
            verificationModel.buildFromResponse(verificationModel.update(verificationId, analysisObj.toString()));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Get updated verification, if it is still processing, poll until complete/failed
        verificationModel.buildFromResponse(verificationModel.show(verificationModel.resourceId));
        while (!verificationModel.verified && !verificationModel.failed && !verificationModel.completed) {
            verificationModel.buildFromResponse(verificationModel.show(verificationModel.resourceId));
        }

        return verificationModel.verified;
    }

    protected JSONArray runAnalysis(final JSONObject body) {
        // Perform analysis on enrollment.wav
        final KnurldAnalysisService knurldAnalysisService = new KnurldAnalysisService(CLIENT_TOKEN);

        // Start analysis on enrollment.wav
        final String[] analysis = {null};
        final JSONArray[] intervals = {null};
        Thread analysisThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    analysis[0] = knurldAnalysisService.startAnalysis(body.toString());
                    String analysisId = new JSONObject(analysis[0]).getString("taskName");
                    intervals[0] = knurldAnalysisService.getAnalysis(analysisId);

                    // Poll for analysis to finish
                    while (intervals[0] == null) {
                            intervals[0] = knurldAnalysisService.getAnalysis(analysisId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        analysisThread.start();

        try {
            analysisThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return intervals[0];
    }

    protected JSONObject prepareAnalysisJSON(JSONArray phrases, JSONArray vocab, int repeats, int words) {
        JSONObject body = new JSONObject();

        // Add phrases to intervals, accounting for enrollmentRepeats
        boolean validPhrases = true;
        JSONArray newPhrases = new JSONArray();
        try {
            for (int i = 0; i< words * repeats; i++) {
                JSONObject j = phrases.getJSONObject(i);
                int start = j.getInt("start");
                int stop = j.getInt("stop");
                if ((stop - start) < 600) {
                    validPhrases = false;
                }
                j.put("phrase", vocab.get(i%words));
                newPhrases.put(j);
            }
            body.put("intervals", newPhrases);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Check if all phrases are valid, if not, try recording enrollment again
        if (validPhrases) {
            return body;
        }
        return null;
    }

}
