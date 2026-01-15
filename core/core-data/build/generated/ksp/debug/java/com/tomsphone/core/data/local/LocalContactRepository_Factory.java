package com.tomsphone.core.data.local;

import com.tomsphone.core.data.local.dao.ContactDao;
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
public final class LocalContactRepository_Factory implements Factory<LocalContactRepository> {
  private final Provider<ContactDao> contactDaoProvider;

  public LocalContactRepository_Factory(Provider<ContactDao> contactDaoProvider) {
    this.contactDaoProvider = contactDaoProvider;
  }

  @Override
  public LocalContactRepository get() {
    return newInstance(contactDaoProvider.get());
  }

  public static LocalContactRepository_Factory create(Provider<ContactDao> contactDaoProvider) {
    return new LocalContactRepository_Factory(contactDaoProvider);
  }

  public static LocalContactRepository newInstance(ContactDao contactDao) {
    return new LocalContactRepository(contactDao);
  }
}
