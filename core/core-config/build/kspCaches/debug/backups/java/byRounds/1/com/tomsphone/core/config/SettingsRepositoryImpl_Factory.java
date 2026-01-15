package com.tomsphone.core.config;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

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
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<Json> jsonProvider;

  public SettingsRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<Json> jsonProvider) {
    this.contextProvider = contextProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(contextProvider.get(), jsonProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<Json> jsonProvider) {
    return new SettingsRepositoryImpl_Factory(contextProvider, jsonProvider);
  }

  public static SettingsRepositoryImpl newInstance(Context context, Json json) {
    return new SettingsRepositoryImpl(context, json);
  }
}
