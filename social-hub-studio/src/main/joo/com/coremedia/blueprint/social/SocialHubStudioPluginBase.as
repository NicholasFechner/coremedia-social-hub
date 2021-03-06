package com.coremedia.blueprint.social {

import com.coremedia.cms.editor.configuration.StudioPlugin;
import com.coremedia.cms.editor.sdk.IEditorContext;
import com.coremedia.cms.editor.sdk.jobs.BackgroundJobsButtonBase;

import joo.classLoader;

public class SocialHubStudioPluginBase extends StudioPlugin {

  public function SocialHubStudioPluginBase(config:SocialHubStudioPlugin = null) {
    super(config);
  }

  override public function init(editorContext:IEditorContext):void {
    super.init(editorContext);

    //ensure that the class the method is replaced from is already loaded
    classLoader.init(BackgroundJobsButtonBase);
    //find original method: you HAVE to use the original JS function name which may differ from the AS name, check JS sources for that
    var originalMethod:Function = (BackgroundJobsButtonBase as Class).prototype['onJobStarted$v9Xb'];
    (BackgroundJobsButtonBase as Class).prototype['onJobStarted$v9Xb'] = function ():void {
      //do nothing
    };
  }
}
}
