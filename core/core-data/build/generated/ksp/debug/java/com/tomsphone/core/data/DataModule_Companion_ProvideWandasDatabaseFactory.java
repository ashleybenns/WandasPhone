package com.tomsphone.core.data;

import android.content.Context;
import com.tomsphone.core.data.local.WandasDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_Companion_ProvideWandasDatabaseFactory implements Factory<WandasDatabase> {
  private final Provider<Context> contextProvider;

  public DataModule_Companion_ProvideWandasDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WandasDatabase get() {
    return provideWandasDatabase(contextProvider.get());
  }

  public static DataModule_Companion_ProvideWandasDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DataModule_Companion_ProvideWandasDatabaseFactory(contextProvider);
  }

  public static WandasDatabase provideWandasDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DataModule.Companion.provideWandasDatabase(context));
  }
}
