package gitflow.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.Key;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import gitflow.GitflowInitOptions;
import gitflow.ui.GitflowInitOptionsDialog;
import gitflow.ui.NotifyUtil;
import org.jetbrains.annotations.NotNull;

public class InitRepoAction extends GitflowAction {

    InitRepoAction() {
        this( "Init Repo");
    }

    InitRepoAction(String actionName) {
        this(null, "Init Repo");
    }

    InitRepoAction(GitRepository repo) {
        this(repo,"Init Repo");
    }

    InitRepoAction(GitRepository repo, String actionName) {
        super(repo, actionName);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        GitflowInitOptionsDialog optionsDialog = new GitflowInitOptionsDialog(myProject, branchUtil.getLocalBranchNames());
        optionsDialog.show();

        if(optionsDialog.isOK()) {
            final GitflowErrorsListener errorLineHandler = new GitflowErrorsListener(myProject);
            final GitflowLineHandler localLineHandler = getLineHandler();
            final GitflowInitOptions initOptions = optionsDialog.getOptions();

            new Task.Backgroundable(myProject, getTitle(),false){
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    GitCommandResult result =
                            executeCommand(initOptions, errorLineHandler,
                                    localLineHandler);

                    if (result.success()) {
                        String publishedFeatureMessage =
                                getPublishedFeatureMessage();
                        NotifyUtil.notifySuccess(myProject, "", publishedFeatureMessage);
                    } else {
                        NotifyUtil.notifyError(myProject, "Error", "Please have a look at the Version Control console for more details");
                    }

                    //update the widget
                    myProject.getMessageBus().syncPublisher(GitRepository.GIT_REPO_CHANGE).repositoryChanged(myRepo);
                    myRepo.update();
                }
            }.queue();
        }

    }

    protected String getPublishedFeatureMessage() {
        return "Initialized gitflow in repo " + myRepo.getRoot().getPresentableName();
    }

    protected GitCommandResult executeCommand(GitflowInitOptions initOptions,
            GitflowErrorsListener errorLineHandler,
            GitflowLineHandler localLineHandler) {
        return myGitflow.initRepo(myRepo, initOptions, errorLineHandler, localLineHandler);
    }

    protected String getTitle() {
        return "Initializing Repo";
    }

    protected GitflowLineHandler getLineHandler() {
        return new LineHandler();
    }


    private class LineHandler extends GitflowLineHandler {
        @Override
        public void onLineAvailable(String line, Key outputType) {
            if (line.contains("Already initialized for gitflow")){
                myErrors.add("Repo already initialized for gitflow");
            }

        }
    }

}