package com.tomsphone.feature.phone;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.telecom.CallManager;
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
public final class EndIncomingCallViewModel_Factory implements Factory<EndIncomingCallViewModel> {
  private final Provider<CallManager> callManagerProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public EndIncomingCallViewModel_Factory(Provider<CallManager> callManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.callManagerProvider = callManagerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public EndIncomingCallViewModel get() {
    return newInstance(callManagerProvider.get(), settingsRepositoryProvider.get());
  }

  public static EndIncomingCallViewModel_Factory create(Provider<CallManager> callManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new EndIncomingCallViewModel_Factory(callManagerProvider, settingsRepositoryProvider);
  }

  public static EndIncomingCallViewModel newInstance(CallManager callManager,
      SettingsRepository settingsRepository) {
    return new EndIncomingCallViewModel(callManager, settingsRepository);
  }
}
