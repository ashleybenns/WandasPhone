package com.tomsphone.feature.carer;

import android.content.Context;
import com.tomsphone.core.config.SettingsRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class CarerSettingsViewModel_Factory implements Factory<CarerSettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public CarerSettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public CarerSettingsViewModel get() {
    return newInstance(contextProvider.get(), settingsRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static CarerSettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new CarerSettingsViewModel_Factory(contextProvider, settingsRepositoryProvider, contactRepositoryProvider);
  }

  public static CarerSettingsViewModel newInstance(Context context,
      SettingsRepository settingsRepository, ContactRepository contactRepository) {
    return new CarerSettingsViewModel(context, settingsRepository, contactRepository);
  }
}
