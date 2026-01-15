package com.tomsphone.core.data;

import com.tomsphone.core.data.local.WandasDatabase;
import com.tomsphone.core.data.local.dao.CallLogDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_Companion_ProvideCallLogDaoFactory implements Factory<CallLogDao> {
  private final Provider<WandasDatabase> databaseProvider;

  public DataModule_Companion_ProvideCallLogDaoFactory(Provider<WandasDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CallLogDao get() {
    return provideCallLogDao(databaseProvider.get());
  }

  public static DataModule_Companion_ProvideCallLogDaoFactory create(
      Provider<WandasDatabase> databaseProvider) {
    return new DataModule_Companion_ProvideCallLogDaoFactory(databaseProvider);
  }

  public static CallLogDao provideCallLogDao(WandasDatabase database) {
    return Preconditions.checkNotNullFromProvides(DataModule.Companion.provideCallLogDao(database));
  }
}
