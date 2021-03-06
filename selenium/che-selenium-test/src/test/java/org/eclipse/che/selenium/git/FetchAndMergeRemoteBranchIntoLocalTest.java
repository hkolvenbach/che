/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.git;

import static org.eclipse.che.selenium.pageobject.Wizard.TypeProject.MAVEN;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.client.TestGitHubServiceClient;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.client.TestSshServiceClient;
import org.eclipse.che.selenium.core.client.TestUserPreferencesServiceClient;
import org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.Events;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.Menu;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.git.Git;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author aleksandr shmaraev */
public class FetchAndMergeRemoteBranchIntoLocalTest {
  private static final String REPO_NAME = NameGenerator.generate("FetchAndMergeTest-", 3);
  private static final String PROJECT_NAME = NameGenerator.generate("FetchAndMergeTest-", 4);
  private static final String CHANGE_CONTENT =
      String.format("//change_content-%s", String.valueOf(System.currentTimeMillis()));

  private GitHub gitHub;
  private GHRepository gitHubRepository;

  @Inject private TestWorkspace ws;
  @Inject private Ide ide;
  @Inject private TestUser productUser;

  @Inject
  @Named("github.username")
  private String gitHubUsername;

  @Inject
  @Named("github.password")
  private String gitHubPassword;

  @Inject private TestProjectServiceClient testProjectServiceClient;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Menu menu;
  @Inject private Git git;
  @Inject private Events eventsPanel;
  @Inject private Loader loader;
  @Inject private CodenvyEditor editor;
  @Inject private Consoles consoles;
  @Inject private TestSshServiceClient testSshServiceClient;
  @Inject private TestUserPreferencesServiceClient testUserPreferencesServiceClient;
  @Inject private TestGitHubServiceClient gitHubClientService;

  @BeforeClass
  public void prepare() throws Exception {
    gitHub = GitHub.connectUsingPassword(gitHubUsername, gitHubPassword);
    gitHubRepository = gitHub.createRepository(REPO_NAME).create();
    String commitMess = String.format("add-new-content-%s ", System.currentTimeMillis());
    testUserPreferencesServiceClient.addGitCommitter(gitHubUsername, productUser.getEmail());
    Path entryPath = Paths.get(getClass().getResource("/projects/guess-project").getPath());
    gitHubClientService.addContentToRepository(entryPath, commitMess, gitHubRepository);
    ide.open(ws);
  }

  @AfterClass
  public void deleteRepo() throws IOException {
    gitHubRepository.delete();
  }

  @Test
  public void fetchUpdatesAndMergeRemoteBranchTolocal() throws IOException {
    // preconditions and import the test repo
    String textFile = "README.md";
    String javaFile = "AppController";
    String jspFile = "index.jsp";
    String pathToJavaFile = "src/main/java/org/eclipse/qa/examples";
    String pathToJspFile = "src/main/webapp";
    String originMaster = "origin/master";
    String fetchMess =
        String.format("%s/%s/%s.git", "Fetched from https://github.com", gitHubUsername, REPO_NAME);
    String mergeMess1 = "Fast-forward Merged commits:";
    String mergeMess2 = "New HEAD commit: ";
    String mergeMess3 = "Already up-to-date";

    projectExplorer.waitProjectExplorer();
    String repoUrl = String.format("https://github.com/%s/%s.git", gitHubUsername, REPO_NAME);
    git.importJavaApp(repoUrl, PROJECT_NAME, MAVEN);

    // change content in the test repo on GitHub
    deleteFileOnGithubSide(String.format("%s/%s", pathToJspFile, jspFile), "delete index.jsp");
    changeContentOnGithubSide(
        String.format("%s/%s.java", pathToJavaFile, javaFile), CHANGE_CONTENT);
    changeContentOnGithubSide(textFile, CHANGE_CONTENT);

    performFetch();
    git.waitGitStatusBarWithMess(fetchMess);

    // open project and check that content is not changed
    projectExplorer.quickExpandWithJavaScript();
    projectExplorer.openItemByPath(
        String.format("%s/%s/%s.java", PROJECT_NAME, pathToJavaFile, javaFile));
    editor.waitActive();
    editor.waitTextNotPresentIntoEditor(CHANGE_CONTENT);
    projectExplorer.openItemByPath(String.format("%s/%s", PROJECT_NAME, textFile));
    editor.waitActive();
    editor.waitTextNotPresentIntoEditor(CHANGE_CONTENT);
    projectExplorer.waitVisibilityByName(jspFile);

    mergeRemoteBranch(originMaster);

    git.waitGitStatusBarWithMess(mergeMess1);
    git.waitGitStatusBarWithMess(mergeMess2);
    eventsPanel.clickEventLogBtn();
    eventsPanel.waitExpectedMessage(mergeMess1);

    // check the content is changed
    editor.selectTabByName(javaFile);
    editor.waitActive();
    editor.waitTextIntoEditor(CHANGE_CONTENT);
    editor.selectTabByName(textFile);
    editor.waitActive();
    editor.waitTextIntoEditor(CHANGE_CONTENT);
    projectExplorer.waitItemInvisibility(
        String.format("%s/%s/%s", PROJECT_NAME, "src/main/webapp", jspFile));

    // merge again
    mergeRemoteBranch(originMaster);
    git.waitGitStatusBarWithMess(mergeMess3);

    // wait commit in git history
    projectExplorer.waitAndSelectItem(PROJECT_NAME);
    menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.SHOW_HISTORY);
    git.waitTextInHistoryForm(CHANGE_CONTENT);
    git.clickOnHistoryRowInСommitsList(0);
    git.waitContentInHistoryEditor(CHANGE_CONTENT);
  }

  private void deleteFileOnGithubSide(String pathToContent, String commitMess) throws IOException {
    gitHubRepository.getFileContent(pathToContent).delete(commitMess);
  }

  private void changeContentOnGithubSide(String pathToContent, String content) throws IOException {
    gitHubRepository
        .getFileContent(String.format("/%s", pathToContent))
        .update(content, "add " + content);
  }

  private void performFetch() {
    menu.runCommand(
        TestMenuCommandsConstants.Git.GIT,
        TestMenuCommandsConstants.Git.Remotes.REMOTES_TOP,
        TestMenuCommandsConstants.Git.Remotes.FETCH);

    git.waitFetchFormOpened();
    git.clickOnFetchButton();
    git.waitFetchFormClosed();
  }

  private void mergeRemoteBranch(String nameRemoteBranch) {
    menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.MERGE);

    git.waitMergeView();
    git.waitMergeReferencePanel();
    git.waitMergeExpandRemoteBranchIcon();
    git.clickMergeExpandRemoteBranchIcon();
    git.waitItemInMergeList(nameRemoteBranch);
    git.clickItemInMergeList(nameRemoteBranch);
    git.clickMergeBtn();
    git.waitMergeViewClosed();
  }
}
