package com.tomsphone.core.telecom;

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
public final class RingtonePlayer_Factory implements Factory<RingtonePlayer> {
  private final Provider<Context> contextProvider;

  public RingtonePlayer_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RingtonePlayer get() {
    return newInstance(contextProvider.get());
  }

  public static RingtonePlayer_Factory create(Provider<Context> contextProvider) {
    return new RingtonePlayer_Factory(contextProvider);
  }

  public static RingtonePlayer newInstance(Context context) {
    return new RingtonePlayer(context);
  }
}
