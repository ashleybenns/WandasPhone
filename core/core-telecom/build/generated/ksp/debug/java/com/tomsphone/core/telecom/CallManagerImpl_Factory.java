package com.tomsphone.core.telecom;

import android.content.Context;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
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
public final class CallManagerImpl_Factory implements Factory<CallManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<MissedCallNagManager> missedCallNagManagerProvider;

  public CallManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider) {
    this.contextProvider = contextProvider;
    this.missedCallNagManagerProvider = missedCallNagManagerProvider;
  }

  @Override
  public CallManagerImpl get() {
    return newInstance(contextProvider.get(), DoubleCheck.lazy(missedCallNagManagerProvider));
  }

  public static CallManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<MissedCallNagManager> missedCallNagManagerProvider) {
    return new CallManagerImpl_Factory(contextProvider, missedCallNagManagerProvider);
  }

  public static CallManagerImpl newInstance(Context context,
      Lazy<MissedCallNagManager> missedCallNagManager) {
    return new CallManagerImpl(context, missedCallNagManager);
  }
}
