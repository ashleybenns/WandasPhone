package com.tomsphone.core.telecom;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.CallLogRepository;
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
public final class WandasCallScreeningService_MembersInjector implements MembersInjector<WandasCallScreeningService> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  private final Provider<WandasTTS> ttsProvider;

  private final Provider<CallManagerImpl> callManagerProvider;

  private final Provider<RingtonePlayer> ringtonePlayerProvider;

  public WandasCallScreeningService_MembersInjector(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider, Provider<WandasTTS> ttsProvider,
      Provider<CallManagerImpl> callManagerProvider,
      Provider<RingtonePlayer> ringtonePlayerProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
    this.ttsProvider = ttsProvider;
    this.callManagerProvider = callManagerProvider;
    this.ringtonePlayerProvider = ringtonePlayerProvider;
  }

  public static MembersInjector<WandasCallScreeningService> create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallLogRepository> callLogRepositoryProvider, Provider<WandasTTS> ttsProvider,
      Provider<CallManagerImpl> callManagerProvider,
      Provider<RingtonePlayer> ringtonePlayerProvider) {
    return new WandasCallScreeningService_MembersInjector(settingsRepositoryProvider, contactRepositoryProvider, callLogRepositoryProvider, ttsProvider, callManagerProvider, ringtonePlayerProvider);
  }

  @Override
  public void injectMembers(WandasCallScreeningService instance) {
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectContactRepository(instance, contactRepositoryProvider.get());
    injectCallLogRepository(instance, callLogRepositoryProvider.get());
    injectTts(instance, ttsProvider.get());
    injectCallManager(instance, callManagerProvider.get());
    injectRingtonePlayer(instance, ringtonePlayerProvider.get());
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasCallScreeningService.settingsRepository")
  public static void injectSettingsRepository(WandasCallScreeningService instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasCallScreeningService.contactRepository")
  public static void injectContactRepository(WandasCallScreeningService instance,
      ContactRepository contactRepository) {
    instance.contactRepository = contactRepository;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasCallScreeningService.callLogRepository")
  public static void injectCallLogRepository(WandasCallScreeningService instance,
      CallLogRepository callLogRepository) {
    instance.callLogRepository = callLogRepository;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasCallScreeningService.tts")
  public static void injectTts(WandasCallScreeningService instance, WandasTTS tts) {
    instance.tts = tts;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasCallScreeningService.callManager")
  public static void injectCallManager(WandasCallScreeningService instance,
      CallManagerImpl callManager) {
    instance.callManager = callManager;
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.WandasCallScreeningService.ringtonePlayer")
  public static void injectRingtonePlayer(WandasCallScreeningService instance,
      RingtonePlayer ringtonePlayer) {
    instance.ringtonePlayer = ringtonePlayer;
  }
}
