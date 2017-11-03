package com.winsun.fruitmix.equipment.search.data;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.github.druk.rxdnssd.RxDnssdEmbedded;
import com.winsun.fruitmix.CustomApplication;
import com.winsun.fruitmix.util.Util;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Created by Administrator on 2017/5/16.
 */

public class EquipmentSearchManager {

    public static final String TAG = EquipmentSearchManager.class.getSimpleName();

    private RxDnssd mRxDnssd;
    private Subscription mSubscription;

    private static final String SERVICE_PORT = "_http._tcp";
    private static final String DEMAIN = "local.";

    public static EquipmentSearchManager instance;

    private EquipmentSearchManager(Context context) {

        mRxDnssd = createDnssd(context);

    }

    private RxDnssd createDnssd(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Log.i(TAG, "Using embedded version of dns sd because of API < 16");
            return new RxDnssdEmbedded();
        }
        if (Build.VERSION.RELEASE.contains("4.4.2") && Build.MANUFACTURER.toLowerCase().contains("samsung")) {
            Log.i(TAG, "Using embedded version of dns sd because of Samsung 4.4.2");
            return new RxDnssdEmbedded();
        }
        Log.i(TAG, "Using systems dns sd daemon");
        return new RxDnssdBindable(context);
    }

    static EquipmentSearchManager getInstance(Context context) {

        if (instance == null) {
            instance = new EquipmentSearchManager(context);
        }

        return instance;
    }

    private void destroyInstance() {
        instance = null;
    }

    public void startDiscovery(final IEquipmentDiscoveryListener listener) {

        mSubscription = mRxDnssd.browse(SERVICE_PORT, DEMAIN)
                .compose(mRxDnssd.resolve())
                .compose(mRxDnssd.queryRecords())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<BonjourService>() {
                    @Override
                    public void call(BonjourService bonjourService) {

                        if (!Util.checkBonjourService(bonjourService)) return;

                        Equipment createdEquipment = Equipment.createEquipment(bonjourService);

                        if (createdEquipment == null) return;

                        Log.d(TAG, "call: createEquipment: " + createdEquipment + " listener: " + listener);

                        listener.call(createdEquipment);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "call: " + throwable);
                    }
                });

    }

    public void stopDiscovery() {

        if (mSubscription != null) {
            mSubscription.unsubscribe();

            mSubscription = null;
        }

        destroyInstance();
    }

    public interface IEquipmentDiscoveryListener {

        void call(Equipment equipment);

    }


}
