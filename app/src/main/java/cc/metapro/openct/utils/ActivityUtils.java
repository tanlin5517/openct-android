package cc.metapro.openct.utils;

/*
 *  Copyright 2016 - 2017 metapro.cc Jeffctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.common.base.Strings;

import cc.metapro.openct.R;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActivityUtils {

    private static final String TAG = "ENCRYPTION";
    private static ProgressDialog pd;

    public static void addFragmentToActivity(@NonNull FragmentManager fragmentManager,
                                             @NonNull Fragment fragment, int frameId) {
        checkNotNull(fragmentManager);
        checkNotNull(fragment);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(frameId, fragment);
        transaction.commit();
    }

    public static ProgressDialog getProgressDialog(Context context, int messageId) {
        pd = new ProgressDialog(context);
        pd.setMessage(context.getString(messageId));
        return pd;
    }

    public static void dismissProgressDialog() {
        if (pd == null) return;
        if (pd.isShowing()) {
            pd.dismiss();
        }
    }

    public static void encryptionCheck(final Context context) {
        Observable
                .create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> e) throws Exception {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        boolean needEncrypt = preferences.getBoolean(context.getString(R.string.need_encryption), false);
                        boolean cmsPasswordEncrypted = preferences.getBoolean(context.getString(R.string.pref_cms_password_encrypted), false);
                        boolean libPasswordEncrypted = preferences.getBoolean(context.getString(R.string.pref_lib_password_encrypted), false);

                        // 设置不加密, 将加密的部分还原
                        if (!needEncrypt) {
                            SharedPreferences.Editor editor = preferences.edit();
                            if (cmsPasswordEncrypted) {
                                editor.putBoolean(context.getString(R.string.pref_cms_password_encrypted), false);
                                String password = preferences.getString(context.getString(R.string.pref_cms_password), "");
                                password = EncryptionUtils.decrypt(Constants.seed, password);
                                editor.putString(context.getString(R.string.pref_cms_password), password);
                            }
                            if (libPasswordEncrypted) {
                                editor.putBoolean(context.getString(R.string.pref_lib_password_encrypted), false);
                                String password = preferences.getString(context.getString(R.string.pref_lib_password), "");
                                password = EncryptionUtils.decrypt(Constants.seed, password);
                                editor.putString(context.getString(R.string.pref_lib_password), password);
                            }
                            editor.apply();
                        }
                        // 设置加密, 将未加密的部分加密
                        else {
                            if (!cmsPasswordEncrypted) {
                                String cmsPassword = preferences.getString(context.getString(R.string.pref_cms_password), "");
                                try {
                                    if (!Strings.isNullOrEmpty(cmsPassword)) {
                                        cmsPassword = EncryptionUtils.encrypt(Constants.seed, cmsPassword);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString(context.getString(R.string.pref_cms_password), cmsPassword);
                                        editor.putBoolean(context.getString(R.string.pref_cms_password_encrypted), true);
                                        editor.apply();
                                    }
                                } catch (Exception exp) {
                                    Log.e(TAG, exp.getMessage(), exp);
                                }
                            }
                            if (!libPasswordEncrypted) {
                                try {
                                    String libPassword = preferences.getString(context.getString(R.string.pref_lib_password), "");
                                    if (!Strings.isNullOrEmpty(libPassword)) {
                                        libPassword = EncryptionUtils.encrypt(Constants.seed, libPassword);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString(context.getString(R.string.pref_cms_password), libPassword);
                                        editor.putBoolean(context.getString(R.string.pref_lib_password_encrypted), true);
                                        editor.apply();
                                    }
                                } catch (Exception exp) {
                                    Log.e(TAG, exp.getMessage(), exp);
                                }
                            }
                        }
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .onErrorReturn(new Function<Throwable, String>() {
                    @Override
                    public String apply(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getMessage(), throwable);
                        return "";
                    }
                }).subscribe();
    }

}
