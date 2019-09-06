package com.coremedia.blueprint.social.config;

import com.coremedia.blueprint.social.adapter.AbstractSocialHubAdapter;
import com.coremedia.blueprint.social.api.SocialHubAdapter;
import com.coremedia.blueprint.social.api.SocialHubAdapterFactory;
import com.coremedia.blueprint.social.api.SocialHubPropertyNames;
import com.coremedia.blueprint.social.scheduler.Scheduler;
import com.coremedia.cap.common.CapSession;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.wrapper.impl.TypedCapStructWrapperFactoryImpl;
import com.coremedia.cap.struct.Struct;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdapterFactoryService {
  private static final Logger LOG = LoggerFactory.getLogger(AdapterFactoryService.class);

  private static final String SETTINGS = "settings";
  private static final String ADAPTERS = "channels";
  private static final String CREATE_ADAPTER_METHOD = "createAdapter";

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private List<SocialHubAdapterFactory> adapterFactories;

  @Autowired
  private Scheduler scheduler;

  public List<SocialHubAdapter> getAdapters(String configPath) {
    List<SocialHubAdapter> adapters = new ArrayList<>();
    CapSession originalSession = contentRepository.getConnection().getConnectionSession().activate();
    try {
      if (configPath != null) {
        Content socialHubFolder = contentRepository.getChild(configPath);
        if (socialHubFolder != null) {
          Set<Content> childDocuments = socialHubFolder.getChildDocuments();
          for (Content childDocument : childDocuments) {
            createAdaptersForConfig(childDocument, adapters);
          }
        }
      }
    } finally {
      //activate the original user session
      originalSession.activate();
    }

    return adapters;
  }

  private void createAdaptersForConfig(Content settings, List<SocialHubAdapter> adapters) {
    Struct settingsStruct = settings.getStruct(SETTINGS);
    if (!settingsStruct.toNestedMaps().containsKey(ADAPTERS)) {
      return;
    }

    List<Struct> structs = settingsStruct.getStructs(ADAPTERS);

    for (Struct struct : structs) {
      try {
        Map<String, Object> config = struct.toNestedMaps();
        boolean enabled = (boolean) config.getOrDefault(SocialHubPropertyNames.ENABLED, true);
        if (!enabled) {
          continue;
        }

        String adapterType = struct.getString(SocialHubPropertyNames.TYPE);
        String id = struct.getString(SocialHubPropertyNames.ID);
        Struct connectorStruct = struct.getStruct(SocialHubPropertyNames.CONNECTOR);
        String displayName = struct.getString(SocialHubPropertyNames.DISPLAY_NAME);

        for (SocialHubAdapterFactory adapterFactory : adapterFactories) {
          if (adapterFactory.getType().name().equalsIgnoreCase(adapterType)) {
            Object connectorSettings = createSettings(adapterFactory, connectorStruct, 0);
            Object adapterSettings = null;
            if (config.containsKey(SocialHubPropertyNames.ADAPTER)) {
              Struct adapterStruct = struct.getStruct(SocialHubPropertyNames.ADAPTER);
              adapterSettings = createSettings(adapterFactory, adapterStruct, 1);
            }

            AbstractSocialHubAdapter adapter = (AbstractSocialHubAdapter) adapterFactory.createAdapter(connectorSettings, adapterSettings);
            adapter.setId(id);
            adapter.setType(adapterType);
            adapter.setDisplayName(displayName);
            adapter.setScheduler(scheduler);

            adapters.add(adapter);
            return;
          }
        }

        throw new IllegalArgumentException("No Social Hub Adapter found for type '" + adapterType + "'");
      } catch (Exception e) {
        LOG.error("Social Hub Adapter creation failed for settings document {}: {}", settings.getPath(), e.getMessage());
      }
    }
  }


  private Object createSettings(SocialHubAdapterFactory adapterFactory, Struct setting, int paramterIndex) {
    TypedCapStructWrapperFactoryImpl typedCapStructWrapperFactory = new TypedCapStructWrapperFactoryImpl();
    Method[] declaredMethods = adapterFactory.getClass().getDeclaredMethods();
    for (Method declaredMethod : declaredMethods) {
      if (declaredMethod.getName().equals(CREATE_ADAPTER_METHOD)) {
        Class<?> type = TypeToken.of(adapterFactory.getClass()).resolveType(SocialHubAdapterFactory.class.getTypeParameters()[paramterIndex]).getRawType();
        return typedCapStructWrapperFactory.createTypedAccessWrapper(type, setting);
      }
    }
    return null;
  }
}
