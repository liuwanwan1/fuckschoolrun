package com.acooldog.toolbox.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.acooldog.toolbox.R;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;

import android.widget.TextView;

public class HomeMineFragment extends Fragment {
    public interface Actions {
        void onCheckUpdateClicked();

        void onSettingsClicked();

        void onInternalLoginClicked();

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
        bindInternalLoginState(view);
        Actions actions = getActions();
        if (actions == null) {
            return;
        }
        view.findViewById(R.id.card_update_check).setOnClickListener(v -> actions.onCheckUpdateClicked());
        view.findViewById(R.id.card_settings).setOnClickListener(v -> actions.onSettingsClicked());
        view.findViewById(R.id.card_internal_login).setOnClickListener(v -> actions.onInternalLoginClicked());
        view.findViewById(R.id.card_dev).setOnClickListener(v -> actions.onDeveloperOptionsClicked());
    }

    @Nullable
    private Actions getActions() {
        if (getActivity() instanceof Actions) {
            return (Actions) getActivity();
        }
        return null;
    }

    private void bindInternalLoginState(@NonNull View root) {
        TextView summaryView = root.findViewById(R.id.home_internal_login_summary);
        if (summaryView == null || getContext() == null) {
            return;
        }
        InternalAccountProfile profile = new InternalAuthStore(getContext().getApplicationContext()).getProfile();
        if (profile != null && !profile.getUsername().isEmpty()) {
            summaryView.setText(getString(R.string.home_internal_login_logged_in, profile.getUsername()));
        } else {
            summaryView.setText(R.string.home_internal_login_summary);
        }
    }
}
