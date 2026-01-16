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
public final class EndOutgoingCallViewModel_Factory implements Factory<EndOutgoingCallViewModel> {
  private final Provider<CallManager> callManagerProvider;

  public EndOutgoingCallViewModel_Factory(Provider<CallManager> callManagerProvider) {
    this.callManagerProvider = callManagerProvider;
  }

  @Override
  public EndOutgoingCallViewModel get() {
    return newInstance(callManagerProvider.get());
  }

  public static EndOutgoingCallViewModel_Factory create(Provider<CallManager> callManagerProvider) {
    return new EndOutgoingCallViewModel_Factory(callManagerProvider);
  }

  public static EndOutgoingCallViewModel newInstance(CallManager callManager) {
    return new EndOutgoingCallViewModel(callManager);
  }
}
