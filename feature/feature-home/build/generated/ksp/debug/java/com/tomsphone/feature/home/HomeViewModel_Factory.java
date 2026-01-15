package com.tomsphone.feature.home;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.telecom.CallManager;
import com.tomsphone.core.telecom.MissedCallNagManager;
import com.tomsphone.core.tts.WandasTTS;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallManager> callManagerProvider;

  private final Provider<MissedCallNagManager> missedCallNagManagerProvider;

  private final Provider<WandasTTS> ttsProvider;

  public HomeViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallManager> callManagerProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider,
      Provider<WandasTTS> ttsProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callManagerProvider = callManagerProvider;
    this.missedCallNagManagerProvider = missedCallNagManagerProvider;
    this.ttsProvider = ttsProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), contactRepositoryProvider.get(), callManagerProvider.get(), missedCallNagManagerProvider.get(), ttsProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallManager> callManagerProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider,
      Provider<WandasTTS> ttsProvider) {
    return new HomeViewModel_Factory(settingsRepositoryProvider, contactRepositoryProvider, callManagerProvider, missedCallNagManagerProvider, ttsProvider);
  }

  public static HomeViewModel newInstance(SettingsRepository settingsRepository,
      ContactRepository contactRepository, CallManager callManager,
      MissedCallNagManager missedCallNagManager, WandasTTS tts) {
    return new HomeViewModel(settingsRepository, contactRepository, callManager, missedCallNagManager, tts);
  }
}
