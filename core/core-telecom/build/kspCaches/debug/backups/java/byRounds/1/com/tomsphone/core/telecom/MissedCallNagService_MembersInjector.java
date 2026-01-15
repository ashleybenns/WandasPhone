package com.tomsphone.core.telecom;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MissedCallNagService_MembersInjector implements MembersInjector<MissedCallNagService> {
  private final Provider<MissedCallNagManager> missedCallNagManagerProvider;

  public MissedCallNagService_MembersInjector(
      Provider<MissedCallNagManager> missedCallNagManagerProvider) {
    this.missedCallNagManagerProvider = missedCallNagManagerProvider;
  }

  public static MembersInjector<MissedCallNagService> create(
      Provider<MissedCallNagManager> missedCallNagManagerProvider) {
    return new MissedCallNagService_MembersInjector(missedCallNagManagerProvider);
  }

  @Override
  public void injectMembers(MissedCallNagService instance) {
    injectMissedCallNagManager(instance, missedCallNagManagerProvider.get());
  }

  @InjectedFieldSignature("com.tomsphone.core.telecom.MissedCallNagService.missedCallNagManager")
  public static void injectMissedCallNagManager(MissedCallNagService instance,
      MissedCallNagManager missedCallNagManager) {
    instance.missedCallNagManager = missedCallNagManager;
  }
}
