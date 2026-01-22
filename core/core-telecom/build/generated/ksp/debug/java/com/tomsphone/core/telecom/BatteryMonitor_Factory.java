package com.tomsphone.core.telecom;

import android.content.Context;
import com.tomsphone.core.tts.WandasTTS;
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
public final class BatteryMonitor_Factory implements Factory<BatteryMonitor> {
  private final Provider<Context> contextProvider;

  private final Provider<WandasTTS> ttsProvider;

  public BatteryMonitor_Factory(Provider<Context> contextProvider,
      Provider<WandasTTS> ttsProvider) {
    this.contextProvider = contextProvider;
    this.ttsProvider = ttsProvider;
  }

  @Override
  public BatteryMonitor get() {
    return newInstance(contextProvider.get(), DoubleCheck.lazy(ttsProvider));
  }

  public static BatteryMonitor_Factory create(Provider<Context> contextProvider,
      Provider<WandasTTS> ttsProvider) {
    return new BatteryMonitor_Factory(contextProvider, ttsProvider);
  }

  public static BatteryMonitor newInstance(Context context, Lazy<WandasTTS> tts) {
    return new BatteryMonitor(context, tts);
  }
}
