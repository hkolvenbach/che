/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.rust.inject;

import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.rust.languageserver.RustLanguageServerLauncher;

// import org.eclipse.che.plugin.cpp.projecttype.CProjectType;
// import org.eclipse.che.plugin.cpp.projecttype.CppProjectType;

/** @author Alexander Andrienko */
/** @author Hanno Kolvenbach */
@DynaModule
public class RustModule extends AbstractModule {
  public static final String LANGUAGE_ID = "rust";
  private static final String[] EXTENSIONS = new String[] {"rs", "RS"};
  private static final String MIME_TYPE = "text/x-rs";

  // @Override
  // protected void configure() {
  //   Multibinder.newSetBinder(binder(), LanguageServerLauncher.class)
  //       .addBinding()
  //       .to(RustLanguageServerLauncher.class);
  // }

  @Override
  protected void configure() {
    // Multibinder<ProjectTypeDef> projectTypeMultibinder =
    //     Multibinder.newSetBinder(binder(), ProjectTypeDef.class);

    // projectTypeMultibinder.addBinding().to(CProjectType.class);
    // projectTypeMultibinder.addBinding().to(CppProjectType.class);

    Multibinder.newSetBinder(binder(), LanguageServerLauncher.class)
        .addBinding()
        .to(RustLanguageServerLauncher.class);

    LanguageDescription description = new LanguageDescription();
    description.setFileExtensions(asList(EXTENSIONS));
    description.setLanguageId(LANGUAGE_ID);
    description.setMimeType(MIME_TYPE);

    Multibinder.newSetBinder(binder(), LanguageDescription.class)
        .addBinding()
        .toInstance(description);
  }
}
