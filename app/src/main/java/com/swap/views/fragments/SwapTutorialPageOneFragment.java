package com.swap.views.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.swap.R;
import com.swap.utilities.Utils;


public class SwapTutorialPageOneFragment extends Fragment {

   /* private TextView textViewSwap;
    private TextView textViewTheFasterWayToTrulyConnect;
    private TextView textViewWelcomeToSwap;
    private TextView textViewLeftSwipeToContinue;*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_swap_tutorial_page_one, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

       /* textViewSwap = (TextView) view.findViewById(R.id.textViewSwap);
        textViewTheFasterWayToTrulyConnect = (TextView) view.findViewById(R.id.textViewTheFasterWayToTrulyConnect);
        textViewWelcomeToSwap = (TextView) view.findViewById(R.id.textViewWelcomeToSwap);
        textViewLeftSwipeToContinue = (TextView) view.findViewById(R.id.textViewLeftSwipeToContinue);
        setFontOnViews();*/
    }
    /*private void setFontOnViews() {
        textViewSwap.setTypeface(Utils.setFont(getActivity(), "lantinghei.ttf"));
        textViewTheFasterWayToTrulyConnect.setTypeface(Utils.setFont(getActivity(), "lantinghei.ttf"));
        textViewWelcomeToSwap.setTypeface(Utils.setFont(getActivity(), "lantinghei.ttf"));
        textViewLeftSwipeToContinue.setTypeface(Utils.setFont(getActivity(), "lantinghei.ttf"));
    }*/
}
