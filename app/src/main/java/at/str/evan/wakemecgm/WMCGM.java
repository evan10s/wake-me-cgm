package at.str.evan.wakemecgm;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class WMCGM extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("bgreadings.realm").build();
        Realm.setDefaultConfiguration(config);
    }
}
