package com.tomsphone.feature.phone;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.telecom.CallManager;
import com.tomsphone.core.telecom.MissedCallNagManager;
import com.tomsphone.core.telecom.RingtonePlayer;
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
public final class IncomingCallViewModel_Factory implements Factory<IncomingCallViewModel> {
  private final Provider<CallManager> callManagerProvider;

  private final Provider<WandasTTS> ttsProvider;

  private final Provider<RingtonePlayer> ringtonePlayerProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<MissedCallNagManager> missedCallNagManagerProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public IncomingCallViewModel_Factory(Provider<CallManager> callManagerProvider,
      Provider<WandasTTS> ttsProvider, Provider<RingtonePlayer> ringtonePlayerProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.callManagerProvider = callManagerProvider;
    this.ttsProvider = ttsProvider;
    this.ringtonePlayerProvider = ringtonePlayerProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.missedCallNagManagerProvider = missedCallNagManagerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public IncomingCallViewModel get() {
    return newInstance(callManagerProvider.get(), ttsProvider.get(), ringtonePlayerProvider.get(), contactRepositoryProvider.get(), missedCallNagManagerProvider.get(), settingsRepositoryProvider.get());
  }

  public static IncomingCallViewModel_Factory create(Provider<CallManager> callManagerProvider,
      Provider<WandasTTS> ttsProvider, Provider<RingtonePlayer> ringtonePlayerProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new IncomingCallViewModel_Factory(callManagerProvider, ttsProvider, ringtonePlayerProvider, contactRepositoryProvider, missedCallNagManagerProvider, settingsRepositoryProvider);
  }

  public static IncomingCallViewModel newInstance(CallManager callManager, WandasTTS tts,
      RingtonePlayer ringtonePlayer, ContactRepository contactRepository,
      MissedCallNagManager missedCallNagManager, SettingsRepository settingsRepository) {
    return new IncomingCallViewModel(callManager, tts, ringtonePlayer, contactRepository, missedCallNagManager, settingsRepository);
  }
}
