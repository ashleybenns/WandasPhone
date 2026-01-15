package com.tomsphone.feature.carer;

import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.ContactRepository;
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
public final class CarerViewModel_Factory implements Factory<CarerViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public CarerViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public CarerViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static CarerViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new CarerViewModel_Factory(settingsRepositoryProvider, contactRepositoryProvider);
  }

  public static CarerViewModel newInstance(SettingsRepository settingsRepository,
      ContactRepository contactRepository) {
    return new CarerViewModel(settingsRepository, contactRepository);
  }
}
