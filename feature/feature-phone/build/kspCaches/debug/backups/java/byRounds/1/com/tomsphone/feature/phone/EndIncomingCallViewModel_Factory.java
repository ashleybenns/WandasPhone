package com.tomsphone.feature.phone;

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

  public EndIncomingCallViewModel_Factory(Provider<CallManager> callManagerProvider) {
    this.callManagerProvider = callManagerProvider;
  }

  @Override
  public EndIncomingCallViewModel get() {
    return newInstance(callManagerProvider.get());
  }

  public static EndIncomingCallViewModel_Factory create(Provider<CallManager> callManagerProvider) {
    return new EndIncomingCallViewModel_Factory(callManagerProvider);
  }

  public static EndIncomingCallViewModel newInstance(CallManager callManager) {
    return new EndIncomingCallViewModel(callManager);
  }
}
