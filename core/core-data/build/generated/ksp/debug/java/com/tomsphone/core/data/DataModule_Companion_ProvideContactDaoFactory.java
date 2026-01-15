package com.tomsphone.core.data;

import com.tomsphone.core.data.local.WandasDatabase;
import com.tomsphone.core.data.local.dao.ContactDao;
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
public final class DataModule_Companion_ProvideContactDaoFactory implements Factory<ContactDao> {
  private final Provider<WandasDatabase> databaseProvider;

  public DataModule_Companion_ProvideContactDaoFactory(Provider<WandasDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ContactDao get() {
    return provideContactDao(databaseProvider.get());
  }

  public static DataModule_Companion_ProvideContactDaoFactory create(
      Provider<WandasDatabase> databaseProvider) {
    return new DataModule_Companion_ProvideContactDaoFactory(databaseProvider);
  }

  public static ContactDao provideContactDao(WandasDatabase database) {
    return Preconditions.checkNotNullFromProvides(DataModule.Companion.provideContactDao(database));
  }
}
