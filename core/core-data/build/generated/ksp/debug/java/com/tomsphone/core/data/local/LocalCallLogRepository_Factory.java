package com.tomsphone.core.data.local;

import com.tomsphone.core.data.local.dao.CallLogDao;
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
public final class LocalCallLogRepository_Factory implements Factory<LocalCallLogRepository> {
  private final Provider<CallLogDao> callLogDaoProvider;

  public LocalCallLogRepository_Factory(Provider<CallLogDao> callLogDaoProvider) {
    this.callLogDaoProvider = callLogDaoProvider;
  }

  @Override
  public LocalCallLogRepository get() {
    return newInstance(callLogDaoProvider.get());
  }

  public static LocalCallLogRepository_Factory create(Provider<CallLogDao> callLogDaoProvider) {
    return new LocalCallLogRepository_Factory(callLogDaoProvider);
  }

  public static LocalCallLogRepository newInstance(CallLogDao callLogDao) {
    return new LocalCallLogRepository(callLogDao);
  }
}
