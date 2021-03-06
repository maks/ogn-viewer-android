package com.meisterschueler.ognviewer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.ogn.client.AircraftBeaconListener;
import org.ogn.client.OgnClient;
import org.ogn.client.ReceiverBeaconListener;
import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.ReceiverBeacon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import co.uk.rushorm.android.AndroidInitializeConfig;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushCore;

public class OgnService extends Service implements AircraftBeaconListener, ReceiverBeaconListener {

    public class LocalBinder extends Binder {
        OgnService getService() {
            return OgnService.this;
        }
    }

    public OgnService() {
    }

    OgnClient ognClient;
    boolean connected = false;

    LocalBroadcastManager localBroadcastManager;
    IBinder binder = new LocalBinder();

    Map<String, ReceiverBeacon> receiverMap = new ConcurrentHashMap<String, ReceiverBeacon>();
    Map<String, AircraftBundle> aircraftMap = new ConcurrentHashMap<String, AircraftBundle>();

    public interface UpdateListener {
        public void updateAircraftBundle(AircraftBundle bundle);
    }

    UpdateListener updateListener = null;
    public void setUpdateListener(UpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    public class AircraftBundle {
        public AircraftBeacon aircraftBeacon;
        public AircraftDescriptor aircraftDescriptor;

        public AircraftBundle(AircraftBeacon aircraftBeacon, AircraftDescriptor aircraftDescriptor) {
            this.aircraftBeacon = aircraftBeacon;
            this.aircraftDescriptor = aircraftDescriptor;
        }
    }


    @Override
    public void onUpdate(AircraftBeacon aircraftBeacon, AircraftDescriptor aircraftDescriptor) {
        AircraftBundle bundle = new AircraftBundle(aircraftBeacon, aircraftDescriptor);
        aircraftMap.put(aircraftBeacon.getAddress(), bundle);

        if (updateListener != null) {
            //updateListener.updateAircraftBundle(bundle);
        }

        Intent intent = new Intent("AIRCRAFT-BEACON");

        // AircraftBeacon
        intent.putExtra("receiverName", aircraftBeacon.getReceiverName());
        intent.putExtra("addressType", aircraftBeacon.getAddressType().getCode());
        intent.putExtra("address", aircraftBeacon.getAddress());
        intent.putExtra("aircraftType", aircraftBeacon.getAircraftType().getCode());
        intent.putExtra("stealth", aircraftBeacon.isStealth());
        intent.putExtra("climbRate", aircraftBeacon.getClimbRate());
        intent.putExtra("turnRate", aircraftBeacon.getTurnRate());
        intent.putExtra("signalStrength", aircraftBeacon.getSignalStrength());
        intent.putExtra("frequencyOffset", aircraftBeacon.getFrequencyOffset());
        intent.putExtra("gpsStatus", aircraftBeacon.getGpsStatus());
        intent.putExtra("errorCount", aircraftBeacon.getErrorCount());
        //String[] getHeardAircraftIds();

        // OgnBeacon
        intent.putExtra("id", aircraftBeacon.getId());
        intent.putExtra("timestamp", aircraftBeacon.getTimestamp());
        intent.putExtra("lat", aircraftBeacon.getLat());
        intent.putExtra("lon", aircraftBeacon.getLon());
        intent.putExtra("alt", aircraftBeacon.getAlt());
        intent.putExtra("track", aircraftBeacon.getTrack());
        intent.putExtra("groundSpeed", aircraftBeacon.getGroundSpeed());
        intent.putExtra("rawPacket", aircraftBeacon.getRawPacket());

        // AircraftDescriptor
        if (aircraftDescriptor != null) {
            intent.putExtra("known", aircraftDescriptor.isKnown());
            intent.putExtra("regNumber", aircraftDescriptor.getRegNumber());
            intent.putExtra("CN", aircraftDescriptor.getCN());
            intent.putExtra("owner", aircraftDescriptor.getOwner());
            intent.putExtra("homeBase", aircraftDescriptor.getHomeBase());
            intent.putExtra("model", aircraftDescriptor.getModel());
            intent.putExtra("freq", aircraftDescriptor.getFreq());
            intent.putExtra("tracked", aircraftDescriptor.isTracked());
            intent.putExtra("identified", aircraftDescriptor.isIdentified());
        }

        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(ReceiverBeacon receiverBeacon) {
        receiverMap.put(receiverBeacon.getId(), receiverBeacon);

        Intent intent = new Intent("RECEIVER-BEACON");

        // ReceiverBeacon
        intent.putExtra("cpuLoad", receiverBeacon.getCpuLoad());
        intent.putExtra("cpuTemp", receiverBeacon.getCpuTemp());
        intent.putExtra("freeRam", receiverBeacon.getFreeRam());
        intent.putExtra("totalRam", receiverBeacon.getTotalRam());
        intent.putExtra("ntpError", receiverBeacon.getNtpError());
        intent.putExtra("rtCrystalCorrection", receiverBeacon.getRtCrystalCorrection());
        intent.putExtra("recCrystalCorrection", receiverBeacon.getRecCrystalCorrection());
        intent.putExtra("recCrystalCorrectionFine", receiverBeacon.getRecCrystalCorrectionFine());
        intent.putExtra("recAbsCorrection", receiverBeacon.getRecAbsCorrection());
        intent.putExtra("recInputNoise", receiverBeacon.getRecInputNoise());
        intent.putExtra("serverName", receiverBeacon.getServerName());
        intent.putExtra("version", receiverBeacon.getVersion());
        intent.putExtra("platform", receiverBeacon.getPlatform());
        intent.putExtra("numericVersion", receiverBeacon.getNumericVersion());

        // OgnBeacon
        intent.putExtra("id", receiverBeacon.getId());
        intent.putExtra("timestamp", receiverBeacon.getTimestamp());
        intent.putExtra("lat", receiverBeacon.getLat());
        intent.putExtra("lon", receiverBeacon.getLon());
        intent.putExtra("alt", receiverBeacon.getAlt());
        intent.putExtra("track", receiverBeacon.getTrack());
        intent.putExtra("groundSpeed", receiverBeacon.getGroundSpeed());
        intent.putExtra("rawPacket", receiverBeacon.getRawPacket());

        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //RushCore.initialize(new AndroidInitializeConfig(getApplicationContext()));
        List<Class<? extends Rush>> classes = new ArrayList<>();
        classes.add(CustomAircraftDescriptor.class);
        AndroidInitializeConfig config = new AndroidInitializeConfig(getApplicationContext());
        config.setClasses(classes);
        RushCore.initialize(config);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        ognClient = AircraftDescriptorProviderHelper.getOgnClient();
        ognClient.subscribeToAircraftBeacons(this);
        ognClient.subscribeToReceiverBeacons(this);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_stat)
                .getNotification();

        String versionName = "?";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MapsActivity.class), 0);
        notification.setLatestEventInfo(this, "OGN Viewer", "Version " + versionName, pendingIntent);

        startForeground(R.string.notification_id, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String aprs_filter = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(R.string.key_aprsfilter_preference), "");

        if (connected == true) {
            ognClient.disconnect();
        }

        if (aprs_filter.isEmpty()) {
            ognClient.connect();
            connected = true;
            Toast.makeText(this, "Connected to OGN without filter", Toast.LENGTH_LONG).show();
        } else {
            ognClient.connect(aprs_filter);
            connected = true;
            Toast.makeText(this, "Connected to OGN. Filter: " + aprs_filter, Toast.LENGTH_LONG).show();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        ognClient.disconnect();
        connected = false;

        Toast.makeText(this, "Disconnected from OGN", Toast.LENGTH_LONG).show();
    }
}
