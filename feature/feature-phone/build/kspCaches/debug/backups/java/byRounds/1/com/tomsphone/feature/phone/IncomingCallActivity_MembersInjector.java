package com.tomsphone.feature.phone;

import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.telecom.CallManager;
import com.tomsphone.core.telecom.MissedCallNagManager;
import com.tomsphone.core.telecom.RingtonePlayer;
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
public final class IncomingCallActivity_MembersInjector implements MembersInjector<IncomingCallActivity> {
  private final Provider<CallManager> callManagerProvider;

  private final Provider<WandasTTS> ttsProvider;

  private final Provider<RingtonePlayer> ringtonePlayerProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<MissedCallNagManager> missedCallNagManagerProvider;

  public IncomingCallActivity_MembersInjector(Provider<CallManager> callManagerProvider,
      Provider<WandasTTS> ttsProvider, Provider<RingtonePlayer> ringtonePlayerProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider) {
    this.callManagerProvider = callManagerProvider;
    this.ttsProvider = ttsProvider;
    this.ringtonePlayerProvider = ringtonePlayerProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.missedCallNagManagerProvider = missedCallNagManagerProvider;
  }

  public static MembersInjector<IncomingCallActivity> create(
      Provider<CallManager> callManagerProvider, Provider<WandasTTS> ttsProvider,
      Provider<RingtonePlayer> ringtonePlayerProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider) {
    return new IncomingCallActivity_MembersInjector(callManagerProvider, ttsProvider, ringtonePlayerProvider, contactRepositoryProvider, missedCallNagManagerProvider);
  }

  @Override
  public void injectMembers(IncomingCallActivity instance) {
    injectCallManager(instance, callManagerProvider.get());
    injectTts(instance, ttsProvider.get());
    injectRingtonePlayer(instance, ringtonePlayerProvider.get());
    injectContactRepository(instance, contactRepositoryProvider.get());
    injectMissedCallNagManager(instance, missedCallNagManagerProvider.get());
  }

  @InjectedFieldSignature("com.tomsphone.feature.phone.IncomingCallActivity.callManager")
  public static void injectCallManager(IncomingCallActivity instance, CallManager callManager) {
    instance.callManager = callManager;
  }

  @InjectedFieldSignature("com.tomsphone.feature.phone.IncomingCallActivity.tts")
  public static void injectTts(IncomingCallActivity instance, WandasTTS tts) {
    instance.tts = tts;
  }

  @InjectedFieldSignature("com.tomsphone.feature.phone.IncomingCallActivity.ringtonePlayer")
  public static void injectRingtonePlayer(IncomingCallActivity instance,
      RingtonePlayer ringtonePlayer) {
    instance.ringtonePlayer = ringtonePlayer;
  }

  @InjectedFieldSignature("com.tomsphone.feature.phone.IncomingCallActivity.contactRepository")
  public static void injectContactRepository(IncomingCallActivity instance,
      ContactRepository contactRepository) {
    instance.contactRepository = contactRepository;
  }

  @InjectedFieldSignature("com.tomsphone.feature.phone.IncomingCallActivity.missedCallNagManager")
  public static void injectMissedCallNagManager(IncomingCallActivity instance,
      MissedCallNagManager missedCallNagManager) {
    instance.missedCallNagManager = missedCallNagManager;
  }
}
