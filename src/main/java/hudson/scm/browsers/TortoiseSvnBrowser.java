package hudson.scm.browsers;

import hudson.model.Descriptor;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionChangeLogSet.RevisionInfo;
import hudson.scm.SubversionRepositoryBrowser;
import hudson.scm.browsers.tsvncmd.Handler;
import hudson.Extension;

import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * {@link RepositoryBrowser} implementation for TortoiseSVN.
 * 
 * @author Daniel Beck
 */
public class TortoiseSvnBrowser extends SubversionRepositoryBrowser
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(TortoiseSvnBrowser.class.getName());

    @DataBoundConstructor
    public TortoiseSvnBrowser() {
        /* no need for any initialization, but Stapler requires @DataBuildConstructor */
    }

    static String determineUrlFromRevisionInfoListAndRevision(List<RevisionInfo> revisions, int revision) throws IOException {
        String bestMatchingUrl = null;
        long bestMatchingRevision = Long.MAX_VALUE;
        for (RevisionInfo ri : revisions) {
            /* use module if its revision is greater than the one to be linked (multiple changes since previous build)
               and if it's a closer match to the reference revision than the previous best matching module */
            if (ri.revision >= revision && ri.revision < bestMatchingRevision) {
                bestMatchingRevision = ri.revision;
                bestMatchingUrl = ri.module;
            }
        }
        return bestMatchingUrl;
    }

    static int offsetForArrayOverlap(String[] left, String[] right) {
        int offset = 0;
        while (!ArrayUtils.isEquals(ArrayUtils.subarray(left, offset, left.length), ArrayUtils.subarray(right, 0, left.length - offset))) {
            offset ++;
            if (offset >= left.length) {
                return -1;
            }
        }
        return offset;
    }

    static String determineUrlFromRevisionInfoListAndRevisionAndPath(List<RevisionInfo> revisions, int revision, String file) throws IOException {
        String bestMatchingUrl = null;
        if (file.startsWith("/")) {
            /* strip leading /, otherwise the items array will start with an empty element, messing up offsetForArrayOverlap. */
            file = file.substring(1);
        }
        long bestMatchingRevision = Long.MAX_VALUE;
        for (RevisionInfo ri : revisions) {
            String[] riModuleItems = ri.module.split("[/]");
            String[] fileItems = file.split("[/]");

            int shift = offsetForArrayOverlap(riModuleItems, fileItems);

            if (shift == -1) {
                /* no overlap found, e.g. ri.module = http://server/foo/bar/baz and file path = /qux/quux */
                continue;
            }

            /* use module if its revision is greater than the one to be linked (in case of multiple changes since previous build)
               and if it's a closer match to the reference revision than the previous best matching module */
            if (ri.revision >= revision && ri.revision < bestMatchingRevision) {
                bestMatchingUrl = StringUtils.appendIfMissing(StringUtils.join(ArrayUtils.subarray(riModuleItems, 0, shift), '/'), "/") + file;
                bestMatchingRevision = ri.revision;
            }
        }

        return bestMatchingUrl;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "TortoiseSVN";
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getDiffLink(SubversionChangeLogSet.Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            /* No diff if it's not an edit. */
            return null;
        }

        int revision = path.getLogEntry().getRevision();
        String file = path.getPath();
        SubversionChangeLogSet cls = path.getLogEntry().getParent();

        String repoUrl = determineUrlFromRevisionInfoListAndRevisionAndPath(cls.getRevisions(), revision, path.getPath());
        if (repoUrl == null)
            /* If the file is not part of any module location of this project at the time of the build, no link is shown.
               This is a feature, not a bug. */
            return null;
        String urlString = "tsvncmd:command:diff?path:" + repoUrl + "?startrev:" + (revision-1) + "?endrev:" + revision;
        return new URL(null, urlString, new Handler());
    }


    /**
     * {@inheritDoc}
     */    
    public URL getFileLink(SubversionChangeLogSet.Path path) throws IOException {
        int revision = path.getLogEntry().getRevision();
        String file = path.getPath();
        SubversionChangeLogSet cls = path.getLogEntry().getParent();

        String repoUrl = determineUrlFromRevisionInfoListAndRevisionAndPath(cls.getRevisions(), revision, path.getPath());
        if (repoUrl == null)
            /* If the file is not part of any module location of this project at the time of the build, no link is shown.
               This is a feature, not a bug. */
            return null;
        String urlString = "tsvncmd:command:log?path:" + repoUrl;
        return new URL(null, urlString, new Handler());
    }


    /**
     * {@inheritDoc}
     */    
    public URL getChangeSetLink(SubversionChangeLogSet.LogEntry changeSet) throws IOException {
        int revision = changeSet.getRevision();
        SubversionChangeLogSet cls = changeSet.getParent();

        String repoUrl = determineUrlFromRevisionInfoListAndRevision(cls.getRevisions(), revision);
        if (repoUrl == null)
            return null;
        String urlString = "tsvncmd:command:diff?path:" + repoUrl + "?startrev:" + (revision-1) + "?endrev:" + revision;
        return new URL(null, urlString, new Handler());
    }

    /* This doesn't actually do anything, just needs to workaround MalformedURLException: unknown protocol: tsvncmd */
    /* package */ static class Handler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            throw new IOException("not implemented!");
        }
    }
}