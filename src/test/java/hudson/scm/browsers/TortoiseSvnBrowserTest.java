package hudson.scm.browsers;

import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionChangeLogSet.RevisionInfo;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

public class TortoiseSvnBrowserTest {

	private static class RevisionInfoMap extends HashMap<String, Integer> {
		public RevisionInfoMap and(String module, int revision) {
			this.put(module, revision);
			return this;
		}
		public List<RevisionInfo> buildRevisionInfoList() throws IOException {
			List<RevisionInfo> revs = new ArrayList<RevisionInfo>();
			for (Map.Entry<String, Integer> e : this.entrySet()) {
				revs.add(new RevisionInfo(e.getKey(), e.getValue()));
			}
			return revs;
		}
	}

	private static RevisionInfoMap from(String module, int revision) {
		return new RevisionInfoMap().and(module, revision);
	}

	@Test
	public void testOffsets() {
		assertEquals(0, TortoiseSvnBrowser.offsetForArrayOverlap(new String[] { "foo", "bar" }, new String[] { "foo", "bar" }));
		assertEquals(0, TortoiseSvnBrowser.offsetForArrayOverlap(new String[] { "foo", "bar" }, new String[] { "foo", "bar", "baz" }));
		assertEquals(1, TortoiseSvnBrowser.offsetForArrayOverlap(new String[] { "foo", "foo", "bar" }, new String[] { "foo", "bar", "baz" }));
		assertEquals(2, TortoiseSvnBrowser.offsetForArrayOverlap(new String[] { "foo", "foo", "bar" }, new String[] { "bar", "bar", "baz" }));
	}

	@Test
	public void testURL() throws Exception {
		// since Java doesn't know the tsvncmd URL scheme, we need to specify a URLStreamHandler to handle it.
		// Doesn't matter that it's a stub implementation, we just need to get rid of the MalformedURLException.
		// Not a real test, more meant to document how this needs to be done.
		new URL(null, "tsvncmd:command:diff?path:https://svnserver/svn/reponame/trunk/foo/bar.baz?startrev=5?endrev=6", 
				new TortoiseSvnBrowser.Handler());
	}

	@Test
	public void testBasic() throws IOException {
		String url = TortoiseSvnBrowser.determineUrlFromRevisionInfoListAndRevision(
			from("http://server/repo/trunk/proj", 12).and("https://server/svn/repo/trunk", 11).buildRevisionInfoList(),
			10
		);
		assertEquals("basic comparison", "https://server/svn/repo/trunk", url);
	}

	@Test
	public void testBasicWithPath() throws IOException {
		String url = TortoiseSvnBrowser.determineUrlFromRevisionInfoListAndRevisionAndPath(
			from("http://server/repo/trunk/proj", 12).and("https://server/svn/repo/trunk", 11).buildRevisionInfoList(),
			10, "/trunk/proj/foo.txt"
		);
		assertEquals("basic comparison with path", "https://server/svn/repo/trunk/proj/foo.txt", url);
	}

	@Test
	public void testNoMatch() throws IOException {
		String url = TortoiseSvnBrowser.determineUrlFromRevisionInfoListAndRevisionAndPath(
			from("http://server/repo/trunk/proj", 12).and("https://server/svn/repo/trunk", 11).buildRevisionInfoList(),
			10, "/foo/proj/foo.txt"
		);
		assertEquals("no match", null, url);
	}
}
