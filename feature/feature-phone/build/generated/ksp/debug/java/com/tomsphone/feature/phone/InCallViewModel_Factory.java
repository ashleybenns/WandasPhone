package com.tomsphone.feature.phone;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.CallLogRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.telecom.CallManager;
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
public final class InCallViewModel_Factory implements Factory<InCallViewModel> {
  private final Provider<CallManager> callManagerProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<WandasTTS> ttsProvider;

  public InCallViewModel_Factory(Provider<CallManager> callManagerProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<WandasTTS> ttsProvider) {
    this.callManagerProvider = callManagerProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.ttsProvider = ttsProvider;
  }

  @Override
  public InCallViewModel get() {
    return newInstance(callManagerProvider.get(), contactRepositoryProvider.get(), callLogRepositoryProvider.get(), settingsRepositoryProvider.get(), ttsProvider.get());
  }

  public static InCallViewModel_Factory create(Provider<CallManager> callManagerProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<WandasTTS> ttsProvider) {
    return new InCallViewModel_Factory(callManagerProvider, contactRepositoryProvider, callLogRepositoryProvider, settingsRepositoryProvider, ttsProvider);
  }

  public static InCallViewModel newInstance(CallManager callManager,
      ContactRepository contactRepository, CallLogRepository callLogRepository,
      SettingsRepository settingsRepository, WandasTTS tts) {
    return new InCallViewModel(callManager, contactRepository, callLogRepository, settingsRepository, tts);
  }
}
