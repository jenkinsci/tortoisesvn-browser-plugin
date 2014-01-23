package hudson.scm.browsers.tsvncmd;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/* This doesn't actually do anything, just needs to workaround MalformedURLException: unknown protocol: tsvncmd */
public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        throw new IOException("not implemented!");
    }
}
