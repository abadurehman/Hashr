package com.misker.mike.hasher;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Main extends AppCompatActivity implements MainView {

    private static final int READ_REQUEST_CODE = 42;
    private ClipboardManager clipboard;
    private Uri fileURI;
    private String hashtype = "MD5";

    @BindView(R.id.hashOutput)
    TextView hashOutput;

    @BindView(R.id.progress)
    ProgressBar progress;

    @BindView(R.id.hashButton)
    Button hashButton;

    @BindView(R.id.hashCmpButton)
    Button hashCmpButton;

    @BindView(R.id.hashText)
    TextView hashText;

    @BindView(R.id.adView)
    AdView mAdView;

    @BindView(R.id.fileButton)
    Button fileButton;

    @BindView(R.id.activity_main)
    View contentView;

    @BindView(R.id.hashSelectionSpinner)
    Spinner selector;

    @BindView(R.id.tabs)
    TabLayout tabs;

    @BindView(R.id.viewpager)
    ViewPager pager;

    @BindString(R.string.banner_ad_unit_id)
    String ad_unit_id;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupFileHashPane(getApplicationContext());

    }
    private void setupFileHashPane(Context context){

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        //setup tabs
        FixedTabsPagerAdapter adapter = new FixedTabsPagerAdapter(getSupportFragmentManager());

        adapter.setContext(context);

        pager.setAdapter(adapter);
        tabs.setupWithViewPager(pager);


        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                closeKeyboard();

                if(tab.getPosition() == 1) {
                    fileButton.setVisibility(View.INVISIBLE);
                    hashText.setVisibility(View.VISIBLE);
                    hashButton.setEnabled(true);
                }
                else {
                    fileButton.setVisibility(View.VISIBLE);
                    hashText.setVisibility(View.INVISIBLE);
                    hashButton.setEnabled(false);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                fileButton.setVisibility(View.VISIBLE);
                hashText.setVisibility(View.INVISIBLE);
                hashButton.setEnabled(true);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        //set visibility and initialize labels/buttons
        hashCmpButton.setText(getString(R.string.compare_hashes));
        hashButton.setEnabled(false);
        progress.setVisibility(View.INVISIBLE);

        //ad init
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(ad_unit_id);

        MobileAds.initialize(this, ad_unit_id);
        AdRequest adRequest = new AdRequest.Builder().setGender(AdRequest.GENDER_MALE).build();
        mAdView.loadAd(adRequest);

        //Done setting up

        fileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFileChooser();
            }
        });

        // handlers for comparing hash text when hashCmpText is clicked
        hashCmpButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                compareHashes();
            }
        });

        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                hashtype = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void closeKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        assert inputManager != null;
        assert getCurrentFocus() != null;
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @OnClick(R.id.hashButton)
    public void hashButtonClick() {
        if(fileButton.getVisibility() == View.VISIBLE) {
            ContentResolver cr = getContentResolver();
            HashRunnable hasher = new HashRunnable(hashtype, cr, this);
            hasher.execute(fileURI);
        }
        else if(hashText.getVisibility() == View.VISIBLE){
            String toHash = hashText.getText().toString();
            HashRunnable hasher = new HashRunnable(hashtype, toHash, this);
            hasher.execute(fileURI);
        }
        closeKeyboard();
        hashCmpButton.setText(R.string.compare_hashes);
        hashCmpButton.setTextColor(Color.WHITE);
    }

    private void compareHashes(){
        Toast toast;
        Context context = getApplicationContext();

        if(!clipboard.hasPrimaryClip()){
            toast = Toast.makeText(context, R.string.emptyClipboard, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        hashCmpButton.setText(clipboard.getPrimaryClip()
                .getItemAt(0).coerceToText(getApplicationContext()).toString().toLowerCase());

        if(hashCmpButton.getText().toString().toUpperCase()
                .equals(hashOutput.getText().toString().toUpperCase())) {
            toast = Toast.makeText(context, getString(R.string.hashesMatch), Toast.LENGTH_SHORT);
            hashCmpButton.setTextColor(Color.GREEN);
            toast.show();
        }
        else {
            toast = Toast.makeText(context, getString(R.string.hashesDontMatch), Toast.LENGTH_SHORT);
            hashCmpButton.setTextColor(Color.RED);
            toast.show();
        }

    }

    /* File chooser for selecting files */
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /* For selecting files for file-hashing */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null) {

            fileURI = resultData.getData();
            //grant permission in advance to prevent SecurityException
            try {
                grantUriPermission(getPackageName(), fileURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            catch (NullPointerException e) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Failed to get file reading permissions.", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            // Check for the freshest data.
            try {
                getContentResolver().takePersistableUriPermission(fileURI,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            catch (SecurityException e) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Opening file failed- report to developer!", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            ContentResolver cr = getContentResolver();
            try {
                InputStream is = cr.openInputStream(fileURI);

                if(is != null)
                    hashButton.setEnabled(true);

            }
            catch (FileNotFoundException e) {
                Log.e("FileDebug", e.getMessage());
            }


        }
    }

    public void displayWaitProgress() {
        progress.setVisibility(View.VISIBLE);
        hashOutput.setText(R.string.waitText);
    }

    public void displayResults(String results) {
        progress.setVisibility(View.INVISIBLE);
        hashOutput.setText(results.toUpperCase());
    }
}