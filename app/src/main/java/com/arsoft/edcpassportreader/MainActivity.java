package com.arsoft.edcpassportreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arsoft.edcpassportreader.common.CommonActivity;
import com.arsoft.edcpassportreader.common.EDCTag;
import com.arsoft.edcpassportreader.databinding.ActivityMainBinding;
import com.arsoft.edcpassportreader.ui.LoadingDialog;
import com.arsoft.edcpassportreader.ui.MessageDialog;
import com.arsoft.edcpassportreader.ui.ProgressDialog;
import com.arsoft.edcpassportreader.ui.StatusDialog;
import com.arsoft.edcpassportreader.ui.bluetooth.adapter.BluetoothData;
import com.arsoft.edcpassportreader.ui.bluetooth.adapter.BluetoothListAdapter;
import com.ingenico.pclservice.PclService;
import com.ingenico.pclutilities.PclUtilities;
import com.regula.documentreader.api.DocumentReader;
import com.regula.documentreader.api.completions.IDocumentReaderCompletion;
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion;
import com.regula.documentreader.api.enums.DocReaderAction;
import com.regula.documentreader.api.enums.Scenario;
import com.regula.documentreader.api.enums.eRFID_DataFile_Type;
import com.regula.documentreader.api.errors.DocumentReaderException;
import com.regula.documentreader.api.params.DocReaderConfig;
import com.regula.documentreader.api.results.DocumentReaderResults;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends CommonActivity implements MessageDialog.MessageDialogListener, BluetoothListAdapter.BluetoothListAdapterListener {
    private String[] PERMISSIONS;
    static final int PERMISSION_REQ = 10000;
    static final String PERMISSION_RATIONAL = "rational";
    static final String PERMISSION_PERMANENTLY = "permanently";
    static final String BLUETOOTH_ENABLE = "bluetooth_enable";
    private ActivityMainBinding binding;
    private PclUtilities pclUtilities;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothListAdapter adapter;
    private boolean mServiceStarted;
    ArrayList<BluetoothData> bluetoothData = new ArrayList<>();
    LoadingDialog loadingDialog = new LoadingDialog();
    ProgressDialog progressDialog = new ProgressDialog();
    EDCTag.EDCTagListener edcTagListener;
    StatusDialog statusDialog = new StatusDialog();

    // Receive Bluetooth Status
    private final BroadcastReceiver bluetoothBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_OFF) {
                    showAlertDialog(
                            getString(R.string.bluetooth_not_enable),
                            BLUETOOTH_ENABLE
                    );
                }
            }
        }
    };

    // Open Bluetooth from app
    ActivityResultLauncher<Intent> bluetoothLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    finishAndRemoveTask();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PERMISSIONS = new String[] {
                    Manifest.permission.NFC,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            PERMISSIONS = new String[] {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.NFC,
            };
        }

        binding.activityMainLlBt.setVisibility(View.GONE);
        binding.activityMainBtRead.setVisibility(View.GONE);

        registerReceiver(bluetoothBroadcast, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        boolean isAllPermissionGranted = true;
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                isAllPermissionGranted = false;
                break;
            }
        }

        if (isAllPermissionGranted) {
            // Check Bluetooth available
            if (bluetoothAdapter == null) {
                showAlertDialog(getString(R.string.bluetooth_not_available), BLUETOOTH_ENABLE);
                return;
            }
            // Check Bluetooth Status
            if (bluetoothAdapter.isEnabled()) {
                initial();
            } else {
                showAlertDialog(getString(R.string.bluetooth_not_enable), BLUETOOTH_ENABLE);
            }
        } else {
            requestPermissions(PERMISSIONS, PERMISSION_REQ);
        }

        setupListener();
    }

    private void setupListener() {

        // SCAN PASSPORT BUTTON
        binding.activityMainBtScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Start Passport Scanner
                DocumentReader.Instance().showScanner(MainActivity.this, new IDocumentReaderCompletion() {
                    @Override
                    public void onCompleted(int action, @Nullable DocumentReaderResults documentReaderResults, @Nullable DocumentReaderException e) {
                        if (action == DocReaderAction.COMPLETE) {
                            binding.activityMainBtRead.setVisibility(View.VISIBLE);
                            binding.activityMainBtScan.setVisibility(View.GONE);
                        } else if (action == DocReaderAction.ERROR) {
                            showAlertDialog(e.getLocalizedMessage(), "error_scanner");
                        }
                    }
                });
            }
        });

        // READ PASSPORT BUTTON
        binding.activityMainBtRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!statusDialog.isAdded()) {
                    statusDialog.show(getSupportFragmentManager(), "status");
                }

                // Start READ EPassport
                DocumentReader.Instance().readRFID(new EDCTag(mPclService, edcTagListener), new IDocumentReaderCompletion() {
                    @Override
                    public void onCompleted(int action, @Nullable DocumentReaderResults documentReaderResults, @Nullable DocumentReaderException e) {
                        switch (action) {
                            case DocReaderAction.COMPLETE:
                                edcTagListener.OnNotify("Read Completed", null);
                                break;
                            case DocReaderAction.CANCEL:
                                edcTagListener.OnNotify("Read Error", null);
                                break;
                            case DocReaderAction.NOTIFICATION:
                                if (documentReaderResults != null && documentReaderResults.documentReaderNotification != null) {
                                    runOnUiThread(() -> {
                                        edcTagListener.OnNotify(
                                                rfidProgress(
                                                        documentReaderResults.documentReaderNotification.code,
                                                        documentReaderResults.documentReaderNotification.value
                                                ), null
                                        );
                                    });
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
            }
        });

        edcTagListener = new EDCTag.EDCTagListener() {
            @Override
            public void OnNotify(String status, String message) {
                statusDialog.Update(status, message);
            }
        };
    }

    // Convert status code to readable text
    private String rfidProgress(int code, int value) {
        int hiword = code & -0x10000;
        @SuppressLint("WrongConstant") int loword = code & 0x0000FFFF;
        if (value == 0) {
            return eRFID_DataFile_Type.getTranslation(getApplicationContext(), loword);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothBroadcast);
        releaseService();
        if (mReleaseService == 1) {
            stopPclService();
        }

        batteryLevel.removeObservers(this);
        serialNumber.removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (requestCode == PERMISSION_REQ) {
                if (Arrays.stream(grantResults).allMatch(i -> i == PackageManager.PERMISSION_GRANTED)) {
                    if (bluetoothAdapter.isEnabled()) {
                        initial();
                    } else {
                        showAlertDialog(
                                getString(R.string.bluetooth_not_enable),
                                BLUETOOTH_ENABLE
                        );
                    }
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initial() {
        initRegular();

        pclUtilities = new PclUtilities(getApplicationContext(), BuildConfig.APPLICATION_ID, "pairing_addr.txt");

        // Get paired bluetooth
        for (PclUtilities.BluetoothCompanion bluetoothCompanion : pclUtilities.GetPairedCompanions()) {
            bluetoothData.add(
                    new BluetoothData(
                            bluetoothCompanion.getBluetoothDevice().getAddress(),
                            bluetoothCompanion.getBluetoothDevice().getName(),
                            0
                    )
            );
        }

        // Show paired bluetooth
        if (bluetoothData.size() > 0) {
            adapter = new BluetoothListAdapter(bluetoothData);
            binding.activityMainRcTerminal.setLayoutManager(new LinearLayoutManager(this));
            binding.activityMainRcTerminal.setAdapter(adapter);
        }
    }

    private void initRegular() {

        // Update Regular database
        DocumentReader.Instance().runAutoUpdate(this, "Full", new IDocumentReaderPrepareCompletion() {
            @Override
            public void onPrepareProgressChanged(int i) {
                if (!progressDialog.isAdded()) {
                    progressDialog.show(getSupportFragmentManager(), "update_database");
                } else {
                    progressDialog.Update("Download database...(" + i + ") %", i);
                }
            }

            @Override
            public void onPrepareCompleted(boolean b, @Nullable DocumentReaderException e) {
                if (progressDialog.isAdded()) {
                    progressDialog.dismiss();
                }

                Bundle bundle = new Bundle();
                bundle.putString("message", "Initialize...");
                loadingDialog.setArguments(bundle);
                loadingDialog.show(getSupportFragmentManager(), "init_regular");

                InputStream licInput = getResources().openRawResource(R.raw.regula);
                try {
                    int available = licInput.available();
                    byte[] license = new byte[available];

                    licInput.read(license);

                    // Check regular license
                    DocumentReader.Instance().initializeReader(MainActivity.this, new DocReaderConfig(license),
                            (success, e1) -> {
                                if (success) {
                                    loadingDialog.dismiss();
                                    // Setup Scenario
                                    DocumentReader.Instance().processParams().scenario = Scenario.SCENARIO_MRZ;
                                } else {
                                    showAlertDialog(e1.getLocalizedMessage(), "init_error");
                                }
                            }
                    );
                } catch (IOException ioException) {
                    showAlertDialog(ioException.getLocalizedMessage(), "io_error");
                }
            }
        });
    }

    private void showAlertDialog(String message, @NonNull String key) {
        MessageDialog dialog = new MessageDialog();
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), key);
    }

    private void startLoading() {
        Bundle b = new Bundle();
        b.putString("message", "Connecting...");
        loadingDialog.setArguments(b);
        loadingDialog.show(getSupportFragmentManager(), "loading");
    }

    private void stopLoading() {
        loadingDialog.dismiss();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPositiveClick(DialogFragment dialog) {
        dialog.dismiss();
        if (Objects.equals(dialog.getTag(), PERMISSION_RATIONAL)) {
            requestPermissions(PERMISSIONS, PERMISSION_REQ);
        } else if (Objects.equals(dialog.getTag(), PERMISSION_PERMANENTLY)) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        } else if (Objects.equals(dialog.getTag(), BLUETOOTH_ENABLE)) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothLauncher.launch(i);
        }
    }

    @Override
    public void onNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
        if (Objects.equals(dialog.getTag(), PERMISSION_RATIONAL)) {
            finishAndRemoveTask();
        } else if (Objects.equals(dialog.getTag(), PERMISSION_PERMANENTLY)) {
            finishAndRemoveTask();
        } else if (Objects.equals(dialog.getTag(), BLUETOOTH_ENABLE)) {
            finishAndRemoveTask();
        }
    }

    @Override
    public void onSwitchChange(Boolean enable, Integer position) {
        bluetoothData.get(position).setEnable(enable);
        if (enable) {
            startLoading();
            // Connect to paired bluetooth
            pclUtilities.ActivateCompanion(bluetoothData.get(position).getMac());

            // Start PCLService -------------
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                if (mServiceStarted) {
                    releaseService();
                    stopPclService();
                }
                startPclService();
                initService();
            });
            //-------------------------------

            mReleaseService = 1;
            if (isCompanionConnected()) {
                Log.e("TEST", "Connected");
            } else {
                Log.e("TEST", "Not connected");
            }

            if (mPclService != null) {
                Log.e("TEST", "Service created");
            }
        } else {
            if (mPclService != null) {
                startLoading();
                PclUtilities.IpTerminal terminal = pclUtilities.new IpTerminal("", "", "255.255.255.255", 0);
                terminal.setSsl(0);

                pclUtilities.activateIpTerminal(terminal);

                // Restart PCLService
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    releaseService();
                    stopPclService();
                    startPclService();
                    initService();
                });
            }
        }
    }

    private void startPclService() {
        if (!mServiceStarted) {
            SharedPreferences settings = getSharedPreferences("PCLSERVICE", MODE_PRIVATE);
            Intent i = new Intent(this, PclService.class);
            i.putExtra("PACKAGE_NAME", BuildConfig.APPLICATION_ID);
            i.putExtra("FILE_NAME", "pairing_addr.txt");
            i.putExtra("ENABLE_LOG", enableLog);

            if (startService(i) != null) {
                mServiceStarted = true;
            }
        }
    }

    private void stopPclService() {
        if (mServiceStarted) {
            Intent i = new Intent(MainActivity.this, PclService.class);
            if (stopService(i)) {
                mServiceStarted = false;
            }
        }
    }

    // Connection status listener
    @Override
    public void onStateChanged(String state) {
        if (state.equals("CONNECTED")) {
            Log.e("TEST", "mini-EDC Connected");
            stopLoading();
            binding.companionStatus.setTextColor(Color.GREEN);
            binding.companionStatus.setText("Connected");

            runGetFullSerialNumber();
            runGetBatteryLevel();

            binding.activityMainLlBt.setVisibility(View.VISIBLE);
        } else {
            Log.e("TEST", "mini-EDC Disconnected");
            stopLoading();
            binding.companionStatus.setTextColor(Color.RED);
            binding.companionStatus.setText("Disconnected");
            binding.companionSn.setText("N/A");
            binding.companionBattery.setText("N/A");

            binding.activityMainLlBt.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPclServiceConnected() {
        Log.d(TAG, "onPclServiceConnected");
        mPclService.addDynamicBridgeLocal(6000, 0);

        if (isCompanionConnected()) {
            Log.e("TEST", "PCL - Connected");
        } else {
            Log.e("TEST", "PCL - Not connected");
        }

        if (mPclService != null) {
            Log.e("TEST", mPclService.getAddonVersion());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        batteryLevel.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer battery) {
                binding.companionBattery.setText(String.format("%d", battery));
            }
        });

        serialNumber.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String serial) {
                binding.companionSn.setText(serial);
            }
        });
    }
}