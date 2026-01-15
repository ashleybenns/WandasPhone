package com.tomsphone.core.telecom;

import android.content.Context;
import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.CallLogRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.tts.WandasTTS;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class MissedCallNagManager_Factory implements Factory<MissedCallNagManager> {
  private final Provider<Context> contextProvider;

  private final Provider<CallLogRepository> callLogRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<WandasTTS> ttsProvider;

  private final Provider<RingtonePlayer> ringtonePlayerProvider;

  public MissedCallNagManager_Factory(Provider<Context> contextProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<WandasTTS> ttsProvider,
      Provider<RingtonePlayer> ringtonePlayerProvider) {
    this.contextProvider = contextProvider;
    this.callLogRepositoryProvider = callLogRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.ttsProvider = ttsProvider;
    this.ringtonePlayerProvider = ringtonePlayerProvider;
  }

  @Override
  public MissedCallNagManager get() {
    return newInstance(contextProvider.get(), callLogRepositoryProvider.get(), contactRepositoryProvider.get(), settingsRepositoryProvider.get(), ttsProvider.get(), ringtonePlayerProvider.get());
  }

  public static MissedCallNagManager_Factory create(Provider<Context> contextProvider,
      Provider<CallLogRepository> callLogRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<WandasTTS> ttsProvider,
      Provider<RingtonePlayer> ringtonePlayerProvider) {
    return new MissedCallNagManager_Factory(contextProvider, callLogRepositoryProvider, contactRepositoryProvider, settingsRepositoryProvider, ttsProvider, ringtonePlayerProvider);
  }

  public static MissedCallNagManager newInstance(Context context,
      CallLogRepository callLogRepository, ContactRepository contactRepository,
      SettingsRepository settingsRepository, WandasTTS tts, RingtonePlayer ringtonePlayer) {
    return new MissedCallNagManager(context, callLogRepository, contactRepository, settingsRepository, tts, ringtonePlayer);
  }
}
