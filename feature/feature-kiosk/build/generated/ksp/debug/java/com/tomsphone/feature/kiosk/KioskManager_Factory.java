package com.tomsphone.feature.kiosk;

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
public final class KioskManager_Factory implements Factory<KioskManager> {
  private final Provider<Context> contextProvider;

  public KioskManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public KioskManager get() {
    return newInstance(contextProvider.get());
  }

  public static KioskManager_Factory create(Provider<Context> contextProvider) {
    return new KioskManager_Factory(contextProvider);
  }

  public static KioskManager newInstance(Context context) {
    return new KioskManager(context);
  }
}
