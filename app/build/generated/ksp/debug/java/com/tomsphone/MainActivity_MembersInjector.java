package com.tomsphone;

import com.tomsphone.core.config.SettingsRepository;
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

  public MainActivity_MembersInjector(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CallManager> callManagerProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.callManagerProvider = callManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CallManager> callManagerProvider) {
    return new MainActivity_MembersInjector(settingsRepositoryProvider, callManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectCallManager(instance, callManagerProvider.get());
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
}
