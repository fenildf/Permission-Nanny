package com.permissionnanny;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.util.ArrayMap;
import com.permissionnanny.common.BundleUtil;
import com.permissionnanny.data.OngoingRequestDB;
import com.permissionnanny.lib.Nanny;
import com.permissionnanny.lib.request.RequestParams;
import com.permissionnanny.lib.request.simple.AccountRequest;
import com.permissionnanny.lib.request.simple.LocationRequest;
import com.permissionnanny.simple.ProxyAccountManagerListener;
import com.permissionnanny.simple.ProxyGpsStatusListener;
import com.permissionnanny.simple.ProxyNmeaListener;
import com.permissionnanny.simple.ProxyOnAccountsUpdateListener;
import com.permissionnanny.simple.RequestLocationUpdatesListener;
import com.permissionnanny.simple.RequestSingleUpdateListener;
import java.security.SecureRandom;
import java.util.Map;
import javax.inject.Inject;
import timber.log.Timber;

/**
 *
 */
public class ProxyService extends BaseService {

    public static final String CLIENT_ADDR = "clientAddr";
    public static final String REQUEST_PARAMS = "requestParams";

    private Map<String, ProxyClient> mClients = new ArrayMap<>();
    private AckReceiver mAckReceiver = new AckReceiver();
    private String mAckServerAddr;

    @Inject OngoingRequestDB mDB;

    @Override
    public void onCreate() {
        super.onCreate();
        getComponent().inject(this);
        mAckServerAddr = Long.toString(new SecureRandom().nextLong());
        registerReceiver(mAckReceiver, new IntentFilter(mAckServerAddr));
        Timber.wtf("init service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { // Service killed by OS? Restore client state
            restoreState();
            return super.onStartCommand(null, flags, startId);
        }
        Timber.wtf("Server started with args: " + BundleUtil.toString(intent));
        String clientId = intent.getStringExtra(CLIENT_ADDR);
        RequestParams requestParams = intent.getParcelableExtra(REQUEST_PARAMS);
        handleRequest(clientId, requestParams, true);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mAckReceiver);
    }

    private void restoreState() {
        int count = 0;
        ArrayMap<String, RequestParams> requests = mDB.getOngoingRequests();
        for (int i = 0, len = requests.size(); i < len; i++) {
            handleRequest(requests.keyAt(i), requests.valueAt(i), false);
        }
        Timber.wtf("restored " + count + " clients");
    }

    /**
     * @param clientAddr    Client address
     * @param requestParams Client request
     * @param cacheRequest  Flag that controls caching request to disk
     */
    private void handleRequest(String clientAddr, RequestParams requestParams, boolean cacheRequest) {
        Timber.wtf("handling client=" + clientAddr + " req=" + requestParams);
        ProxyListener listener;
        switch (requestParams.opCode) {
            case AccountRequest.ADD_ON_ACCOUNTS_UPDATED_LISTENER:
                listener = new ProxyOnAccountsUpdateListener(this, clientAddr);
                break;
            case AccountRequest.GET_ACCOUNTS_BY_TYPE_AND_FEATURES:
                listener = new ProxyAccountManagerListener.GetAccountsByTypeAndFeatures(this, clientAddr);
                break;
            case AccountRequest.GET_AUTH_TOKEN1:
                listener = new ProxyAccountManagerListener.GetAuthToken1(this, clientAddr);
                break;
            case AccountRequest.GET_AUTH_TOKEN2:
                listener = new ProxyAccountManagerListener.GetAuthToken2(this, clientAddr);
                break;
            case AccountRequest.HAS_FEATURES:
                listener = new ProxyAccountManagerListener.HasFeatures(this, clientAddr);
                break;
            case AccountRequest.REMOVE_ACCOUNT:
                listener = new ProxyAccountManagerListener.RemoveAccount(this, clientAddr);
                break;
            case AccountRequest.RENAME_ACCOUNT:
                listener = new ProxyAccountManagerListener.RenameAccount(this, clientAddr);
                break;
            case LocationRequest.ADD_GPS_STATUS_LISTENER:
                listener = new ProxyGpsStatusListener(this, clientAddr);
                break;
            case LocationRequest.ADD_NMEA_LISTENER:
                listener = new ProxyNmeaListener(this, clientAddr);
                break;
            case LocationRequest.REQUEST_LOCATION_UPDATES1:
                listener = new RequestLocationUpdatesListener.Api1(this, clientAddr);
                break;
            case LocationRequest.REQUEST_LOCATION_UPDATES2:
                listener = new RequestLocationUpdatesListener.Api2(this, clientAddr);
                break;
            case LocationRequest.REQUEST_SINGLE_UPDATE:
                listener = new RequestSingleUpdateListener.Api(this, clientAddr);
                break;
            case LocationRequest.REQUEST_SINGLE_UPDATE1:
                listener = new RequestSingleUpdateListener.Api1(this, clientAddr);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported opcode " + requestParams.opCode);
        }

        Bundle response = startRequest(clientAddr, requestParams, listener, cacheRequest);
        Intent intent = new Intent(clientAddr).putExtras(response);
        sendBroadcast(intent);
    }

    /**
     * @param clientAddr   Client address
     * @param params       Client request
     * @param listener
     * @param cacheRequest {@code true} to cache request to disk after registration succeeds
     * @return Response bundle to return to the client
     */
    private Bundle startRequest(String clientAddr, RequestParams params, ProxyListener listener, boolean cacheRequest) {
        try {
            listener.register(this, params);
        } catch (Throwable error) {
            return ResponseFactory.INSTANCE.newBadRequestResponse(Nanny.AUTHORIZATION_SERVICE, error).build();
        }

        // Good request? Cache request to memory and disk
        mClients.put(clientAddr, new ProxyClient(clientAddr, params, listener));
        if (cacheRequest) {
            mDB.putOngoingRequest(clientAddr, params);
        }
        return ResponseFactory.INSTANCE.newAllowResponse(Nanny.AUTHORIZATION_SERVICE).build();
    }

    public void removeProxyClient(String clientAddr) {
        mClients.remove(clientAddr);
        mDB.delOngoingRequest(clientAddr);
        if (mClients.isEmpty()) { // no more clients? kill service
            stopSelf();
        }
    }

    public String getAckAddress() {
        return mAckServerAddr;
    }

    class AckReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: validate
            String clientAddr = intent.getStringExtra(Nanny.CLIENT_ADDRESS);
            ProxyClient client = mClients.get(clientAddr);
            if (client != null) {
                client.mListener.updateAck(SystemClock.elapsedRealtime());
            }
        }
    }
}
