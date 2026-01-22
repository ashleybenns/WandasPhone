package com.tomsphone;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.tomsphone.core.config.ConfigModule_Companion_ProvideJsonFactory;
import com.tomsphone.core.config.SettingsRepositoryImpl;
import com.tomsphone.core.data.DataModule_Companion_ProvideCallLogDaoFactory;
import com.tomsphone.core.data.DataModule_Companion_ProvideContactDaoFactory;
import com.tomsphone.core.data.DataModule_Companion_ProvideWandasDatabaseFactory;
import com.tomsphone.core.data.local.LocalCallLogRepository;
import com.tomsphone.core.data.local.LocalContactRepository;
import com.tomsphone.core.data.local.WandasDatabase;
import com.tomsphone.core.data.local.dao.CallLogDao;
import com.tomsphone.core.data.local.dao.ContactDao;
import com.tomsphone.core.data.repository.CallLogRepository;
import com.tomsphone.core.data.repository.ContactRepository;
import com.tomsphone.core.telecom.BatteryMonitor;
import com.tomsphone.core.telecom.CallManagerImpl;
import com.tomsphone.core.telecom.MissedCallNagManager;
import com.tomsphone.core.telecom.MissedCallNagService;
import com.tomsphone.core.telecom.MissedCallNagService_MembersInjector;
import com.tomsphone.core.telecom.RingtonePlayer;
import com.tomsphone.core.telecom.WandasCallScreeningService;
import com.tomsphone.core.telecom.WandasCallScreeningService_MembersInjector;
import com.tomsphone.core.telecom.WandasInCallService;
import com.tomsphone.core.telecom.WandasInCallService_MembersInjector;
import com.tomsphone.core.tts.AndroidTTSImpl;
import com.tomsphone.core.tts.WandasTTS;
import com.tomsphone.feature.carer.CarerSettingsViewModel;
import com.tomsphone.feature.carer.CarerSettingsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.tomsphone.feature.home.HomeViewModel;
import com.tomsphone.feature.home.HomeViewModel_HiltModules_KeyModule_ProvideFactory;
import com.tomsphone.feature.phone.EndIncomingCallViewModel;
import com.tomsphone.feature.phone.EndIncomingCallViewModel_HiltModules_KeyModule_ProvideFactory;
import com.tomsphone.feature.phone.EndOutgoingCallViewModel;
import com.tomsphone.feature.phone.EndOutgoingCallViewModel_HiltModules_KeyModule_ProvideFactory;
import com.tomsphone.feature.phone.InCallViewModel;
import com.tomsphone.feature.phone.InCallViewModel_HiltModules_KeyModule_ProvideFactory;
import com.tomsphone.feature.phone.IncomingCallViewModel;
import com.tomsphone.feature.phone.IncomingCallViewModel_HiltModules_KeyModule_ProvideFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SetBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

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
public final class DaggerTomsPhoneApplication_HiltComponents_SingletonC {
  private DaggerTomsPhoneApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public TomsPhoneApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements TomsPhoneApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements TomsPhoneApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements TomsPhoneApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements TomsPhoneApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements TomsPhoneApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements TomsPhoneApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements TomsPhoneApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public TomsPhoneApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends TomsPhoneApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends TomsPhoneApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends TomsPhoneApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends TomsPhoneApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Set<String> getViewModelKeys() {
      return SetBuilder.<String>newSetBuilder(6).add(CarerSettingsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(EndIncomingCallViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(EndOutgoingCallViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(HomeViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(InCallViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(IncomingCallViewModel_HiltModules_KeyModule_ProvideFactory.provide()).build();
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectSettingsRepository(instance, singletonCImpl.settingsRepositoryImplProvider.get());
      MainActivity_MembersInjector.injectCallManager(instance, singletonCImpl.callManagerImplProvider.get());
      MainActivity_MembersInjector.injectBatteryMonitor(instance, singletonCImpl.batteryMonitorProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends TomsPhoneApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<CarerSettingsViewModel> carerSettingsViewModelProvider;

    private Provider<EndIncomingCallViewModel> endIncomingCallViewModelProvider;

    private Provider<EndOutgoingCallViewModel> endOutgoingCallViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<InCallViewModel> inCallViewModelProvider;

    private Provider<IncomingCallViewModel> incomingCallViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.carerSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.endIncomingCallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.endOutgoingCallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.inCallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.incomingCallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<String, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put("com.tomsphone.feature.carer.CarerSettingsViewModel", ((Provider) carerSettingsViewModelProvider)).put("com.tomsphone.feature.phone.EndIncomingCallViewModel", ((Provider) endIncomingCallViewModelProvider)).put("com.tomsphone.feature.phone.EndOutgoingCallViewModel", ((Provider) endOutgoingCallViewModelProvider)).put("com.tomsphone.feature.home.HomeViewModel", ((Provider) homeViewModelProvider)).put("com.tomsphone.feature.phone.InCallViewModel", ((Provider) inCallViewModelProvider)).put("com.tomsphone.feature.phone.IncomingCallViewModel", ((Provider) incomingCallViewModelProvider)).build();
    }

    @Override
    public Map<String, Object> getHiltViewModelAssistedMap() {
      return Collections.<String, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.tomsphone.feature.carer.CarerSettingsViewModel 
          return (T) new CarerSettingsViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.bindContactRepositoryProvider.get());

          case 1: // com.tomsphone.feature.phone.EndIncomingCallViewModel 
          return (T) new EndIncomingCallViewModel(singletonCImpl.callManagerImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get());

          case 2: // com.tomsphone.feature.phone.EndOutgoingCallViewModel 
          return (T) new EndOutgoingCallViewModel(singletonCImpl.callManagerImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get());

          case 3: // com.tomsphone.feature.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.bindContactRepositoryProvider.get(), singletonCImpl.callManagerImplProvider.get(), singletonCImpl.missedCallNagManagerProvider.get(), (WandasTTS) ((Provider) singletonCImpl.androidTTSImplProvider).get());

          case 4: // com.tomsphone.feature.phone.InCallViewModel 
          return (T) new InCallViewModel(singletonCImpl.callManagerImplProvider.get(), singletonCImpl.bindContactRepositoryProvider.get(), singletonCImpl.bindCallLogRepositoryProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get(), (WandasTTS) ((Provider) singletonCImpl.androidTTSImplProvider).get());

          case 5: // com.tomsphone.feature.phone.IncomingCallViewModel 
          return (T) new IncomingCallViewModel(singletonCImpl.callManagerImplProvider.get(), (WandasTTS) ((Provider) singletonCImpl.androidTTSImplProvider).get(), singletonCImpl.ringtonePlayerProvider.get(), singletonCImpl.bindContactRepositoryProvider.get(), singletonCImpl.missedCallNagManagerProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends TomsPhoneApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends TomsPhoneApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectMissedCallNagService(MissedCallNagService arg0) {
      injectMissedCallNagService2(arg0);
    }

    @Override
    public void injectWandasCallScreeningService(WandasCallScreeningService arg0) {
      injectWandasCallScreeningService2(arg0);
    }

    @Override
    public void injectWandasInCallService(WandasInCallService arg0) {
      injectWandasInCallService2(arg0);
    }

    private MissedCallNagService injectMissedCallNagService2(MissedCallNagService instance) {
      MissedCallNagService_MembersInjector.injectMissedCallNagManager(instance, singletonCImpl.missedCallNagManagerProvider.get());
      return instance;
    }

    private WandasCallScreeningService injectWandasCallScreeningService2(
        WandasCallScreeningService instance) {
      WandasCallScreeningService_MembersInjector.injectSettingsRepository(instance, singletonCImpl.settingsRepositoryImplProvider.get());
      WandasCallScreeningService_MembersInjector.injectContactRepository(instance, singletonCImpl.bindContactRepositoryProvider.get());
      WandasCallScreeningService_MembersInjector.injectCallLogRepository(instance, singletonCImpl.bindCallLogRepositoryProvider.get());
      WandasCallScreeningService_MembersInjector.injectTts(instance, (WandasTTS) ((Provider) singletonCImpl.androidTTSImplProvider).get());
      WandasCallScreeningService_MembersInjector.injectCallManager(instance, singletonCImpl.callManagerImplProvider.get());
      WandasCallScreeningService_MembersInjector.injectRingtonePlayer(instance, singletonCImpl.ringtonePlayerProvider.get());
      return instance;
    }

    private WandasInCallService injectWandasInCallService2(WandasInCallService instance) {
      WandasInCallService_MembersInjector.injectCallManager(instance, singletonCImpl.callManagerImplProvider.get());
      WandasInCallService_MembersInjector.injectTts(instance, (WandasTTS) ((Provider) singletonCImpl.androidTTSImplProvider).get());
      WandasInCallService_MembersInjector.injectSettingsRepository(instance, singletonCImpl.settingsRepositoryImplProvider.get());
      WandasInCallService_MembersInjector.injectContactRepository(instance, singletonCImpl.bindContactRepositoryProvider.get());
      WandasInCallService_MembersInjector.injectMissedCallNagManager(instance, DoubleCheck.lazy(singletonCImpl.missedCallNagManagerProvider));
      return instance;
    }
  }

  private static final class SingletonCImpl extends TomsPhoneApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<Json> provideJsonProvider;

    private Provider<SettingsRepositoryImpl> settingsRepositoryImplProvider;

    private Provider<WandasDatabase> provideWandasDatabaseProvider;

    private Provider<LocalCallLogRepository> localCallLogRepositoryProvider;

    private Provider<CallLogRepository> bindCallLogRepositoryProvider;

    private Provider<LocalContactRepository> localContactRepositoryProvider;

    private Provider<ContactRepository> bindContactRepositoryProvider;

    private Provider<AndroidTTSImpl> androidTTSImplProvider;

    private Provider<RingtonePlayer> ringtonePlayerProvider;

    private Provider<MissedCallNagManager> missedCallNagManagerProvider;

    private Provider<CallManagerImpl> callManagerImplProvider;

    private Provider<BatteryMonitor> batteryMonitorProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private CallLogDao callLogDao() {
      return DataModule_Companion_ProvideCallLogDaoFactory.provideCallLogDao(provideWandasDatabaseProvider.get());
    }

    private ContactDao contactDao() {
      return DataModule_Companion_ProvideContactDaoFactory.provideContactDao(provideWandasDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 1));
      this.settingsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepositoryImpl>(singletonCImpl, 0));
      this.provideWandasDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<WandasDatabase>(singletonCImpl, 5));
      this.localCallLogRepositoryProvider = new SwitchingProvider<>(singletonCImpl, 4);
      this.bindCallLogRepositoryProvider = DoubleCheck.provider((Provider) localCallLogRepositoryProvider);
      this.localContactRepositoryProvider = new SwitchingProvider<>(singletonCImpl, 6);
      this.bindContactRepositoryProvider = DoubleCheck.provider((Provider) localContactRepositoryProvider);
      this.androidTTSImplProvider = DoubleCheck.provider(new SwitchingProvider<AndroidTTSImpl>(singletonCImpl, 7));
      this.ringtonePlayerProvider = DoubleCheck.provider(new SwitchingProvider<RingtonePlayer>(singletonCImpl, 8));
      this.missedCallNagManagerProvider = DoubleCheck.provider(new SwitchingProvider<MissedCallNagManager>(singletonCImpl, 3));
      this.callManagerImplProvider = DoubleCheck.provider(new SwitchingProvider<CallManagerImpl>(singletonCImpl, 2));
      this.batteryMonitorProvider = DoubleCheck.provider(new SwitchingProvider<BatteryMonitor>(singletonCImpl, 9));
    }

    @Override
    public void injectTomsPhoneApplication(TomsPhoneApplication tomsPhoneApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.tomsphone.core.config.SettingsRepositoryImpl 
          return (T) new SettingsRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideJsonProvider.get());

          case 1: // kotlinx.serialization.json.Json 
          return (T) ConfigModule_Companion_ProvideJsonFactory.provideJson();

          case 2: // com.tomsphone.core.telecom.CallManagerImpl 
          return (T) new CallManagerImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), DoubleCheck.lazy(singletonCImpl.missedCallNagManagerProvider));

          case 3: // com.tomsphone.core.telecom.MissedCallNagManager 
          return (T) new MissedCallNagManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.bindCallLogRepositoryProvider.get(), singletonCImpl.bindContactRepositoryProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get(), (WandasTTS) ((Provider) singletonCImpl.androidTTSImplProvider).get(), singletonCImpl.ringtonePlayerProvider.get());

          case 4: // com.tomsphone.core.data.local.LocalCallLogRepository 
          return (T) new LocalCallLogRepository(singletonCImpl.callLogDao());

          case 5: // com.tomsphone.core.data.local.WandasDatabase 
          return (T) DataModule_Companion_ProvideWandasDatabaseFactory.provideWandasDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.tomsphone.core.data.local.LocalContactRepository 
          return (T) new LocalContactRepository(singletonCImpl.contactDao());

          case 7: // com.tomsphone.core.tts.AndroidTTSImpl 
          return (T) new AndroidTTSImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.tomsphone.core.telecom.RingtonePlayer 
          return (T) new RingtonePlayer(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 9: // com.tomsphone.core.telecom.BatteryMonitor 
          return (T) new BatteryMonitor(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), DoubleCheck.lazy(((Provider) singletonCImpl.androidTTSImplProvider)));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
