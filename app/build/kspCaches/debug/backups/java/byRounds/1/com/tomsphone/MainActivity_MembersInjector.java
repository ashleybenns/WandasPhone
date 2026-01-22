package com.tomsphone;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.telecom.BatteryMonitor;
import com.tomsphone.core.telecom.CallManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<CallManager> callManagerProvider;

  private final Provider<BatteryMonitor> batteryMonitorProvider;

  public MainActivity_MembersInjector(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CallManager> callManagerProvider, Provider<BatteryMonitor> batteryMonitorProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.callManagerProvider = callManagerProvider;
    this.batteryMonitorProvider = batteryMonitorProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CallManager> callManagerProvider, Provider<BatteryMonitor> batteryMonitorProvider) {
    return new MainActivity_MembersInjector(settingsRepositoryProvider, callManagerProvider, batteryMonitorProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectCallManager(instance, callManagerProvider.get());
    injectBatteryMonitor(instance, batteryMonitorProvider.get());
  }

  @InjectedFieldSignature("com.tomsphone.MainActivity.settingsRepository")
  public static void injectSettingsRepository(MainActivity instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.tomsphone.MainActivity.callManager")
  public static void injectCallManager(MainActivity instance, CallManager callManager) {
    instance.callManager = callManager;
  }

  @InjectedFieldSignature("com.tomsphone.MainActivity.batteryMonitor")
  public static void injectBatteryMonitor(MainActivity instance, BatteryMonitor batteryMonitor) {
    instance.batteryMonitor = batteryMonitor;
  }
}
