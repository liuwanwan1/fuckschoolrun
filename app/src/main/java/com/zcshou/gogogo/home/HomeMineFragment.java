package com.acooldog.toolbox.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.acooldog.toolbox.R;

public class HomeMineFragment extends Fragment {
    public interface Actions {
        void onCheckUpdateClicked();

        void onSettingsClicked();

        void onDeveloperOptionsClicked();
    }

    public static HomeMineFragment newInstance() {
        return new HomeMineFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Actions actions = getActions();
        if (actions == null) {
            return;
        }
        view.findViewById(R.id.card_update_check).setOnClickListener(v -> actions.onCheckUpdateClicked());
        view.findViewById(R.id.card_settings).setOnClickListener(v -> actions.onSettingsClicked());
        view.findViewById(R.id.card_dev).setOnClickListener(v -> actions.onDeveloperOptionsClicked());
    }

    @Nullable
    private Actions getActions() {
        if (getActivity() instanceof Actions) {
            return (Actions) getActivity();
        }
        return null;
    }
}
