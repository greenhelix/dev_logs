#### adb 를 연결한다. 

#### pm list packages -f | grep {명청}
#### pm list packages -f
#### exit
#### adb pull /system/priv-app/SeiTvSettings/SeiTvSettings.apk
ex)
```bash
package:/data/app/com.kakao.talk-wuLPpyq8x_j7b7eUR_6vGg==/base.apk=com.kakao.talk
package:/data/app/com.kakao.taxi-_6AcQILJ-fTin_aO2jbH_Q==/base.apk=com.kakao.taxi
SEI804DT:/ $ pm list packages -f | grep settings
package:/system/priv-app/SeiTvSettings/SeiTvSettings.apk=com.android.tv.settings
package:/vendor/overlay/DroidTvSettings__auto_generated_rro_vendor.apk=com.droidlogic.tv.settings.auto_generated_rro_vendor__
package:/product/overlay/TvSettingsGmsOverlay.apk=com.android.tv.settings.gms.resoverlay
package:/system_ext/priv-app/DroidTvSettings/DroidTvSettings.apk=com.droidlogic.tv.settings
package:/product/overlay/TvSettingsProviderOverlay.apk=com.android.tv.overlay.settingsprovider
package:/product/priv-app/SettingsIntelligence/SettingsIntelligence.apk=com.android.settings.intelligence
package:/system/priv-app/SettingsProvider/SettingsProvider.apk=com.android.providers.settings
package:/product/overlay/SettingsProvider__auto_generated_rro_product.apk=com.android.providers.settings.auto_generated_rro_product__
package:/vendor/overlay/SettingsProvider__auto_generated_rro_vendor.apk=com.android.providers.settings.auto_generated_rro_vendor__
package:/product/overlay/DroidATVTvSettingsResOverlay.apk=com.droidlogic.tv.settings.resoverlay
package:/vendor/overlay/SeiTvSettings__auto_generated_rro_vendor.apk=com.android.tv.settings.auto_generated_rro_vendor__
SEI804DT:/ $ exit

C:\GreenHelix>adb pull /system/priv-app/SeiTvSettings/SeiTvSettings.apk
/system/priv-app/SeiTvSettings/SeiTvSettings.apk: 1 file pulled, 0 skipped. 23.6 MB/s (39548724 bytes in 1.599s)
```




===================================


