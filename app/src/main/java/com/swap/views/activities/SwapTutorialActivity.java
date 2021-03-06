package com.swap.views.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import com.swap.R;
import com.swap.adapters.SwapTutorialAdapter;
import com.swap.utilities.Preferences;
import com.viewpagerindicator.CirclePageIndicator;

public class SwapTutorialActivity extends AppCompatActivity {
    ProgressDialog pDialog ;
    String[] sliderImageArray = {"tutorialOne", "tutorialTwo", "tutorialThree", "tutorialFour", "tutorialFive", "tutorialSix", "tutorialSeven", "tutorialEight", "tutorialNine", "tutorialTen"};
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_tutorial);

        pDialog = new ProgressDialog(SwapTutorialActivity.this);
        pDialog.setMessage(getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();

        viewPagerSetup();
        Preferences.saveBooleanValue(this, Preferences.IsConnectSocialMedia, false);
    }

    private void viewPagerSetup() {
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        pagerAdapter = new SwapTutorialAdapter(getSupportFragmentManager(), sliderImageArray, this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);
        CirclePageIndicator titleIndicator = (CirclePageIndicator) findViewById(R.id.circlePageIndicator);
        titleIndicator.setViewPager(viewPager);
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.cancel();
        }
    }
}
