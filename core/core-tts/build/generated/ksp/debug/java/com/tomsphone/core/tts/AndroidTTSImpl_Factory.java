package com.tomsphone.core.tts;

import android.content.Context;
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
public final class AndroidTTSImpl_Factory implements Factory<AndroidTTSImpl> {
  private final Provider<Context> contextProvider;

  public AndroidTTSImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AndroidTTSImpl get() {
    return newInstance(contextProvider.get());
  }

  public static AndroidTTSImpl_Factory create(Provider<Context> contextProvider) {
    return new AndroidTTSImpl_Factory(contextProvider);
  }

  public static AndroidTTSImpl newInstance(Context context) {
    return new AndroidTTSImpl(context);
  }
}
