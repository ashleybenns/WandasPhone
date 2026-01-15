package com.tomsphone.core.telecom;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.tts.WandasTTS;
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
public final class WandasInCallService_MembersInjector implements MembersInjector<WandasInCallService> {
  private final Provider<CallManagerImpl> callManagerProvider;

  private final Provider<WandasTTS> ttsProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public WandasInCallService_MembersInjector(Provider<CallManagerImpl> callManagerProvider,
      Provider<WandasTTS> ttsProvider, Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.callManagerProvider = callManagerProvider;
    this.ttsProvider = ttsProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  public static MembersInjector<WandasInCallService> create(
      Provider<CallManagerImpl> callManagerProvider, Provider<WandasTTS> ttsProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new WandasInCallService_MembersInjector(callManagerProvider, ttsProvider, settingsRepositoryProvider, contactRepositoryProvider);
  }

  @Override
  public void injectMembers(WandasInCallService instance) {
    injectCallManager(instance, callManagerProvider.get());
    injectTts(instance, ttsProvider.get());
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectContactRepository(instance, contactRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasInCallService.callManager")
  public static void injectCallManager(WandasInCallService instance, CallManagerImpl callManager) {
    instance.callManager = callManager;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasInCallService.tts")
  public static void injectTts(WandasInCallService instance, WandasTTS tts) {
    instance.tts = tts;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasInCallService.settingsRepository")
  public static void injectSettingsRepository(WandasInCallService instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasInCallService.contactRepository")
  public static void injectContactRepository(WandasInCallService instance,
      ContactRepository contactRepository) {
    instance.contactRepository = contactRepository;
  }
}
