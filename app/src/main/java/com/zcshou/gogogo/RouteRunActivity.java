package com.acooldog.toolbox;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.acooldog.toolbox.nfc.data.AndroidNfcPayloadDispatcher;
import com.acooldog.toolbox.nfc.domain.NfcPayload;
import com.acooldog.toolbox.nfc.domain.NfcPayloadDispatchResult;
import com.acooldog.toolbox.nfc.domain.SendNfcPayloadUseCase;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.config.SimulationPrefsStore;
import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;
import com.acooldog.toolbox.route.domain.model.RouteSimulationConfig;
import com.acooldog.toolbox.route.domain.model.RouteShareInfo;
import com.acooldog.toolbox.route.domain.service.RouteEditUtils;
import com.acooldog.toolbox.route.domain.service.LocationSimulationGateway;
import com.acooldog.toolbox.route.presentation.RouteRunViewModel;
import com.acooldog.toolbox.root.RootDiagnosticCompatibilityCatalog;
import com.acooldog.toolbox.root.RootEnvironmentInspector;
import com.acooldog.toolbox.root.RootEnvironmentReport;
import com.acooldog.toolbox.root.RootDiagnosticModule;
import com.acooldog.toolbox.root.RootDiagnosticSettings;
import com.acooldog.toolbox.root.RootDiagnosticSettingsStore;
import com.acooldog.toolbox.root.RootDiagnosticSessionController;
import com.acooldog.toolbox.root.RootDiagnosticSessionReport;
import com.acooldog.toolbox.root.LsposedDiagnosticBridge;
import com.acooldog.toolbox.root.RootFeature;
import com.acooldog.toolbox.root.RootFeatureConfig;
import com.acooldog.toolbox.root.RootFeatureConfigStore;
import com.acooldog.toolbox.root.RootFeatureRuntimeController;
import com.acooldog.toolbox.root.RootFeatureRuntimeReport;
import com.acooldog.toolbox.root.RootGmTestData;
import com.acooldog.toolbox.root.RootGmTestDataGenerator;
import com.acooldog.toolbox.root.RootSensorMotionProfile;
import com.acooldog.toolbox.root.RootSignalStrengthProfile;
import com.acooldog.toolbox.root.RootShellProbeResult;
import com.acooldog.toolbox.root.RootTestAuditLogger;
import com.acooldog.toolbox.share.domain.model.SharedNfcEntry;
import com.acooldog.toolbox.share.domain.model.SharedRoutePayload;
import com.acooldog.toolbox.share.domain.model.SharedRouteSummary;
import com.acooldog.toolbox.share.domain.model.SharedSimulationConfigEntry;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.share.presentation.ShareModule;
import com.acooldog.toolbox.service.ServiceGo;
import com.acooldog.toolbox.utils.GoUtils;
import com.acooldog.toolbox.utils.MapUtils;
import com.acooldog.toolbox.utils.SearchSortUtils;
import com.elvishew.xlog.XLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.view.ViewGroup;

public class RouteRunActivity extends BaseActivity {
    public static final String EXTRA_ROUTE_ID = "route_id";
    public static final String EXTRA_SHARED_ROUTE_ID = "shared_route_id";
    public static final String EXTRA_SHARED_ROUTE_NAME = "shared_route_name";
    public static final String EXTRA_PENDING_NFC_URL = "pending_nfc_url";
    public static final String EXTRA_PENDING_NFC_PACKAGE = "pending_nfc_package";
    public static final String EXTRA_PENDING_NFC_SOURCE = "pending_nfc_source";
    private static final String ROUTE_EDIT_MARKER_INDEX_KEY = "route_edit_marker_index";
    private static final String ROUTE_EDIT_MARKER_TYPE_KEY = "route_edit_marker_type";
    private static final String ROUTE_EDIT_MARKER_TYPE_VERTEX = "route_edit_vertex";
    private static final long REMINDER_REPLAY_INTERVAL_MILLIS = 5000L;
    private static final long[] COMPLETION_VIBRATION_PATTERN = new long[] {0L, 400L, 300L};
    private static final int MAX_EDITABLE_ROUTE_VERTICES = 24;
    private static final double ROUTE_INSERT_MAX_DISTANCE_METERS = 20d;
    private static final float ROUTE_INSERT_SCREEN_HIT_SLOP_DP = 28f;
    private static final int ANDROID_9_API = 28;
    private static final int ANDROID_13_API = 33;

    private RouteRunViewModel viewModel;
    private MapView mapView;
    private BaiduMap baiduMap;
    private TextView currentRouteView;
    private TextView routeEditHintView;
    private View panelContentLayout;
    private View bottomControlLayout;
    private Spinner simulationModeSpinner;
    private View speedControlLayout;
    private View cadenceControlLayout;
    private EditText speedInput;
    private EditText cadenceInput;
    private EditText loopInput;
    private CheckBox speedFloatCheckBox;
    private Button toggleButton;
    private Button realRunLinkButton;
    private ImageButton panelToggleButton;
    private Switch liveRouteEditSwitch;
    private Marker simulationMarker;
    private BitmapDescriptor simulationProgressDescriptor;
    private BitmapDescriptor routeEditVertexDescriptor;
    private BitmapDescriptor routeEditVertexSelectedDescriptor;
    private ServiceGo.ServiceGoBinder serviceBinder;
    private boolean bound;
    private boolean mockLocationPromptShown;
    private boolean restoringSimulationPrefs;
    private boolean realRunLinkRunning;
    private boolean pendingRealRunLinkStart;
    private boolean panelCollapsed;
    private boolean liveRouteEditEnabled;
    private boolean liveRouteEditVerticesVisible;
    private boolean suppressLiveRouteEditSwitchCallback;
    private boolean routeEditTapCandidate;
    private boolean suppressNextRouteEditTap;
    private View privacyMaskPanel;
    private TextView privacyMaskText;
    private ExecutorService ioExecutor;
    private SimulationPrefsStore prefsStore;
    private SendNfcPayloadUseCase sendNfcPayloadUseCase;
    private RootEnvironmentInspector rootEnvironmentInspector;
    private RootTestAuditLogger rootAuditLogger;
    private RootFeatureConfigStore rootFeatureConfigStore;
    private RootFeatureRuntimeController rootFeatureRuntimeController;
    private RootDiagnosticSessionController rootDiagnosticSessionController;
    private RootDiagnosticSettingsStore rootDiagnosticSettingsStore;
    private RootEnvironmentReport latestRootEnvironmentReport;
    private RootFeatureConfig latestRootFeatureConfig;
    private RootFeatureRuntimeReport latestRootFeatureRuntimeReport;
    private RootDiagnosticSettings latestRootDiagnosticSettings;
    private RootDiagnosticSessionReport latestRootDiagnosticReport;
    private boolean rootTestSessionConfirmed;
    private boolean rootShellAuthorized;
    private boolean suppressRootFeatureSwitchCallbacks;
    private boolean lastRouteDiagnosticLocationAvailable;
    private double lastRouteDiagnosticLongitude;
    private double lastRouteDiagnosticLatitude;
    private double lastRouteDiagnosticAltitude;
    private float lastRouteDiagnosticSpeed;
    private float lastRouteDiagnosticBearing;
    private BroadcastReceiver rootDiagnosticStateRequestReceiver;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;
    private float initialStepCounterValue = -1f;
    private float lastStepCounterValue = -1f;
    private double pendingLinkedDistanceMeters;
    private PendingMotion pendingMotion;
    private long lastHandledCompletionToken = -1L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Ringtone activeReminderRingtone;
    private FrameLayout simulationSettingsOverlay;
    private FrameLayout simulationSettingsContentFrame;
    private TextView simulationSettingsTitleView;
    private Button simulationSettingsPrimaryButton;
    private View simulationSettingsFormView;
    private boolean simulationSettingsSubPageVisible;
    private RootSettingsSaveAction simulationSettingsActiveSaveAction;
    private TextView settingsReminderToneView;
    private TextView settingsNonRootTabView;
    private TextView settingsRootTabView;
    private View settingsRootContainer;
    private View settingsRootEnvironmentSection;
    private View settingsRootRestrictedSection;
    private View settingsRootAlgorithmLabSection;
    private View settingsRootLogsSection;
    private TextView settingsRootInternalStatusView;
    private TextView settingsRootModeStatusView;
    private TextView settingsRootStatusView;
    private TextView settingsRootAuthorizationStatusView;
    private TextView settingsRootHiddenStatusView;
    private TextView settingsRootDeveloperStatusView;
    private TextView settingsRootMockStatusView;
    private TextView settingsRootHookStatusView;
    private TextView settingsRootTargetStatusView;
    private TextView settingsRootCompatibilityStatusView;
    private TextView settingsRootFeatureConfigStatusView;
    private TextView settingsRootDiagnosticLogView;
    private TextView settingsRootAuditLogView;
    private Button settingsRootRefreshButton;
    private Button settingsRootConfirmSessionButton;
    private Button settingsRootRequestSuButton;
    private Button settingsRootPickTargetButton;
    private Button settingsRootNmeaSettingsButton;
    private Button settingsRootSignalSettingsButton;
    private Button settingsRootBypassSettingsButton;
    private Button settingsRootHookSettingsButton;
    private Button settingsRootServiceSettingsButton;
    private Button settingsRootSensorSettingsButton;
    private Button settingsRootReloadConfigButton;
    private Button settingsRootGenerateGmButton;
    private Button settingsAlgorithmLabButton;
    private Button settingsTestInstructionStudioButton;
    private Button settingsScenarioLibraryButton;
    private Button settingsPressureLabButton;
    private Switch settingsRootEnvironmentSwitch;
    private Switch settingsRootModeSwitch;
    private Switch settingsRootSuProbeSwitch;
    private Switch settingsRootEncryptedAuditSwitch;
    private Switch settingsRootGmInterfaceSwitch;
    private Switch settingsRootFridaInjectionSwitch;
    private Switch settingsRootNmeaSwitch;
    private Switch settingsRootSignalSwitch;
    private Switch settingsRootBypassSwitch;
    private Switch settingsRootHookSwitch;
    private Switch settingsRootServiceLogSwitch;
    private Switch settingsRootSensorSwitch;
    private EditText settingsLinkRatioLeftInput;
    private EditText settingsLinkRatioRightInput;
    private EditText settingsStepsPerMeterInput;
    private EditText settingsLoopInput;
    private SeekBar settingsSatelliteSeekBar;
    private TextView settingsSatelliteValueView;
    private RadioGroup settingsSignalQualityGroup;
    private SeekBar settingsHdopSeekBar;
    private TextView settingsHdopValueView;
    private SeekBar settingsUpdateIntervalSeekBar;
    private TextView settingsUpdateIntervalValueView;
    private Switch settingsNetworkSimulationSwitch;
    private Switch settingsDynamicIntensitySwitch;
    private EditText settingsIntensityRangeInput;
    private SeekBar settingsIntensityFrequencySeekBar;
    private TextView settingsIntensityFrequencyValueView;
    private Switch settingsPathVariationSwitch;
    private EditText settingsPathVariationInput;
    private Switch settingsAltitudeVariationSwitch;
    private EditText settingsAltitudeBaseInput;
    private EditText settingsAltitudeVariationRangeInput;
    private EditText settingsAltitudeHeightInput;
    private SeekBar settingsAltitudeProbabilitySeekBar;
    private TextView settingsAltitudeProbabilityValueView;
    private Switch settingsFloatingWindowSwitch;
    private SeekBar settingsFloatingWindowScaleSeekBar;
    private SeekBar settingsFloatingWindowButtonSizeSeekBar;
    private TextView settingsFloatingWindowScaleView;
    private TextView settingsFloatingWindowButtonSizeView;
    private View settingsFloatingWindowPreview;
    private final Runnable reminderReplayRunnable = this::replayCompletionReminder;
    private final RootFeatureConfigStore.OnConfigChangedListener rootFeatureConfigListener =
            config -> runOnUiThread(() -> applyRootFeatureConfig(config, "hot_reload", true));
    private Vibrator vibrator;
    private float panelDragStartY;
    private boolean panelDragTriggered;
    private int panelDragThresholdPx;
    private int selectedEditableRoutePointIndex = -1;
    private int suppressRouteMarkerSelectionIndex = -1;
    private int routeEditTapSlopPx;
    private int routeEditRouteHitSlopPx;
    private float routeEditTapDownX;
    private float routeEditTapDownY;
    private NfcPayload pendingSimulationNfcPayload;
    private boolean completionNoticePending;
    private String selectedRouteIdForEditState;
    private RouteDefinition originalEditableRouteDefinition;
    private View completionOverlay;
    private WindowManager floatingWindowManager;
    private FrameLayout floatingWindowRoot;
    private MapView floatingWindowMapView;
    private BaiduMap floatingWindowBaiduMap;
    private TextView floatingWindowTitleView;
    private View floatingWindowHandle;
    private View floatingWindowResizeHandle;
    private TextView floatingWindowPauseButton;
    private TextView floatingWindowResumeButton;
    private WindowManager.LayoutParams floatingWindowLayoutParams;
    private Marker floatingWindowMarker;
    private boolean floatingWindowVisible;
    private double floatingWindowBdLongitude;
    private double floatingWindowBdLatitude;
    private boolean floatingWindowHasPoint;
    private float floatingWindowDragStartRawX;
    private float floatingWindowDragStartRawY;
    private int floatingWindowDragStartX;
    private int floatingWindowDragStartY;
    private float floatingWindowResizeStartRawX;
    private float floatingWindowResizeStartRawY;
    private float floatingWindowResizeStartScale;
    private final List<Integer> editableRoutePointIndices = new ArrayList<>();
    private final Runnable showFloatingWindowRetryRunnable = this::maybeShowFloatingWindow;

    private final ActivityResultLauncher<String> activityRecognitionPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    if (pendingRealRunLinkStart) {
                        pendingRealRunLinkStart = false;
                        beginRealRunLink();
                    }
                } else {
                    pendingRealRunLinkStart = false;
                    stopLinkedSimulationState();
                    GoUtils.DisplayToast(this, getString(R.string.route_link_permission_denied));
                }
            });

    private final SensorEventListener stepSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!realRunLinkRunning || event == null || event.sensor == null) {
                return;
            }
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                handleStepCounterEvent(event);
            } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                int steps = Math.max(1, Math.round(event.values[0]));
                consumeDetectedSteps(steps);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No-op.
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (ServiceGo.ServiceGoBinder) service;
            flushPendingMotion();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBinder = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_run);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(RouteRunViewModel.class);
        mapView = findViewById(R.id.map_run);
        baiduMap = mapView.getMap();
        currentRouteView = findViewById(R.id.tv_current_route);
        routeEditHintView = findViewById(R.id.tv_run_route_edit_hint);
        panelContentLayout = findViewById(R.id.layout_run_content);
        bottomControlLayout = findViewById(R.id.layout_run_bottom_control);
        simulationModeSpinner = findViewById(R.id.spinner_run_mode);
        speedControlLayout = findViewById(R.id.layout_run_speed_control);
        cadenceControlLayout = findViewById(R.id.layout_run_cadence_control);
        speedInput = findViewById(R.id.et_run_speed);
        cadenceInput = findViewById(R.id.et_run_cadence);
        loopInput = findViewById(R.id.et_loop_count);
        speedFloatCheckBox = findViewById(R.id.cb_speed_float);
        privacyMaskPanel = findViewById(R.id.route_privacy_mask_panel);
        privacyMaskText = findViewById(R.id.route_privacy_mask_text);
        Button loadButton = findViewById(R.id.btn_run_load);
        Button nfcEntryButton = findViewById(R.id.btn_run_nfc_entry);
        toggleButton = findViewById(R.id.btn_run_toggle);
        realRunLinkButton = findViewById(R.id.btn_run_real_link);
        panelToggleButton = findViewById(R.id.btn_run_panel_toggle);
        liveRouteEditSwitch = findViewById(R.id.switch_run_live_edit);
        Button simulationSettingsButton = findViewById(R.id.btn_run_simulation_settings);
        ioExecutor = Executors.newSingleThreadExecutor();
        prefsStore = new SimulationPrefsStore(getApplicationContext());
        sendNfcPayloadUseCase = new SendNfcPayloadUseCase(new AndroidNfcPayloadDispatcher());
        rootEnvironmentInspector = new RootEnvironmentInspector(getApplicationContext());
        rootAuditLogger = new RootTestAuditLogger(getApplicationContext());
        rootFeatureConfigStore = new RootFeatureConfigStore(getApplicationContext());
        rootFeatureRuntimeController = new RootFeatureRuntimeController(getApplicationContext());
        rootDiagnosticSessionController = new RootDiagnosticSessionController(getApplicationContext());
        rootDiagnosticSettingsStore = new RootDiagnosticSettingsStore(getApplicationContext());
        latestRootFeatureConfig = rootFeatureConfigStore.load();
        latestRootFeatureRuntimeReport = rootFeatureRuntimeController.reload(latestRootFeatureConfig);
        latestRootDiagnosticSettings = rootDiagnosticSettingsStore.load();
        registerRootDiagnosticStateRequestReceiver();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetectorSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        panelDragThresholdPx = ViewConfiguration.get(this).getScaledTouchSlop() * 2;
        routeEditTapSlopPx = ViewConfiguration.get(this).getScaledTouchSlop();
        routeEditRouteHitSlopPx = Math.round(dp(ROUTE_INSERT_SCREEN_HIT_SLOP_DP));
        completionOverlay = findViewById(R.id.route_completion_overlay);

        setupSimulationModeSpinner();
        setupRouteEditingInteractions();
        restoreSimulationPrefs();
        bindSimulationConfigInputs();
        updatePanelCollapsedState();
        capturePendingNfcPayload(getIntent());
        bindCompletionOverlay();

        loadButton.setOnClickListener(v -> showLoadOptions());
        nfcEntryButton.setOnClickListener(v -> simulateCampusRunEntry());
        toggleButton.setOnClickListener(v -> toggleSimulation());
        realRunLinkButton.setOnClickListener(v -> toggleRealRunLink());
        panelToggleButton.setOnClickListener(v -> togglePanelCollapsed());
        if (liveRouteEditSwitch != null) {
            liveRouteEditSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleLiveRouteEditToggle(isChecked));
        }
        findViewById(R.id.layout_run_panel_handle).setOnTouchListener(this::handlePanelDragGesture);
        simulationSettingsButton.setOnClickListener(v -> showSimulationSettingsDialog());
        updateLiveRouteEditHint();

        observeViewModel();
        loadRoutes();
        promptMockLocationIfNeeded();

        String preselectedRouteId = getIntent().getStringExtra(EXTRA_ROUTE_ID);
        String preselectedSharedRouteId = getIntent().getStringExtra(EXTRA_SHARED_ROUTE_ID);
        if (!TextUtils.isEmpty(preselectedRouteId)) {
            try {
                viewModel.selectRouteById(preselectedRouteId);
            } catch (Exception ignored) {
                // Ignore and keep picker available.
            }
        } else if (!TextUtils.isEmpty(preselectedSharedRouteId)) {
            loadSharedRouteForSimulation(preselectedSharedRouteId, getIntent().getStringExtra(EXTRA_SHARED_ROUTE_NAME));
        } else {
            restoreLastRouteSelection();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (shouldHandleRouteEditActivityTouch(event)) {
            handleRouteEditMapTouch(event);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainHandler.removeCallbacks(showFloatingWindowRetryRunnable);
        mapView.onResume();
        if (rootDiagnosticSettingsStore != null) {
            latestRootDiagnosticSettings = rootDiagnosticSettingsStore.load();
        }
        hideFloatingWindow();
        if (prefsStore.isRouteCompletionPending()) {
            completionNoticePending = true;
        }
        showPendingCompletionNoticeIfPossible();
    }

    @Override
    protected void onPause() {
        stopReminderFeedback();
        persistSimulationPrefs();
        mapView.onPause();
        super.onPause();
        mainHandler.postDelayed(showFloatingWindowRetryRunnable, 250L);
    }

    @Override
    protected void onStop() {
        maybeShowFloatingWindow();
        mainHandler.postDelayed(showFloatingWindowRetryRunnable, 600L);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (simulationSettingsOverlay != null) {
            handleSimulationSettingsBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        stopRealRunSensorListener();
        stopReminderFeedback();
        hideSimulationSettingsOverlay();
        mainHandler.removeCallbacks(showFloatingWindowRetryRunnable);
        completionNoticePending = false;
        hideCompletionOverlay();
        removeFloatingWindow();
        viewModel.stopSimulation();
        if (rootDiagnosticSessionController != null && rootDiagnosticSessionController.isRunning()) {
            RootDiagnosticSessionController.FinishResult result = rootDiagnosticSessionController.finishSession();
            appendRootAudit("目标APK诊断因页面销毁自动结束: " + result.getMessage());
        }
        unregisterRootDiagnosticStateRequestReceiver();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        recycleMapDescriptors();
        mapView.onDestroy();
        super.onDestroy();
    }

    private void registerRootDiagnosticStateRequestReceiver() {
        if (rootDiagnosticStateRequestReceiver != null) {
            return;
        }
        rootDiagnosticStateRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null
                        || !LsposedDiagnosticBridge.ACTION_DIAGNOSTIC_STATE_REQUEST.equals(intent.getAction())
                        || rootDiagnosticSessionController == null) {
                    return;
                }
                String targetPackage = intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_TARGET_PACKAGE);
                boolean replayed = rootDiagnosticSessionController.rebroadcastActiveSession(
                        TextUtils.isEmpty(targetPackage) ? "target_process_request" : "target_process_request:" + targetPackage
                );
                if (replayed) {
                    appendRootAudit("响应LSPosed目标进程状态请求: target=" + targetPackage);
                    renderRootDiagnosticPanel();
                }
            }
        };
        IntentFilter filter = new IntentFilter(LsposedDiagnosticBridge.ACTION_DIAGNOSTIC_STATE_REQUEST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(rootDiagnosticStateRequestReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(rootDiagnosticStateRequestReceiver, filter);
        }
    }

    private void unregisterRootDiagnosticStateRequestReceiver() {
        if (rootDiagnosticStateRequestReceiver == null) {
            return;
        }
        try {
            unregisterReceiver(rootDiagnosticStateRequestReceiver);
        } catch (Exception ignored) {
            // Receiver may already be gone during activity teardown.
        }
        rootDiagnosticStateRequestReceiver = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void observeViewModel() {
        viewModel.getSelectedRoute().observe(this, routeDefinition -> {
            if (routeDefinition == null) {
                currentRouteView.setText(getString(R.string.route_current_none));
                baiduMap.clear();
                simulationMarker = null;
                clearLiveRouteEditState(false);
                updatePrivacyMask(null);
                updateLiveRouteEditHint();
                renderToggleButtonState();
                return;
            }
            applySelectedRouteEditState(routeDefinition);
            currentRouteView.setText(getString(R.string.route_current_format, buildRouteLabel(routeDefinition)));
            drawRoute(routeDefinition, !liveRouteEditVerticesVisible);
            updatePrivacyMask(routeDefinition);
            updateFloatingWindowRoute(routeDefinition);
            persistSimulationPrefs();
            updateLiveRouteEditHint();
            renderToggleButtonState();
        });

        viewModel.getSimulationFrame().observe(this, simulationFrame -> {
            if (simulationFrame == null) {
                return;
            }
            LatLng latLng = new LatLng(
                    simulationFrame.getPoint().getBdLatitude(),
                    simulationFrame.getPoint().getBdLongitude()
            );
            if (simulationMarker == null) {
                simulationMarker = (Marker) baiduMap.addOverlay(new MarkerOptions()
                        .position(latLng)
                        .icon(getSimulationProgressDescriptor()));
            } else {
                simulationMarker.setPosition(latLng);
            }
            if (!liveRouteEditVerticesVisible) {
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
            }
            updateFloatingWindowPosition(
                    simulationFrame.getPoint().getWgsLongitude(),
                    simulationFrame.getPoint().getWgsLatitude()
            );
        });

        viewModel.isRunning().observe(this, isRunning -> {
            renderToggleButtonState();
            updateFloatingWindowControlsState();
        });

        viewModel.isResumable().observe(this, resumable -> {
            renderToggleButtonState();
            updateFloatingWindowControlsState();
        });

        viewModel.getSimulationCompletedEvent().observe(this, token -> {
            if (token == null || token == lastHandledCompletionToken) {
                return;
            }
            lastHandledCompletionToken = token;
            stopLinkedSimulationState();
            finishRootDiagnosticSession(true);
            prefsStore.setRouteCompletionPending(true);
            completionNoticePending = true;
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                bringRouteRunToFrontForCompletion();
            }
            showPendingCompletionNoticeIfPossible();
        });
    }

    private void loadRoutes() {
        try {
            viewModel.refreshRoutes();
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, "读取路线失败");
        }
    }

    private void showLoadOptions() {
        String[] options = {
                getString(R.string.route_pick_local_option),
                getString(R.string.route_pick_shared_option)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_pick_source_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRoutePicker();
                    } else {
                        loadSharedRoutes();
                    }
                })
                .show();
    }

    private void showRoutePicker() {
        List<RouteDefinition> routes = viewModel.getRoutes().getValue();
        if (routes == null || routes.isEmpty()) {
            GoUtils.DisplayToast(this, "请先创建、导入或下载路线");
            return;
        }

        CharSequence[] routeNames = new CharSequence[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = buildRouteLabel(routes.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("选择路线")
                .setItems(routeNames, (dialog, which) -> viewModel.selectRoute(routes.get(which)))
                .show();
    }

    private void loadSharedRoutes() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.route_shared_loading));
        ioExecutor.execute(() -> {
            try {
                List<SharedRouteSummary> routes = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoutes();
                runOnUiThread(() -> showSharedRoutePicker(routes));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_load_failed, exception)));
            }
        });
    }

    private void showSharedRoutePicker(List<SharedRouteSummary> routes) {
        if (routes == null || routes.isEmpty()) {
            GoUtils.DisplayToast(this, getString(R.string.route_shared_empty));
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_picker, null);
        EditText searchInput = dialogView.findViewById(R.id.searchable_picker_input);
        ListView listView = dialogView.findViewById(R.id.searchable_picker_list);
        LinearLayout letterRail = dialogView.findViewById(R.id.searchable_picker_letter_rail);
        List<SearchableRouteItem> allItems = buildRouteItems(routes);
        List<SearchableRouteItem> filteredItems = new ArrayList<>(allItems);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        fillRouteAdapter(adapter, filteredItems);
        listView.setAdapter(adapter);
        updatePickerLetterRail(letterRail, listView, filteredItems);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_shared_pick_title)
                .setView(dialogView)
                .setNegativeButton(R.string.route_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> resizeDialogWindow(dialog, 0.82f, 0.70f));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (filteredItems.isEmpty()) {
                return;
            }
            dialog.dismiss();
            SharedRouteSummary summary = filteredItems.get(position).summary;
            if (summary.isPrivacyMode()) {
                loadSharedRouteForSimulation(summary.getId(), summary.getName());
            } else {
                downloadSharedRoute(summary);
            }
        });
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterRouteItems(editable == null ? "" : editable.toString(), allItems, filteredItems, adapter);
                updatePickerLetterRail(letterRail, listView, filteredItems);
            }
        });
        dialog.show();
    }

    private void downloadSharedRoute(SharedRouteSummary summary) {
        GoUtils.DisplayToast(this, getString(R.string.route_shared_downloading));
        ioExecutor.execute(() -> {
            try {
                SharedRoutePayload payload = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoute(summary.getId());
                String localName = TextUtils.isEmpty(payload.getName()) ? summary.getName() : payload.getName();
                viewModel.saveRoute(localName, payload.getPoints(), payload.toShareInfo(true));
                runOnUiThread(() -> GoUtils.DisplayToast(
                        this,
                        getString(payload.isPrivacyMode() ? R.string.route_shared_downloaded_private : R.string.route_shared_downloaded)
                ));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_download_failed, exception)));
            }
        });
    }

    private void loadSharedRouteForSimulation(String shareId, @Nullable String fallbackName) {
        if (TextUtils.isEmpty(shareId)) {
            return;
        }
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.route_shared_downloading));
        ioExecutor.execute(() -> {
            try {
                SharedRoutePayload payload = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedRoute(shareId);
                String routeName = TextUtils.isEmpty(payload.getName()) ? fallbackName : payload.getName();
                if (TextUtils.isEmpty(routeName)) {
                    routeName = "shared_" + shareId;
                }
                RouteDefinition routeDefinition = new RouteDefinition(
                        "shared_preview_" + shareId,
                        routeName,
                        payload.getCreatedAt(),
                        System.currentTimeMillis(),
                        payload.getPoints(),
                        new java.io.File(getCacheDir(), "shared_preview_" + shareId + ".route.json"),
                        new RouteShareInfo(
                                shareId,
                                true,
                                payload.isPrivacyMode(),
                                true,
                                payload.getCreatedAt()
                        )
                );
                runOnUiThread(() -> {
                    viewModel.selectRoute(routeDefinition);
                    GoUtils.DisplayToast(
                            this,
                            getString(payload.isPrivacyMode() ? R.string.route_privacy_simulation_loaded : R.string.route_shared_downloaded)
                    );
                });
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_shared_download_failed, exception)));
            }
        });
    }

    private void capturePendingNfcPayload(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String url = intent.getStringExtra(EXTRA_PENDING_NFC_URL);
        String packageName = intent.getStringExtra(EXTRA_PENDING_NFC_PACKAGE);
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(packageName)) {
            return;
        }
        String source = intent.getStringExtra(EXTRA_PENDING_NFC_SOURCE);
        pendingSimulationNfcPayload = new NfcPayload(
                url,
                packageName,
                TextUtils.isEmpty(source) ? "manual" : source
        );
        GoUtils.DisplayToast(this, getString(R.string.route_nfc_pending_dispatch));
        intent.removeExtra(EXTRA_PENDING_NFC_URL);
        intent.removeExtra(EXTRA_PENDING_NFC_PACKAGE);
        intent.removeExtra(EXTRA_PENDING_NFC_SOURCE);
    }

    private void simulateCampusRunEntry() {
        String url = prefsStore.getNfcUrl();
        String packageName = prefsStore.getNfcPackageName();
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(packageName)) {
            showNfcConfigChoiceDialog();
            return;
        }
        applyNfcConfigAndQueue(new SharedNfcEntry("", "", url, packageName, prefsStore.getNfcSource(), System.currentTimeMillis()));
    }

    private void showNfcConfigChoiceDialog() {
        String[] options = {
                getString(R.string.route_nfc_download_option),
                getString(R.string.route_nfc_manual_option)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_nfc_setup_title)
                .setMessage(R.string.route_nfc_setup_message)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        loadSharedNfcEntriesForSimulation();
                    } else {
                        showManualNfcInputDialog();
                    }
                })
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .show();
    }

    private void loadSharedNfcEntriesForSimulation() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.nfc_download_loading));
        ioExecutor.execute(() -> {
            try {
                List<SharedNfcEntry> entries = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedNfcEntries();
                runOnUiThread(() -> showSharedNfcPickerForSimulation(entries));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(this, buildDetailedToast(R.string.nfc_download_failed, exception)));
            }
        });
    }

    private void showSharedNfcPickerForSimulation(List<SharedNfcEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            GoUtils.DisplayToast(this, getString(R.string.nfc_download_empty));
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_picker, null);
        EditText searchInput = dialogView.findViewById(R.id.searchable_picker_input);
        ListView listView = dialogView.findViewById(R.id.searchable_picker_list);
        LinearLayout letterRail = dialogView.findViewById(R.id.searchable_picker_letter_rail);
        List<SearchableNfcItem> allItems = buildNfcItems(entries);
        List<SearchableNfcItem> filteredItems = new ArrayList<>(allItems);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        fillNfcAdapter(adapter, filteredItems);
        listView.setAdapter(adapter);
        updatePickerLetterRail(letterRail, listView, filteredItems);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.nfc_download_pick_title)
                .setView(dialogView)
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> resizeDialogWindow(dialog, 0.82f, 0.70f));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (filteredItems.isEmpty()) {
                return;
            }
            dialog.dismiss();
            applyNfcConfigAndQueue(filteredItems.get(position).entry);
        });
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterNfcItems(editable == null ? "" : editable.toString(), allItems, filteredItems, adapter);
                updatePickerLetterRail(letterRail, listView, filteredItems);
            }
        });
        dialog.show();
    }

    private void showManualNfcInputDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nfc_manual_input, null);
        EditText urlEdit = dialogView.findViewById(R.id.nfc_manual_url_input);
        EditText packageEdit = dialogView.findViewById(R.id.nfc_manual_package_input);
        urlEdit.setText(prefsStore.getNfcUrl());
        packageEdit.setText(prefsStore.getNfcPackageName());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_nfc_manual_title)
                .setView(dialogView)
                .setPositiveButton(R.string.route_nfc_manual_confirm, null)
                .setNegativeButton(R.string.nfc_share_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String url = urlEdit.getText() == null ? "" : urlEdit.getText().toString().trim();
            String packageName = packageEdit.getText() == null ? "" : packageEdit.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                GoUtils.DisplayToast(this, getString(R.string.nfc_mock_need_url));
                return;
            }
            if (TextUtils.isEmpty(packageName)) {
                GoUtils.DisplayToast(this, getString(R.string.nfc_mock_need_package));
                return;
            }
            dialog.dismiss();
            applyNfcConfigAndQueue(new SharedNfcEntry("", "", url, packageName, "manual", System.currentTimeMillis()));
        }));
        dialog.show();
    }

    private void applyNfcConfigAndQueue(SharedNfcEntry entry) {
        String source = TextUtils.isEmpty(entry.getSource()) ? "manual" : entry.getSource();
        prefsStore.saveNfcConfig(entry.getUrl(), entry.getPackageName(), source);
        NfcPayload payload = new NfcPayload(entry.getUrl(), entry.getPackageName(), source);
        Boolean running = viewModel.isRunning().getValue();
        if (running != null && running) {
            dispatchNfcPayload(payload);
            return;
        }
        queuePendingSimulationNfcPayload(payload);
    }

    private void dispatchNfcPayload(NfcPayload payload) {
        NfcPayloadDispatchResult result = sendNfcPayloadUseCase.send(this, payload);
        if (result.getStatus() == NfcPayloadDispatchResult.Status.NDEF_SENT) {
            GoUtils.DisplayToast(this, getString(R.string.nfc_mock_sent));
            return;
        }
        if (result.getStatus() == NfcPayloadDispatchResult.Status.FALLBACK_VIEW_SENT) {
            GoUtils.DisplayToast(this, getString(R.string.nfc_mock_opened_browser));
            return;
        }
        String detail = result.getDetail();
        GoUtils.DisplayToast(
                this,
                getString(R.string.nfc_mock_failed) + (TextUtils.isEmpty(detail) ? "" : detail)
        );
    }

    private void queuePendingSimulationNfcPayload(@NonNull NfcPayload payload) {
        pendingSimulationNfcPayload = payload;
        GoUtils.DisplayToast(this, getString(R.string.route_nfc_pending_dispatch));
    }

    private void dispatchPendingSimulationNfcPayloadIfNeeded() {
        if (pendingSimulationNfcPayload == null) {
            return;
        }
        NfcPayload payload = pendingSimulationNfcPayload;
        pendingSimulationNfcPayload = null;
        dispatchNfcPayload(payload);
    }

    private void toggleSimulation() {
        Boolean running = viewModel.isRunning().getValue();
        if (running != null && running) {
            if (realRunLinkRunning) {
                stopLinkedSimulationState();
            }
            hideCompletionOverlay();
            hideFloatingWindow();
            viewModel.pauseSimulation();
            finishRootDiagnosticSession(true);
            renderToggleButtonState();
            GoUtils.DisplayToast(this, getString(R.string.route_simulation_paused));
            return;
        }

        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (routeDefinition == null) {
            GoUtils.DisplayToast(this, "请先选择路线");
            return;
        }
        if (!ensureSimulationReady()) {
            return;
        }

        try {
            RouteSimulationConfig config = buildSimulationConfig(null);
            boolean resumeCurrentRoute = viewModel.hasResumableSimulationForSelectedRoute();
            prefsStore.setRouteCompletionPending(false);
            completionNoticePending = false;
            hideCompletionOverlay();
            ensureFloatingWindowPermissionIfNeeded();
            RoutePoint seedPoint = resolveSimulationSeedPoint(routeDefinition, resumeCurrentRoute);
            syncRootDiagnosticLocation(
                    seedPoint.getWgsLongitude(),
                    seedPoint.getWgsLatitude(),
                    seedPoint.getAltitude(),
                    0f,
                    0f
            );
            ensureServiceStarted(routeDefinition, seedPoint);
            viewModel.startSimulation(config, new ServiceGateway());
            dispatchPendingSimulationNfcPayloadIfNeeded();
            maybeStartRootDiagnosticForSimulation();
            renderToggleButtonState();
            GoUtils.DisplayToast(this, getString(
                    resumeCurrentRoute ? R.string.route_simulation_resumed : R.string.route_simulation_started
            ));
        } catch (IllegalArgumentException exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_simulation_start_failed, exception));
        }
    }

    private void pauseSimulationFromFloatingWindow() {
        if (!Boolean.TRUE.equals(viewModel.isRunning().getValue())) {
            updateFloatingWindowControlsState();
            return;
        }
        if (realRunLinkRunning) {
            stopLinkedSimulationState();
        }
        hideCompletionOverlay();
        viewModel.pauseSimulation();
        finishRootDiagnosticSession(true);
        renderToggleButtonState();
        updateFloatingWindowControlsState();
        GoUtils.DisplayToast(this, getString(R.string.route_simulation_paused));
    }

    private void resumeSimulationFromFloatingWindow() {
        if (Boolean.TRUE.equals(viewModel.isRunning().getValue())) {
            updateFloatingWindowControlsState();
            return;
        }
        toggleSimulation();
        updateFloatingWindowControlsState();
    }

    private boolean ensureSimulationReady() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return false;
        }
        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.showEnableGpsDialog(this);
            return false;
        }
        if (!GoUtils.isAllowMockLocation(this)) {
            GoUtils.showEnableMockLocationDialog(this);
            return false;
        }
        return true;
    }

    private void promptMockLocationIfNeeded() {
        toggleButton.post(() -> {
            if (mockLocationPromptShown || isFinishing() || isDestroyed()) {
                return;
            }
            if (!GoUtils.isAllowMockLocation(this)) {
                mockLocationPromptShown = true;
                GoUtils.showEnableMockLocationDialog(this);
            }
        });
    }

    private void ensureServiceStarted(RouteDefinition routeDefinition, @Nullable RoutePoint seedPoint) {
        RoutePoint firstPoint = seedPoint == null ? routeDefinition.getPoints().get(0) : seedPoint;
        Intent serviceIntent = new Intent(this, ServiceGo.class);
        serviceIntent.putExtra(MainActivity.LNG_MSG_ID, firstPoint.getWgsLongitude());
        serviceIntent.putExtra(MainActivity.LAT_MSG_ID, firstPoint.getWgsLatitude());
        serviceIntent.putExtra(MainActivity.ALT_MSG_ID, firstPoint.getAltitude());
        startForegroundService(serviceIntent);
        if (!bound) {
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
            bound = true;
        }
    }

    @Nullable
    private RoutePoint resolveSimulationSeedPoint(RouteDefinition routeDefinition, boolean resumeCurrentRoute) {
        if (!resumeCurrentRoute) {
            return routeDefinition.getPoints().get(0);
        }
        com.acooldog.toolbox.route.domain.model.SimulationFrame currentFrame = viewModel.getSimulationFrame().getValue();
        if (currentFrame != null && currentFrame.getPoint() != null) {
            return currentFrame.getPoint();
        }
        return routeDefinition.getPoints().get(0);
    }

    private void setupRouteEditingInteractions() {
        if (baiduMap == null) {
            return;
        }
        baiduMap.setOnPolylineClickListener(this::handleEditableRoutePolylineClick);
        baiduMap.setOnMarkerClickListener(this::handleRouteMarkerClick);
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Route edit taps are handled from raw touch coordinates so taps on the polyline
                // still map back to the nearest route segment even when the overlay swallows clicks.
            }

            @Override
            public void onMapPoiClick(com.baidu.mapapi.map.MapPoi mapPoi) {
                // No-op.
            }
        });
    }

    private void handleLiveRouteEditToggle(boolean enabled) {
        if (suppressLiveRouteEditSwitchCallback) {
            return;
        }
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (enabled && routeDefinition != null && !canEditSelectedRouteLive(routeDefinition)) {
            GoUtils.DisplayToast(this, getString(R.string.route_live_edit_not_supported));
            setLiveRouteEditSwitchChecked(false);
            return;
        }
        if (enabled && routeDefinition != null) {
            originalEditableRouteDefinition = cloneRouteDefinition(routeDefinition);
        }
        if (!enabled && originalEditableRouteDefinition != null) {
            RouteDefinition routeToRestore = originalEditableRouteDefinition;
            liveRouteEditEnabled = false;
            originalEditableRouteDefinition = null;
            clearLiveRouteEditState(true);
            viewModel.replaceSelectedRoute(routeToRestore);
            updateLiveRouteEditHint();
            return;
        }
        liveRouteEditEnabled = enabled;
        clearLiveRouteEditState(true);
        if (routeDefinition != null) {
            if (enabled && canEditSelectedRouteLive(routeDefinition)) {
                liveRouteEditVerticesVisible = true;
                editableRoutePointIndices.addAll(resolveEditableRoutePointIndices(routeDefinition.getPoints()));
                drawRoute(routeDefinition, true);
            } else {
                drawRoute(routeDefinition, false);
            }
        }
        updateLiveRouteEditHint();
    }

    private void setLiveRouteEditSwitchChecked(boolean checked) {
        if (liveRouteEditSwitch == null) {
            liveRouteEditEnabled = checked;
            return;
        }
        suppressLiveRouteEditSwitchCallback = true;
        liveRouteEditSwitch.setChecked(checked);
        suppressLiveRouteEditSwitchCallback = false;
        liveRouteEditEnabled = checked;
    }

    private void applySelectedRouteEditState(@NonNull RouteDefinition routeDefinition) {
        if (!TextUtils.equals(selectedRouteIdForEditState, routeDefinition.getId())) {
            selectedRouteIdForEditState = routeDefinition.getId();
            clearLiveRouteEditState(true);
            if (liveRouteEditEnabled) {
                originalEditableRouteDefinition = cloneRouteDefinition(routeDefinition);
            }
        }
        if (liveRouteEditEnabled && !canEditSelectedRouteLive(routeDefinition)) {
            clearLiveRouteEditState(true);
        } else if (liveRouteEditEnabled && !liveRouteEditVerticesVisible) {
            liveRouteEditVerticesVisible = true;
            selectedEditableRoutePointIndex = -1;
            editableRoutePointIndices.clear();
            editableRoutePointIndices.addAll(resolveEditableRoutePointIndices(routeDefinition.getPoints()));
        }
    }

    private void clearLiveRouteEditState(boolean keepToggle) {
        liveRouteEditVerticesVisible = false;
        selectedEditableRoutePointIndex = -1;
        suppressRouteMarkerSelectionIndex = -1;
        routeEditTapCandidate = false;
        suppressNextRouteEditTap = false;
        editableRoutePointIndices.clear();
        if (!keepToggle) {
            selectedRouteIdForEditState = null;
            originalEditableRouteDefinition = null;
            setLiveRouteEditSwitchChecked(false);
        }
    }

    private boolean canEditSelectedRouteLive(@Nullable RouteDefinition routeDefinition) {
        return routeDefinition != null
                && !routeDefinition.shouldMaskMapForSimulation();
    }

    private boolean shouldHandleRouteEditActivityTouch(@Nullable MotionEvent event) {
        if (event == null || !liveRouteEditEnabled || !liveRouteEditVerticesVisible) {
            return false;
        }
        if (completionOverlay != null && completionOverlay.getVisibility() == View.VISIBLE) {
            return false;
        }
        if (privacyMaskPanel != null && privacyMaskPanel.getVisibility() == View.VISIBLE) {
            return false;
        }
        return isTouchInsideView(mapView, event) && !isTouchInsideView(bottomControlLayout, event);
    }

    private boolean isTouchInsideView(@Nullable View view, @NonNull MotionEvent event) {
        if (view == null || view.getVisibility() != View.VISIBLE || view.getWidth() <= 0 || view.getHeight() <= 0) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        return rawX >= location[0]
                && rawX <= location[0] + view.getWidth()
                && rawY >= location[1]
                && rawY <= location[1] + view.getHeight();
    }

    private void handleRouteEditMapTouch(@Nullable MotionEvent event) {
        if (event == null || !liveRouteEditEnabled || !liveRouteEditVerticesVisible) {
            routeEditTapCandidate = false;
            return;
        }
        Point mapPoint = toMapLocalPoint(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                suppressRouteMarkerSelectionIndex = -1;
                routeEditTapCandidate = true;
                routeEditTapDownX = mapPoint.x;
                routeEditTapDownY = mapPoint.y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (routeEditTapCandidate) {
                    float deltaX = mapPoint.x - routeEditTapDownX;
                    float deltaY = mapPoint.y - routeEditTapDownY;
                    if ((deltaX * deltaX) + (deltaY * deltaY) > (routeEditTapSlopPx * routeEditTapSlopPx)) {
                        routeEditTapCandidate = false;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                routeEditTapCandidate = false;
                break;
            case MotionEvent.ACTION_UP:
                if (!routeEditTapCandidate) {
                    return;
                }
                routeEditTapCandidate = false;
                Point screenPoint = new Point(mapPoint.x, mapPoint.y);
                mainHandler.postDelayed(() -> handleRouteEditTapFromScreen(screenPoint), 24L);
                break;
            default:
                break;
        }
    }

    @NonNull
    private Point toMapLocalPoint(@NonNull MotionEvent event) {
        if (mapView == null) {
            return new Point(Math.round(event.getX()), Math.round(event.getY()));
        }
        int[] location = new int[2];
        mapView.getLocationOnScreen(location);
        return new Point(
                Math.round(event.getRawX() - location[0]),
                Math.round(event.getRawY() - location[1])
        );
    }

    private void handleRouteEditTapFromScreen(@NonNull Point screenPoint) {
        if (!liveRouteEditEnabled || !liveRouteEditVerticesVisible || baiduMap == null) {
            suppressNextRouteEditTap = false;
            return;
        }
        if (suppressNextRouteEditTap) {
            suppressNextRouteEditTap = false;
            return;
        }
        LatLng latLng = baiduMap.getProjection().fromScreenLocation(screenPoint);
        if (latLng == null) {
            return;
        }
        if (selectedEditableRoutePointIndex >= 0) {
            moveSelectedEditableVertexTo(latLng);
        } else {
            insertEditableVertexNearClick(latLng, screenPoint);
        }
    }

    private boolean handleEditableRoutePolylineClick(Polyline polyline) {
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (!liveRouteEditEnabled || !canEditSelectedRouteLive(routeDefinition) || routeDefinition == null) {
            return false;
        }
        if (liveRouteEditVerticesVisible) {
            return false;
        }
        suppressNextRouteEditTap = true;
        liveRouteEditVerticesVisible = true;
        selectedEditableRoutePointIndex = -1;
        editableRoutePointIndices.clear();
        editableRoutePointIndices.addAll(resolveEditableRoutePointIndices(routeDefinition.getPoints()));
        drawRoute(routeDefinition, false);
        focusRouteOnMap(buildRouteLatLngs(routeDefinition));
        updateLiveRouteEditHint();
        return true;
    }

    private boolean handleRouteMarkerClick(Marker marker) {
        if (marker == null || marker.getExtraInfo() == null) {
            return false;
        }
        String markerType = marker.getExtraInfo().getString(ROUTE_EDIT_MARKER_TYPE_KEY, "");
        if (!ROUTE_EDIT_MARKER_TYPE_VERTEX.equals(markerType) || !liveRouteEditEnabled || !liveRouteEditVerticesVisible) {
            return false;
        }
        suppressNextRouteEditTap = true;
        int tappedIndex = marker.getExtraInfo().getInt(ROUTE_EDIT_MARKER_INDEX_KEY, -1);
        if (tappedIndex == suppressRouteMarkerSelectionIndex) {
            suppressRouteMarkerSelectionIndex = -1;
            selectedEditableRoutePointIndex = -1;
            updateLiveRouteEditHint();
            return true;
        }
        selectedEditableRoutePointIndex = tappedIndex == selectedEditableRoutePointIndex ? -1 : tappedIndex;
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (routeDefinition != null) {
            drawRoute(routeDefinition, false);
        }
        updateLiveRouteEditHint();
        return true;
    }

    private void insertEditableVertexNearClick(@NonNull LatLng latLng, @Nullable Point screenPoint) {
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (!canEditSelectedRouteLive(routeDefinition) || routeDefinition == null) {
            return;
        }
        List<RoutePoint> currentPoints = routeDefinition.getPoints();
        if (currentPoints.size() < 2) {
            return;
        }
        List<RoutePoint> updatedPoints;
        int insertIndex;
        ScreenRouteProjection screenProjection = screenPoint == null
                ? null
                : projectScreenPointOntoRoute(screenPoint, currentPoints);
        if (screenProjection != null && screenProjection.distanceToRoutePixels <= routeEditRouteHitSlopPx) {
            insertIndex = screenProjection.segmentStartIndex + 1;
            updatedPoints = new ArrayList<>(currentPoints);
            updatedPoints.add(insertIndex, screenProjection.projectedPoint);
        } else {
            RouteEditUtils.ProjectionResult projectionResult = RouteEditUtils.projectPointOntoRoute(
                    currentPoints,
                    buildMovedRoutePoint(currentPoints.get(0), latLng)
            );
            if (!projectionResult.isValid()
                    || projectionResult.getDistanceToRouteMeters() > ROUTE_INSERT_MAX_DISTANCE_METERS) {
                return;
            }
            updatedPoints = RouteEditUtils.insertPointOnRoute(currentPoints, projectionResult);
            insertIndex = projectionResult.getSegmentStartIndex() + 1;
        }
        if (editableRoutePointIndices.isEmpty()) {
            editableRoutePointIndices.addAll(resolveEditableRoutePointIndices(currentPoints));
        }
        shiftEditableRoutePointIndicesForInsert(insertIndex, updatedPoints.size());
        selectedEditableRoutePointIndex = -1;
        suppressRouteMarkerSelectionIndex = insertIndex;
        RouteDefinition updatedRoute = new RouteDefinition(
                routeDefinition.getId(),
                routeDefinition.getName(),
                routeDefinition.getCreatedAt(),
                System.currentTimeMillis(),
                updatedPoints,
                routeDefinition.getFile(),
                routeDefinition.getShareInfo()
        );
        viewModel.replaceSelectedRoute(updatedRoute);
        updateLiveRouteEditHint();
    }

    private void shiftEditableRoutePointIndicesForInsert(int insertIndex, int updatedPointCount) {
        if (insertIndex < 0 || insertIndex >= updatedPointCount) {
            return;
        }
        List<Integer> shiftedIndices = new ArrayList<>();
        for (int pointIndex : editableRoutePointIndices) {
            int shiftedIndex = pointIndex >= insertIndex ? pointIndex + 1 : pointIndex;
            if (shiftedIndex >= 0 && shiftedIndex < updatedPointCount && !shiftedIndices.contains(shiftedIndex)) {
                shiftedIndices.add(shiftedIndex);
            }
        }
        if (!shiftedIndices.contains(insertIndex)) {
            shiftedIndices.add(insertIndex);
        }
        shiftedIndices.sort(Comparator.naturalOrder());
        editableRoutePointIndices.clear();
        editableRoutePointIndices.addAll(shiftedIndices);
    }

    @Nullable
    private ScreenRouteProjection projectScreenPointOntoRoute(
            @NonNull Point screenPoint,
            @NonNull List<RoutePoint> routePoints
    ) {
        if (baiduMap == null || routePoints.size() < 2) {
            return null;
        }
        ScreenRouteProjection bestProjection = null;
        for (int index = 0; index < routePoints.size() - 1; index++) {
            RoutePoint startPoint = routePoints.get(index);
            RoutePoint endPoint = routePoints.get(index + 1);
            Point startScreenPoint = baiduMap.getProjection().toScreenLocation(
                    new LatLng(startPoint.getBdLatitude(), startPoint.getBdLongitude())
            );
            Point endScreenPoint = baiduMap.getProjection().toScreenLocation(
                    new LatLng(endPoint.getBdLatitude(), endPoint.getBdLongitude())
            );
            if (startScreenPoint == null || endScreenPoint == null) {
                continue;
            }
            SegmentProjection segmentProjection = projectPointOntoScreenSegment(
                    screenPoint,
                    startScreenPoint,
                    endScreenPoint
            );
            if (bestProjection == null
                    || segmentProjection.distancePixels < bestProjection.distanceToRoutePixels) {
                bestProjection = new ScreenRouteProjection(
                        index,
                        interpolateRoutePoint(startPoint, endPoint, segmentProjection.segmentRatio),
                        segmentProjection.distancePixels
                );
            }
        }
        return bestProjection;
    }

    @NonNull
    private SegmentProjection projectPointOntoScreenSegment(
            @NonNull Point target,
            @NonNull Point start,
            @NonNull Point end
    ) {
        double segmentX = end.x - start.x;
        double segmentY = end.y - start.y;
        double segmentLengthSquared = (segmentX * segmentX) + (segmentY * segmentY);
        if (segmentLengthSquared <= 0.000001d) {
            return new SegmentProjection(0d, Math.hypot(target.x - start.x, target.y - start.y));
        }
        double ratio = ((target.x - start.x) * segmentX + (target.y - start.y) * segmentY) / segmentLengthSquared;
        double clampedRatio = Math.max(0d, Math.min(1d, ratio));
        double projectedX = start.x + (segmentX * clampedRatio);
        double projectedY = start.y + (segmentY * clampedRatio);
        return new SegmentProjection(clampedRatio, Math.hypot(target.x - projectedX, target.y - projectedY));
    }

    @NonNull
    private RoutePoint interpolateRoutePoint(
            @NonNull RoutePoint startPoint,
            @NonNull RoutePoint endPoint,
            double ratio
    ) {
        double clampedRatio = Math.max(0d, Math.min(1d, ratio));
        return new RoutePoint(
                lerp(startPoint.getBdLongitude(), endPoint.getBdLongitude(), clampedRatio),
                lerp(startPoint.getBdLatitude(), endPoint.getBdLatitude(), clampedRatio),
                lerp(startPoint.getWgsLongitude(), endPoint.getWgsLongitude(), clampedRatio),
                lerp(startPoint.getWgsLatitude(), endPoint.getWgsLatitude(), clampedRatio),
                lerp(startPoint.getAltitude(), endPoint.getAltitude(), clampedRatio)
        );
    }

    private double lerp(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }

    private void moveSelectedEditableVertexTo(@NonNull LatLng latLng) {
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (!canEditSelectedRouteLive(routeDefinition) || routeDefinition == null) {
            return;
        }
        List<RoutePoint> currentPoints = routeDefinition.getPoints();
        if (selectedEditableRoutePointIndex < 0 || selectedEditableRoutePointIndex >= currentPoints.size()) {
            return;
        }
        List<RoutePoint> updatedPoints = new ArrayList<>(currentPoints);
        updatedPoints.set(
                selectedEditableRoutePointIndex,
                buildMovedRoutePoint(currentPoints.get(selectedEditableRoutePointIndex), latLng)
        );
        RouteDefinition updatedRoute = new RouteDefinition(
                routeDefinition.getId(),
                routeDefinition.getName(),
                routeDefinition.getCreatedAt(),
                System.currentTimeMillis(),
                updatedPoints,
                routeDefinition.getFile(),
                routeDefinition.getShareInfo()
        );
        viewModel.replaceSelectedRoute(updatedRoute);
        updateLiveRouteEditHint();
    }

    private RoutePoint buildMovedRoutePoint(@NonNull RoutePoint sourcePoint, @NonNull LatLng bdLatLng) {
        double[] wgsCoordinates = MapUtils.bd2wgs(bdLatLng.longitude, bdLatLng.latitude);
        return new RoutePoint(
                bdLatLng.longitude,
                bdLatLng.latitude,
                wgsCoordinates[0],
                wgsCoordinates[1],
                sourcePoint.getAltitude()
        );
    }

    private void drawRoute(RouteDefinition routeDefinition, boolean focusRoute) {
        baiduMap.clear();
        simulationMarker = null;
        List<LatLng> latLngs = buildRouteLatLngs(routeDefinition);
        if (latLngs.size() > 1) {
            OverlayOptions polyline = new PolylineOptions()
                    .width(8)
                    .color(0xAA1565C0)
                    .clickable(
                            liveRouteEditEnabled
                                    && canEditSelectedRouteLive(routeDefinition)
                                    && !liveRouteEditVerticesVisible
                    )
                    .points(latLngs);
            baiduMap.addOverlay(polyline);
        }
        if (liveRouteEditEnabled && liveRouteEditVerticesVisible && canEditSelectedRouteLive(routeDefinition)) {
            renderEditableRouteVertices(routeDefinition);
        }
        syncSimulationMarkerFromCurrentFrame();
        if (focusRoute) {
            focusRouteOnMap(latLngs);
        }
    }

    private void renderEditableRouteVertices(@NonNull RouteDefinition routeDefinition) {
        List<RoutePoint> routePoints = routeDefinition.getPoints();
        if (editableRoutePointIndices.isEmpty()) {
            editableRoutePointIndices.addAll(resolveEditableRoutePointIndices(routePoints));
        }
        for (int pointIndex : editableRoutePointIndices) {
            if (pointIndex < 0 || pointIndex >= routePoints.size()) {
                continue;
            }
            RoutePoint routePoint = routePoints.get(pointIndex);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(routePoint.getBdLatitude(), routePoint.getBdLongitude()))
                    .icon(pointIndex == selectedEditableRoutePointIndex
                            ? getRouteEditVertexSelectedDescriptor()
                            : getRouteEditVertexDescriptor());
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putInt(ROUTE_EDIT_MARKER_INDEX_KEY, pointIndex);
            bundle.putString(ROUTE_EDIT_MARKER_TYPE_KEY, ROUTE_EDIT_MARKER_TYPE_VERTEX);
            markerOptions.extraInfo(bundle);
            baiduMap.addOverlay(markerOptions);
        }
    }

    private void syncSimulationMarkerFromCurrentFrame() {
        com.acooldog.toolbox.route.domain.model.SimulationFrame currentFrame = viewModel.getSimulationFrame().getValue();
        if (currentFrame == null || currentFrame.getPoint() == null) {
            return;
        }
        LatLng latLng = new LatLng(
                currentFrame.getPoint().getBdLatitude(),
                currentFrame.getPoint().getBdLongitude()
        );
        simulationMarker = (Marker) baiduMap.addOverlay(new MarkerOptions()
                .position(latLng)
                .icon(getSimulationProgressDescriptor()));
    }

    @NonNull
    private List<LatLng> buildRouteLatLngs(@NonNull RouteDefinition routeDefinition) {
        return buildRouteLatLngs(routeDefinition.getPoints());
    }

    @NonNull
    private List<LatLng> buildRouteLatLngs(@NonNull List<RoutePoint> routePoints) {
        List<LatLng> latLngs = new ArrayList<>();
        for (RoutePoint routePoint : routePoints) {
            latLngs.add(new LatLng(routePoint.getBdLatitude(), routePoint.getBdLongitude()));
        }
        return latLngs;
    }

    @NonNull
    private List<Integer> resolveEditableRoutePointIndices(@NonNull List<RoutePoint> routePoints) {
        List<Integer> indices = new ArrayList<>();
        int pointCount = routePoints.size();
        if (pointCount <= MAX_EDITABLE_ROUTE_VERTICES) {
            for (int index = 0; index < pointCount; index++) {
                indices.add(index);
            }
            return indices;
        }
        double step = (pointCount - 1d) / (MAX_EDITABLE_ROUTE_VERTICES - 1d);
        for (int index = 0; index < MAX_EDITABLE_ROUTE_VERTICES; index++) {
            int pointIndex = index == MAX_EDITABLE_ROUTE_VERTICES - 1
                    ? pointCount - 1
                    : (int) Math.round(index * step);
            if (!indices.contains(pointIndex)) {
                indices.add(pointIndex);
            }
        }
        return indices;
    }

    private void focusRouteOnMap(@NonNull List<LatLng> latLngs) {
        if (latLngs.isEmpty() || mapView == null || baiduMap == null) {
            return;
        }
        mapView.post(() -> {
            if (latLngs.size() == 1) {
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLngs.get(0), 18f));
                return;
            }
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latLng : latLngs) {
                builder.include(latLng);
            }
            int horizontalPadding = Math.round(dp(48f));
            int topPadding = Math.round(dp(48f));
            int bottomPadding = panelCollapsed ? Math.round(dp(120f)) : Math.round(dp(260f));
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLngBounds(
                    builder.build(),
                    horizontalPadding,
                    topPadding,
                    horizontalPadding,
                    bottomPadding
            );
            baiduMap.animateMapStatus(update);
        });
    }

    private void updateLiveRouteEditHint() {
        if (routeEditHintView == null) {
            return;
        }
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (!liveRouteEditEnabled) {
            routeEditHintView.setText(R.string.route_live_edit_hint_disabled);
            return;
        }
        if (routeDefinition == null) {
            routeEditHintView.setText(R.string.route_live_edit_hint_need_route);
            return;
        }
        if (!canEditSelectedRouteLive(routeDefinition)) {
            routeEditHintView.setText(R.string.route_live_edit_not_supported);
            return;
        }
        if (!liveRouteEditVerticesVisible) {
            routeEditHintView.setText(R.string.route_live_edit_hint_tap_route);
            return;
        }
        if (selectedEditableRoutePointIndex < 0) {
            routeEditHintView.setText(R.string.route_live_edit_hint_pick_vertex_or_add);
            return;
        }
        routeEditHintView.setText(getString(
                R.string.route_live_edit_hint_move_vertex_click,
                selectedEditableRoutePointIndex + 1
        ));
    }

    @NonNull
    private RouteDefinition cloneRouteDefinition(@NonNull RouteDefinition routeDefinition) {
        return new RouteDefinition(
                routeDefinition.getId(),
                routeDefinition.getName(),
                routeDefinition.getCreatedAt(),
                routeDefinition.getUpdatedAt(),
                new ArrayList<>(routeDefinition.getPoints()),
                routeDefinition.getFile(),
                routeDefinition.getShareInfo()
        );
    }

    private void updatePrivacyMask(@Nullable RouteDefinition routeDefinition) {
        boolean shouldMask = routeDefinition != null && routeDefinition.shouldMaskMapForSimulation();
        privacyMaskPanel.setVisibility(shouldMask ? View.VISIBLE : View.GONE);
        privacyMaskText.setText(R.string.route_privacy_overlay_message);
    }

    private String buildRouteLabel(RouteDefinition routeDefinition) {
        if (routeDefinition == null) {
            return "";
        }
        if (routeDefinition.shouldMaskMapForSimulation()) {
            return getString(R.string.route_label_shared_private, routeDefinition.getName());
        }
        if (routeDefinition.isDownloadedFromShared()) {
            return getString(R.string.route_label_shared_downloaded, routeDefinition.getName());
        }
        if (routeDefinition.isSharedRoute()) {
            return getString(R.string.route_label_shared_uploaded, routeDefinition.getName());
        }
        return routeDefinition.getName();
    }

    private String buildDetailedToast(int prefixResId, Exception exception) {
        String detail = exception == null || exception.getMessage() == null ? "" : exception.getMessage().trim();
        if (detail.isEmpty()) {
            return getString(prefixResId);
        }
        return getString(prefixResId) + " " + detail;
    }

    private void setupSimulationModeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.route_mode_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        simulationModeSpinner.setAdapter(adapter);
        simulationModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSimulationModeViews(getSelectedSimulationMode());
                if (!restoringSimulationPrefs) {
                    persistSimulationPrefs();
                    applySimulationConfigHotIfPossible();
                    renderToggleButtonState();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op.
            }
        });
    }

    private RouteSimulationConfig.Mode getSelectedSimulationMode() {
        return simulationModeSpinner.getSelectedItemPosition() == 1
                ? RouteSimulationConfig.Mode.CADENCE
                : RouteSimulationConfig.Mode.SPEED;
    }

    private void updateSimulationModeViews(RouteSimulationConfig.Mode mode) {
        boolean cadenceMode = mode == RouteSimulationConfig.Mode.CADENCE;
        speedControlLayout.setVisibility(cadenceMode ? View.GONE : View.VISIBLE);
        cadenceControlLayout.setVisibility(cadenceMode ? View.VISIBLE : View.GONE);
        realRunLinkButton.setEnabled(!cadenceMode);
        realRunLinkButton.setAlpha(cadenceMode ? 0.6f : 1.0f);
        if (cadenceMode && realRunLinkRunning) {
            stopLinkedSimulationState();
            viewModel.pauseSimulation();
            renderToggleButtonState();
        }
    }

    private void bindSimulationConfigInputs() {
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                handleSimulationConfigInputChanged();
            }
        };
        speedInput.addTextChangedListener(watcher);
        cadenceInput.addTextChangedListener(watcher);
        loopInput.addTextChangedListener(watcher);
        speedFloatCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> handleSimulationConfigInputChanged());
    }

    private void handleSimulationConfigInputChanged() {
        if (restoringSimulationPrefs) {
            return;
        }
        persistSimulationPrefs();
        applySimulationConfigHotIfPossible();
        renderToggleButtonState();
    }

    private void applySimulationConfigHotIfPossible() {
        if (viewModel == null || !viewModel.hasActiveSimulation()) {
            return;
        }
        boolean running = Boolean.TRUE.equals(viewModel.isRunning().getValue());
        if (!running && !viewModel.hasResumableSimulationForSelectedRoute()) {
            return;
        }
        try {
            viewModel.updateSimulationConfig(buildSimulationConfig(null));
            renderToggleButtonState();
        } catch (IllegalArgumentException ignored) {
            // Ignore transient invalid values while the user is typing.
        }
    }

    private void renderToggleButtonState() {
        if (toggleButton == null || viewModel == null) {
            return;
        }
        boolean running = Boolean.TRUE.equals(viewModel.isRunning().getValue());
        boolean resumable = viewModel.hasResumableSimulationForSelectedRoute();
        if (running) {
            toggleButton.setText(R.string.route_stop_button);
        } else if (resumable) {
            toggleButton.setText(R.string.route_resume_button);
        } else {
            toggleButton.setText(R.string.route_start_button);
        }
    }

    private void restoreSimulationPrefs() {
        restoringSimulationPrefs = true;
        String routeMode = prefsStore.getRouteMode();
        simulationModeSpinner.setSelection(
                SimulationPrefsStore.ROUTE_MODE_CADENCE.equals(routeMode) ? 1 : 0,
                false
        );
        speedInput.setText(prefsStore.getRouteSpeed());
        cadenceInput.setText(prefsStore.getRouteCadence());
        loopInput.setText(prefsStore.getRouteLoopCount());
        speedFloatCheckBox.setChecked(prefsStore.isRouteSpeedFloat());
        updateSimulationModeViews(getSelectedSimulationMode());
        restoringSimulationPrefs = false;
    }

    private void persistSimulationPrefs() {
        if (prefsStore == null || simulationModeSpinner == null) {
            return;
        }
        RouteDefinition selectedRoute = viewModel.getSelectedRoute().getValue();
        prefsStore.saveRouteConfig(
                getSelectedSimulationMode() == RouteSimulationConfig.Mode.CADENCE
                        ? SimulationPrefsStore.ROUTE_MODE_CADENCE
                        : SimulationPrefsStore.ROUTE_MODE_SPEED,
                speedInput.getText() == null ? "" : speedInput.getText().toString(),
                cadenceInput.getText() == null ? "" : cadenceInput.getText().toString(),
                loopInput.getText() == null ? "" : loopInput.getText().toString(),
                speedFloatCheckBox.isChecked(),
                selectedRoute == null ? "" : selectedRoute.getId(),
                prefsStore.getRouteLinkRatioNumerator()
        );
    }

    private double parseSimulationValue(EditText input) {
        if (input == null || input.getText() == null) {
            throw new IllegalArgumentException("simulation value is required");
        }
        String raw = input.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            throw new IllegalArgumentException("simulation value is required");
        }
        double value = Double.parseDouble(raw);
        if (value <= 0d) {
            throw new IllegalArgumentException("simulation value must be positive");
        }
        return value;
    }

    private int parseLoopCount(EditText input) {
        if (input == null || input.getText() == null) {
            throw new IllegalArgumentException("loop count is required");
        }
        String raw = input.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            throw new IllegalArgumentException("loop count is required");
        }
        int value = Integer.parseInt(raw);
        if (value <= 0) {
            throw new IllegalArgumentException("loop count must be positive");
        }
        return value;
    }

    private RouteSimulationConfig buildSimulationConfig(@Nullable RouteSimulationConfig.Mode modeOverride) {
        try {
            RouteSimulationConfig.Mode simulationMode = modeOverride == null ? getSelectedSimulationMode() : modeOverride;
            double speed = simulationMode == RouteSimulationConfig.Mode.SPEED
                    ? parseSimulationValue(speedInput)
                    : 0d;
            double cadence = simulationMode == RouteSimulationConfig.Mode.CADENCE
                    ? parseSimulationValue(cadenceInput)
                    : 0d;
            int loopCount = parseLoopCount(loopInput);
            boolean dynamicIntensityEnabled = speedFloatCheckBox.isChecked();
            double intensityVariationRange = dynamicIntensityEnabled
                    ? parsePositiveDouble(
                            prefsStore.getRouteIntensityVariationRange(),
                            getString(R.string.route_dynamic_intensity_range_invalid)
                    )
                    : 0d;
            double intensityVariationFrequency = dynamicIntensityEnabled
                    ? prefsStore.getRouteIntensityVariationFrequency()
                    : 0d;
            boolean naturalPathVariationEnabled = prefsStore.isRouteNaturalPathVariationEnabled();
            double pathVariationAmplitude = naturalPathVariationEnabled
                    ? parsePositiveDouble(
                            prefsStore.getRoutePathVariationAmplitude(),
                            getString(R.string.route_path_variation_invalid)
                    )
                    : 0d;
            boolean naturalAltitudeVariationEnabled = prefsStore.isRouteNaturalAltitudeVariationEnabled();
            double altitudeBaseMeters = naturalAltitudeVariationEnabled
                    ? parseNonNegativeDouble(
                            prefsStore.getRouteAltitudeBaseMeters(),
                            getString(R.string.route_altitude_base_invalid)
                    )
                    : RouteSimulationConfig.DEFAULT_ALTITUDE_BASE_METERS;
            double altitudeVariationRange = naturalAltitudeVariationEnabled
                    ? parseNonNegativeDouble(
                            prefsStore.getRouteAltitudeVariationRange(),
                            getString(R.string.route_altitude_variation_range_invalid)
                    )
                    : 0d;
            double altitudeVariationHeightCentimeters = naturalAltitudeVariationEnabled
                    ? parseNonNegativeDouble(
                            prefsStore.getRouteAltitudeVariationHeightCm(),
                            getString(R.string.route_altitude_variation_height_invalid)
                    )
                    : 0d;
            double altitudeVariationProbability = naturalAltitudeVariationEnabled
                    ? prefsStore.getRouteAltitudeVariationProbability()
                    : 0d;
            double linkRatioNumerator = parsePositiveDouble(
                    prefsStore.getRouteLinkRatioNumerator(),
                    getString(R.string.route_link_invalid_ratio)
            );
            double stepsPerMeter = parsePositiveDouble(
                    prefsStore.getRouteStepsPerMeter(),
                    getString(R.string.route_link_steps_per_meter_invalid)
            );
            return new RouteSimulationConfig(
                    simulationMode,
                    speed,
                    cadence,
                    loopCount,
                    dynamicIntensityEnabled,
                    intensityVariationRange,
                    intensityVariationFrequency,
                    naturalPathVariationEnabled,
                    pathVariationAmplitude,
                    naturalAltitudeVariationEnabled,
                    altitudeBaseMeters,
                    altitudeVariationRange,
                    altitudeVariationHeightCentimeters,
                    altitudeVariationProbability,
                    linkRatioNumerator,
                    stepsPerMeter,
                    1000L
            );
        } catch (IllegalArgumentException exception) {
            if (isDetailedSimulationConfigError(exception.getMessage())) {
                throw exception;
            }
            throw new IllegalArgumentException(getString(R.string.route_simulation_invalid));
        }
    }

    private boolean isDetailedSimulationConfigError(@Nullable String message) {
        return TextUtils.equals(message, getString(R.string.route_dynamic_intensity_range_invalid))
                || TextUtils.equals(message, getString(R.string.route_path_variation_invalid))
                || TextUtils.equals(message, getString(R.string.route_altitude_base_invalid))
                || TextUtils.equals(message, getString(R.string.route_altitude_variation_range_invalid))
                || TextUtils.equals(message, getString(R.string.route_altitude_variation_height_invalid));
    }

    private void toggleRealRunLink() {
        if (realRunLinkRunning) {
            stopLinkedSimulationState();
            viewModel.pauseSimulation();
            renderToggleButtonState();
            GoUtils.DisplayToast(this, getString(R.string.route_link_stopped));
            return;
        }
        Boolean running = viewModel.isRunning().getValue();
        if (running != null && running) {
            GoUtils.DisplayToast(this, "请先停止当前模拟");
            return;
        }
        if (getSelectedSimulationMode() != RouteSimulationConfig.Mode.SPEED) {
            GoUtils.DisplayToast(this, getString(R.string.route_link_mode_speed_only));
            return;
        }
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (routeDefinition == null) {
            GoUtils.DisplayToast(this, "请先选择路线");
            return;
        }
        if (!ensureSimulationReady()) {
            return;
        }
        try {
            RouteSimulationConfig config = buildSimulationConfig(RouteSimulationConfig.Mode.SPEED);
            double ratioNumerator = config.getLinkRatioNumerator();
            boolean resumeCurrentRoute = viewModel.hasResumableSimulationForSelectedRoute();
            RoutePoint seedPoint = resolveSimulationSeedPoint(routeDefinition, resumeCurrentRoute);
            ensureServiceStarted(routeDefinition, seedPoint);
            viewModel.startLinkedSimulation(config, new ServiceGateway());
            showSimulationMarkerAt(seedPoint);
            pendingLinkedDistanceMeters = 0d;
            initialStepCounterValue = -1f;
            lastStepCounterValue = -1f;
            realRunLinkRunning = true;
            realRunLinkButton.setText(R.string.route_link_stop_button);
            requestActivityRecognitionAndStart(ratioNumerator);
        } catch (IllegalArgumentException exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        } catch (Exception exception) {
            stopLinkedSimulationState();
            GoUtils.DisplayToast(this, buildDetailedToast(R.string.route_simulation_start_failed, exception));
        }
    }

    private void requestActivityRecognitionAndStart(double ratioNumerator) {
        pendingLinkedDistanceMeters = resolveLinkedActualDistanceMeters(ratioNumerator);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            beginRealRunLink();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            beginRealRunLink();
            return;
        }
        pendingRealRunLinkStart = true;
        activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
    }

    private void beginRealRunLink() {
        if (!registerRealRunSensorListener()) {
            stopLinkedSimulationState();
            viewModel.stopSimulation();
            GoUtils.DisplayToast(this, getString(R.string.route_link_sensor_unavailable));
            return;
        }
        dispatchPendingSimulationNfcPayloadIfNeeded();
        GoUtils.DisplayToast(this, getString(R.string.route_link_started));
    }

    private boolean registerRealRunSensorListener() {
        if (sensorManager == null) {
            return false;
        }
        stopRealRunSensorListener();
        if (stepDetectorSensor != null) {
            return sensorManager.registerListener(stepSensorListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (stepCounterSensor != null) {
            return sensorManager.registerListener(stepSensorListener, stepCounterSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        return false;
    }

    private void stopRealRunSensorListener() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(stepSensorListener);
        }
    }

    private void stopLinkedSimulationState() {
        pendingRealRunLinkStart = false;
        realRunLinkRunning = false;
        initialStepCounterValue = -1f;
        lastStepCounterValue = -1f;
        pendingLinkedDistanceMeters = 0d;
        stopRealRunSensorListener();
        realRunLinkButton.setText(R.string.route_link_start_button);
    }

    private void handleStepCounterEvent(SensorEvent event) {
        if (event.values == null || event.values.length == 0) {
            return;
        }
        float currentValue = event.values[0];
        if (initialStepCounterValue < 0f) {
            initialStepCounterValue = currentValue;
            lastStepCounterValue = currentValue;
            return;
        }
        if (lastStepCounterValue < 0f) {
            lastStepCounterValue = currentValue;
            return;
        }
        int steps = Math.max(0, Math.round(currentValue - lastStepCounterValue));
        lastStepCounterValue = currentValue;
        if (steps > 0) {
            consumeDetectedSteps(steps);
        }
    }

    private void consumeDetectedSteps(int steps) {
        if (!realRunLinkRunning || steps <= 0) {
            return;
        }
        double stepsPerMeter = parsePositiveDouble(
                prefsStore.getRouteStepsPerMeter(),
                getString(R.string.route_link_steps_per_meter_invalid)
        );
        pendingLinkedDistanceMeters += steps / stepsPerMeter;
        XLog.d("RouteRunActivity: linked sensor steps=" + steps
                + ", stepsPerMeter=" + stepsPerMeter
                + ", accumulatedDistance=" + pendingLinkedDistanceMeters);
        while (pendingLinkedDistanceMeters >= 0d && realRunLinkRunning) {
            double thresholdMeters = resolveLinkedActualDistanceMeters(parsePositiveDouble(
                    prefsStore.getRouteLinkRatioNumerator(),
                    getString(R.string.route_link_invalid_ratio)
            ));
            if (pendingLinkedDistanceMeters < thresholdMeters) {
                break;
            }
            pendingLinkedDistanceMeters -= thresholdMeters;
            advanceLinkedSimulationOnUiThread(thresholdMeters);
            break;
        }
    }

    private void advanceLinkedSimulationOnUiThread(double thresholdMeters) {
        runOnUiThread(() -> {
            if (!realRunLinkRunning) {
                return;
            }
            com.acooldog.toolbox.route.domain.model.SimulationFrame frame = viewModel.advanceSimulationOnceAndGetFrame();
            if (frame != null) {
                showSimulationMarkerAt(frame.getPoint());
                XLog.d("RouteRunActivity: linked advance triggered, thresholdMeters=" + thresholdMeters
                        + ", remainingDistance=" + pendingLinkedDistanceMeters
                        + ", point=" + frame.getPoint().getBdLatitude() + "," + frame.getPoint().getBdLongitude());
            }
            if (frame == null || frame.isFinished()) {
                stopLinkedSimulationState();
            }
        });
    }

    private double resolveLinkedActualDistanceMeters(double ratioNumerator) {
        double speedValue = parseSimulationValue(speedInput);
        double denominator = parseSimulationValue(speedInput);
        return ratioNumerator * (speedValue / denominator);
    }

    private String resolveReminderToneTitle() {
        String savedTitle = prefsStore.getRouteReminderToneTitle();
        return TextUtils.isEmpty(savedTitle) ? getString(R.string.route_link_ringtone_default) : savedTitle;
    }

    private void showSimulationSettingsDialog() {
        if (simulationSettingsOverlay != null) {
            showSimulationSettingsHomePage();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_route_simulation_settings, null);
        settingsLinkRatioLeftInput = dialogView.findViewById(R.id.et_dialog_link_ratio_left);
        settingsLinkRatioRightInput = dialogView.findViewById(R.id.et_dialog_link_ratio_right);
        settingsStepsPerMeterInput = dialogView.findViewById(R.id.et_dialog_steps_per_meter);
        settingsLoopInput = dialogView.findViewById(R.id.et_dialog_loop_count);
        settingsSatelliteSeekBar = dialogView.findViewById(R.id.seek_dialog_satellite);
        settingsSatelliteValueView = dialogView.findViewById(R.id.tv_dialog_satellite_value);
        settingsSignalQualityGroup = dialogView.findViewById(R.id.rg_dialog_signal_quality);
        settingsHdopSeekBar = dialogView.findViewById(R.id.seek_dialog_hdop);
        settingsHdopValueView = dialogView.findViewById(R.id.tv_dialog_hdop_value);
        settingsUpdateIntervalSeekBar = dialogView.findViewById(R.id.seek_dialog_update_interval);
        settingsUpdateIntervalValueView = dialogView.findViewById(R.id.tv_dialog_update_interval_value);
        settingsNetworkSimulationSwitch = dialogView.findViewById(R.id.switch_dialog_network_simulation);
        settingsDynamicIntensitySwitch = dialogView.findViewById(R.id.switch_dialog_dynamic_intensity);
        settingsIntensityRangeInput = dialogView.findViewById(R.id.et_dialog_intensity_range);
        settingsIntensityFrequencySeekBar = dialogView.findViewById(R.id.seek_dialog_intensity_frequency);
        settingsIntensityFrequencyValueView = dialogView.findViewById(R.id.tv_dialog_intensity_frequency_value);
        settingsPathVariationSwitch = dialogView.findViewById(R.id.switch_dialog_path_variation);
        settingsPathVariationInput = dialogView.findViewById(R.id.et_dialog_path_variation);
        settingsAltitudeVariationSwitch = dialogView.findViewById(R.id.switch_dialog_altitude_variation);
        settingsAltitudeBaseInput = dialogView.findViewById(R.id.et_dialog_altitude_base);
        settingsAltitudeVariationRangeInput = dialogView.findViewById(R.id.et_dialog_altitude_variation_range);
        settingsAltitudeHeightInput = dialogView.findViewById(R.id.et_dialog_altitude_height);
        settingsAltitudeProbabilitySeekBar = dialogView.findViewById(R.id.seek_dialog_altitude_probability);
        settingsAltitudeProbabilityValueView = dialogView.findViewById(R.id.tv_dialog_altitude_probability_value);
        settingsFloatingWindowSwitch = dialogView.findViewById(R.id.switch_dialog_floating_window);
        settingsFloatingWindowScaleSeekBar = dialogView.findViewById(R.id.seek_dialog_floating_window_scale);
        settingsFloatingWindowButtonSizeSeekBar = dialogView.findViewById(R.id.seek_dialog_floating_window_button_size);
        settingsFloatingWindowScaleView = dialogView.findViewById(R.id.tv_dialog_floating_window_scale);
        settingsFloatingWindowButtonSizeView = dialogView.findViewById(R.id.tv_dialog_floating_window_button_size);
        settingsFloatingWindowPreview = dialogView.findViewById(R.id.view_dialog_floating_window_preview);
        settingsReminderToneView = dialogView.findViewById(R.id.tv_dialog_ringtone);
        settingsNonRootTabView = dialogView.findViewById(R.id.tv_dialog_settings_tab_non_root);
        settingsRootTabView = dialogView.findViewById(R.id.tv_dialog_settings_tab_root);
        settingsRootContainer = dialogView.findViewById(R.id.layout_dialog_settings_root_container);
        settingsRootEnvironmentSection = dialogView.findViewById(R.id.section_settings_root_environment);
        settingsRootRestrictedSection = dialogView.findViewById(R.id.section_settings_root_restricted);
        settingsRootAlgorithmLabSection = dialogView.findViewById(R.id.section_settings_algorithm_lab);
        settingsRootLogsSection = dialogView.findViewById(R.id.section_settings_root_logs);
        settingsRootInternalStatusView = dialogView.findViewById(R.id.tv_dialog_root_internal_status);
        settingsRootModeStatusView = dialogView.findViewById(R.id.tv_dialog_root_mode_status);
        settingsRootStatusView = dialogView.findViewById(R.id.tv_dialog_root_status);
        settingsRootAuthorizationStatusView = dialogView.findViewById(R.id.tv_dialog_root_authorization_status);
        settingsRootHiddenStatusView = dialogView.findViewById(R.id.tv_dialog_root_hidden_status);
        settingsRootDeveloperStatusView = dialogView.findViewById(R.id.tv_dialog_root_developer_status);
        settingsRootMockStatusView = dialogView.findViewById(R.id.tv_dialog_root_mock_status);
        settingsRootHookStatusView = dialogView.findViewById(R.id.tv_dialog_root_hook_status);
        settingsRootTargetStatusView = dialogView.findViewById(R.id.tv_dialog_root_target_status);
        settingsRootCompatibilityStatusView = dialogView.findViewById(R.id.tv_dialog_root_compatibility_status);
        settingsRootFeatureConfigStatusView = dialogView.findViewById(R.id.tv_dialog_root_feature_config_status);
        settingsRootDiagnosticLogView = dialogView.findViewById(R.id.tv_dialog_root_diagnostic_log);
        settingsRootAuditLogView = dialogView.findViewById(R.id.tv_dialog_root_audit_log);
        settingsRootRefreshButton = dialogView.findViewById(R.id.btn_dialog_root_refresh);
        settingsRootConfirmSessionButton = dialogView.findViewById(R.id.btn_dialog_root_confirm_session);
        settingsRootRequestSuButton = dialogView.findViewById(R.id.btn_dialog_root_request_su);
        settingsRootPickTargetButton = dialogView.findViewById(R.id.btn_dialog_root_pick_target);
        settingsRootNmeaSettingsButton = dialogView.findViewById(R.id.btn_dialog_root_nmea_settings);
        settingsRootSignalSettingsButton = dialogView.findViewById(R.id.btn_dialog_root_signal_settings);
        settingsRootBypassSettingsButton = dialogView.findViewById(R.id.btn_dialog_root_bypass_settings);
        settingsRootHookSettingsButton = dialogView.findViewById(R.id.btn_dialog_root_hook_settings);
        settingsRootServiceSettingsButton = dialogView.findViewById(R.id.btn_dialog_root_service_settings);
        settingsRootSensorSettingsButton = dialogView.findViewById(R.id.btn_dialog_root_sensor_settings);
        settingsRootReloadConfigButton = dialogView.findViewById(R.id.btn_dialog_root_reload_config);
        settingsRootGenerateGmButton = dialogView.findViewById(R.id.btn_dialog_root_generate_gm);
        settingsAlgorithmLabButton = dialogView.findViewById(R.id.btn_dialog_algorithm_lab);
        settingsRootModeSwitch = dialogView.findViewById(R.id.switch_dialog_root_mode);
        settingsRootEnvironmentSwitch = dialogView.findViewById(R.id.switch_dialog_root_environment);
        settingsRootSuProbeSwitch = dialogView.findViewById(R.id.switch_dialog_root_su_probe);
        settingsRootEncryptedAuditSwitch = dialogView.findViewById(R.id.switch_dialog_root_encrypted_audit);
        settingsRootGmInterfaceSwitch = dialogView.findViewById(R.id.switch_dialog_root_gm_interface);
        settingsRootFridaInjectionSwitch = dialogView.findViewById(R.id.switch_dialog_root_frida_injection);
        settingsRootNmeaSwitch = dialogView.findViewById(R.id.switch_dialog_root_nmea);
        settingsRootSignalSwitch = dialogView.findViewById(R.id.switch_dialog_root_signal);
        settingsRootBypassSwitch = dialogView.findViewById(R.id.switch_dialog_root_bypass);
        settingsRootHookSwitch = dialogView.findViewById(R.id.switch_dialog_root_hook);
        settingsRootServiceLogSwitch = dialogView.findViewById(R.id.switch_dialog_root_service_log);
        settingsRootSensorSwitch = dialogView.findViewById(R.id.switch_dialog_root_sensor);
        installDebugTestStudioButtons(dialogView);
        Button pickRingtoneButton = dialogView.findViewById(R.id.btn_dialog_pick_ringtone);
        Button uploadSettingsButton = dialogView.findViewById(R.id.btn_dialog_upload_settings);
        Button downloadSettingsButton = dialogView.findViewById(R.id.btn_dialog_download_settings);
        Button rootSwitchNonRootButton = dialogView.findViewById(R.id.btn_dialog_root_switch_non_root);
        View dynamicIntensityContent = dialogView.findViewById(R.id.layout_dialog_dynamic_intensity_content);
        View pathVariationContent = dialogView.findViewById(R.id.layout_dialog_path_variation_content);
        View altitudeVariationContent = dialogView.findViewById(R.id.layout_dialog_altitude_variation_content);
        View floatingWindowContent = dialogView.findViewById(R.id.layout_dialog_floating_window_content);

        settingsLinkRatioLeftInput.setText(prefsStore.getRouteLinkRatioNumerator());
        settingsLinkRatioRightInput.setText(speedInput.getText() == null ? "" : speedInput.getText().toString().trim());
        settingsStepsPerMeterInput.setText(prefsStore.getRouteStepsPerMeter());
        settingsLoopInput.setText(loopInput.getText() == null ? "" : loopInput.getText().toString().trim());
        settingsSatelliteSeekBar.setProgress(prefsStore.getNmeaSatelliteCount() - 1);
        updateSatelliteValue(settingsSatelliteSeekBar.getProgress());
        settingsSignalQualityGroup.check(signalQualityToButtonId(prefsStore.getNmeaSignalQuality()));
        settingsHdopSeekBar.setProgress(Math.round(prefsStore.getNmeaHdop() * 10f));
        updateHdopValue(settingsHdopSeekBar.getProgress());
        settingsUpdateIntervalSeekBar.setProgress(updateIntervalToProgress(prefsStore.getLocationUpdateIntervalMillis()));
        updateUpdateIntervalValue(settingsUpdateIntervalSeekBar.getProgress());
        settingsNetworkSimulationSwitch.setChecked(prefsStore.isNetworkSimulationEnabled());
        settingsDynamicIntensitySwitch.setChecked(speedFloatCheckBox.isChecked());
        settingsIntensityRangeInput.setText(prefsStore.getRouteIntensityVariationRange());
        settingsIntensityFrequencySeekBar.setProgress(
                Math.round(prefsStore.getRouteIntensityVariationFrequency() * 100f)
        );
        updateIntensityFrequencyValue(settingsIntensityFrequencySeekBar.getProgress());
        settingsPathVariationSwitch.setChecked(prefsStore.isRouteNaturalPathVariationEnabled());
        settingsPathVariationInput.setText(prefsStore.getRoutePathVariationAmplitude());
        settingsAltitudeVariationSwitch.setChecked(prefsStore.isRouteNaturalAltitudeVariationEnabled());
        settingsAltitudeBaseInput.setText(prefsStore.getRouteAltitudeBaseMeters());
        settingsAltitudeVariationRangeInput.setText(prefsStore.getRouteAltitudeVariationRange());
        settingsAltitudeHeightInput.setText(prefsStore.getRouteAltitudeVariationHeightCm());
        settingsAltitudeProbabilitySeekBar.setProgress(
                Math.round(prefsStore.getRouteAltitudeVariationProbability() * 100f)
        );
        updateAltitudeProbabilityValue(settingsAltitudeProbabilitySeekBar.getProgress());
        settingsFloatingWindowSwitch.setChecked(prefsStore.isRouteFloatingWindowEnabled());
        settingsFloatingWindowScaleSeekBar.setProgress(Math.round(prefsStore.getRouteFloatingWindowScale() * 100f));
        settingsFloatingWindowButtonSizeSeekBar.setProgress(Math.round(prefsStore.getRouteFloatingWindowButtonSizeDp()));
        updateFloatingWindowPreview();
        settingsReminderToneView.setText(resolveReminderToneTitle());
        pickRingtoneButton.setOnClickListener(v -> showReminderTonePickerDialog());
        uploadSettingsButton.setOnClickListener(v -> promptUploadSimulationSettings());
        downloadSettingsButton.setOnClickListener(v -> loadSharedSimulationSettings());
        if (settingsNonRootTabView != null) {
            settingsNonRootTabView.setOnClickListener(v -> selectSimulationSettingsMode(false));
        }
        if (settingsRootTabView != null) {
            settingsRootTabView.setOnClickListener(v -> selectSimulationSettingsMode(true));
        }
        if (settingsRootRefreshButton != null) {
            settingsRootRefreshButton.setOnClickListener(v -> refreshRootEnvironmentReport(false));
        }
        if (settingsRootConfirmSessionButton != null) {
            settingsRootConfirmSessionButton.setOnClickListener(v -> confirmRootTestSession());
        }
        if (settingsRootRequestSuButton != null) {
            settingsRootRequestSuButton.setOnClickListener(v -> requestRootShellAuthorization());
        }
        if (settingsRootPickTargetButton != null) {
            settingsRootPickTargetButton.setOnClickListener(v -> openLsposedScopeSelectorFromRootPanel());
        }
        if (settingsRootModeSwitch != null) {
            settingsRootModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (suppressRootFeatureSwitchCallbacks) {
                    return;
                }
                updateRootModeEnabled(isChecked);
            });
        }
        bindRootModuleSettingsButtons();
        bindRootFeatureSwitches();
        if (settingsRootReloadConfigButton != null) {
            settingsRootReloadConfigButton.setOnClickListener(v -> reloadRootFeatureConfig("manual_reload", true));
        }
        if (settingsRootGenerateGmButton != null) {
            settingsRootGenerateGmButton.setOnClickListener(v -> generateRootGmTestPreview());
        }
        if (settingsAlgorithmLabButton != null) {
            settingsAlgorithmLabButton.setOnClickListener(v -> openAlgorithmTestLab());
        }
        if (rootSwitchNonRootButton != null) {
            rootSwitchNonRootButton.setOnClickListener(v -> selectSimulationSettingsMode(false));
        }
        settingsSatelliteSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSatelliteValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        settingsHdopSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateHdopValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        settingsUpdateIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateUpdateIntervalValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        settingsDynamicIntensitySwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateSettingsContentState(dynamicIntensityContent, isChecked)
        );
        settingsPathVariationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateSettingsContentState(pathVariationContent, isChecked)
        );
        settingsAltitudeVariationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateSettingsContentState(altitudeVariationContent, isChecked)
        );
        settingsFloatingWindowSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateSettingsContentState(floatingWindowContent, isChecked)
        );
        settingsIntensityFrequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateIntensityFrequencyValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        settingsAltitudeProbabilitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateAltitudeProbabilityValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        settingsFloatingWindowScaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFloatingWindowPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        settingsFloatingWindowButtonSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFloatingWindowPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        updateSettingsContentState(dynamicIntensityContent, settingsDynamicIntensitySwitch.isChecked());
        updateSettingsContentState(pathVariationContent, settingsPathVariationSwitch.isChecked());
        updateSettingsContentState(altitudeVariationContent, settingsAltitudeVariationSwitch.isChecked());
        updateSettingsContentState(floatingWindowContent, settingsFloatingWindowSwitch.isChecked());
        if (rootFeatureConfigStore != null) {
            rootFeatureConfigStore.registerListener(rootFeatureConfigListener);
        }
        reloadRootFeatureConfig("dialog_open", false);
        selectSimulationSettingsMode(SimulationPrefsStore.ROUTE_SETTINGS_MODE_ROOT.equals(prefsStore.getRouteSettingsMode()));
        refreshInternalAccountProfileForRootGate();
        refreshRootEnvironmentReport(false);

        simulationSettingsFormView = dialogView;
        setupSimulationSettingsNavigator(dialogView);
        showSimulationSettingsOverlay();
        showSimulationSettingsHomePage();
    }

    private void showSimulationSettingsOverlay() {
        if (simulationSettingsOverlay != null) {
            return;
        }
        simulationSettingsOverlay = new FrameLayout(this);
        simulationSettingsOverlay.setBackgroundColor(Color.parseColor("#F5F7FA"));
        simulationSettingsOverlay.setClickable(true);
        simulationSettingsOverlay.setFocusable(true);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.parseColor("#F5F7FA"));
        simulationSettingsOverlay.addView(shell, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(Math.round(dp(8f)), 0, Math.round(dp(12f)), 0);
        toolbar.setBackgroundColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(dp(2f));
        }
        shell.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.round(dp(56f))
        ));

        ImageButton backButton = new ImageButton(this);
        backButton.setImageResource(android.R.drawable.ic_menu_revert);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setContentDescription("返回路线模拟");
        backButton.setOnClickListener(v -> handleSimulationSettingsBack());
        toolbar.addView(backButton, new LinearLayout.LayoutParams(
                Math.round(dp(44f)),
                Math.round(dp(44f))
        ));

        simulationSettingsTitleView = new TextView(this);
        simulationSettingsTitleView.setText(R.string.route_link_settings_title);
        simulationSettingsTitleView.setTextColor(Color.parseColor("#1F2A37"));
        simulationSettingsTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        simulationSettingsTitleView.setGravity(Gravity.CENTER_VERTICAL);
        simulationSettingsTitleView.setSingleLine(true);
        simulationSettingsTitleView.setEllipsize(TextUtils.TruncateAt.END);
        toolbar.addView(simulationSettingsTitleView, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));

        simulationSettingsContentFrame = new FrameLayout(this);
        shell.addView(simulationSettingsContentFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        simulationSettingsPrimaryButton = new Button(this);
        simulationSettingsPrimaryButton.setText(R.string.route_link_settings_confirm);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(
                Math.round(dp(16f)),
                Math.round(dp(8f)),
                Math.round(dp(16f)),
                Math.round(dp(12f))
        );
        shell.addView(simulationSettingsPrimaryButton, buttonParams);

        addContentView(simulationSettingsOverlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private void showSimulationSettingsHomePage() {
        if (simulationSettingsContentFrame == null) {
            return;
        }
        simulationSettingsSubPageVisible = false;
        simulationSettingsActiveSaveAction = null;
        if (simulationSettingsTitleView != null) {
            simulationSettingsTitleView.setText(R.string.route_link_settings_title);
        }
        if (simulationSettingsPrimaryButton != null) {
            simulationSettingsPrimaryButton.setVisibility(View.VISIBLE);
            simulationSettingsPrimaryButton.setText(R.string.route_link_settings_confirm);
            simulationSettingsPrimaryButton.setOnClickListener(v -> saveSimulationSettings());
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = Math.round(dp(16f));
        page.setPadding(horizontalPadding, Math.round(dp(14f)), horizontalPadding, Math.round(dp(24f)));
        scrollView.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        EditText searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint(R.string.route_settings_search_hint);
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        page.addView(searchInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        List<SimulationSettingsHomeRow> rows = new ArrayList<>();
        TextView nonRootCategory = addSimulationSettingsCategory(page, getString(R.string.route_settings_tab_non_root));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "联动比例",
                prefsStore.getRouteLinkRatioNumerator() + " : " + safeText(speedInput),
                "联动 比例 link ratio",
                v -> showSimulationSettingsFormPage("联动比例", false, R.id.section_settings_link));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "步数换算",
                "每米 " + prefsStore.getRouteStepsPerMeter() + " 步",
                "步数 步频 米数 steps",
                v -> showSimulationSettingsFormPage("步数换算", false, R.id.section_settings_steps));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "循环次数",
                safeText(loopInput) + " 次",
                "循环 次数 loop",
                v -> showSimulationSettingsFormPage("循环次数", false, R.id.section_settings_loop));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "定位信号",
                "卫星 " + prefsStore.getNmeaSatelliteCount()
                        + " · HDOP " + String.format(Locale.US, "%.2f", prefsStore.getNmeaHdop()),
                "NMEA GPS 卫星 信号 精度 更新 interval",
                v -> showSimulationSettingsFormPage("定位信号", false, R.id.section_settings_nmea));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "速度强度浮动",
                stateText(speedFloatCheckBox != null && speedFloatCheckBox.isChecked())
                        + " · 范围 " + prefsStore.getRouteIntensityVariationRange(),
                "强度 浮动 dynamic intensity",
                v -> showSimulationSettingsFormPage("速度强度浮动", false, R.id.section_settings_dynamic));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "路线自然偏移",
                stateText(prefsStore.isRouteNaturalPathVariationEnabled())
                        + " · " + prefsStore.getRoutePathVariationAmplitude() + " 米",
                "路径 自然 path",
                v -> showSimulationSettingsFormPage("路线自然偏移", false, R.id.section_settings_path));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "海拔与身高基线",
                prefsStore.getRouteAltitudeBaseMeters() + " 米 · 身高 "
                        + prefsStore.getRouteAltitudeVariationHeightCm() + " cm",
                "海拔 身高 altitude height",
                v -> showSimulationSettingsFormPage("海拔与身高基线", false, R.id.section_settings_altitude));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "悬浮窗",
                stateText(prefsStore.isRouteFloatingWindowEnabled()),
                "悬浮窗 floating overlay",
                v -> showSimulationSettingsFormPage("悬浮窗", false, R.id.section_settings_floating));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "提醒铃声",
                resolveReminderToneTitle(),
                "铃声 提醒 ringtone",
                v -> showSimulationSettingsFormPage("提醒铃声", false, R.id.section_settings_ringtone));
        addSimulationSettingsEntry(page, rows, nonRootCategory,
                "共享设置",
                "上传或下载模拟设置",
                "共享 上传 下载 share",
                v -> showSimulationSettingsFormPage("共享设置", false, R.id.section_settings_share));

        if (isInternalRootTestingEnabled()) {
            TextView rootCategory = addSimulationSettingsCategory(page, getString(R.string.route_settings_tab_root));
            addRootSettingsDropdown(page);
            addSimulationSettingsEntry(page, rows, rootCategory,
                    "Root授权",
                    rootModeHomeSummary(),
                    "Root 授权 检测",
                    v -> showSimulationSettingsFormPage("Root授权", true, R.id.section_settings_root_auth));
            if (isRootControlsUnlocked()) {
                addSimulationSettingsEntry(page, rows, rootCategory,
                        "环境状态",
                        "隐藏Root、开发者选项、模拟位置和Hook环境",
                        "Root 环境 开发者 模拟位置 Hook",
                        v -> showSimulationSettingsFormPage("环境状态", true, R.id.section_settings_root_environment));
                addSimulationSettingsEntry(page, rows, rootCategory,
                        "作用域与诊断状态",
                        "LSPosed作用域、模块开关、兼容性和诊断事件",
                        "Root LSPosed 作用域 目标 诊断 模块",
                        v -> showSimulationSettingsFormPage("作用域与诊断状态", true, R.id.section_settings_root_restricted));
                addRootModuleHomeEntry(page, rows, rootCategory, RootDiagnosticModule.LOCATION_NMEA);
                addRootModuleHomeEntry(page, rows, rootCategory, RootDiagnosticModule.RADIO_WIFI_SIGNAL);
                addRootModuleHomeEntry(page, rows, rootCategory, RootDiagnosticModule.DETECTION_BYPASS);
                addRootModuleHomeEntry(page, rows, rootCategory, RootDiagnosticModule.TARGET_APP_HOOK);
                addRootModuleHomeEntry(page, rows, rootCategory, RootDiagnosticModule.SERVICE_STREAM);
                addRootModuleHomeEntry(page, rows, rootCategory, RootDiagnosticModule.SENSOR_INJECTION);
                addSimulationSettingsEntry(page, rows, rootCategory,
                        getString(R.string.route_algorithm_lab_title),
                        getString(R.string.route_algorithm_lab_summary),
                        "算法 验证 实验室 步频 GPS 传感器",
                        v -> showSimulationSettingsFormPage(getString(R.string.route_algorithm_lab_title), true,
                                R.id.section_settings_algorithm_lab));
                addSimulationSettingsEntry(page, rows, rootCategory,
                        getString(R.string.route_root_audit_title),
                        "查看Root诊断审计日志",
                        "审计 日志 audit log",
                        v -> showSimulationSettingsFormPage(getString(R.string.route_root_audit_title), true,
                                R.id.section_settings_root_logs));
            }
        }

        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterSimulationSettingsHomeRows(rows, editable == null ? "" : editable.toString());
            }
        });
        setSimulationSettingsContent(scrollView);
    }

    @NonNull
    private String safeText(@Nullable TextView textView) {
        if (textView == null || textView.getText() == null) {
            return "";
        }
        return textView.getText().toString().trim();
    }

    @NonNull
    private String rootModeHomeSummary() {
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        if (!isRootDetectedForRootMode()) {
            return getString(R.string.route_root_mode_need_root);
        }
        return config.isRootModeEnabled()
                ? getString(R.string.route_root_mode_enabled_hint)
                : getString(R.string.route_root_mode_locked_hint);
    }

    private TextView addSimulationSettingsCategory(@NonNull LinearLayout page, @NonNull String title) {
        TextView category = new TextView(this);
        category.setText(title);
        category.setTextColor(Color.parseColor("#607085"));
        category.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        category.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = Math.round(dp(16f));
        page.addView(category, params);
        return category;
    }

    private void addSimulationSettingsEntry(
            @NonNull LinearLayout page,
            @NonNull List<SimulationSettingsHomeRow> rows,
            @NonNull View category,
            @NonNull String title,
            @NonNull String summary,
            @NonNull String keywords,
            @NonNull View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(
                Math.round(dp(12f)),
                Math.round(dp(10f)),
                Math.round(dp(10f)),
                Math.round(dp(10f))
        );
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(8f));
        background.setStroke(Math.round(dp(1f)), Color.parseColor("#DDE4EE"));
        row.setBackground(background);
        row.setOnClickListener(listener);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.parseColor("#1F2A37"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        TextView summaryView = new TextView(this);
        summaryView.setText(summary);
        summaryView.setTextColor(Color.parseColor("#607085"));
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        summaryView.setMaxLines(2);
        summaryView.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textColumn.addView(summaryView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView arrowView = new TextView(this);
        arrowView.setText(">");
        arrowView.setTextColor(Color.parseColor("#8A97A8"));
        arrowView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        arrowView.setGravity(Gravity.CENTER);
        row.addView(arrowView, new LinearLayout.LayoutParams(
                Math.round(dp(28f)),
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = Math.round(dp(8f));
        page.addView(row, rowParams);
        rows.add(new SimulationSettingsHomeRow(row, category, title + " " + summary + " " + keywords));
    }

    private void addRootModuleHomeEntry(
            @NonNull LinearLayout page,
            @NonNull List<SimulationSettingsHomeRow> rows,
            @NonNull View category,
            @NonNull RootDiagnosticModule module
    ) {
        RootDiagnosticSettings settings = latestRootDiagnosticSettings == null
                ? RootDiagnosticSettings.defaults()
                : latestRootDiagnosticSettings;
        RootFeature feature = rootFeatureForModule(module);
        String enabledText = feature == null ? "" : stateText(isRootFeatureEnabled(feature)) + " · ";
        addSimulationSettingsEntry(page, rows, category,
                module.getTitle(),
                enabledText + settings.summarize(module),
                module.getTitle() + " " + module.getHookSurface(),
                v -> showRootModuleSettingsDialog(module));
    }

    private void addRootSettingsDropdown(@NonNull LinearLayout page) {
        List<String> labels = new ArrayList<>();
        List<View.OnClickListener> actions = new ArrayList<>();
        labels.add("选择Root设置项");
        actions.add(null);
        labels.add("Root授权");
        actions.add(v -> showSimulationSettingsFormPage("Root授权", true, R.id.section_settings_root_auth));
        if (isRootControlsUnlocked()) {
            labels.add("环境状态");
            actions.add(v -> showSimulationSettingsFormPage("环境状态", true, R.id.section_settings_root_environment));
            labels.add("作用域与诊断状态");
            actions.add(v -> showSimulationSettingsFormPage("作用域与诊断状态", true,
                    R.id.section_settings_root_restricted));
            addRootDropdownModule(labels, actions, RootDiagnosticModule.LOCATION_NMEA);
            addRootDropdownModule(labels, actions, RootDiagnosticModule.RADIO_WIFI_SIGNAL);
            addRootDropdownModule(labels, actions, RootDiagnosticModule.DETECTION_BYPASS);
            addRootDropdownModule(labels, actions, RootDiagnosticModule.TARGET_APP_HOOK);
            addRootDropdownModule(labels, actions, RootDiagnosticModule.SERVICE_STREAM);
            addRootDropdownModule(labels, actions, RootDiagnosticModule.SENSOR_INJECTION);
        }

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0 || position >= actions.size()) {
                    return;
                }
                View.OnClickListener action = actions.get(position);
                if (action != null) {
                    action.onClick(spinner);
                }
                spinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op.
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = Math.round(dp(8f));
        page.addView(spinner, params);
    }

    private void addRootDropdownModule(
            @NonNull List<String> labels,
            @NonNull List<View.OnClickListener> actions,
            @NonNull RootDiagnosticModule module
    ) {
        labels.add(module.getTitle());
        actions.add(v -> showRootModuleSettingsDialog(module));
    }

    private void filterSimulationSettingsHomeRows(
            @NonNull List<SimulationSettingsHomeRow> rows,
            @NonNull String query
    ) {
        String normalized = query.trim().toLowerCase(Locale.getDefault());
        for (SimulationSettingsHomeRow row : rows) {
            row.category.setVisibility(View.GONE);
        }
        for (SimulationSettingsHomeRow row : rows) {
            boolean matched = TextUtils.isEmpty(normalized)
                    || row.keywords.toLowerCase(Locale.getDefault()).contains(normalized);
            row.row.setVisibility(matched ? View.VISIBLE : View.GONE);
            if (matched) {
                row.category.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showSimulationSettingsFormPage(
            @NonNull String title,
            boolean rootPage,
            int... visibleSectionIds
    ) {
        if (simulationSettingsFormView == null || simulationSettingsContentFrame == null) {
            return;
        }
        simulationSettingsSubPageVisible = true;
        simulationSettingsActiveSaveAction = null;
        if (simulationSettingsTitleView != null) {
            simulationSettingsTitleView.setText(title);
        }
        if (simulationSettingsPrimaryButton != null) {
            simulationSettingsPrimaryButton.setVisibility(rootPage ? View.GONE : View.VISIBLE);
            simulationSettingsPrimaryButton.setText(R.string.route_link_settings_confirm);
            simulationSettingsPrimaryButton.setOnClickListener(v -> saveSimulationSettings());
        }
        if (prefsStore != null) {
            prefsStore.saveRouteSettingsMode(rootPage
                    ? SimulationPrefsStore.ROUTE_SETTINGS_MODE_ROOT
                    : SimulationPrefsStore.ROUTE_SETTINGS_MODE_NON_ROOT);
        }
        if (rootPage && !renderRootAccessGate()) {
            return;
        }
        if (rootPage) {
            renderRootEnvironmentReport();
        }
        prepareSimulationSettingsFormForSubPage();
        hideAllSimulationSettingsFormSections();
        if (settingsRootContainer != null) {
            settingsRootContainer.setVisibility(rootPage ? View.VISIBLE : View.GONE);
        }
        if (rootPage) {
            setSimulationSettingsFormViewVisibility(R.id.tv_dialog_root_testing_notice, View.VISIBLE);
        }
        for (int viewId : visibleSectionIds) {
            setSimulationSettingsFormViewVisibility(viewId, View.VISIBLE);
        }
        setSimulationSettingsContent(simulationSettingsFormView);
        ScrollView scrollView = simulationSettingsFormView.findViewById(R.id.scroll_dialog_settings);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
        }
    }

    private void prepareSimulationSettingsFormForSubPage() {
        setSimulationSettingsFormViewVisibility(R.id.et_dialog_settings_search, View.GONE);
        setSimulationSettingsFormViewVisibility(R.id.layout_dialog_settings_mode_tabs, View.GONE);
        setSimulationSettingsFormViewVisibility(R.id.layout_dialog_settings_letter_rail, View.GONE);
        if (simulationSettingsFormView != null) {
            ScrollView scrollView = simulationSettingsFormView.findViewById(R.id.scroll_dialog_settings);
            if (scrollView != null) {
                ViewGroup.LayoutParams params = scrollView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                scrollView.setLayoutParams(params);
                scrollView.setFillViewport(false);
            }
        }
    }

    private void hideAllSimulationSettingsFormSections() {
        int[] sectionIds = new int[] {
                R.id.section_settings_link,
                R.id.section_settings_steps,
                R.id.section_settings_loop,
                R.id.section_settings_nmea,
                R.id.section_settings_dynamic,
                R.id.section_settings_path,
                R.id.section_settings_altitude,
                R.id.section_settings_floating,
                R.id.section_settings_ringtone,
                R.id.section_settings_share,
                R.id.tv_dialog_root_testing_notice,
                R.id.section_settings_root_auth,
                R.id.section_settings_root_environment,
                R.id.section_settings_root_restricted,
                R.id.section_settings_algorithm_lab,
                R.id.section_settings_root_logs
        };
        for (int sectionId : sectionIds) {
            setSimulationSettingsFormViewVisibility(sectionId, View.GONE);
        }
    }

    private void setSimulationSettingsFormViewVisibility(int viewId, int visibility) {
        if (simulationSettingsFormView == null) {
            return;
        }
        View view = simulationSettingsFormView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private void setSimulationSettingsContent(@NonNull View view) {
        if (simulationSettingsContentFrame == null) {
            return;
        }
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        simulationSettingsContentFrame.removeAllViews();
        simulationSettingsContentFrame.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private void handleSimulationSettingsBack() {
        if (simulationSettingsSubPageVisible) {
            showSimulationSettingsHomePage();
        } else {
            hideSimulationSettingsOverlay();
        }
    }

    private void hideSimulationSettingsOverlay() {
        if (rootFeatureConfigStore != null) {
            rootFeatureConfigStore.unregisterListener(rootFeatureConfigListener);
        }
        stopReminderFeedback();
        if (simulationSettingsOverlay != null && simulationSettingsOverlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) simulationSettingsOverlay.getParent()).removeView(simulationSettingsOverlay);
        }
        simulationSettingsOverlay = null;
        simulationSettingsContentFrame = null;
        simulationSettingsTitleView = null;
        simulationSettingsPrimaryButton = null;
        simulationSettingsFormView = null;
        simulationSettingsSubPageVisible = false;
        simulationSettingsActiveSaveAction = null;
        clearSimulationSettingsViewReferences();
    }

    private void clearSimulationSettingsViewReferences() {
        settingsLinkRatioLeftInput = null;
        settingsLinkRatioRightInput = null;
        settingsStepsPerMeterInput = null;
        settingsLoopInput = null;
        settingsSatelliteSeekBar = null;
        settingsSatelliteValueView = null;
        settingsSignalQualityGroup = null;
        settingsHdopSeekBar = null;
        settingsHdopValueView = null;
        settingsUpdateIntervalSeekBar = null;
        settingsUpdateIntervalValueView = null;
        settingsNetworkSimulationSwitch = null;
        settingsDynamicIntensitySwitch = null;
        settingsIntensityRangeInput = null;
        settingsIntensityFrequencySeekBar = null;
        settingsIntensityFrequencyValueView = null;
        settingsPathVariationSwitch = null;
        settingsPathVariationInput = null;
        settingsAltitudeVariationSwitch = null;
        settingsAltitudeBaseInput = null;
        settingsAltitudeVariationRangeInput = null;
        settingsAltitudeHeightInput = null;
        settingsAltitudeProbabilitySeekBar = null;
        settingsAltitudeProbabilityValueView = null;
        settingsFloatingWindowSwitch = null;
        settingsFloatingWindowScaleSeekBar = null;
        settingsFloatingWindowButtonSizeSeekBar = null;
        settingsFloatingWindowScaleView = null;
        settingsFloatingWindowButtonSizeView = null;
        settingsFloatingWindowPreview = null;
        settingsReminderToneView = null;
        settingsNonRootTabView = null;
        settingsRootTabView = null;
        settingsRootContainer = null;
        settingsRootEnvironmentSection = null;
        settingsRootRestrictedSection = null;
        settingsRootAlgorithmLabSection = null;
        settingsRootLogsSection = null;
        settingsRootInternalStatusView = null;
        settingsRootModeStatusView = null;
        settingsRootStatusView = null;
        settingsRootAuthorizationStatusView = null;
        settingsRootHiddenStatusView = null;
        settingsRootDeveloperStatusView = null;
        settingsRootMockStatusView = null;
        settingsRootHookStatusView = null;
        settingsRootTargetStatusView = null;
        settingsRootCompatibilityStatusView = null;
        settingsRootFeatureConfigStatusView = null;
        settingsRootDiagnosticLogView = null;
        settingsRootAuditLogView = null;
        settingsRootRefreshButton = null;
        settingsRootConfirmSessionButton = null;
        settingsRootRequestSuButton = null;
        settingsRootPickTargetButton = null;
        settingsRootNmeaSettingsButton = null;
        settingsRootSignalSettingsButton = null;
        settingsRootBypassSettingsButton = null;
        settingsRootHookSettingsButton = null;
        settingsRootServiceSettingsButton = null;
        settingsRootSensorSettingsButton = null;
        settingsRootReloadConfigButton = null;
        settingsRootGenerateGmButton = null;
        settingsAlgorithmLabButton = null;
        settingsTestInstructionStudioButton = null;
        settingsScenarioLibraryButton = null;
        settingsPressureLabButton = null;
        settingsRootModeSwitch = null;
        settingsRootEnvironmentSwitch = null;
        settingsRootSuProbeSwitch = null;
        settingsRootEncryptedAuditSwitch = null;
        settingsRootGmInterfaceSwitch = null;
        settingsRootFridaInjectionSwitch = null;
        settingsRootNmeaSwitch = null;
        settingsRootSignalSwitch = null;
        settingsRootBypassSwitch = null;
        settingsRootHookSwitch = null;
        settingsRootServiceLogSwitch = null;
        settingsRootSensorSwitch = null;
    }

    private void selectSimulationSettingsMode(boolean rootMode) {
        if (prefsStore != null) {
            prefsStore.saveRouteSettingsMode(
                    rootMode ? SimulationPrefsStore.ROUTE_SETTINGS_MODE_ROOT : SimulationPrefsStore.ROUTE_SETTINGS_MODE_NON_ROOT
            );
        }
        updateSettingsTabStyle(settingsNonRootTabView, !rootMode);
        updateSettingsTabStyle(settingsRootTabView, rootMode);
        setNonRootSettingsSectionsVisible(!rootMode);
        if (settingsRootContainer != null) {
            settingsRootContainer.setVisibility(rootMode ? View.VISIBLE : View.GONE);
        }
        if (rootMode && !renderRootAccessGate()) {
            return;
        }
        renderRootEnvironmentReport();
        if (rootMode && latestRootEnvironmentReport != null && !latestRootEnvironmentReport.hasRootIndicators()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_no_root_switch_hint));
        }
    }

    private void updateSettingsTabStyle(@Nullable TextView tabView, boolean selected) {
        if (tabView == null) {
            return;
        }
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(6f));
        background.setColor(selected ? ContextCompat.getColor(this, R.color.colorPrimary) : Color.TRANSPARENT);
        tabView.setBackground(background);
        tabView.setTextColor(selected ? Color.WHITE : Color.parseColor("#54657E"));
    }

    private void setNonRootSettingsSectionsVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        setSimulationSettingsSectionVisibility(R.id.section_settings_link, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_steps, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_loop, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_nmea, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_dynamic, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_path, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_altitude, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_floating, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_ringtone, visibility);
        setSimulationSettingsSectionVisibility(R.id.section_settings_share, visibility);
    }

    private void setSimulationSettingsSectionVisibility(int viewId, int visibility) {
        View view = findSimulationSettingsDialogViewById(viewId);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    @Nullable
    private View findSimulationSettingsDialogViewById(int viewId) {
        if (simulationSettingsFormView == null) {
            return null;
        }
        return simulationSettingsFormView.findViewById(viewId);
    }

    private void bindRootFeatureSwitches() {
        bindRootFeatureSwitch(settingsRootEnvironmentSwitch, RootFeature.ENVIRONMENT_INSPECTION);
        bindRootFeatureSwitch(settingsRootSuProbeSwitch, RootFeature.ROOT_SHELL_PROBE);
        bindRootFeatureSwitch(settingsRootEncryptedAuditSwitch, RootFeature.ENCRYPTED_AUDIT_LOG);
        bindRootFeatureSwitch(settingsRootGmInterfaceSwitch, RootFeature.GM_TEST_INTERFACE);
        bindRootFeatureSwitch(settingsRootFridaInjectionSwitch, RootFeature.FRIDA_DYNAMIC_INJECTION);
        bindRootFeatureSwitch(settingsRootNmeaSwitch, RootFeature.ROOT_NMEA_INJECTION);
        bindRootFeatureSwitch(settingsRootSignalSwitch, RootFeature.SIGNAL_SIMULATION);
        bindRootFeatureSwitch(settingsRootBypassSwitch, RootFeature.MOCK_LOCATION_BYPASS);
        bindRootFeatureSwitch(settingsRootHookSwitch, RootFeature.TARGET_APP_HOOK);
        bindRootFeatureSwitch(settingsRootServiceLogSwitch, RootFeature.SYSTEM_SERVICE_STREAM_LOG);
        bindRootFeatureSwitch(settingsRootSensorSwitch, RootFeature.SENSOR_EVENT_INJECTION);
    }

    private void bindRootModuleSettingsButtons() {
        bindRootModuleSettingsButton(settingsRootNmeaSettingsButton, RootDiagnosticModule.LOCATION_NMEA);
        bindRootModuleSettingsButton(settingsRootSignalSettingsButton, RootDiagnosticModule.RADIO_WIFI_SIGNAL);
        bindRootModuleSettingsButton(settingsRootBypassSettingsButton, RootDiagnosticModule.DETECTION_BYPASS);
        bindRootModuleSettingsButton(settingsRootHookSettingsButton, RootDiagnosticModule.TARGET_APP_HOOK);
        bindRootModuleSettingsButton(settingsRootServiceSettingsButton, RootDiagnosticModule.SERVICE_STREAM);
        bindRootModuleSettingsButton(settingsRootSensorSettingsButton, RootDiagnosticModule.SENSOR_INJECTION);
    }

    private void bindRootModuleSettingsButton(@Nullable Button button, @NonNull RootDiagnosticModule module) {
        if (button == null) {
            return;
        }
        button.setOnClickListener(v -> showRootModuleSettingsDialog(module));
    }

    private void bindRootFeatureSwitch(@Nullable Switch switchView, @NonNull RootFeature feature) {
        if (switchView == null) {
            return;
        }
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressRootFeatureSwitchCallbacks) {
                return;
            }
            updateRootFeatureSwitch(feature, isChecked);
        });
    }

    private void updateRootFeatureSwitch(@NonNull RootFeature feature, boolean enabled) {
        if (!isInternalRootTestingEnabled()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_internal));
            renderRootFeatureConfig();
            return;
        }
        if (!isRootControlsUnlocked()) {
            showRootModeGateToast();
            renderRootFeatureConfig();
            return;
        }
        if (rootFeatureConfigStore == null) {
            return;
        }
        RootFeatureConfig config = rootFeatureConfigStore.setFeature(feature, enabled);
        applyRootFeatureConfig(config, "switch_" + feature.getConfigKey(), true);
        GoUtils.DisplayToast(this, getString(R.string.route_root_config_saved));
    }

    private void updateRootModeEnabled(boolean enabled) {
        if (!isInternalRootTestingEnabled()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_internal));
            renderRootFeatureConfig();
            return;
        }
        if (enabled && !isRootDetectedForRootMode()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_mode_need_root));
            renderRootFeatureConfig();
            return;
        }
        if (rootFeatureConfigStore == null) {
            return;
        }
        if (!enabled && rootDiagnosticSessionController != null && rootDiagnosticSessionController.isRunning()) {
            finishRootDiagnosticSession(true);
        }
        if (!enabled) {
            rootTestSessionConfirmed = false;
            rootShellAuthorized = false;
        }
        RootFeatureConfig config = rootFeatureConfigStore.load().withRootModeEnabled(enabled);
        if (enabled) {
            config = config.withInjectionFramework(RootFeatureConfig.InjectionFramework.LSPOSED)
                    .withAllFeaturesEnabled();
        }
        rootFeatureConfigStore.save(config);
        applyRootFeatureConfig(config, "root_mode_toggle", true);
        GoUtils.DisplayToast(this, getString(R.string.route_root_config_saved));
    }

    @NonNull
    private RootFeatureConfig enableDefaultRootModulesForRouteTakeover() {
        RootFeatureConfig config = rootFeatureConfigStore == null
                ? (latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig)
                : rootFeatureConfigStore.load();
        config = config.withInjectionFramework(RootFeatureConfig.InjectionFramework.LSPOSED)
                .withAllFeaturesEnabled();
        if (rootFeatureConfigStore != null) {
            rootFeatureConfigStore.save(config);
        }
        applyRootFeatureConfig(config, "root_route_takeover_defaults", true);
        return config;
    }

    private void reloadRootFeatureConfig(@NonNull String reason, boolean writeAudit) {
        if (rootFeatureConfigStore == null) {
            return;
        }
        RootFeatureConfig config = rootFeatureConfigStore.load();
        if (config.getInjectionFramework() == RootFeatureConfig.InjectionFramework.FRIDA) {
            config = config.withInjectionFramework(RootFeatureConfig.InjectionFramework.LSPOSED);
            rootFeatureConfigStore.save(config);
        }
        applyRootFeatureConfig(config, reason, writeAudit);
    }

    private void applyRootFeatureConfig(
            @NonNull RootFeatureConfig config,
            @NonNull String reason,
            boolean writeAudit
    ) {
        latestRootFeatureConfig = config;
        if (rootFeatureRuntimeController != null) {
            latestRootFeatureRuntimeReport = rootFeatureRuntimeController.reload(config);
        }
        renderRootFeatureConfig();
        if (writeAudit) {
            appendRootAudit("Root能力配置热重载: reason=" + reason
                    + ", version=" + config.getVersion()
                    + ", framework=" + config.getInjectionFramework()
                    + ", injection=" + config.isEnabled(RootFeature.FRIDA_DYNAMIC_INJECTION)
                    + ", gm=" + config.isEnabled(RootFeature.GM_TEST_INTERFACE));
        }
    }

    private boolean isRootModeConfiguredEnabled() {
        RootFeatureConfig config = latestRootFeatureConfig;
        if (config == null && rootFeatureConfigStore != null) {
            config = rootFeatureConfigStore.load();
            latestRootFeatureConfig = config;
        }
        return config != null && config.isRootModeEnabled();
    }

    private boolean isRootDetectedForRootMode() {
        RootEnvironmentReport report = latestRootEnvironmentReport;
        return rootShellAuthorized || (report != null && report.hasRootIndicators());
    }

    private boolean isRootControlsUnlocked() {
        return isInternalRootTestingEnabled()
                && isRootModeConfiguredEnabled()
                && isRootDetectedForRootMode();
    }

    private boolean isRootControlsUnlocked(@NonNull RootFeatureConfig config) {
        return isInternalRootTestingEnabled()
                && config.isRootModeEnabled()
                && isRootDetectedForRootMode();
    }

    private void showRootModeGateToast() {
        if (!isInternalRootTestingEnabled()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_internal));
        } else if (!isRootDetectedForRootMode()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_mode_need_root));
        } else {
            GoUtils.DisplayToast(this, getString(R.string.route_root_mode_need_enabled));
        }
    }

    private void renderRootModeGate(@NonNull RootFeatureConfig config, boolean internalEnabled) {
        boolean rootDetected = isRootDetectedForRootMode();
        boolean rootModeChecked = internalEnabled && rootDetected && config.isRootModeEnabled();
        suppressRootFeatureSwitchCallbacks = true;
        if (settingsRootModeSwitch != null) {
            settingsRootModeSwitch.setChecked(rootModeChecked);
            settingsRootModeSwitch.setEnabled(internalEnabled && rootDetected
                    && (rootDiagnosticSessionController == null || !rootDiagnosticSessionController.isRunning()));
        }
        suppressRootFeatureSwitchCallbacks = false;
        if (settingsRootModeStatusView != null) {
            if (!internalEnabled) {
                settingsRootModeStatusView.setText(R.string.route_root_internal_disabled);
            } else if (!rootDetected) {
                settingsRootModeStatusView.setText(R.string.route_root_mode_need_root);
            } else if (config.isRootModeEnabled()) {
                settingsRootModeStatusView.setText(R.string.route_root_mode_enabled_hint);
            } else {
                settingsRootModeStatusView.setText(R.string.route_root_mode_locked_hint);
            }
        }
        int controlsVisibility = rootModeChecked ? View.VISIBLE : View.GONE;
        setNullableViewVisibility(settingsRootEnvironmentSection, controlsVisibility);
        setNullableViewVisibility(settingsRootRestrictedSection, controlsVisibility);
        setNullableViewVisibility(settingsRootAlgorithmLabSection, controlsVisibility);
        setNullableViewVisibility(settingsRootLogsSection, controlsVisibility);
    }

    private void setNullableViewVisibility(@Nullable View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private void renderRootFeatureConfig() {
        if (!renderRootAccessGate()) {
            return;
        }
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        RootFeatureRuntimeReport report = latestRootFeatureRuntimeReport;
        if (report == null && rootFeatureRuntimeController != null) {
            report = rootFeatureRuntimeController.reload(config);
            latestRootFeatureRuntimeReport = report;
        }

        boolean internalEnabled = isInternalRootTestingEnabled();
        renderRootModeGate(config, internalEnabled);
        boolean controlsUnlocked = isRootControlsUnlocked(config);
        suppressRootFeatureSwitchCallbacks = true;
        setRootFeatureSwitchState(settingsRootEnvironmentSwitch, config, RootFeature.ENVIRONMENT_INSPECTION, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootSuProbeSwitch, config, RootFeature.ROOT_SHELL_PROBE, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootEncryptedAuditSwitch, config, RootFeature.ENCRYPTED_AUDIT_LOG, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootGmInterfaceSwitch, config, RootFeature.GM_TEST_INTERFACE, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootFridaInjectionSwitch, config, RootFeature.FRIDA_DYNAMIC_INJECTION, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootNmeaSwitch, config, RootFeature.ROOT_NMEA_INJECTION, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootSignalSwitch, config, RootFeature.SIGNAL_SIMULATION, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootBypassSwitch, config, RootFeature.MOCK_LOCATION_BYPASS, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootHookSwitch, config, RootFeature.TARGET_APP_HOOK, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootServiceLogSwitch, config, RootFeature.SYSTEM_SERVICE_STREAM_LOG, controlsUnlocked);
        setRootFeatureSwitchState(settingsRootSensorSwitch, config, RootFeature.SENSOR_EVENT_INJECTION, controlsUnlocked);
        suppressRootFeatureSwitchCallbacks = false;

        if (settingsRootReloadConfigButton != null) {
            settingsRootReloadConfigButton.setEnabled(controlsUnlocked);
        }
        if (settingsRootGenerateGmButton != null) {
            settingsRootGenerateGmButton.setEnabled(controlsUnlocked
                    && rootTestSessionConfirmed
                    && config.isEnabled(RootFeature.GM_TEST_INTERFACE));
        }
        if (settingsRootFeatureConfigStatusView != null && report != null) {
            settingsRootFeatureConfigStatusView.setText(getString(
                    R.string.route_root_feature_config_format,
                    config.getVersion(),
                    config.getInjectionFramework().name(),
                    androidCompatibilityText(),
                    TextUtils.isEmpty(config.getTargetPackageName())
                            ? (config.getInjectionFramework() == RootFeatureConfig.InjectionFramework.LSPOSED
                                    ? LsposedDiagnosticBridge.SCOPE_TARGET_LABEL
                                    : "未选择")
                            : config.getTargetPackageName(),
                    joinLines(report.summarizeLines(), 8)
            ));
        }
        renderRootDiagnosticPanel();
    }

    private void setRootFeatureSwitchState(
            @Nullable Switch switchView,
            @NonNull RootFeatureConfig config,
            @NonNull RootFeature feature,
            boolean enabled
    ) {
        if (switchView == null) {
            return;
        }
        switchView.setChecked(config.isEnabled(feature));
        switchView.setEnabled(enabled);
    }

    private boolean isRootFeatureEnabled(@NonNull RootFeature feature) {
        RootFeatureConfig config = latestRootFeatureConfig;
        if (config == null && rootFeatureConfigStore != null) {
            config = rootFeatureConfigStore.load();
            latestRootFeatureConfig = config;
        }
        return config != null && config.isEnabled(feature);
    }

    private void appendRootAudit(@NonNull String event) {
        if (rootAuditLogger != null && isRootFeatureEnabled(RootFeature.ENCRYPTED_AUDIT_LOG)) {
            rootAuditLogger.append(event);
        }
    }

    @NonNull
    private String androidCompatibilityText() {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < ANDROID_9_API) {
            return "API " + sdk + " 低于Android 9目标范围";
        }
        if (sdk <= ANDROID_13_API) {
            return "API " + sdk + " 位于Android 9-13目标范围";
        }
        return "API " + sdk + " 高于Android 13，沿用受控测试边界";
    }

    @NonNull
    private String joinLines(@NonNull List<String> lines, int limit) {
        StringBuilder builder = new StringBuilder();
        int count = Math.min(lines.size(), limit);
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(index));
        }
        if (lines.size() > limit) {
            builder.append('\n').append("...");
        }
        return builder.toString();
    }

    private void renderRootDiagnosticPanel() {
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        String targetPackageName = config.getTargetPackageName();
        boolean targetSelected = !TextUtils.isEmpty(targetPackageName);
        boolean lsposedMode = config.getInjectionFramework() == RootFeatureConfig.InjectionFramework.LSPOSED;
        boolean controlsUnlocked = isRootControlsUnlocked(config);
        boolean running = rootDiagnosticSessionController != null && rootDiagnosticSessionController.isRunning();

        if (settingsRootTargetStatusView != null) {
            if (lsposedMode) {
                settingsRootTargetStatusView.setText(getString(
                        R.string.route_root_lsposed_scope_status,
                        LsposedDiagnosticBridge.describeManagerState(this),
                        RootDiagnosticModule.summarizeEnabled(config)
                ) + "\n" + getString(R.string.route_root_diagnostic_auto_hint));
            } else if (!targetSelected) {
                settingsRootTargetStatusView.setText(R.string.route_root_target_none);
            } else {
                settingsRootTargetStatusView.setText(getString(
                        R.string.route_root_target_format,
                        resolveAppLabel(targetPackageName),
                        targetPackageName,
                        resolveAppVersion(targetPackageName),
                        RootDiagnosticModule.summarizeEnabled(config)
                ) + "\n" + getString(R.string.route_root_diagnostic_auto_hint));
            }
        }
        if (settingsRootPickTargetButton != null) {
            settingsRootPickTargetButton.setEnabled(controlsUnlocked && !running);
            settingsRootPickTargetButton.setText(R.string.route_root_open_lsposed_scope_button);
        }
        if (settingsRootCompatibilityStatusView != null) {
            settingsRootCompatibilityStatusView.setText(
                    RootDiagnosticCompatibilityCatalog.summarizeForDisplay(this)
            );
        }
        setModuleSettingsButtonState(settingsRootNmeaSettingsButton, controlsUnlocked && !running, RootDiagnosticModule.LOCATION_NMEA);
        setModuleSettingsButtonState(settingsRootSignalSettingsButton, controlsUnlocked && !running, RootDiagnosticModule.RADIO_WIFI_SIGNAL);
        setModuleSettingsButtonState(settingsRootBypassSettingsButton, controlsUnlocked && !running, RootDiagnosticModule.DETECTION_BYPASS);
        setModuleSettingsButtonState(settingsRootHookSettingsButton, controlsUnlocked && !running, RootDiagnosticModule.TARGET_APP_HOOK);
        setModuleSettingsButtonState(settingsRootServiceSettingsButton, controlsUnlocked && !running, RootDiagnosticModule.SERVICE_STREAM);
        setModuleSettingsButtonState(settingsRootSensorSettingsButton, controlsUnlocked && !running, RootDiagnosticModule.SENSOR_INJECTION);
        if (settingsRootDiagnosticLogView != null) {
            List<String> lines = rootDiagnosticSessionController == null
                    ? new ArrayList<>()
                    : rootDiagnosticSessionController.getRecentEventLines(10);
            if (lines.isEmpty()) {
                File reportFile = rootDiagnosticSessionController == null ? null : rootDiagnosticSessionController.getLatestReportFile();
                if (reportFile == null) {
                    settingsRootDiagnosticLogView.setText(R.string.route_root_diagnostic_log_empty);
                } else {
                    settingsRootDiagnosticLogView.setText(getString(
                            R.string.route_root_diagnostic_report_saved,
                            reportFile.getAbsolutePath()
                    ));
                }
            } else {
                settingsRootDiagnosticLogView.setText(joinLines(lines, 10));
            }
        }
    }

    private void setModuleSettingsButtonState(
            @Nullable Button button,
            boolean enabled,
            @NonNull RootDiagnosticModule module
    ) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        RootDiagnosticSettings settings = latestRootDiagnosticSettings == null
                ? RootDiagnosticSettings.defaults()
                : latestRootDiagnosticSettings;
        button.setText(getString(R.string.route_root_module_settings_button)
                + " · " + module.getTitle()
                + "\n" + settings.summarize(module));
    }

    private void openLsposedScopeSelectorFromRootPanel() {
        if (!isInternalRootTestingEnabled()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_internal));
            return;
        }
        if (!isRootControlsUnlocked()) {
            showRootModeGateToast();
            return;
        }
        if (rootDiagnosticSessionController != null && rootDiagnosticSessionController.isRunning()) {
            GoUtils.DisplayToast(this, "诊断运行中，结束路线模拟后再修改LSPosed作用域。");
            return;
        }
        if (rootFeatureConfigStore != null) {
            RootFeatureConfig config = rootFeatureConfigStore.setInjectionFramework(
                    RootFeatureConfig.InjectionFramework.LSPOSED
            );
            applyRootFeatureConfig(config, "lsposed_scope_open", true);
        }
        boolean opened = LsposedDiagnosticBridge.openManager(this);
        appendRootAudit("打开LSPosed作用域选择: opened=" + opened);
        GoUtils.DisplayToast(this, opened
                ? getString(R.string.route_root_lsposed_scope_opened)
                : getString(R.string.route_root_lsposed_open_failed));
    }

    private void showRootModuleSettingsDialog(@NonNull RootDiagnosticModule module) {
        if (!isRootControlsUnlocked()) {
            showRootModeGateToast();
            return;
        }
        if (rootDiagnosticSessionController != null && rootDiagnosticSessionController.isRunning()) {
            GoUtils.DisplayToast(this, "诊断运行中，结束路线模拟后再修改模块设置。");
            return;
        }
        RootDiagnosticSettings settings = latestRootDiagnosticSettings == null
                ? RootDiagnosticSettings.defaults()
                : latestRootDiagnosticSettings;
        LinearLayout content = createRootSettingsForm();
        switch (module) {
            case LOCATION_NMEA:
                showLocationModuleSettings(content, settings);
                break;
            case RADIO_WIFI_SIGNAL:
                showSignalModuleSettings(content, settings);
                break;
            case DETECTION_BYPASS:
                showBypassModuleSettings(content, settings);
                break;
            case TARGET_APP_HOOK:
                showTargetHookModuleSettings(content, settings);
                break;
            case SERVICE_STREAM:
                showServiceStreamModuleSettings(content, settings);
                break;
            case SENSOR_INJECTION:
                showSensorModuleSettings(content, settings);
                break;
            default:
                break;
        }
    }

    private void showLocationModuleSettings(
            @NonNull LinearLayout content,
            @NonNull RootDiagnosticSettings settings
    ) {
        addRootSettingsText(content, "经纬度、海拔、速度和航向由路线模拟实时联动，Root 模式下采用 GlobalTraveling 接管逻辑，不支持在模块内手动填写。");
        EditText satellitesInput = addRootSettingsEdit(content, "卫星数", settings.getLocationSatellites());
        EditText hdopInput = addRootSettingsEdit(content, "HDOP", settings.getLocationHdop());
        showRootSettingsDialog(RootDiagnosticModule.LOCATION_NMEA, content, () -> saveRootDiagnosticSettings(
                settings.withLocation(
                        settings.getLocationLatitude(),
                        settings.getLocationLongitude(),
                        settings.getLocationSpeedMetersPerSecond(),
                        settings.getLocationAltitudeMeters(),
                        settings.getLocationBearingDegrees(),
                        parseRootSettingInt(satellitesInput, "卫星数"),
                        parseRootSettingDouble(hdopInput, "HDOP")
                )
        ));
    }

    private void showSignalModuleSettings(
            @NonNull LinearLayout content,
            @NonNull RootDiagnosticSettings settings
    ) {
        EditText bssidInput = addRootSettingsEdit(content, "Wi-Fi BSSID", settings.getWifiBssid());
        EditText ssidInput = addRootSettingsEdit(content, "Wi-Fi SSID", settings.getWifiSsid());
        EditText operatorInput = addRootSettingsEdit(content, "运营商MCC/MNC", settings.getNetworkOperator());
        EditText countryInput = addRootSettingsEdit(content, "网络国家码", settings.getNetworkCountry());
        EditText wifiRssiInput = addRootSettingsEdit(content, "Wi-Fi RSSI dBm (-30~-100)", settings.getWifiRssiDbm());
        EditText wifiJitterInput = addRootSettingsEdit(content, "Wi-Fi 抖动 dBm (0~20)", settings.getWifiJitterDbm());
        EditText cellDbmInput = addRootSettingsEdit(content, "Cell dBm (-50~-125)", settings.getCellDbm());
        EditText cellJitterInput = addRootSettingsEdit(content, "Cell 抖动 dBm (0~20)", settings.getCellJitterDbm());
        showRootSettingsDialog(RootDiagnosticModule.RADIO_WIFI_SIGNAL, content, () -> saveRootDiagnosticSettings(
                settings.withSignal(
                        requireRootSettingText(bssidInput, "Wi-Fi BSSID"),
                        requireRootSettingText(ssidInput, "Wi-Fi SSID"),
                        requireRootSettingText(operatorInput, "运营商MCC/MNC"),
                        requireRootSettingText(countryInput, "网络国家码"),
                        new RootSignalStrengthProfile(
                                parseRootSettingInt(wifiRssiInput, "Wi-Fi RSSI"),
                                parseRootSettingInt(wifiJitterInput, "Wi-Fi 抖动"),
                                parseRootSettingInt(cellDbmInput, "Cell dBm"),
                                parseRootSettingInt(cellJitterInput, "Cell 抖动")
                        )
                )
        ));
    }

    private void showBypassModuleSettings(
            @NonNull LinearLayout content,
            @NonNull RootDiagnosticSettings settings
    ) {
        Switch rootSwitch = addRootSettingsSwitch(content, "隐藏root文件/命令检测返回", settings.isBypassRootArtifacts());
        Switch debuggerSwitch = addRootSettingsSwitch(content, "调试器检测返回false", settings.isBypassDebugger());
        Switch mockSwitch = addRootSettingsSwitch(content, "mock_location读取返回0", settings.isBypassMockLocation());
        showRootSettingsDialog(RootDiagnosticModule.DETECTION_BYPASS, content, () -> saveRootDiagnosticSettings(
                settings.withBypass(rootSwitch.isChecked(), debuggerSwitch.isChecked(), mockSwitch.isChecked())
        ));
    }

    private void showTargetHookModuleSettings(
            @NonNull LinearLayout content,
            @NonNull RootDiagnosticSettings settings
    ) {
        EditText maxMethodsInput = addRootSettingsEdit(content, "最多Hook布尔检测方法数", settings.getTargetHookMaxMethods());
        showRootSettingsDialog(RootDiagnosticModule.TARGET_APP_HOOK, content, () -> saveRootDiagnosticSettings(
                settings.withTargetHookMaxMethods(parseRootSettingInt(maxMethodsInput, "最多Hook方法数"))
        ));
    }

    private void showServiceStreamModuleSettings(
            @NonNull LinearLayout content,
            @NonNull RootDiagnosticSettings settings
    ) {
        Switch clipboardSwitch = addRootSettingsSwitch(content, "剪贴板读写返回空/阻断", settings.isServiceClipboardNull());
        Switch bluetoothSwitch = addRootSettingsSwitch(content, "蓝牙状态返回关闭", settings.isServiceBluetoothDisabled());
        Switch nfcSwitch = addRootSettingsSwitch(content, "NFC状态返回关闭", settings.isServiceNfcDisabled());
        showRootSettingsDialog(RootDiagnosticModule.SERVICE_STREAM, content, () -> saveRootDiagnosticSettings(
                settings.withServiceStream(clipboardSwitch.isChecked(), bluetoothSwitch.isChecked(), nfcSwitch.isChecked())
        ));
    }

    private void showSensorModuleSettings(
            @NonNull LinearLayout content,
            @NonNull RootDiagnosticSettings settings
    ) {
        TextView hint = new TextView(this);
        hint.setText("参考我司内部 FUCK-RUN 的正弦波步频模型：每次诊断在范围内随机选择固定步频，范围限制 140-220 SPM。");
        hint.setTextColor(Color.parseColor("#455A64"));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        content.addView(hint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        EditText minCadenceInput = addRootSettingsEdit(content, "最低步频 SPM", settings.getSensorMinCadence());
        EditText maxCadenceInput = addRootSettingsEdit(content, "最高步频 SPM", settings.getSensorMaxCadence());
        EditText waveInput = addRootSettingsEdit(content, "Z轴波形振幅", settings.getSensorWaveAmplitude());
        EditText jitterRangeInput = addRootSettingsEdit(content, "自然抖动范围 m/s²", settings.getSensorNaturalJitterRange());
        EditText jitterProbabilityInput = addRootSettingsEdit(content, "自然抖动概率 0-1", settings.getSensorNaturalJitterProbability());
        Button waveformButton = new Button(this);
        waveformButton.setText("录入/编辑传感器运动波形");
        waveformButton.setOnClickListener(v -> startActivity(new Intent(this, RootSensorWaveformActivity.class)));
        content.addView(waveformButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        showRootSettingsDialog(RootDiagnosticModule.SENSOR_INJECTION, content, () -> saveRootDiagnosticSettings(
                buildSensorSettingsFromInputs(
                        settings,
                        minCadenceInput,
                        maxCadenceInput,
                        waveInput,
                        jitterRangeInput,
                        jitterProbabilityInput
                )
        ));
    }

    @NonNull
    private RootDiagnosticSettings buildSensorSettingsFromInputs(
            @NonNull RootDiagnosticSettings dialogSettings,
            @NonNull EditText minCadenceInput,
            @NonNull EditText maxCadenceInput,
            @NonNull EditText waveInput,
            @NonNull EditText jitterRangeInput,
            @NonNull EditText jitterProbabilityInput
    ) {
        RootSensorMotionProfile latestProfile = loadLatestSensorMotionProfile(dialogSettings);
        double dialogJitterRange = parseRootSettingDouble(jitterRangeInput, "自然抖动范围");
        double dialogJitterProbability = parseRootSettingDouble(jitterProbabilityInput, "自然抖动概率");
        double nextJitterRange = nearlySame(dialogJitterRange, dialogSettings.getSensorNaturalJitterRange())
                ? latestProfile.getNaturalJitterRange()
                : dialogJitterRange;
        double nextJitterProbability = nearlySame(
                dialogJitterProbability,
                dialogSettings.getSensorNaturalJitterProbability()
        ) ? latestProfile.getNaturalJitterProbability() : dialogJitterProbability;
        return dialogSettings.withSensor(
                parseRootSettingDouble(minCadenceInput, "最低步频"),
                parseRootSettingDouble(maxCadenceInput, "最高步频"),
                parseRootSettingDouble(waveInput, "Z轴波形振幅"),
                new RootSensorMotionProfile(
                        nextJitterRange,
                        nextJitterProbability,
                        latestProfile.getWaveformSamples()
                )
        );
    }

    private boolean nearlySame(double left, double right) {
        return Math.abs(left - right) < 0.0001d;
    }

    @NonNull
    private RootSensorMotionProfile loadLatestSensorMotionProfile(@NonNull RootDiagnosticSettings fallbackSettings) {
        if (rootDiagnosticSettingsStore == null) {
            return fallbackSettings.getSensorMotionProfile();
        }
        return rootDiagnosticSettingsStore.load().getSensorMotionProfile();
    }

    @NonNull
    private LinearLayout createRootSettingsForm() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(dp(10f));
        content.setPadding(padding, padding, padding, padding);
        return content;
    }

    private void addRootSettingsText(@NonNull LinearLayout content, @NonNull String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(Color.parseColor("#455A64"));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        textView.setLineSpacing(dp(2f), 1.0f);
        content.addView(textView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    @NonNull
    private EditText addRootSettingsEdit(@NonNull LinearLayout content, @NonNull String label, double value) {
        return addRootSettingsEdit(content, label, String.format(Locale.US, "%.2f", value));
    }

    @NonNull
    private EditText addRootSettingsEdit(@NonNull LinearLayout content, @NonNull String label, int value) {
        return addRootSettingsEdit(content, label, String.valueOf(value));
    }

    @NonNull
    private EditText addRootSettingsEdit(@NonNull LinearLayout content, @NonNull String label, @NonNull String value) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.parseColor("#455A64"));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = Math.round(dp(8f));
        content.addView(labelView, labelParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        content.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return input;
    }

    @NonNull
    private Switch addRootSettingsSwitch(@NonNull LinearLayout content, @NonNull String label, boolean checked) {
        Switch switchView = new Switch(this);
        switchView.setText(label);
        switchView.setChecked(checked);
        content.addView(switchView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return switchView;
    }

    private void showRootSettingsDialog(
            @NonNull RootDiagnosticModule module,
            @NonNull LinearLayout content,
            @NonNull RootSettingsSaveAction saveAction
    ) {
        if (simulationSettingsOverlay == null) {
            showSimulationSettingsDialog();
        }
        simulationSettingsSubPageVisible = true;
        simulationSettingsActiveSaveAction = saveAction;
        if (simulationSettingsTitleView != null) {
            simulationSettingsTitleView.setText(module.getTitle() + "设置");
        }
        if (simulationSettingsPrimaryButton != null) {
            simulationSettingsPrimaryButton.setVisibility(View.VISIBLE);
            simulationSettingsPrimaryButton.setText(R.string.route_link_settings_confirm);
            simulationSettingsPrimaryButton.setOnClickListener(v -> {
                try {
                    if (simulationSettingsActiveSaveAction != null) {
                        simulationSettingsActiveSaveAction.save();
                    }
                    showSimulationSettingsHomePage();
                } catch (Exception exception) {
                    GoUtils.DisplayToast(this, exception.getMessage() == null ? "模块设置无效。" : exception.getMessage());
                }
            });
        }

        LinearLayout page = createRootSettingsForm();
        Switch moduleSwitch = createRootModuleSwitch(module);
        if (moduleSwitch != null) {
            page.addView(moduleSwitch, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }
        if (content.getParent() instanceof ViewGroup) {
            ((ViewGroup) content.getParent()).removeView(content);
        }
        page.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        setSimulationSettingsContent(scrollView);
    }

    @Nullable
    private Switch createRootModuleSwitch(@NonNull RootDiagnosticModule module) {
        RootFeature feature = rootFeatureForModule(module);
        if (feature == null) {
            return null;
        }
        Switch switchView = new Switch(this);
        switchView.setText(module.getTitle() + "模块开关");
        switchView.setChecked(isRootFeatureEnabled(feature));
        switchView.setEnabled(isRootControlsUnlocked());
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> updateRootFeatureSwitch(feature, isChecked));
        return switchView;
    }

    @Nullable
    private RootFeature rootFeatureForModule(@NonNull RootDiagnosticModule module) {
        switch (module) {
            case LOCATION_NMEA:
                return RootFeature.ROOT_NMEA_INJECTION;
            case RADIO_WIFI_SIGNAL:
                return RootFeature.SIGNAL_SIMULATION;
            case DETECTION_BYPASS:
                return RootFeature.MOCK_LOCATION_BYPASS;
            case TARGET_APP_HOOK:
                return RootFeature.TARGET_APP_HOOK;
            case SERVICE_STREAM:
                return RootFeature.SYSTEM_SERVICE_STREAM_LOG;
            case SENSOR_INJECTION:
                return RootFeature.SENSOR_EVENT_INJECTION;
            default:
                return null;
        }
    }

    private void saveRootDiagnosticSettings(@NonNull RootDiagnosticSettings settings) {
        latestRootDiagnosticSettings = settings;
        if (rootDiagnosticSettingsStore != null) {
            rootDiagnosticSettingsStore.save(settings);
        }
        appendRootAudit("更新目标APK诊断模块设置: " + settings.toJson());
        if (rootDiagnosticSessionController != null
                && rootDiagnosticSessionController.updateActiveSettings(settings)) {
            appendRootAudit("已同步运行中的LSPosed诊断模块设置。");
        }
        renderRootDiagnosticPanel();
        GoUtils.DisplayToast(this, "模块设置已保存。");
    }

    private double parseRootSettingDouble(@NonNull EditText input, @NonNull String label) {
        String value = requireRootSettingText(input, label);
        try {
            return Double.parseDouble(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "必须是数字。");
        }
    }

    private int parseRootSettingInt(@NonNull EditText input, @NonNull String label) {
        String value = requireRootSettingText(input, label);
        try {
            return Integer.parseInt(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "必须是整数。");
        }
    }

    @NonNull
    private String requireRootSettingText(@NonNull EditText input, @NonNull String label) {
        if (input.getText() == null) {
            throw new IllegalArgumentException(label + "不能为空。");
        }
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空。");
        }
        return value;
    }

    private void confirmStartRootDiagnosticSession() {
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        boolean lsposedMode = config.getInjectionFramework() == RootFeatureConfig.InjectionFramework.LSPOSED;
        if (!isRootControlsUnlocked(config)) {
            showRootModeGateToast();
            return;
        }
        if (!lsposedMode && TextUtils.isEmpty(config.getTargetPackageName())) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_diagnostic_need_target));
            return;
        }
        if (!rootTestSessionConfirmed || (!lsposedMode && !rootShellAuthorized)) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_diagnostic_need_su));
            return;
        }
        if (RootDiagnosticModule.enabledIn(config).isEmpty()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_diagnostic_no_modules));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_root_diagnostic_start_title)
                .setMessage(getString(
                        R.string.route_root_diagnostic_start_message,
                        lsposedMode ? LsposedDiagnosticBridge.SCOPE_TARGET_LABEL : config.getTargetPackageName(),
                        RootDiagnosticModule.summarizeEnabled(config)
                ))
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> startRootDiagnosticSession())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void startRootDiagnosticSession() {
        startRootDiagnosticSession(true);
    }

    private boolean startRootDiagnosticSession(boolean showToast) {
        if (rootDiagnosticSessionController == null) {
            return false;
        }
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        RootDiagnosticSettings settings = latestRootDiagnosticSettings == null
                ? RootDiagnosticSettings.defaults()
                : latestRootDiagnosticSettings;
        if (!isRootControlsUnlocked(config)) {
            if (showToast) {
                showRootModeGateToast();
            }
            return false;
        }
        settings = withLatestRouteDiagnosticLocationIfNeeded(config, settings);
        RootDiagnosticSessionController.StartResult result = rootDiagnosticSessionController.startSession(
                config,
                settings,
                event -> runOnUiThread(this::renderRootDiagnosticPanel)
        );
        appendRootAudit("目标APK诊断启动: target=" + config.getTargetPackageName()
                + ", modules=" + RootDiagnosticModule.summarizeEnabled(config)
                + ", settings=" + settings.toJson()
                + ", result=" + result.getMessage());
        renderRootDiagnosticPanel();
        updateRootAuditLog();
        if (showToast) {
            GoUtils.DisplayToast(this, result.isStarted()
                    ? getString(R.string.route_root_diagnostic_started, result.getMessage())
                    : result.getMessage());
        }
        return result.isStarted();
    }

    @NonNull
    private RootDiagnosticSettings withLatestRouteDiagnosticLocationIfNeeded(
            @NonNull RootFeatureConfig config,
            @NonNull RootDiagnosticSettings settings
    ) {
        if (!config.isEnabled(RootFeature.ROOT_NMEA_INJECTION)
                || !lastRouteDiagnosticLocationAvailable) {
            return settings;
        }
        return settings.withLocation(
                lastRouteDiagnosticLatitude,
                lastRouteDiagnosticLongitude,
                lastRouteDiagnosticSpeed,
                lastRouteDiagnosticAltitude,
                lastRouteDiagnosticBearing,
                settings.getLocationSatellites(),
                settings.getLocationHdop()
        );
    }

    private void finishRootDiagnosticSession() {
        finishRootDiagnosticSession(true);
    }

    private void finishRootDiagnosticSession(boolean showReport) {
        if (rootDiagnosticSessionController == null) {
            return;
        }
        if (!rootDiagnosticSessionController.isRunning()) {
            return;
        }
        RootDiagnosticSessionController.FinishResult result = rootDiagnosticSessionController.finishSession();
        latestRootDiagnosticReport = result.getReport();
        appendRootAudit("目标APK诊断结束: " + result.getMessage());
        renderRootDiagnosticPanel();
        updateRootAuditLog();
        if (showReport && result.getReport() != null) {
            showRootDiagnosticReport(result);
        } else if (showReport) {
            GoUtils.DisplayToast(this, result.getMessage());
        }
    }

    private void maybeStartRootDiagnosticForSimulation() {
        if (rootDiagnosticSessionController == null || rootDiagnosticSessionController.isRunning()) {
            return;
        }
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        boolean lsposedMode = config.getInjectionFramework() == RootFeatureConfig.InjectionFramework.LSPOSED;
        if (!isRootControlsUnlocked(config)
                || (!lsposedMode && TextUtils.isEmpty(config.getTargetPackageName()))
                || !config.isEnabled(RootFeature.FRIDA_DYNAMIC_INJECTION)
                || RootDiagnosticModule.enabledIn(config).isEmpty()) {
            return;
        }
        if (!rootTestSessionConfirmed || (!lsposedMode && !rootShellAuthorized)) {
            GoUtils.DisplayToast(this, lsposedMode
                    ? "目标APK诊断未启动：请先确认测试会话，并在LSPosed中启用本模块作用域。"
                    : "目标APK诊断未启动：请先在Root设置中确认测试会话并完成Root授权。");
            return;
        }
        boolean started = startRootDiagnosticSession(false);
        if (started) {
            GoUtils.DisplayToast(this, "目标APK诊断已随路线模拟自动启动。");
        }
    }

    private void syncRootDiagnosticLocation(
            double longitude,
            double latitude,
            double altitude,
            float speed,
            float bearing
    ) {
        lastRouteDiagnosticLocationAvailable = true;
        lastRouteDiagnosticLongitude = longitude;
        lastRouteDiagnosticLatitude = latitude;
        lastRouteDiagnosticAltitude = altitude;
        lastRouteDiagnosticSpeed = speed;
        lastRouteDiagnosticBearing = bearing;
        if (rootDiagnosticSessionController == null || !rootDiagnosticSessionController.isRunning()) {
            return;
        }
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        if (!isRootControlsUnlocked(config) || !config.isEnabled(RootFeature.ROOT_NMEA_INJECTION)) {
            return;
        }
        rootDiagnosticSessionController.syncRouteLocation(longitude, latitude, altitude, speed, bearing);
    }

    private void showRootDiagnosticReport(@NonNull RootDiagnosticSessionController.FinishResult result) {
        RootDiagnosticSessionReport report = result.getReport();
        if (report == null) {
            return;
        }
        File reportFile = result.getReportFile();
        String reportPath = reportFile == null ? "未保存" : reportFile.getAbsolutePath();
        String message = getString(R.string.route_root_diagnostic_report_saved, reportPath)
                + "\n\n"
                + previewText(report.toText(), 3200);
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_root_diagnostic_finished_title)
                .setMessage(message)
                .setPositiveButton(R.string.route_link_settings_confirm, null)
                .show();
    }

    @NonNull
    private String resolveAppLabel(@NonNull String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return resolveAppLabel(packageManager, applicationInfo);
        } catch (Exception ignored) {
            return packageName;
        }
    }

    @NonNull
    private String resolveAppLabel(@NonNull PackageManager packageManager, @NonNull ApplicationInfo applicationInfo) {
        try {
            CharSequence label = applicationInfo.loadLabel(packageManager);
            if (label != null && !TextUtils.isEmpty(label.toString().trim())) {
                return label.toString().trim();
            }
        } catch (Exception ignored) {
            // Fall through to package name.
        }
        return applicationInfo.packageName;
    }

    @NonNull
    private String resolveAppVersion(@NonNull String packageName) {
        return resolveAppVersion(getPackageManager(), packageName);
    }

    @NonNull
    private String resolveAppVersion(@NonNull PackageManager packageManager, @NonNull String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            String versionName = packageInfo.versionName;
            if (!TextUtils.isEmpty(versionName)) {
                return versionName;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return String.valueOf(packageInfo.getLongVersionCode());
            }
            return String.valueOf(packageInfo.versionCode);
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private void refreshRootEnvironmentReport(boolean promptWhenNotRoot) {
        if (rootEnvironmentInspector == null || ioExecutor == null || ioExecutor.isShutdown()) {
            return;
        }
        boolean environmentFeatureEnabled = isRootFeatureEnabled(RootFeature.ENVIRONMENT_INSPECTION);
        if (!environmentFeatureEnabled) {
            if (settingsRootStatusView != null) {
                settingsRootStatusView.setText("Root状态：环境检测开关关闭。");
            }
            appendRootAudit("Root环境检测跳过: featureDisabled=true");
        }
        if (settingsRootStatusView != null) {
            settingsRootStatusView.setText(R.string.route_root_status_checking);
        }
        ioExecutor.execute(() -> {
            RootEnvironmentReport report = rootEnvironmentInspector.inspect();
            appendRootAudit("刷新Root环境检测: " + report.summarizeForAudit());
            runOnUiThread(() -> {
                latestRootEnvironmentReport = report;
                renderRootEnvironmentReport();
                if (promptWhenNotRoot && !report.hasRootIndicators()) {
                    GoUtils.DisplayToast(this, getString(R.string.route_root_no_root_switch_hint));
                }
            });
        });
    }

    private void renderRootEnvironmentReport() {
        if (!renderRootAccessGate()) {
            return;
        }
        boolean internalEnabled = isInternalRootTestingEnabled();
        RootFeatureConfig config = latestRootFeatureConfig == null ? RootFeatureConfig.defaults() : latestRootFeatureConfig;
        boolean controlsUnlocked = isRootControlsUnlocked(config);
        if (settingsRootInternalStatusView != null) {
            settingsRootInternalStatusView.setText(
                    internalEnabled ? R.string.route_root_internal_enabled : R.string.route_root_internal_disabled
            );
        }
        if (settingsRootRefreshButton != null) {
            settingsRootRefreshButton.setEnabled(true);
        }
        if (settingsRootConfirmSessionButton != null) {
            settingsRootConfirmSessionButton.setEnabled(controlsUnlocked);
        }
        if (settingsRootRequestSuButton != null) {
            settingsRootRequestSuButton.setEnabled(controlsUnlocked
                    && rootTestSessionConfirmed
                    && isRootFeatureEnabled(RootFeature.ROOT_SHELL_PROBE));
        }
        if (settingsAlgorithmLabButton != null) {
            settingsAlgorithmLabButton.setEnabled(BuildConfig.ENABLE_ALGORITHM_TEST);
        }
        if (settingsTestInstructionStudioButton != null) {
            settingsTestInstructionStudioButton.setEnabled(BuildConfig.ENABLE_ALGORITHM_TEST);
        }
        if (settingsScenarioLibraryButton != null) {
            settingsScenarioLibraryButton.setEnabled(BuildConfig.ENABLE_ALGORITHM_TEST);
        }
        if (settingsPressureLabButton != null) {
            settingsPressureLabButton.setEnabled(BuildConfig.ENABLE_ALGORITHM_TEST && BuildConfig.INTERNAL_ROOT_TESTING_ENABLED);
        }
        updateRootAuthorizationStatus(null);
        updateRootAuditLog();
        renderRootFeatureConfig();

        RootEnvironmentReport report = latestRootEnvironmentReport;
        if (report == null) {
            return;
        }
        if (settingsRootStatusView != null) {
            String status = report.hasRootIndicators()
                    ? getString(R.string.route_root_status_detected)
                    : getString(R.string.route_root_status_not_detected);
            settingsRootStatusView.setText(status + "\n" + getString(
                    R.string.route_root_status_detail,
                    joinListForDisplay(report.getRootManagerPackages()),
                    joinListForDisplay(report.getSuBinaryPaths()),
                    joinListForDisplay(report.getRootShellIndicators())
            ));
        }
        if (settingsRootHiddenStatusView != null) {
            settingsRootHiddenStatusView.setText(
                    report.isHiddenRootLikely()
                            ? R.string.route_root_hidden_possible
                            : R.string.route_root_hidden_normal
            );
        }
        if (settingsRootDeveloperStatusView != null) {
            settingsRootDeveloperStatusView.setText(getString(
                    R.string.route_root_developer_status,
                    stateText(report.isDeveloperOptionsEnabled())
            ));
        }
        if (settingsRootMockStatusView != null) {
            settingsRootMockStatusView.setText(getString(
                    R.string.route_root_mock_status,
                    allowedText(report.isMockLocationAllowedForThisApp()),
                    stateText(report.isLegacyMockLocationEnabled())
            ));
        }
        if (settingsRootHookStatusView != null) {
            settingsRootHookStatusView.setText(getString(
                    R.string.route_root_hook_status,
                    joinListForDisplay(report.getHookFrameworkPackages())
            ));
        }
    }

    private boolean isInternalRootTestingEnabled() {
        return BuildConfig.INTERNAL_ROOT_TESTING_ENABLED
                && new InternalAuthStore(getApplicationContext()).canUseRootDiagnostics();
    }

    private void refreshInternalAccountProfileForRootGate() {
        if (ioExecutor == null || ioExecutor.isShutdown()) {
            return;
        }
        InternalAuthStore authStore = new InternalAuthStore(getApplicationContext());
        String token = authStore.getToken();
        if (TextUtils.isEmpty(token)) {
            renderRootAccessGate();
            return;
        }
        ioExecutor.execute(() -> {
            try {
                InternalAccountProfile profile = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getInternalAccountProfile(token);
                authStore.saveProfile(profile);
                runOnUiThread(() -> {
                    if (renderRootAccessGate()) {
                        renderRootEnvironmentReport();
                    }
                });
            } catch (Exception ignored) {
                runOnUiThread(this::renderRootAccessGate);
            }
        });
    }

    private boolean renderRootAccessGate() {
        boolean allowed = isInternalRootTestingEnabled();
        setRootContainerChildrenVisible(allowed);
        return allowed;
    }

    private void setRootContainerChildrenVisible(boolean visible) {
        if (!(settingsRootContainer instanceof ViewGroup)) {
            return;
        }
        ViewGroup rootGroup = (ViewGroup) settingsRootContainer;
        int visibility = visible ? View.VISIBLE : View.GONE;
        for (int index = 0; index < rootGroup.getChildCount(); index++) {
            rootGroup.getChildAt(index).setVisibility(visibility);
        }
    }

    private void confirmRootTestSession() {
        if (!isInternalRootTestingEnabled()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_internal));
            return;
        }
        if (!isRootControlsUnlocked()) {
            showRootModeGateToast();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_root_confirm_title)
                .setMessage(R.string.route_root_confirm_message)
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    rootTestSessionConfirmed = true;
                    rootShellAuthorized = false;
                    enableDefaultRootModulesForRouteTakeover();
                    appendRootAudit("确认Root测试会话");
                    renderRootEnvironmentReport();
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void requestRootShellAuthorization() {
        if (!isInternalRootTestingEnabled()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_internal));
            return;
        }
        if (!isRootControlsUnlocked()) {
            showRootModeGateToast();
            return;
        }
        if (!isRootFeatureEnabled(RootFeature.ROOT_SHELL_PROBE)) {
            GoUtils.DisplayToast(this, "Root授权探测开关未开启。");
            return;
        }
        if (!rootTestSessionConfirmed) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_need_session_confirm));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.route_root_request_su_title)
                .setMessage(R.string.route_root_request_su_message)
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> runRootShellProbe())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void generateRootGmTestPreview() {
        if (!isRootControlsUnlocked()
                || !rootTestSessionConfirmed
                || !isRootFeatureEnabled(RootFeature.GM_TEST_INTERFACE)) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_gm_need_enabled));
            return;
        }
        RouteDefinition routeDefinition = viewModel.getSelectedRoute().getValue();
        if (routeDefinition == null || !routeDefinition.hasEnoughPoints()) {
            GoUtils.DisplayToast(this, getString(R.string.route_root_gm_need_route));
            return;
        }
        try {
            double speedMetersPerSecond = getSelectedSimulationMode() == RouteSimulationConfig.Mode.SPEED
                    ? parseSimulationValue(speedInput)
                    : 4.0d;
            speedMetersPerSecond = Math.max(0.5d, Math.min(12.0d, speedMetersPerSecond));
            double requestedDistanceMeters = Math.max(100d, speedMetersPerSecond * 60d);
            RootGmTestData data = new RootGmTestDataGenerator().generate(
                    routeDefinition,
                    speedMetersPerSecond,
                    requestedDistanceMeters,
                    Math.max(100L, prefsStore.getLocationUpdateIntervalMillis())
            );
            appendRootAudit("GM测试数据生成: " + data.summary() + ", jsonLength=" + data.toJson().length());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.route_root_gm_title)
                    .setMessage(data.summary() + "\n\n" + previewText(data.toJson(), 1600))
                    .setPositiveButton(R.string.route_link_settings_confirm, null)
                    .show();
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage() == null ? "GM测试数据生成失败。" : exception.getMessage());
        }
    }

    @NonNull
    private String previewText(@NonNull String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...";
    }

    private void openAlgorithmTestLab() {
        if (!BuildConfig.ENABLE_ALGORITHM_TEST) {
            GoUtils.DisplayToast(this, getString(R.string.route_algorithm_lab_disabled));
            return;
        }
        try {
            Class<?> activityClass = Class.forName("com.acooldog.toolbox.AlgorithmTestLabActivity");
            Intent intent = new Intent(this, activityClass);
            startActivity(intent);
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.route_algorithm_lab_disabled));
        }
    }

    private void installDebugTestStudioButtons(@NonNull View dialogView) {
        if (!BuildConfig.ENABLE_ALGORITHM_TEST) {
            return;
        }
        View section = dialogView.findViewById(R.id.section_settings_algorithm_lab);
        if (!(section instanceof LinearLayout)) {
            return;
        }
        LinearLayout sectionLayout = (LinearLayout) section;
        settingsTestInstructionStudioButton = new Button(this);
        settingsTestInstructionStudioButton.setText("打开测试指令工作室");
        settingsTestInstructionStudioButton.setOnClickListener(v -> confirmOpenTestInstructionStudio(false));
        sectionLayout.addView(settingsTestInstructionStudioButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        settingsScenarioLibraryButton = new Button(this);
        settingsScenarioLibraryButton.setText("打开场景模板库");
        settingsScenarioLibraryButton.setOnClickListener(v -> confirmOpenTestInstructionStudio(true));
        sectionLayout.addView(settingsScenarioLibraryButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        settingsPressureLabButton = new Button(this);
        settingsPressureLabButton.setText("打开反作弊压力测试平台");
        settingsPressureLabButton.setOnClickListener(v -> confirmOpenInternalPressureLab());
        sectionLayout.addView(settingsPressureLabButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    private void confirmOpenTestInstructionStudio(boolean openLibrary) {
        if (!BuildConfig.DEBUG || !BuildConfig.ENABLE_ALGORITHM_TEST || !new InternalAuthStore(getApplicationContext()).isLoggedIn()) {
            GoUtils.DisplayToast(this, "测试指令工作室仅在DEBUG模式对内测人员开放。");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("开启测试指令工作室")
                .setMessage("此功能仅用于内部算法验证，不注入系统。")
                .setPositiveButton(R.string.route_link_settings_confirm, (firstDialog, firstWhich) ->
                        new AlertDialog.Builder(this)
                                .setTitle("开启测试指令工作室")
                                .setMessage("所有操作将被记录到加密审计日志。")
                                .setPositiveButton(R.string.route_link_settings_confirm, (secondDialog, secondWhich) ->
                                        openTestInstructionStudio(openLibrary))
                                .setNegativeButton(R.string.route_link_settings_cancel, null)
                                .show())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void openTestInstructionStudio(boolean openLibrary) {
        try {
            Class<?> activityClass = Class.forName("com.acooldog.toolbox.TestInstructionStudioActivity");
            Intent intent = new Intent(this, activityClass);
            intent.putExtra("open_library", openLibrary);
            startActivity(intent);
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.route_algorithm_lab_disabled));
        }
    }

    private void confirmOpenInternalPressureLab() {
        if (!BuildConfig.DEBUG
                || !BuildConfig.ENABLE_ALGORITHM_TEST
                || !BuildConfig.INTERNAL_ROOT_TESTING_ENABLED
                || !new InternalAuthStore(getApplicationContext()).isLoggedIn()) {
            GoUtils.DisplayToast(this, "反作弊压力测试平台仅在DEBUG内测账号下开放。");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("开启反作弊压力测试平台")
                .setMessage("该平台仅生成FOR TESTING ONLY测试报告，不执行系统注入、Hook加载、信号伪造或隐藏能力。")
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> openInternalPressureLab())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void openInternalPressureLab() {
        try {
            Class<?> activityClass = Class.forName("com.acooldog.toolbox.InternalPressureLabActivity");
            startActivity(new Intent(this, activityClass));
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.route_algorithm_lab_disabled));
        }
    }

    private void runRootShellProbe() {
        if (rootEnvironmentInspector == null || ioExecutor == null || ioExecutor.isShutdown()) {
            return;
        }
        if (!isRootFeatureEnabled(RootFeature.ROOT_SHELL_PROBE)) {
            GoUtils.DisplayToast(this, "Root授权探测开关未开启。");
            return;
        }
        if (settingsRootRequestSuButton != null) {
            settingsRootRequestSuButton.setEnabled(false);
        }
        if (settingsRootAuthorizationStatusView != null) {
            settingsRootAuthorizationStatusView.setText(R.string.route_root_status_checking);
        }
        ioExecutor.execute(() -> {
            RootShellProbeResult result = rootEnvironmentInspector.requestRootShellProbe();
            appendRootAudit("Root授权探测: authorized=" + result.isAuthorized()
                    + ", timedOut=" + result.isTimedOut()
                    + ", exitCode=" + result.getExitCode()
                    + ", output=" + result.getOutput());
            runOnUiThread(() -> {
                rootShellAuthorized = result.isAuthorized();
                updateRootAuthorizationStatus(result);
                renderRootDiagnosticPanel();
                updateRootAuditLog();
                if (settingsRootRequestSuButton != null) {
                    settingsRootRequestSuButton.setEnabled(isInternalRootTestingEnabled()
                            && rootTestSessionConfirmed
                            && isRootFeatureEnabled(RootFeature.ROOT_SHELL_PROBE));
                }
            });
        });
    }

    private void updateRootAuthorizationStatus(@Nullable RootShellProbeResult result) {
        if (settingsRootAuthorizationStatusView == null) {
            return;
        }
        if (result != null) {
            if (result.isTimedOut()) {
                settingsRootAuthorizationStatusView.setText(R.string.route_root_authorization_timeout);
            } else if (result.isAuthorized()) {
                settingsRootAuthorizationStatusView.setText(buildRootAuthorizationMessage(
                        getString(R.string.route_root_authorization_granted),
                        result.getOutput()
                ));
            } else {
                settingsRootAuthorizationStatusView.setText(buildRootAuthorizationMessage(
                        getString(R.string.route_root_authorization_denied, result.getExitCode()),
                        result.getOutput()
                ));
            }
            return;
        }
        if (rootShellAuthorized) {
            settingsRootAuthorizationStatusView.setText(R.string.route_root_authorization_granted);
        } else if (rootTestSessionConfirmed) {
            settingsRootAuthorizationStatusView.setText(R.string.route_root_authorization_session_confirmed);
        } else {
            settingsRootAuthorizationStatusView.setText(R.string.route_root_authorization_not_requested);
        }
    }

    @NonNull
    private String buildRootAuthorizationMessage(@NonNull String status, @NonNull String output) {
        if (TextUtils.isEmpty(output)) {
            return status;
        }
        return status + "\n" + getString(R.string.route_root_authorization_output, output);
    }

    private void updateRootAuditLog() {
        if (settingsRootAuditLogView == null || rootAuditLogger == null) {
            return;
        }
        List<String> entries = rootAuditLogger.getRecentEntries();
        if (entries.isEmpty()) {
            settingsRootAuditLogView.setText(R.string.route_root_audit_empty);
            return;
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(entries.size(), 8);
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(entries.get(index));
        }
        settingsRootAuditLogView.setText(builder.toString());
    }

    @NonNull
    private String joinListForDisplay(@NonNull List<String> values) {
        if (values.isEmpty()) {
            return getString(R.string.route_root_package_none);
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    @NonNull
    private String stateText(boolean enabled) {
        return getString(enabled ? R.string.route_root_enabled_state_on : R.string.route_root_enabled_state_off);
    }

    @NonNull
    private String allowedText(boolean allowed) {
        return getString(allowed ? R.string.route_root_allowed_state_yes : R.string.route_root_allowed_state_no);
    }

    private void saveSimulationSettings() {
        try {
            String ratioNumerator = requireNonEmpty(settingsLinkRatioLeftInput, getString(R.string.route_link_invalid_ratio));
            String stepsPerMeter = requireNonEmpty(settingsStepsPerMeterInput, getString(R.string.route_link_steps_per_meter_invalid));
            parsePositiveDouble(ratioNumerator, getString(R.string.route_link_invalid_ratio));
            parsePositiveDouble(stepsPerMeter, getString(R.string.route_link_steps_per_meter_invalid));
            int loopCount = parseLoopCount(settingsLoopInput);
            boolean dynamicIntensityEnabled = settingsDynamicIntensitySwitch != null
                    && settingsDynamicIntensitySwitch.isChecked();
            boolean naturalPathVariationEnabled = settingsPathVariationSwitch != null
                    && settingsPathVariationSwitch.isChecked();
            boolean naturalAltitudeVariationEnabled = settingsAltitudeVariationSwitch != null
                    && settingsAltitudeVariationSwitch.isChecked();
            String intensityRange = resolveNumericSetting(
                    settingsIntensityRangeInput,
                    prefsStore.getRouteIntensityVariationRange(),
                    getString(R.string.route_dynamic_intensity_range_invalid)
            );
            String pathVariationAmplitude = resolveNumericSetting(
                    settingsPathVariationInput,
                    prefsStore.getRoutePathVariationAmplitude(),
                    getString(R.string.route_path_variation_invalid)
            );
            String altitudeBaseMeters = resolveNonNegativeSetting(
                    settingsAltitudeBaseInput,
                    prefsStore.getRouteAltitudeBaseMeters(),
                    getString(R.string.route_altitude_base_invalid)
            );
            String altitudeVariationRange = resolveNonNegativeSetting(
                    settingsAltitudeVariationRangeInput,
                    prefsStore.getRouteAltitudeVariationRange(),
                    getString(R.string.route_altitude_variation_range_invalid)
            );
            String altitudeHeight = resolveNonNegativeSetting(
                    settingsAltitudeHeightInput,
                    prefsStore.getRouteAltitudeVariationHeightCm(),
                    getString(R.string.route_altitude_variation_height_invalid)
            );
            float intensityFrequency = settingsIntensityFrequencySeekBar == null
                    ? prefsStore.getRouteIntensityVariationFrequency()
                    : settingsIntensityFrequencySeekBar.getProgress() / 100f;
            float altitudeVariationProbability = settingsAltitudeProbabilitySeekBar == null
                    ? prefsStore.getRouteAltitudeVariationProbability()
                    : settingsAltitudeProbabilitySeekBar.getProgress() / 100f;
            boolean floatingWindowEnabled = settingsFloatingWindowSwitch != null && settingsFloatingWindowSwitch.isChecked();
            float floatingWindowScale = settingsFloatingWindowScaleSeekBar == null
                    ? prefsStore.getRouteFloatingWindowScale()
                    : settingsFloatingWindowScaleSeekBar.getProgress() / 100f;
            float floatingWindowButtonSize = settingsFloatingWindowButtonSizeSeekBar == null
                    ? prefsStore.getRouteFloatingWindowButtonSizeDp()
                    : settingsFloatingWindowButtonSizeSeekBar.getProgress();
            int satelliteCount = settingsSatelliteSeekBar == null
                    ? prefsStore.getNmeaSatelliteCount()
                    : settingsSatelliteSeekBar.getProgress() + 1;
            int signalQuality = selectedSignalQuality();
            float hdop = settingsHdopSeekBar == null
                    ? prefsStore.getNmeaHdop()
                    : hdopProgressToValue(settingsHdopSeekBar.getProgress());
            int updateIntervalMillis = settingsUpdateIntervalSeekBar == null
                    ? prefsStore.getLocationUpdateIntervalMillis()
                    : updateIntervalProgressToValue(settingsUpdateIntervalSeekBar.getProgress());
            boolean networkSimulationEnabled = settingsNetworkSimulationSwitch == null
                    ? prefsStore.isNetworkSimulationEnabled()
                    : settingsNetworkSimulationSwitch.isChecked();
            if (floatingWindowEnabled && !Settings.canDrawOverlays(getApplicationContext())) {
                GoUtils.showEnableFloatWindowDialog(this);
                throw new IllegalArgumentException(getString(R.string.route_floating_window_permission_required));
            }

            loopInput.setText(String.valueOf(loopCount));
            speedFloatCheckBox.setChecked(dynamicIntensityEnabled);
            prefsStore.saveRouteStepsPerMeter(stepsPerMeter);
            prefsStore.saveRouteIntensityVariationSettings(intensityRange, intensityFrequency);
            prefsStore.saveRoutePathVariationSettings(naturalPathVariationEnabled, pathVariationAmplitude);
            prefsStore.saveRouteAltitudeVariationSettings(
                    naturalAltitudeVariationEnabled,
                    altitudeBaseMeters,
                    altitudeVariationRange,
                    altitudeHeight,
                    altitudeVariationProbability
            );
            prefsStore.saveRouteFloatingWindowSettings(floatingWindowEnabled, floatingWindowScale, floatingWindowButtonSize);
            prefsStore.saveNmeaSettings(
                    satelliteCount,
                    signalQuality,
                    hdop,
                    updateIntervalMillis,
                    networkSimulationEnabled
            );
            persistSimulationPrefsWithRatio(ratioNumerator);
            applySimulationConfigHotIfPossible();
            if (serviceBinder != null) {
                serviceBinder.reloadSimulationSettings();
            }
            refreshFloatingWindowSizeFromPrefs();
            if (!floatingWindowEnabled) {
                hideFloatingWindow();
            }
            renderToggleButtonState();
            if (simulationSettingsOverlay != null) {
                hideSimulationSettingsOverlay();
            }
        } catch (IllegalArgumentException exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void promptUploadSimulationSettings() {
        EditText nameInput = new EditText(this);
        nameInput.setHint(R.string.route_settings_upload_hint);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_settings_upload_title)
                .setView(nameInput)
                .setPositiveButton(R.string.route_settings_upload_button, null)
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                GoUtils.DisplayToast(this, getString(R.string.route_settings_upload_hint));
                return;
            }
            dialog.dismiss();
            uploadSimulationSettings(name);
        }));
        dialog.show();
    }

    private void uploadSimulationSettings(String name) {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        try {
            RouteSimulationConfig config = buildSimulationConfigForDialogInputs();
            String token = new InternalAuthStore(getApplicationContext()).getToken();
            ioExecutor.execute(() -> {
                try {
                    ShareModule.from(getApplicationContext())
                            .shareApiClient()
                            .uploadSharedSimulationConfig(name, config, token);
                    runOnUiThread(() -> GoUtils.DisplayToast(this, getString(R.string.route_settings_upload_success)));
                } catch (Exception exception) {
                    runOnUiThread(() -> GoUtils.DisplayToast(
                            this,
                            buildDetailedToast(R.string.route_settings_upload_failed, exception)
                    ));
                }
            });
        } catch (IllegalArgumentException exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private RouteSimulationConfig buildSimulationConfigForDialogInputs() {
        RouteSimulationConfig.Mode simulationMode = getSelectedSimulationMode();
        double speed = simulationMode == RouteSimulationConfig.Mode.SPEED
                ? parseSimulationValue(speedInput)
                : 0d;
        double cadence = simulationMode == RouteSimulationConfig.Mode.CADENCE
                ? parseSimulationValue(cadenceInput)
                : 0d;
        int loopCount = parseLoopCount(settingsLoopInput);
        boolean dynamicIntensityEnabled = settingsDynamicIntensitySwitch != null && settingsDynamicIntensitySwitch.isChecked();
        boolean naturalPathVariationEnabled = settingsPathVariationSwitch != null && settingsPathVariationSwitch.isChecked();
        boolean naturalAltitudeVariationEnabled = settingsAltitudeVariationSwitch != null
                && settingsAltitudeVariationSwitch.isChecked();
        String intensityRange = resolveNumericSetting(
                settingsIntensityRangeInput,
                prefsStore.getRouteIntensityVariationRange(),
                getString(R.string.route_dynamic_intensity_range_invalid)
        );
        String pathVariationAmplitude = resolveNumericSetting(
                settingsPathVariationInput,
                prefsStore.getRoutePathVariationAmplitude(),
                getString(R.string.route_path_variation_invalid)
        );
        String altitudeBaseMeters = resolveNonNegativeSetting(
                settingsAltitudeBaseInput,
                prefsStore.getRouteAltitudeBaseMeters(),
                getString(R.string.route_altitude_base_invalid)
        );
        String altitudeVariationRange = resolveNonNegativeSetting(
                settingsAltitudeVariationRangeInput,
                prefsStore.getRouteAltitudeVariationRange(),
                getString(R.string.route_altitude_variation_range_invalid)
        );
        String altitudeHeight = resolveNonNegativeSetting(
                settingsAltitudeHeightInput,
                prefsStore.getRouteAltitudeVariationHeightCm(),
                getString(R.string.route_altitude_variation_height_invalid)
        );
        float intensityFrequency = settingsIntensityFrequencySeekBar == null
                ? prefsStore.getRouteIntensityVariationFrequency()
                : settingsIntensityFrequencySeekBar.getProgress() / 100f;
        float altitudeVariationProbability = settingsAltitudeProbabilitySeekBar == null
                ? prefsStore.getRouteAltitudeVariationProbability()
                : settingsAltitudeProbabilitySeekBar.getProgress() / 100f;
        String ratioNumerator = requireNonEmpty(settingsLinkRatioLeftInput, getString(R.string.route_link_invalid_ratio));
        String stepsPerMeter = requireNonEmpty(settingsStepsPerMeterInput, getString(R.string.route_link_steps_per_meter_invalid));
        return new RouteSimulationConfig(
                simulationMode,
                speed,
                cadence,
                loopCount,
                dynamicIntensityEnabled,
                parsePositiveDouble(intensityRange, getString(R.string.route_dynamic_intensity_range_invalid)),
                intensityFrequency,
                naturalPathVariationEnabled,
                parsePositiveDouble(pathVariationAmplitude, getString(R.string.route_path_variation_invalid)),
                naturalAltitudeVariationEnabled,
                parseNonNegativeDouble(altitudeBaseMeters, getString(R.string.route_altitude_base_invalid)),
                parseNonNegativeDouble(altitudeVariationRange, getString(R.string.route_altitude_variation_range_invalid)),
                parseNonNegativeDouble(altitudeHeight, getString(R.string.route_altitude_variation_height_invalid)),
                altitudeVariationProbability,
                parsePositiveDouble(ratioNumerator, getString(R.string.route_link_invalid_ratio)),
                parsePositiveDouble(stepsPerMeter, getString(R.string.route_link_steps_per_meter_invalid)),
                1000L
        );
    }

    private void loadSharedSimulationSettings() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getString(R.string.app_error_network));
            return;
        }
        GoUtils.DisplayToast(this, getString(R.string.route_settings_download_loading));
        ioExecutor.execute(() -> {
            try {
                List<SharedSimulationConfigEntry> items = ShareModule.from(getApplicationContext())
                        .shareApiClient()
                        .getSharedSimulationConfigs("");
                runOnUiThread(() -> showSharedSimulationConfigPicker(items));
            } catch (Exception exception) {
                runOnUiThread(() -> GoUtils.DisplayToast(
                        this,
                        buildDetailedToast(R.string.route_settings_download_failed, exception)
                ));
            }
        });
    }

    private void showSharedSimulationConfigPicker(List<SharedSimulationConfigEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            GoUtils.DisplayToast(this, getString(R.string.route_settings_download_empty));
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_picker, null);
        EditText searchInput = dialogView.findViewById(R.id.searchable_picker_input);
        ListView listView = dialogView.findViewById(R.id.searchable_picker_list);
        LinearLayout letterRail = dialogView.findViewById(R.id.searchable_picker_letter_rail);
        List<SearchableSimulationConfigItem> allItems = buildSimulationConfigItems(entries);
        List<SearchableSimulationConfigItem> filteredItems = new ArrayList<>(allItems);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        fillSimulationConfigAdapter(adapter, filteredItems);
        listView.setAdapter(adapter);
        updatePickerLetterRail(letterRail, listView, filteredItems);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_settings_download_title)
                .setView(dialogView)
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> resizeDialogWindow(dialog, 0.82f, 0.70f));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (filteredItems.isEmpty()) {
                return;
            }
            dialog.dismiss();
            applySharedSimulationConfig(filteredItems.get(position).entry);
        });
        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterSimulationConfigItems(editable == null ? "" : editable.toString(), allItems, filteredItems, adapter);
                updatePickerLetterRail(letterRail, listView, filteredItems);
            }
        });
        dialog.show();
    }

    private void applySharedSimulationConfig(SharedSimulationConfigEntry entry) {
        if (entry == null) {
            return;
        }
        simulationModeSpinner.setSelection("cadence".equalsIgnoreCase(entry.getMode()) ? 1 : 0, false);
        speedInput.setText(trimDouble(entry.getSpeed()));
        cadenceInput.setText(trimDouble(entry.getCadence()));
        loopInput.setText(String.valueOf(entry.getLoopCount()));
        speedFloatCheckBox.setChecked(entry.isDynamicIntensityEnabled());
        prefsStore.saveRouteIntensityVariationSettings(trimDouble(entry.getIntensityVariationRange()), (float) entry.getIntensityVariationFrequency());
        prefsStore.saveRoutePathVariationSettings(entry.isNaturalPathVariationEnabled(), trimDouble(entry.getPathVariationAmplitude()));
        prefsStore.saveRouteAltitudeVariationSettings(
                entry.isNaturalAltitudeVariationEnabled(),
                trimDouble(entry.getAltitudeBaseMeters()),
                trimDouble(entry.getAltitudeVariationRange()),
                trimDouble(entry.getAltitudeVariationHeightCentimeters()),
                (float) entry.getAltitudeVariationProbability()
        );
        prefsStore.saveRouteStepsPerMeter(trimDouble(entry.getStepsPerMeter()));
        persistSimulationPrefsWithRatio(trimDouble(entry.getLinkRatioNumerator()));
        if (settingsLinkRatioLeftInput != null) {
            settingsLinkRatioLeftInput.setText(trimDouble(entry.getLinkRatioNumerator()));
        }
        if (settingsLinkRatioRightInput != null) {
            settingsLinkRatioRightInput.setText(trimDouble(entry.getSpeed()));
        }
        if (settingsStepsPerMeterInput != null) {
            settingsStepsPerMeterInput.setText(trimDouble(entry.getStepsPerMeter()));
        }
        if (settingsLoopInput != null) {
            settingsLoopInput.setText(String.valueOf(entry.getLoopCount()));
        }
        if (settingsDynamicIntensitySwitch != null) {
            settingsDynamicIntensitySwitch.setChecked(entry.isDynamicIntensityEnabled());
        }
        if (settingsIntensityRangeInput != null) {
            settingsIntensityRangeInput.setText(trimDouble(entry.getIntensityVariationRange()));
        }
        if (settingsIntensityFrequencySeekBar != null) {
            settingsIntensityFrequencySeekBar.setProgress(Math.round((float) entry.getIntensityVariationFrequency() * 100f));
            updateIntensityFrequencyValue(settingsIntensityFrequencySeekBar.getProgress());
        }
        if (settingsPathVariationSwitch != null) {
            settingsPathVariationSwitch.setChecked(entry.isNaturalPathVariationEnabled());
        }
        if (settingsPathVariationInput != null) {
            settingsPathVariationInput.setText(trimDouble(entry.getPathVariationAmplitude()));
        }
        if (settingsAltitudeVariationSwitch != null) {
            settingsAltitudeVariationSwitch.setChecked(entry.isNaturalAltitudeVariationEnabled());
        }
        if (settingsAltitudeBaseInput != null) {
            settingsAltitudeBaseInput.setText(trimDouble(entry.getAltitudeBaseMeters()));
        }
        if (settingsAltitudeVariationRangeInput != null) {
            settingsAltitudeVariationRangeInput.setText(trimDouble(entry.getAltitudeVariationRange()));
        }
        if (settingsAltitudeHeightInput != null) {
            settingsAltitudeHeightInput.setText(trimDouble(entry.getAltitudeVariationHeightCentimeters()));
        }
        if (settingsAltitudeProbabilitySeekBar != null) {
            settingsAltitudeProbabilitySeekBar.setProgress(Math.round((float) entry.getAltitudeVariationProbability() * 100f));
            updateAltitudeProbabilityValue(settingsAltitudeProbabilitySeekBar.getProgress());
        }
        updateSimulationModeViews(getSelectedSimulationMode());
        applySimulationConfigHotIfPossible();
        GoUtils.DisplayToast(this, getString(R.string.route_settings_apply_success));
    }

    private String trimDouble(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001d) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.getDefault(), "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void persistSimulationPrefsWithRatio(String ratioNumerator) {
        RouteDefinition selectedRoute = viewModel.getSelectedRoute().getValue();
        prefsStore.saveRouteConfig(
                getSelectedSimulationMode() == RouteSimulationConfig.Mode.CADENCE
                        ? SimulationPrefsStore.ROUTE_MODE_CADENCE
                        : SimulationPrefsStore.ROUTE_MODE_SPEED,
                speedInput.getText() == null ? "" : speedInput.getText().toString(),
                cadenceInput.getText() == null ? "" : cadenceInput.getText().toString(),
                loopInput.getText() == null ? "" : loopInput.getText().toString(),
                speedFloatCheckBox.isChecked(),
                selectedRoute == null ? "" : selectedRoute.getId(),
                ratioNumerator
        );
    }

    private String requireNonEmpty(@Nullable EditText input, String errorMessage) {
        if (input == null || input.getText() == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        String raw = input.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return raw;
    }

    private double parsePositiveDouble(String raw, String errorMessage) {
        try {
            double value = Double.parseDouble(raw);
            if (value <= 0d) {
                throw new IllegalArgumentException(errorMessage);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private double parseNonNegativeDouble(String raw, String errorMessage) {
        try {
            double value = Double.parseDouble(raw);
            if (value < 0d) {
                throw new IllegalArgumentException(errorMessage);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String resolveNumericSetting(@Nullable EditText input, String fallback, String errorMessage) {
        if (input == null || input.getText() == null) {
            return fallback;
        }
        String raw = input.getText().toString().trim();
        String resolved = TextUtils.isEmpty(raw) ? fallback : raw;
        parsePositiveDouble(resolved, errorMessage);
        return resolved;
    }

    private String resolveNonNegativeSetting(@Nullable EditText input, String fallback, String errorMessage) {
        if (input == null || input.getText() == null) {
            return fallback;
        }
        String raw = input.getText().toString().trim();
        String resolved = TextUtils.isEmpty(raw) ? fallback : raw;
        parseNonNegativeDouble(resolved, errorMessage);
        return resolved;
    }

    private void updateIntensityFrequencyValue(int progress) {
        if (settingsIntensityFrequencyValueView == null) {
            return;
        }
        settingsIntensityFrequencyValueView.setText(
                getString(R.string.route_dynamic_intensity_frequency_value, progress / 100f)
        );
    }

    private void updateSatelliteValue(int progress) {
        if (settingsSatelliteValueView == null) {
            return;
        }
        int satelliteCount = Math.max(
                SimulationPrefsStore.MIN_NMEA_SATELLITE_COUNT,
                Math.min(SimulationPrefsStore.MAX_NMEA_SATELLITE_COUNT, progress + 1)
        );
        settingsSatelliteValueView.setText(getString(R.string.route_nmea_satellite_value, satelliteCount));
    }

    private void updateHdopValue(int progress) {
        if (settingsHdopValueView == null) {
            return;
        }
        settingsHdopValueView.setText(getString(R.string.route_nmea_hdop_value, hdopProgressToValue(progress)));
    }

    private void updateUpdateIntervalValue(int progress) {
        if (settingsUpdateIntervalValueView == null) {
            return;
        }
        settingsUpdateIntervalValueView.setText(
                getString(R.string.route_nmea_update_interval_value, updateIntervalProgressToValue(progress))
        );
    }

    private float hdopProgressToValue(int progress) {
        float value = progress / 10f;
        return Math.max(
                SimulationPrefsStore.MIN_NMEA_HDOP,
                Math.min(SimulationPrefsStore.MAX_NMEA_HDOP, value)
        );
    }

    private int updateIntervalProgressToValue(int progress) {
        int value = (progress + 1) * 100;
        return Math.max(
                SimulationPrefsStore.MIN_LOCATION_UPDATE_INTERVAL_MS,
                Math.min(SimulationPrefsStore.MAX_LOCATION_UPDATE_INTERVAL_MS, value)
        );
    }

    private int updateIntervalToProgress(int updateIntervalMillis) {
        int clamped = Math.max(
                SimulationPrefsStore.MIN_LOCATION_UPDATE_INTERVAL_MS,
                Math.min(SimulationPrefsStore.MAX_LOCATION_UPDATE_INTERVAL_MS, updateIntervalMillis)
        );
        return (clamped / 100) - 1;
    }

    private int signalQualityToButtonId(int signalQuality) {
        if (signalQuality == SimulationPrefsStore.MIN_NMEA_SIGNAL_QUALITY) {
            return R.id.rb_dialog_signal_weak;
        }
        if (signalQuality == SimulationPrefsStore.DEFAULT_NMEA_SIGNAL_QUALITY) {
            return R.id.rb_dialog_signal_strong;
        }
        return R.id.rb_dialog_signal_medium;
    }

    private int selectedSignalQuality() {
        if (settingsSignalQualityGroup == null) {
            return prefsStore.getNmeaSignalQuality();
        }
        int checkedId = settingsSignalQualityGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_dialog_signal_weak) {
            return SimulationPrefsStore.MIN_NMEA_SIGNAL_QUALITY;
        }
        if (checkedId == R.id.rb_dialog_signal_medium) {
            return 1;
        }
        return SimulationPrefsStore.DEFAULT_NMEA_SIGNAL_QUALITY;
    }

    private void updateAltitudeProbabilityValue(int progress) {
        if (settingsAltitudeProbabilityValueView == null) {
            return;
        }
        settingsAltitudeProbabilityValueView.setText(
                getString(R.string.route_altitude_variation_probability_value, progress / 100f)
        );
    }

    private void updateFloatingWindowPreview() {
        int scaleProgress = settingsFloatingWindowScaleSeekBar == null
                ? Math.round(prefsStore.getRouteFloatingWindowScale() * 100f)
                : settingsFloatingWindowScaleSeekBar.getProgress();
        int buttonSizeProgress = settingsFloatingWindowButtonSizeSeekBar == null
                ? Math.round(prefsStore.getRouteFloatingWindowButtonSizeDp())
                : settingsFloatingWindowButtonSizeSeekBar.getProgress();
        int clamped = Math.max(35, Math.min(90, scaleProgress));
        int buttonSize = Math.max(32, Math.min(72, buttonSizeProgress));
        if (settingsFloatingWindowScaleView != null) {
            settingsFloatingWindowScaleView.setText(getString(R.string.route_floating_window_scale_format, clamped));
        }
        if (settingsFloatingWindowButtonSizeView != null) {
            settingsFloatingWindowButtonSizeView.setText(getString(R.string.route_floating_window_button_size_format, buttonSize));
        }
        if (settingsFloatingWindowPreview != null) {
            ViewGroup.LayoutParams params = settingsFloatingWindowPreview.getLayoutParams();
            if (params != null) {
                params.width = Math.round(dp(clamped * 2.1f));
                params.height = Math.round(dp(clamped * 1.45f));
                settingsFloatingWindowPreview.setLayoutParams(params);
            }
            TextView pausePreview = settingsFloatingWindowPreview.findViewById(R.id.tv_dialog_floating_preview_pause);
            TextView resumePreview = settingsFloatingWindowPreview.findViewById(R.id.tv_dialog_floating_preview_resume);
            updatePreviewButtonSize(pausePreview, buttonSize);
            updatePreviewButtonSize(resumePreview, buttonSize);
        }
    }

    private void updatePreviewButtonSize(@Nullable TextView button, int buttonSizeDp) {
        if (button == null) {
            return;
        }
        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params != null) {
            params.width = Math.round(dp(buttonSizeDp));
            params.height = Math.round(dp(Math.max(24, Math.round(buttonSizeDp * 0.56f))));
            button.setLayoutParams(params);
        }
    }

    private void resizeDialogWindow(@Nullable AlertDialog dialog, float widthRatio, float heightRatio) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        int width = Math.round(getResources().getDisplayMetrics().widthPixels * widthRatio);
        int height = Math.round(getResources().getDisplayMetrics().heightPixels * heightRatio);
        dialog.getWindow().setLayout(width, height);
    }

    private void setupSimulationSettingsNavigator(@NonNull View dialogView) {
        ScrollView scrollView = dialogView.findViewById(R.id.scroll_dialog_settings);
        EditText searchInput = dialogView.findViewById(R.id.et_dialog_settings_search);
        LinearLayout letterRail = dialogView.findViewById(R.id.layout_dialog_settings_letter_rail);
        if (scrollView == null || searchInput == null || letterRail == null) {
            return;
        }

        List<SettingsSection> sections = new ArrayList<>();
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_link),
                getString(R.string.route_settings_letter_link),
                "联动 比例 link ratio"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_steps),
                getString(R.string.route_settings_letter_steps),
                "步数 步频 米数 steps"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_loop),
                getString(R.string.route_settings_letter_loop),
                "循环 次数 loop"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_nmea),
                getString(R.string.route_settings_letter_nmea),
                "NMEA GPS 卫星 信号 精度 更新 interval"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_dynamic),
                getString(R.string.route_settings_letter_dynamic),
                "强度 浮动 dynamic intensity"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_path),
                getString(R.string.route_settings_letter_path),
                "路径 自然 path"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_altitude),
                getString(R.string.route_settings_letter_altitude),
                "altitude height"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_floating),
                getString(R.string.route_settings_letter_floating),
                "悬浮窗 floating overlay"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_ringtone),
                getString(R.string.route_settings_letter_ringtone),
                "铃声 提醒 ringtone"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_share),
                getString(R.string.route_settings_letter_share),
                "共享 上传 下载 share"
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.layout_dialog_settings_root_container),
                getString(R.string.route_settings_letter_root),
                "Root 授权 检测 审计 Hook 开发者选项 模拟位置 算法 验证 实验室 测试数据 管理 步频 GPS 传感器 一致性 测试指令 工作室 场景模板 回放 压力测试 反作弊 NMEA 环境",
                true
        ));
        sections.add(new SettingsSection(
                dialogView.findViewById(R.id.section_settings_algorithm_lab),
                getString(R.string.route_settings_letter_root),
                "算法 验证 实验室 步频 GPS 传感器 一致性 测试数据 管理 测试指令 工作室 场景模板 回放 压力测试 反作弊 NMEA 环境",
                true
        ));

        letterRail.removeAllViews();
        for (SettingsSection section : sections) {
            if (section.view == null) {
                continue;
            }
            TextView letterView = new TextView(this);
            letterView.setText(section.letter);
            letterView.setTextColor(Color.parseColor("#54657E"));
            letterView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            letterView.setGravity(Gravity.CENTER);
            letterView.setPadding(0, Math.round(dp(4f)), 0, Math.round(dp(4f)));
            letterView.setOnClickListener(v -> scrollView.post(() -> scrollView.smoothScrollTo(0, section.view.getTop())));
            letterRail.addView(letterView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }

        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable == null ? "" : editable.toString().trim().toLowerCase(Locale.getDefault());
                boolean rootMode = settingsRootContainer != null && settingsRootContainer.getVisibility() == View.VISIBLE;
                View firstMatch = null;
                for (SettingsSection section : sections) {
                    if (section.view == null) {
                        continue;
                    }
                    boolean matched = TextUtils.isEmpty(query)
                            || section.keywords.toLowerCase(Locale.getDefault()).contains(query)
                            || section.letter.toLowerCase(Locale.getDefault()).contains(query);
                    boolean modeMatched = rootMode == section.rootOnly;
                    section.view.setVisibility(matched && modeMatched ? View.VISIBLE : View.GONE);
                    if (matched && modeMatched && firstMatch == null) {
                        firstMatch = section.view;
                    }
                }
                if (firstMatch != null) {
                    View target = firstMatch;
                    scrollView.post(() -> scrollView.smoothScrollTo(0, target.getTop()));
                } else {
                    scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
                }
            }
        });
    }

    private void updatePickerLetterRail(
            @Nullable LinearLayout letterRail,
            @Nullable ListView listView,
            @NonNull List<? extends PickerIndexable> items
    ) {
        if (letterRail == null || listView == null) {
            return;
        }
        letterRail.removeAllViews();
        String lastLetter = null;
        for (PickerIndexable item : items) {
            if (item == null) {
                continue;
            }
            String letter = resolvePickerLetter(item.getSortKey());
            if (TextUtils.equals(lastLetter, letter)) {
                continue;
            }
            lastLetter = letter;
            TextView letterView = new TextView(this);
            letterView.setText(letter);
            letterView.setTextColor(Color.parseColor("#54657E"));
            letterView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            letterView.setGravity(Gravity.CENTER);
            letterView.setPadding(0, Math.round(dp(4f)), 0, Math.round(dp(4f)));
            letterView.setOnClickListener(v -> {
                int targetIndex = findFirstIndexForLetter(items, letter);
                if (targetIndex >= 0) {
                    listView.setSelection(targetIndex);
                }
            });
            letterRail.addView(letterView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    private int findFirstIndexForLetter(@NonNull List<? extends PickerIndexable> items, @NonNull String targetLetter) {
        for (int index = 0; index < items.size(); index++) {
            PickerIndexable item = items.get(index);
            if (item != null && TextUtils.equals(resolvePickerLetter(item.getSortKey()), targetLetter)) {
                return index;
            }
        }
        return -1;
    }

    @NonNull
    private String resolvePickerLetter(@Nullable String sortKey) {
        if (TextUtils.isEmpty(sortKey)) {
            return "#";
        }
        char firstChar = Character.toUpperCase(sortKey.charAt(0));
        return firstChar >= 'A' && firstChar <= 'Z' ? String.valueOf(firstChar) : "#";
    }

    private void updateSettingsContentState(@Nullable View content, boolean enabled) {
        if (content == null) {
            return;
        }
        content.setAlpha(enabled ? 1.0f : 0.45f);
        setViewEnabledRecursively(content, enabled);
    }

    private void setViewEnabledRecursively(@Nullable View view, boolean enabled) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setViewEnabledRecursively(group.getChildAt(index), enabled);
            }
        }
    }

    private void showReminderTonePickerDialog() {
        AlertDialog progressDialog = buildRingtoneProgressDialog();
        progressDialog.show();
        ioExecutor.execute(() -> {
            List<RingtoneOption> options = loadRingtoneOptions(progressDialog);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (isFinishing() || isDestroyed()) {
                    stopReminderFeedback();
                    return;
                }
                showRingtoneOptionsDialog(options);
            });
        });
    }

    private AlertDialog buildRingtoneProgressDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(getResources().getDisplayMetrics().density * 20f);
        content.setPadding(padding, padding, padding, padding);
        TextView progressText = new TextView(this);
        progressText.setId(View.generateViewId());
        progressText.setText(getString(R.string.route_link_loading_ringtones));
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );
        progressBar.setIndeterminate(false);
        progressBar.setMax(1);
        content.addView(progressText);
        content.addView(progressBar);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_link_ringtone_title)
                .setView(content)
                .setCancelable(false)
                .create();
        dialog.setOnShowListener(ignored -> {
            content.setTag(progressText);
            progressBar.setTag("progress_bar");
        });
        return dialog;
    }

    private List<RingtoneOption> loadRingtoneOptions(AlertDialog progressDialog) {
        List<RingtoneOption> options = new ArrayList<>();
        options.add(new RingtoneOption(
                getString(R.string.route_link_ringtone_default),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ));
        RingtoneManager ringtoneManager = new RingtoneManager(this);
        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE);
        android.database.Cursor cursor = ringtoneManager.getCursor();
        int total = cursor == null ? 0 : cursor.getCount();
        int processed = 0;
        while (cursor != null && cursor.moveToNext()) {
            int position = cursor.getPosition();
            Uri uri = ringtoneManager.getRingtoneUri(position);
            if (uri != null) {
                Ringtone ringtone = ringtoneManager.getRingtone(position);
                String title = ringtone == null ? uri.toString() : ringtone.getTitle(this);
                options.add(new RingtoneOption(title, uri));
            }
            processed++;
            int current = processed;
            runOnUiThread(() -> updateRingtoneProgressDialog(progressDialog, current, total));
        }
        if (cursor != null) {
            cursor.close();
        }
        return options;
    }

    private void updateRingtoneProgressDialog(AlertDialog dialog, int current, int total) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        View content = dialog.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) content;
        TextView progressText = findFirstViewOfType(group, TextView.class);
        android.widget.ProgressBar progressBar = findFirstViewOfType(group, android.widget.ProgressBar.class);
        if (progressText != null) {
            progressText.setText(getString(
                    R.string.route_link_loading_ringtones_progress,
                    Math.max(0, current),
                    Math.max(1, total)
            ));
        }
        if (progressBar != null) {
            progressBar.setMax(Math.max(1, total));
            progressBar.setProgress(Math.min(Math.max(0, current), Math.max(1, total)));
        }
    }

    private void showRingtoneOptionsDialog(List<RingtoneOption> options) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(getResources().getDisplayMetrics().density * 16f);
        container.setPadding(padding, padding, padding, padding);
        scrollView.addView(container);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.route_link_ringtone_title)
                .setView(scrollView)
                .setNegativeButton(R.string.route_link_settings_cancel, (d, which) -> stopReminderFeedback())
                .create();
        for (RingtoneOption option : options) {
            container.addView(createRingtoneOptionView(option, dialog));
        }
        dialog.setOnDismissListener(d -> stopReminderFeedback());
        dialog.show();
    }

    private void showSimulationMarkerAt(RoutePoint point) {
        if (point == null || baiduMap == null) {
            return;
        }
        LatLng latLng = new LatLng(point.getBdLatitude(), point.getBdLongitude());
        if (simulationMarker == null) {
            simulationMarker = (Marker) baiduMap.addOverlay(new MarkerOptions()
                    .position(latLng)
                    .icon(getSimulationProgressDescriptor()));
        } else {
            simulationMarker.setPosition(latLng);
        }
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
    }

    private void flushPendingMotion() {
        if (serviceBinder == null || pendingMotion == null) {
            return;
        }
        PendingMotion motion = pendingMotion;
        pendingMotion = null;
        serviceBinder.setMotion(
                motion.longitude,
                motion.latitude,
                motion.altitude,
                motion.speed,
                motion.bearing
        );
        XLog.d("RouteRunActivity: flushed pending motion to ServiceGo");
    }

    private View createRingtoneOptionView(RingtoneOption option, AlertDialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, Math.round(getResources().getDisplayMetrics().density * 12f));

        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        title.setText(option.title);

        Button playButton = new Button(this);
        playButton.setText(R.string.route_link_ringtone_play);
        playButton.setOnClickListener(v -> previewReminderTone(option.uri));

        Button confirmButton = new Button(this);
        confirmButton.setText(R.string.route_link_ringtone_confirm);
        confirmButton.setOnClickListener(v -> {
            prefsStore.saveRouteReminderTone(option.uri == null ? "" : option.uri.toString(), option.title);
            if (settingsReminderToneView != null) {
                settingsReminderToneView.setText(option.title);
            }
            stopReminderFeedback();
            dialog.dismiss();
        });

        row.addView(title);
        row.addView(playButton);
        row.addView(confirmButton);
        return row;
    }

    private void previewReminderTone(@Nullable Uri uri) {
        stopReminderFeedback();
        if (uri == null) {
            return;
        }
        try {
            activeReminderRingtone = RingtoneManager.getRingtone(this, uri);
            if (activeReminderRingtone != null) {
                activeReminderRingtone.play();
            }
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, getString(R.string.route_link_ringtone_permission_failed));
        }
    }

    private void bindCompletionOverlay() {
        if (completionOverlay == null) {
            return;
        }
        completionOverlay.setOnClickListener(v -> {
            // Block touches to the map while completion prompt is visible.
        });
        View ackButton = findViewById(R.id.route_completion_ack_button);
        if (ackButton != null) {
            ackButton.setOnClickListener(v -> acknowledgeCompletionNotice());
        }
    }

    private void showPendingCompletionNoticeIfPossible() {
        if (!completionNoticePending) {
            return;
        }
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            return;
        }
        completionNoticePending = false;
        showCompletionOverlay();
        startCompletionReminder();
    }

    private void showCompletionOverlay() {
        if (completionOverlay != null) {
            completionOverlay.setVisibility(View.VISIBLE);
            completionOverlay.bringToFront();
        }
    }

    private void hideCompletionOverlay() {
        if (completionOverlay != null) {
            completionOverlay.setVisibility(View.GONE);
        }
    }

    private void acknowledgeCompletionNotice() {
        prefsStore.setRouteCompletionPending(false);
        completionNoticePending = false;
        stopReminderFeedback();
        hideCompletionOverlay();
    }

    private void ensureFloatingWindowPermissionIfNeeded() {
        if (!prefsStore.isRouteFloatingWindowEnabled()) {
            return;
        }
        if (Settings.canDrawOverlays(getApplicationContext())) {
            return;
        }
        GoUtils.showEnableFloatWindowDialog(this);
    }

    private void maybeShowFloatingWindow() {
        if (!shouldShowFloatingWindow()) {
            hideFloatingWindow();
            return;
        }
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            return;
        }
        ensureFloatingWindowView();
        refreshFloatingWindowSizeFromPrefs();
        updateFloatingWindowRoute(viewModel.getSelectedRoute().getValue());
        syncFloatingWindowMarker();
        try {
            if (floatingWindowRoot != null && floatingWindowRoot.getParent() == null) {
                floatingWindowManager.addView(floatingWindowRoot, floatingWindowLayoutParams);
                floatingWindowMapView.onResume();
            } else if (floatingWindowRoot != null) {
                floatingWindowManager.updateViewLayout(floatingWindowRoot, floatingWindowLayoutParams);
            }
            floatingWindowVisible = true;
            updateFloatingWindowControlsState();
        } catch (Exception exception) {
            XLog.e("RouteRunActivity: failed to show floating window: " + exception.getMessage());
        }
    }

    private boolean shouldShowFloatingWindow() {
        return prefsStore.isRouteFloatingWindowEnabled()
                && !isFinishing()
                && !isDestroyed()
                && (Boolean.TRUE.equals(viewModel.isRunning().getValue()) || viewModel.hasActiveSimulation() || realRunLinkRunning);
    }

    private void hideFloatingWindow() {
        if (floatingWindowRoot == null || floatingWindowManager == null || floatingWindowRoot.getParent() == null) {
            floatingWindowVisible = false;
            return;
        }
        try {
            if (floatingWindowMapView != null) {
                floatingWindowMapView.onPause();
            }
            floatingWindowManager.removeViewImmediate(floatingWindowRoot);
        } catch (Exception ignored) {
        }
        floatingWindowVisible = false;
    }

    private void removeFloatingWindow() {
        hideFloatingWindow();
        if (floatingWindowMapView != null) {
            floatingWindowMapView.onDestroy();
        }
        floatingWindowRoot = null;
        floatingWindowBaiduMap = null;
        floatingWindowMapView = null;
        floatingWindowTitleView = null;
        floatingWindowPauseButton = null;
        floatingWindowResumeButton = null;
        floatingWindowResizeHandle = null;
        floatingWindowHandle = null;
        floatingWindowMarker = null;
        floatingWindowLayoutParams = null;
    }

    private void ensureFloatingWindowView() {
        if (floatingWindowRoot != null
                && floatingWindowMapView != null
                && floatingWindowTitleView != null
                && floatingWindowHandle != null
                && floatingWindowPauseButton != null
                && floatingWindowResumeButton != null
                && floatingWindowResizeHandle != null
                && floatingWindowLayoutParams != null) {
            return;
        }
        floatingWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        FrameLayout root = new FrameLayout(this);
        root.setFocusable(false);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#F7F9FC"));
        background.setCornerRadius(dp(18f));
        background.setStroke(Math.max(1, Math.round(dp(1f))), Color.parseColor("#D0D7E2"));
        root.setBackground(background);
        root.setPadding(Math.round(dp(12f)), Math.round(dp(12f)), Math.round(dp(12f)), Math.round(dp(12f)));
        root.setElevation(dp(8f));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        root.addView(container, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        TextView titleView = new TextView(this);
        titleView.setTextColor(Color.parseColor("#111111"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        titleView.setText(routeTitleForFloatingWindow(viewModel.getSelectedRoute().getValue()));
        container.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        MapView mapView = new MapView(this);
        mapView.showZoomControls(false);
        mapView.showScaleControl(false);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        mapParams.topMargin = Math.round(dp(10f));
        container.addView(mapView, mapParams);

        LinearLayout controlRow = new LinearLayout(this);
        controlRow.setGravity(Gravity.CENTER);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);
        controlRow.setPadding(0, Math.round(dp(8f)), 0, 0);
        TextView pauseButton = buildFloatingControlButton(
                getString(R.string.route_floating_window_pause),
                Color.parseColor("#1565C0")
        );
        TextView resumeButton = buildFloatingControlButton(
                getString(R.string.route_floating_window_resume),
                Color.parseColor("#2E7D32")
        );
        controlRow.addView(pauseButton);
        LinearLayout.LayoutParams resumeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        resumeParams.leftMargin = Math.round(dp(8f));
        controlRow.addView(resumeButton, resumeParams);
        container.addView(controlRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout handle = new LinearLayout(this);
        handle.setGravity(Gravity.CENTER_VERTICAL);
        handle.setOrientation(LinearLayout.HORIZONTAL);
        handle.setPadding(0, Math.round(dp(10f)), 0, 0);
        LinearLayout resizeHandle = new LinearLayout(this);
        resizeHandle.setGravity(Gravity.CENTER);
        View resizeBar = new View(this);
        GradientDrawable resizeDrawable = new GradientDrawable();
        resizeDrawable.setColor(Color.parseColor("#6B7280"));
        resizeDrawable.setCornerRadius(dp(4f));
        resizeBar.setBackground(resizeDrawable);
        resizeHandle.addView(resizeBar, new LinearLayout.LayoutParams(
                Math.round(dp(28f)),
                Math.round(dp(6f))
        ));
        handle.addView(resizeHandle, new LinearLayout.LayoutParams(
                Math.round(dp(48f)),
                Math.round(dp(28f))
        ));
        View handleBar = new View(this);
        GradientDrawable handleDrawable = new GradientDrawable();
        handleDrawable.setColor(Color.parseColor("#B8C3D8"));
        handleDrawable.setCornerRadius(dp(4f));
        handleBar.setBackground(handleDrawable);
        LinearLayout.LayoutParams handleBarParams = new LinearLayout.LayoutParams(
                0,
                Math.round(dp(6f)),
                1f
        );
        handleBarParams.leftMargin = Math.round(dp(8f));
        handleBarParams.rightMargin = Math.round(dp(8f));
        handle.addView(handleBar, handleBarParams);
        container.addView(handle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        BaiduMap overlayMap = mapView.getMap();
        overlayMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        overlayMap.setMyLocationEnabled(false);
        overlayMap.getUiSettings().setAllGesturesEnabled(false);
        overlayMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                bringRouteRunToFront();
            }

            @Override
            public void onMapPoiClick(com.baidu.mapapi.map.MapPoi mapPoi) {
                bringRouteRunToFront();
            }
        });
        overlayMap.setOnMarkerClickListener(marker -> {
            bringRouteRunToFront();
            return true;
        });
        mapView.setOnClickListener(v -> bringRouteRunToFrontForCompletion());
        pauseButton.setOnClickListener(v -> pauseSimulationFromFloatingWindow());
        resumeButton.setOnClickListener(v -> resumeSimulationFromFloatingWindow());
        resizeHandle.setOnTouchListener(this::handleFloatingWindowResize);
        handle.setOnTouchListener(this::handleFloatingWindowDrag);

        floatingWindowLayoutParams = buildFloatingWindowLayoutParams();
        floatingWindowRoot = root;
        floatingWindowMapView = mapView;
        floatingWindowBaiduMap = overlayMap;
        floatingWindowTitleView = titleView;
        floatingWindowPauseButton = pauseButton;
        floatingWindowResumeButton = resumeButton;
        floatingWindowResizeHandle = resizeHandle;
        floatingWindowHandle = handle;
        applyFloatingWindowButtonSize();
        updateFloatingWindowControlsState();
    }

    private TextView buildFloatingControlButton(@NonNull String text, int backgroundColor) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        button.setGravity(Gravity.CENTER);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(dp(8f));
        button.setBackground(drawable);
        return button;
    }

    private void applyFloatingWindowButtonSize() {
        int desiredButtonWidth = Math.round(dp(prefsStore.getRouteFloatingWindowButtonSizeDp()));
        int buttonWidth = desiredButtonWidth;
        if (floatingWindowLayoutParams != null) {
            int maxButtonWidth = Math.max(
                    Math.round(dp(32f)),
                    (floatingWindowLayoutParams.width - Math.round(dp(48f))) / 2
            );
            buttonWidth = Math.min(desiredButtonWidth, maxButtonWidth);
        }
        int buttonHeight = Math.round(dp(Math.max(30f, prefsStore.getRouteFloatingWindowButtonSizeDp() * 0.62f)));
        updateFloatingControlButtonSize(floatingWindowPauseButton, buttonWidth, buttonHeight);
        updateFloatingControlButtonSize(floatingWindowResumeButton, buttonWidth, buttonHeight);
    }

    private void updateFloatingControlButtonSize(@Nullable TextView button, int width, int height) {
        if (button == null) {
            return;
        }
        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params == null) {
            params = new LinearLayout.LayoutParams(width, height);
        }
        params.width = width;
        params.height = height;
        button.setLayoutParams(params);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, width >= Math.round(dp(50f)) ? 13f : 12f);
    }

    private void updateFloatingWindowControlsState() {
        boolean running = Boolean.TRUE.equals(viewModel.isRunning().getValue());
        boolean resumable = viewModel.hasResumableSimulationForSelectedRoute();
        setFloatingControlEnabled(floatingWindowPauseButton, running);
        setFloatingControlEnabled(floatingWindowResumeButton, !running && resumable);
    }

    private void setFloatingControlEnabled(@Nullable TextView button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.42f);
    }

    private WindowManager.LayoutParams buildFloatingWindowLayoutParams() {
        float scale = prefsStore.getRouteFloatingWindowScale();
        int width = Math.round(getResources().getDisplayMetrics().widthPixels * scale);
        int height = Math.max(Math.round(width * 0.72f), Math.round(dp(180f)));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        params.gravity = Gravity.START | Gravity.TOP;
        params.width = width;
        params.height = height;
        params.x = Math.max(0, getResources().getDisplayMetrics().widthPixels - width - Math.round(dp(12f)));
        params.y = Math.round(dp(112f));
        return params;
    }

    private void refreshFloatingWindowSizeFromPrefs() {
        applyFloatingWindowButtonSize();
        if (floatingWindowLayoutParams == null) {
            return;
        }
        updateFloatingWindowLayoutForScale(prefsStore.getRouteFloatingWindowScale(), false);
    }

    private void updateFloatingWindowLayoutForScale(float scale, boolean updateWindow) {
        if (floatingWindowLayoutParams == null) {
            return;
        }
        float clampedScale = Math.max(0.35f, Math.min(0.90f, scale));
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int width = Math.round(displayWidth * clampedScale);
        int height = Math.max(Math.round(width * 0.72f), Math.round(dp(180f)));
        floatingWindowLayoutParams.width = width;
        floatingWindowLayoutParams.height = height;
        floatingWindowLayoutParams.x = Math.max(0, Math.min(floatingWindowLayoutParams.x, displayWidth - width));
        applyFloatingWindowButtonSize();
        if (updateWindow && floatingWindowRoot != null && floatingWindowRoot.getParent() != null && floatingWindowManager != null) {
            try {
                floatingWindowManager.updateViewLayout(floatingWindowRoot, floatingWindowLayoutParams);
            } catch (Exception exception) {
                XLog.e("RouteRunActivity: failed to resize floating window: " + exception.getMessage());
            }
        }
    }

    private void updateFloatingWindowRoute(@Nullable RouteDefinition routeDefinition) {
        if (floatingWindowTitleView != null) {
            floatingWindowTitleView.setText(routeTitleForFloatingWindow(routeDefinition));
        }
        if (floatingWindowBaiduMap == null) {
            return;
        }
        floatingWindowBaiduMap.clear();
        floatingWindowMarker = null;
        if (routeDefinition == null || routeDefinition.getPoints().isEmpty()) {
            return;
        }
        List<LatLng> latLngs = new ArrayList<>();
        for (RoutePoint routePoint : routeDefinition.getPoints()) {
            latLngs.add(new LatLng(routePoint.getBdLatitude(), routePoint.getBdLongitude()));
        }
        if (latLngs.size() > 1) {
            floatingWindowBaiduMap.addOverlay(new PolylineOptions()
                    .width(8)
                    .color(0xAA1565C0)
                    .points(latLngs));
        }
        floatingWindowBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(
                new MapStatus.Builder()
                        .target(latLngs.get(0))
                        .zoom(18f)
                        .build()
        ));
        syncFloatingWindowMarker();
    }

    private void syncFloatingWindowMarker() {
        if (floatingWindowBaiduMap == null || !floatingWindowHasPoint) {
            return;
        }
        LatLng latLng = new LatLng(floatingWindowBdLatitude, floatingWindowBdLongitude);
        if (floatingWindowMarker == null) {
            floatingWindowMarker = (Marker) floatingWindowBaiduMap.addOverlay(new MarkerOptions()
                    .position(latLng)
                    .icon(getSimulationProgressDescriptor()));
        } else {
            floatingWindowMarker.setPosition(latLng);
        }
        floatingWindowBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
    }

    private String routeTitleForFloatingWindow(@Nullable RouteDefinition routeDefinition) {
        if (routeDefinition == null || TextUtils.isEmpty(routeDefinition.getName())) {
            return getString(R.string.route_floating_window_title);
        }
        return getString(R.string.route_floating_window_title) + " · " + routeDefinition.getName();
    }

    private void updateFloatingWindowPosition(double longitude, double latitude) {
        double[] bdCoordinates = MapUtils.wgs2bd09(longitude, latitude);
        floatingWindowBdLongitude = bdCoordinates[0];
        floatingWindowBdLatitude = bdCoordinates[1];
        floatingWindowHasPoint = true;
        syncFloatingWindowMarker();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private boolean handleFloatingWindowDrag(View view, MotionEvent event) {
        if (event == null || floatingWindowRoot == null || floatingWindowLayoutParams == null || floatingWindowManager == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                floatingWindowDragStartRawX = event.getRawX();
                floatingWindowDragStartRawY = event.getRawY();
                floatingWindowDragStartX = floatingWindowLayoutParams.x;
                floatingWindowDragStartY = floatingWindowLayoutParams.y;
                return true;
            case MotionEvent.ACTION_MOVE:
                int deltaX = Math.round(event.getRawX() - floatingWindowDragStartRawX);
                int deltaY = Math.round(event.getRawY() - floatingWindowDragStartRawY);
                floatingWindowLayoutParams.x = Math.max(0, floatingWindowDragStartX + deltaX);
                floatingWindowLayoutParams.y = Math.max(0, floatingWindowDragStartY + deltaY);
                try {
                    floatingWindowManager.updateViewLayout(floatingWindowRoot, floatingWindowLayoutParams);
                } catch (Exception ignored) {
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return false;
        }
    }

    private boolean handleFloatingWindowResize(View view, MotionEvent event) {
        if (event == null || floatingWindowRoot == null || floatingWindowLayoutParams == null || floatingWindowManager == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                floatingWindowResizeStartRawX = event.getRawX();
                floatingWindowResizeStartRawY = event.getRawY();
                floatingWindowResizeStartScale = floatingWindowLayoutParams.width
                        / (float) Math.max(1, getResources().getDisplayMetrics().widthPixels);
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - floatingWindowResizeStartRawX;
                float deltaY = event.getRawY() - floatingWindowResizeStartRawY;
                float scaleDelta = ((-deltaX) + deltaY) / Math.max(1f, getResources().getDisplayMetrics().widthPixels);
                updateFloatingWindowLayoutForScale(floatingWindowResizeStartScale + scaleDelta, true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float currentScale = floatingWindowLayoutParams.width
                        / (float) Math.max(1, getResources().getDisplayMetrics().widthPixels);
                prefsStore.saveRouteFloatingWindowSettings(
                        prefsStore.isRouteFloatingWindowEnabled(),
                        currentScale,
                        prefsStore.getRouteFloatingWindowButtonSizeDp()
                );
                return true;
            default:
                return false;
        }
    }

    private void bringRouteRunToFrontForCompletion() {
        bringRouteRunToFront();
    }

    private void bringRouteRunToFront() {
        Intent intent = new Intent(this, RouteRunActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void startCompletionReminder() {
        Uri ringtoneUri = TextUtils.isEmpty(prefsStore.getRouteReminderToneUri())
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                : Uri.parse(prefsStore.getRouteReminderToneUri());
        previewReminderTone(ringtoneUri);
        mainHandler.postDelayed(reminderReplayRunnable, REMINDER_REPLAY_INTERVAL_MILLIS);
        startCompletionVibration();
    }

    private void replayCompletionReminder() {
        if (completionOverlay == null || completionOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        Uri ringtoneUri = TextUtils.isEmpty(prefsStore.getRouteReminderToneUri())
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                : Uri.parse(prefsStore.getRouteReminderToneUri());
        previewReminderTone(ringtoneUri);
        mainHandler.postDelayed(reminderReplayRunnable, REMINDER_REPLAY_INTERVAL_MILLIS);
    }

    private void startCompletionVibration() {
        if (vibrator == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(COMPLETION_VIBRATION_PATTERN, 1));
        } else {
            vibrator.vibrate(COMPLETION_VIBRATION_PATTERN, 1);
        }
    }

    private void stopReminderFeedback() {
        mainHandler.removeCallbacks(reminderReplayRunnable);
        if (activeReminderRingtone != null && activeReminderRingtone.isPlaying()) {
            activeReminderRingtone.stop();
        }
        activeReminderRingtone = null;
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void togglePanelCollapsed() {
        panelCollapsed = !panelCollapsed;
        updatePanelCollapsedState();
    }

    private boolean handlePanelDragGesture(View view, MotionEvent event) {
        if (event == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                panelDragStartY = event.getRawY();
                panelDragTriggered = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getRawY() - panelDragStartY;
                if (!panelDragTriggered && !panelCollapsed && deltaY > panelDragThresholdPx) {
                    panelCollapsed = true;
                    panelDragTriggered = true;
                    updatePanelCollapsedState();
                } else if (!panelDragTriggered && panelCollapsed && deltaY < -panelDragThresholdPx) {
                    panelCollapsed = false;
                    panelDragTriggered = true;
                    updatePanelCollapsedState();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!panelDragTriggered) {
                    togglePanelCollapsed();
                }
                return true;
            default:
                return false;
        }
    }

    private void updatePanelCollapsedState() {
        if (panelContentLayout != null) {
            panelContentLayout.setVisibility(panelCollapsed ? View.GONE : View.VISIBLE);
        }
        if (panelToggleButton != null) {
            panelToggleButton.setImageResource(
                    panelCollapsed ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float
            );
        }
    }

    @Nullable
    private <T extends View> T findFirstViewOfType(ViewGroup parent, Class<T> type) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            View child = parent.getChildAt(index);
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof ViewGroup) {
                T nested = findFirstViewOfType((ViewGroup) child, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private void restoreLastRouteSelection() {
        String routeId = prefsStore.getLastRouteId();
        if (TextUtils.isEmpty(routeId)) {
            return;
        }
        try {
            viewModel.selectRouteById(routeId);
        } catch (Exception ignored) {
            // Ignore when the previous route no longer exists.
        }
    }

    private final class ServiceGateway implements LocationSimulationGateway {
        @Override
        public void pushLocation(double longitude, double latitude, double altitude, float speed, float bearing) {
            updateFloatingWindowPosition(longitude, latitude);
            syncRootDiagnosticLocation(longitude, latitude, altitude, speed, bearing);
            if (serviceBinder != null) {
                serviceBinder.setMotion(longitude, latitude, altitude, speed, bearing);
                XLog.d("RouteRunActivity: pushed motion directly to ServiceGo");
            } else {
                pendingMotion = new PendingMotion(longitude, latitude, altitude, speed, bearing);
                XLog.d("RouteRunActivity: cached pending motion, ServiceGo binder not ready");
            }
        }
    }

    private static final class PendingMotion {
        private final double longitude;
        private final double latitude;
        private final double altitude;
        private final float speed;
        private final float bearing;

        private PendingMotion(double longitude, double latitude, double altitude, float speed, float bearing) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.altitude = altitude;
            this.speed = speed;
            this.bearing = bearing;
        }
    }

    private static final class ScreenRouteProjection {
        private final int segmentStartIndex;
        private final RoutePoint projectedPoint;
        private final double distanceToRoutePixels;

        private ScreenRouteProjection(
                int segmentStartIndex,
                @NonNull RoutePoint projectedPoint,
                double distanceToRoutePixels
        ) {
            this.segmentStartIndex = segmentStartIndex;
            this.projectedPoint = projectedPoint;
            this.distanceToRoutePixels = distanceToRoutePixels;
        }
    }

    private static final class SegmentProjection {
        private final double segmentRatio;
        private final double distancePixels;

        private SegmentProjection(double segmentRatio, double distancePixels) {
            this.segmentRatio = segmentRatio;
            this.distancePixels = distancePixels;
        }
    }

    @NonNull
    private BitmapDescriptor getSimulationProgressDescriptor() {
        if (simulationProgressDescriptor == null) {
            simulationProgressDescriptor = createCircleMarkerDescriptor(18, "#D32F2F");
        }
        return simulationProgressDescriptor;
    }

    @NonNull
    private BitmapDescriptor getRouteEditVertexDescriptor() {
        if (routeEditVertexDescriptor == null) {
            routeEditVertexDescriptor = createCircleMarkerDescriptor(14, "#2E7D32");
        }
        return routeEditVertexDescriptor;
    }

    @NonNull
    private BitmapDescriptor getRouteEditVertexSelectedDescriptor() {
        if (routeEditVertexSelectedDescriptor == null) {
            routeEditVertexSelectedDescriptor = createCircleMarkerDescriptor(18, "#F57C00");
        }
        return routeEditVertexSelectedDescriptor;
    }

    @NonNull
    private BitmapDescriptor createCircleMarkerDescriptor(int sizeDp, @NonNull String fillColor) {
        int sizePx = Math.max(1, Math.round(getResources().getDisplayMetrics().density * sizeDp));
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float center = sizePx / 2f;
        float outerRadius = center;
        float innerRadius = Math.max(1f, outerRadius - (getResources().getDisplayMetrics().density * 3f));

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, outerRadius, strokePaint);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor(fillColor));
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center, center, innerRadius, fillPaint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void recycleMapDescriptors() {
        if (simulationProgressDescriptor != null) {
            simulationProgressDescriptor.recycle();
            simulationProgressDescriptor = null;
        }
        if (routeEditVertexDescriptor != null) {
            routeEditVertexDescriptor.recycle();
            routeEditVertexDescriptor = null;
        }
        if (routeEditVertexSelectedDescriptor != null) {
            routeEditVertexSelectedDescriptor.recycle();
            routeEditVertexSelectedDescriptor = null;
        }
    }

    private static final class SearchableRouteItem implements PickerIndexable {
        private final SharedRouteSummary summary;
        private final String displayText;
        private final String sortKey;

        private SearchableRouteItem(SharedRouteSummary summary, String displayText, String sortKey) {
            this.summary = summary;
            this.displayText = displayText;
            this.sortKey = sortKey;
        }

        @NonNull
        @Override
        public String getSortKey() {
            return sortKey;
        }
    }

    private static final class SearchableNfcItem implements PickerIndexable {
        private final SharedNfcEntry entry;
        private final String displayText;
        private final String sortKey;

        private SearchableNfcItem(SharedNfcEntry entry, String displayText, String sortKey) {
            this.entry = entry;
            this.displayText = displayText;
            this.sortKey = sortKey;
        }

        @NonNull
        @Override
        public String getSortKey() {
            return sortKey;
        }
    }

    private static final class SearchableSimulationConfigItem implements PickerIndexable {
        private final SharedSimulationConfigEntry entry;
        private final String displayText;
        private final String sortKey;

        private SearchableSimulationConfigItem(SharedSimulationConfigEntry entry, String displayText, String sortKey) {
            this.entry = entry;
            this.displayText = displayText;
            this.sortKey = sortKey;
        }

        @NonNull
        @Override
        public String getSortKey() {
            return sortKey;
        }
    }

    private static final class RingtoneOption {
        private final String title;
        private final Uri uri;

        private RingtoneOption(String title, Uri uri) {
            this.title = title;
            this.uri = uri;
        }
    }

    private static final class SettingsSection {
        private final View view;
        private final String letter;
        private final String keywords;
        private final boolean rootOnly;

        private SettingsSection(@Nullable View view, @NonNull String letter, @NonNull String keywords) {
            this(view, letter, keywords, false);
        }

        private SettingsSection(@Nullable View view, @NonNull String letter, @NonNull String keywords, boolean rootOnly) {
            this.view = view;
            this.letter = letter;
            this.keywords = keywords;
            this.rootOnly = rootOnly;
        }
    }

    private static final class SimulationSettingsHomeRow {
        private final View row;
        private final View category;
        private final String keywords;

        private SimulationSettingsHomeRow(
                @NonNull View row,
                @NonNull View category,
                @NonNull String keywords
        ) {
            this.row = row;
            this.category = category;
            this.keywords = keywords;
        }
    }

    private interface PickerIndexable {
        @NonNull String getSortKey();
    }

    private interface RootSettingsSaveAction {
        void save();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op.
        }
    }

    private List<SearchableRouteItem> buildRouteItems(List<SharedRouteSummary> routes) {
        List<SearchableRouteItem> items = new ArrayList<>();
        for (SharedRouteSummary route : routes) {
            String privacyTag = route.isPrivacyMode() ? getString(R.string.route_shared_privacy_tag) + " " : "";
            String displayText = String.format(
                    Locale.getDefault(),
                    "%s%s · %d %s",
                    privacyTag,
                    route.getName(),
                    route.getPointCount(),
                    getString(R.string.route_points_unit)
            );
            items.add(new SearchableRouteItem(route, displayText, SearchSortUtils.buildSortKey(route.getName())));
        }
        items.sort(Comparator.comparing(item -> item.sortKey));
        return items;
    }

    private void fillRouteAdapter(ArrayAdapter<String> adapter, List<SearchableRouteItem> items) {
        adapter.clear();
        if (items.isEmpty()) {
            adapter.add(getString(R.string.searchable_picker_empty));
        } else {
            for (SearchableRouteItem item : items) {
                adapter.add(item.displayText);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterRouteItems(
            String query,
            List<SearchableRouteItem> sourceItems,
            List<SearchableRouteItem> filteredItems,
            ArrayAdapter<String> adapter
    ) {
        filteredItems.clear();
        for (SearchableRouteItem item : sourceItems) {
            if (SearchSortUtils.matches(query, item.summary.getName())) {
                filteredItems.add(item);
            }
        }
        fillRouteAdapter(adapter, filteredItems);
    }

    private List<SearchableSimulationConfigItem> buildSimulationConfigItems(List<SharedSimulationConfigEntry> entries) {
        List<SearchableSimulationConfigItem> items = new ArrayList<>();
        for (SharedSimulationConfigEntry entry : entries) {
            String displayText = String.format(
                    Locale.getDefault(),
                    "%s [%s] 循环 %d",
                    entry.getName(),
                    TextUtils.isEmpty(entry.getAuthorName()) ? "匿名" : entry.getAuthorName(),
                    entry.getLoopCount()
            );
            items.add(new SearchableSimulationConfigItem(
                    entry,
                    displayText,
                    SearchSortUtils.buildSortKey(entry.getName())
            ));
        }
        items.sort(Comparator.comparing(item -> item.sortKey));
        return items;
    }

    private void fillSimulationConfigAdapter(ArrayAdapter<String> adapter, List<SearchableSimulationConfigItem> items) {
        adapter.clear();
        if (items.isEmpty()) {
            adapter.add(getString(R.string.searchable_picker_empty));
        } else {
            for (SearchableSimulationConfigItem item : items) {
                adapter.add(item.displayText);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterSimulationConfigItems(
            String query,
            List<SearchableSimulationConfigItem> sourceItems,
            List<SearchableSimulationConfigItem> filteredItems,
            ArrayAdapter<String> adapter
    ) {
        filteredItems.clear();
        for (SearchableSimulationConfigItem item : sourceItems) {
            if (SearchSortUtils.matches(query, item.entry.getName())
                    || SearchSortUtils.matches(query, item.entry.getAuthorName())) {
                filteredItems.add(item);
            }
        }
        fillSimulationConfigAdapter(adapter, filteredItems);
    }

    private List<SearchableNfcItem> buildNfcItems(List<SharedNfcEntry> entries) {
        List<SearchableNfcItem> items = new ArrayList<>();
        for (SharedNfcEntry entry : entries) {
            String displayText = entry.getName() + " · " + entry.getPackageName();
            items.add(new SearchableNfcItem(entry, displayText, SearchSortUtils.buildSortKey(entry.getName())));
        }
        items.sort(Comparator.comparing(item -> item.sortKey));
        return items;
    }

    private void fillNfcAdapter(ArrayAdapter<String> adapter, List<SearchableNfcItem> items) {
        adapter.clear();
        if (items.isEmpty()) {
            adapter.add(getString(R.string.searchable_picker_empty));
        } else {
            for (SearchableNfcItem item : items) {
                adapter.add(item.displayText);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterNfcItems(
            String query,
            List<SearchableNfcItem> sourceItems,
            List<SearchableNfcItem> filteredItems,
            ArrayAdapter<String> adapter
    ) {
        filteredItems.clear();
        for (SearchableNfcItem item : sourceItems) {
            if (SearchSortUtils.matches(query, item.entry.getName())
                    || SearchSortUtils.matches(query, item.entry.getPackageName())) {
                filteredItems.add(item);
            }
        }
        fillNfcAdapter(adapter, filteredItems);
    }

}
