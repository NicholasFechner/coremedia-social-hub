package com.coremedia.blueprint.social.beans {
import com.coremedia.ui.data.beanFactory;
import com.coremedia.ui.data.impl.BeanFactoryImpl;
import com.coremedia.ui.data.impl.RemoteBeanImpl;
import com.coremedia.ui.data.impl.RemoteServiceMethod;
import com.coremedia.ui.data.impl.RemoteServiceMethodResponse;
import com.coremedia.ui.data.impl.SubBean;

import ext.JSON;

[RestResource(uriTemplate="socialhub/composermodel/{id:[^/]+}/{adapterId:[^/]+}")]
public class ComposerModelImpl extends RemoteBeanImpl implements ComposerModel {

  function ComposerModelImpl(path:String) {
    super(path);
  }

  override public function get(property:*):* {
    if(property === "type") {
      return beanFactory.createLocalBean({'name': 'ComposerModel'});
    }
    return super.get(property);
  }

  override protected function isSubObject(value:*, propertyPath:*):Boolean {
    return propertyPath === SocialHubPropertyNames.COMPOSER_PROPERTIES;
  }

  override protected function createSubBean(propertyPathPrefix:String):SubBean {
    return new ComposerModelPropertiesImpl(this, propertyPathPrefix);
  }

  public function getProperties():ComposerModelProperties {
    return get(SocialHubPropertyNames.COMPOSER_PROPERTIES);
  }

  public function getAttachments():Array {
    return get(SocialHubPropertyNames.COMPOSER_ATTACHMENTS);
  }

  public function getAdapterType():String {
    return get(SocialHubPropertyNames.COMPOSER_ADAPTER_TYPE);
  }

  public function getPublicationDate():Date {
    return getProperties().get(SocialHubPropertyNames.COMPOSER_PUBLICATION_DATE);
  }

  public function getMessageText():String {
    if (getProperties() === undefined) {
      return undefined;
    }
    return getProperties().get(SocialHubPropertyNames.COMPOSER_MESSAGE_TEXT);
  }

  public function getTitle():String {
    if (getProperties() === undefined) {
      return undefined;
    }
    return getProperties().get(SocialHubPropertyNames.COMPOSER_MESSAGE_TITLE);
  }

  public function send(callback:Function = undefined):void {
    var method:RemoteServiceMethod = new RemoteServiceMethod(getUriPath(), 'POST');
    method.request({},
            function (response:RemoteServiceMethodResponse):void {
              if(callback) {
                callback(null);
              }
            },
            function (response:RemoteServiceMethodResponse):void {
              if(callback) {
                callback(response.getError());
              }
            }
    );
  }

  public function reset(callback:Function = undefined):void {
    var method:RemoteServiceMethod = new RemoteServiceMethod(getUriPath(), 'DELETE');
    method.request({},
            function (response:RemoteServiceMethodResponse):void {
              var result:Boolean = response.response.responseText;
              if(callback) {
                callback(result);
              }

            },
            function (response:RemoteServiceMethodResponse):void {
              if(callback) {
                callback(response.getError());
              }
            }
    );
  }
}
}