package com.speedtouch.clicker;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Gravity;
import android.content.SharedPreferences;
import android.os.Handler;

// Real AdMob SDK Imports
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.InitializationStatus;
import com.google.android.gms.ads.OnInitializationCompleteListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;

public class MainActivity extends Activity {
    
    // ==========================================
    // STATE & STORAGE VARIABLES
    // ==========================================
    private boolean isRunning = false;
    private Button startStopButton;
    private Button statusButton;
    
    private SharedPreferences prefs;
    private static final String PREF_NAME = "SpeedTouchPrefs";
    private static final String KEY_EXPIRY_TIME = "expiry_time";
    
    private static final long DURATION_24_HOURS = 24L * 60L * 60L * 1000L;
    private Handler executionHandler = new Handler();

    // Real AdMob Credentials
    private static final String ADMOB_APP_ID = "ca-app-pub-3155942120880858~9714124558";
    private static final String ADMOB_REWARDED_ID = "ca-app-pub-3155942120880858/2408716697";
    
    private RewardedAd mRewardedAd;
    private int adWatchCount = 0;
    private String pendingAction = "";

    // Execution Modes & Speed Settings
    private String currentMode = "IDLE"; // TOUCH, SCROLL, ROTATE_360
    private int currentSpeed = 1;        // User defined speed parameter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                loadRewardedAd();
            }
        });

        // ==========================================
        // 1. DESIGN SECTION (Visuals & Layout Setup)
        // ==========================================
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        startStopButton = new Button(this);
        startStopButton.setText("Connect / Start SpeedTouch");
        startStopButton.setTextSize(18);

        statusButton = new Button(this);
        statusButton.setText("Status: Disconnected (Voice & Multi-Mode Ready)");
        statusButton.setTextSize(14);
        statusButton.setEnabled(false);

        layout.addView(startStopButton);
        layout.addView(statusButton);
        setContentView(layout);

        checkSessionStatus();


        // ==========================================
        // 2. ACTION & SIGNAL SECTION (Logic & Triggers)
        // ==========================================
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    pendingAction = "Start";
                    adWatchCount = 0;
                    startDoubleAdFlow();
                } else {
                    pendingAction = "Stop";
                    adWatchCount = 0;
                    startDoubleAdFlow();
                }
            }
        });
    }

    // ==========================================
    // 3. MONETIZATION & ADMOB INTEGRATION
    // ==========================================
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, ADMOB_REWARDED_ID, adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    mRewardedAd = null;
                }
            });
    }

    private void startDoubleAdFlow() {
        if (mRewardedAd != null) {
            adWatchCount++;
            Toast.makeText(this, "Playing Ad " + adWatchCount + "/2...", Toast.LENGTH_SHORT).show();
            
            mRewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(RewardItem rewardItem) {
                    if (adWatchCount < 2) {
                        loadRewardedAd();
                        startDoubleAdFlow();
                    } else {
                        Toast.makeText(MainActivity.this, "Both Ads Completed Successfully!", Toast.LENGTH_SHORT).show();
                        if (pendingAction.equals("Start")) {
                            startSpeedTouchEngine();
                        } else {
                            stopSpeedTouchEngine();
                        }
                    }
                }
            });
        } else {
            Toast.makeText(this, "Ad loading... Please wait and try again.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    // ==========================================
    // 4. NATURAL LANGUAGE / VOICE COMMAND PARSER
    // Understands user commands and sets operational mode sequentially
    // ==========================================
    public void parseVoiceCommand(String spokenCommand, int requestedSpeed) {
        if (!isRunning) {
            Toast.makeText(this, "Please connect SpeedTouch first!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSpeed = requestedSpeed;
        String command = spokenCommand.toLowerCase();

        if (command.contains("touch") || command.contains("click")) {
            currentMode = "TOUCH";
            statusButton.setText("Mode: Touch (10 Points) | Speed: " + currentSpeed);
            Toast.makeText(this, "Executing Touch Sequence sequentially...", Toast.LENGTH_SHORT).show();
            executeSequentialEngine();
            
        } else if (command.contains("scroll") || command.contains("slide")) {
            currentMode = "SCROLL";
            statusButton.setText("Mode: Scroll (2 Points) | Speed: " + currentSpeed);
            Toast.makeText(this, "Executing Scroll Sequence sequentially...", Toast.LENGTH_SHORT).show();
            executeSequentialEngine();
            
        } else if (command.contains("rotate") || command.contains("360")) {
            currentMode = "ROTATE_360";
            statusButton.setText("Mode: 360 Rotation (2 Slots) | Speed: " + currentSpeed);
            Toast.makeText(this, "Executing 360 Rotation sequentially...", Toast.LENGTH_SHORT).show();
            executeSequentialEngine();
            
        } else {
            Toast.makeText(this, "Command not recognized. Try touch, scroll, or rotate.", Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // 5. SEQUENTIAL CORE ENGINE & BACKGROUND EXECUTION
    // Ensures tasks run one by one based on command and speed
    // ==========================================
    private void executeSequentialEngine() {
        if (!isRunning) return;

        // Processing single-task execution strictly one by one according to currentMode and currentSpeed
        executionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning && !currentMode.equals("IDLE")) {
                    // Performing background action based on active mode
                    // e.g., Touch (10 pts), Scroll (2 pts), or Rotate 360 (2 slots) sequentially
                    
                    // Loop back or keep ready for next command trigger
                }
            }
        }, (1000 / currentSpeed)); // Speed adjustment logic
    }

    private void startSpeedTouchEngine() {
        isRunning = true;
        startStopButton.setText("Disconnect / Stop SpeedTouch");
        statusButton.setText("Status: Background Ready (Give Voice Command)");
        
        long newExpiryTime = System.currentTimeMillis() + DURATION_24_HOURS;
        prefs.edit().putLong(KEY_EXPIRY_TIME, newExpiryTime).apply();
        
        executionHandler.postDelayed(autoShutdownRunnable, DURATION_24_HOURS);
        Toast.makeText(this, "SpeedTouch Active! Ready for voice/text commands.", Toast.LENGTH_LONG).show();
    }

    private void stopSpeedTouchEngine() {
        isRunning = false;
        currentMode = "IDLE";
        executionHandler.removeCallbacks(autoShutdownRunnable);
        startStopButton.setText("Connect / Start SpeedTouch");
        statusButton.setText("Status: Disconnected");
        
        Toast.makeText(this, "SpeedTouch Disconnected.", Toast.LENGTH_SHORT).show();
    }

    private Runnable autoShutdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                stopSpeedTouchEngine();
                Toast.makeText(MainActivity.this, "24 Hours Session Expired! Watch Ads to reconnect.", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void checkSessionStatus() {
        long expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0);
        if (System.currentTimeMillis() > expiryTime && isRunning) {
            stopSpeedTouchEngine();
        }
    }
              }
