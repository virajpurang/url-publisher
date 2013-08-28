package org.jenkinsci.plugins.urlpublisher;

import java.io.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Notifier;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.EnvironmentContributingAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import javax.servlet.ServletException;
import java.util.List;

/**
 * Main class for this Jenkins Plugin, it extends Notifier class.
 * Class Notifier extends Publisher (An implementing class of BuildStep interface), implements ExtensionPoint.
 * @author Viraj Purang
 */

public class UrlPublisher extends Notifier {

    // Declared as final variable to avoid value changes (constant?)
    public final String publishURL;

    /**
     * DataBoundConstructor comes from Stapler web framework used by Jenkins/Hudson. It helps construct objects from
     * a data model (JSON?). For a public constructor annotated with @DataBoundConstructor, it binds fields from a
     * object by matching the field name to the constructor parameter name.
     */

    @DataBoundConstructor
    public UrlPublisher(String publishURL) {
        this.publishURL = publishURL;
    }

    /**
     * getRequiredMonitorService is a method inherited from interface hudson.tasks.BuildStep. A BuildStep amounts to
     * ONE step of the entire build process. It is instantiated when user saves the job config, stays in memory until the job
     * configuration is overwritten.
     */

    public BuildStepMonitor getRequiredMonitorService() {
        // By returning BuildStepMonitor.NONE, no external synchronization is performed on this build step.
        // A recommended value for current plugins.
        return BuildStepMonitor.NONE;
    }

    /**
     *  Perform method should work without the Override. Possibly preferred that way. 2 Sources - BuildStep or the
     *  deprecated BuildStepCompatibilityLayer <Subclassed by Publisher>. The method itself, runs the step over the given
     *  build and reports the progress to the listener.
     */

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws
            InterruptedException, IOException {
        PostMethod post = new PostMethod(publishURL);
        boolean result = true;
        HttpClient client = getHttpClient();
        try {
            int status = client.executeMethod(post);
            listener.getLogger().printf("Triggered URL %s ",
                    ((status == 200 || status == 302)? "Successfully!\n"    : "But Not Successfully!\n"));
            listener.getLogger().printf("Status Code for URL %s is %s \n", publishURL, status);

/*            Result buildResult = build.getResult();

            if (!Result.SUCCESS.equals(buildResult)) {
                // Don't process for unsuccessful builds
                listener.getLogger().printf("Build status is not SUCCESS (" + build.getResult().toString() + ").");
                return true;
            }
*/
            EnvVarAction httpStatusAction = new EnvVarAction("HTTP_STATUS_ACTION", String
                    .valueOf(status));
            build.addAction(httpStatusAction);

        } catch (IOException e) {
            listener.error("Failed to triggered the suggested URL -> %s \n", e.getMessage());
        } finally {
            post.releaseConnection();
        }
        return true;
    }

    // Getter
    HttpClient getHttpClient() {
        return new HttpClient();
    }

    /**
     * {@link UrlPublisher} descriptor class
     *
     * BuildStepDescriptor<T> is a Descriptor for Builder and Publisher (in our context concrete value = Publisher). It
     * extends the Descriptor.
     *
     * It contains Metadata about the configurable instance and publisher/notifier object uses BuildStepDescriptor to
     * provide information to Jenkins, provide defaults for configuration fields and Perform Data validation (using a
     * method that returns a FormValidation object - which we are not using in this example.
     */
    @Extension
    public static class UrlPublisherDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "My URL Publisher";
        }
    }

     /**
     * Build action related to EnvVars.
     *
     * @author vpurang
     */
    public static class EnvVarAction implements EnvironmentContributingAction {
        private final String name;
        private final String value;

        public EnvVarAction(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        public String getIconFileName() {
            return null;
        }

        public String getDisplayName() {
            return null;
        }

        public String getUrlName() {
            return null;
        }

        public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
            env.put(name, value);
		}
	}

}