package com.cabosschecklist;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WikiPictures}: the requests made, the thumbnail address read and checked, the picture decoded and fitted to its
 * box, and the answers kept and asked for once.
 */
public class WikiPicturesTest
{
	/** The wiki's page answer for Duke Sucellus: a PNG thumbnail on the wiki's own host. */
	private static final String DUKE = "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":1,\"ns\":0,\"title\":"
		+ "\"Duke Sucellus\",\"thumbnail\":{\"source\":\"https://oldschool.runescape.wiki/images/thumb/Duke_Sucellus.png/"
		+ "128px-Duke_Sucellus.png?abcde\",\"width\":128,\"height\":142},\"pageimage\":\"Duke_Sucellus.png\"}]}}";

	/** The wiki's page answer for a page with no thumbnail. */
	private static final String NO_THUMBNAIL = "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":2,\"ns\":0,"
		+ "\"title\":\"Zulrah\"}]}}";

	private final Gson gson = new Gson();

	/** The bytes of the fixture picture: 128 x 40, transparent but for a red rectangle. */
	private static byte[] fixtureBytes() throws IOException
	{
		try (InputStream in = WikiPicturesTest.class.getResourceAsStream("/com/cabosschecklist/picture-fixture.png"))
		{
			assertNotNull("the fixture is on the test classpath", in);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] chunk = new byte[4096];
			for (int read = in.read(chunk); read >= 0; read = in.read(chunk))
			{
				out.write(chunk, 0, read);
			}
			return out.toByteArray();
		}
	}

	/** A 200 answer holding {@code json}. */
	private static Response ok(String json)
	{
		return answer(200, "application/json", json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	/** An answer with {@code code} and {@code body} of the given type. */
	private static Response answer(int code, String type, byte[] body)
	{
		return new Response.Builder()
			.request(WikiPictures.requestFor("Duke Sucellus"))
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "No")
			.body(ResponseBody.create(MediaType.parse(type), body))
			.build();
	}

	/** A 200 answer holding the fixture picture. */
	private static Response picture() throws IOException
	{
		return answer(200, "image/png", fixtureBytes());
	}

	@Test
	public void theRequestNamesThePluginAndThePage()
	{
		final Request request = WikiPictures.requestFor("Duke Sucellus");
		assertEquals("GET", request.method());
		assertEquals("https", request.url().scheme());
		assertEquals("oldschool.runescape.wiki", request.url().host());
		assertEquals("/api.php", request.url().encodedPath());
		assertEquals("query", request.url().queryParameter("action"));
		assertEquals("pageimages", request.url().queryParameter("prop"));
		assertEquals("thumbnail", request.url().queryParameter("piprop"));
		assertEquals("128", request.url().queryParameter("pithumbsize"));
		assertEquals("1", request.url().queryParameter("redirects"));
		assertEquals("json", request.url().queryParameter("format"));
		assertEquals("2", request.url().queryParameter("formatversion"));
		assertEquals("Duke Sucellus", request.url().queryParameter("titles"));
		assertEquals("nothing else is sent", 8, request.url().querySize());
		assertEquals(WikiPictures.USER_AGENT, request.header("User-Agent"));
		assertTrue("the User-Agent names the plugin: " + request.header("User-Agent"),
			request.header("User-Agent").contains("2h CA Boss Checklist"));

		final HttpUrl address = HttpUrl.parse("https://oldschool.runescape.wiki/images/thumb/a.png/128px-a.png?1");
		final Request picture = WikiPictures.requestForPicture(address);
		assertEquals("GET", picture.method());
		assertEquals(address, picture.url());
		assertEquals(WikiPictures.USER_AGENT, picture.header("User-Agent"));
	}

	@Test
	public void theThumbnailAddressIsReadAndChecked()
	{
		final HttpUrl duke = WikiPictures.thumbnailUrl(DUKE, gson);
		assertNotNull(duke);
		assertEquals("https://oldschool.runescape.wiki/images/thumb/Duke_Sucellus.png/128px-Duke_Sucellus.png?abcde",
			duke.toString());

		assertNull("no thumbnail", WikiPictures.thumbnailUrl(NO_THUMBNAIL, gson));
		assertNull("another host", WikiPictures.thumbnailUrl(page("https://example.com/a.png"), gson));
		assertNull("not https", WikiPictures.thumbnailUrl(page("http://oldschool.runescape.wiki/a.png"), gson));
		assertNull("a WebP file", WikiPictures.thumbnailUrl(
			page("https://oldschool.runescape.wiki/images/thumb/Mad_Angel.png/128px-Mad_Angel.png.webp"), gson));
		assertNull("a page that is missing", WikiPictures.thumbnailUrl(
			"{\"query\":{\"pages\":[{\"title\":\"X\",\"missing\":true}]}}", gson));
		assertNull("an empty answer", WikiPictures.thumbnailUrl("{}", gson));
		assertNull("nothing at all", WikiPictures.thumbnailUrl("", gson));
		assertNull("text that is not JSON", WikiPictures.thumbnailUrl("not json", gson));

		assertNotNull("a JPEG", WikiPictures.thumbnailUrl(page("https://oldschool.runescape.wiki/a/b.JPG"), gson));
		assertNotNull("a GIF", WikiPictures.thumbnailUrl(page("https://oldschool.runescape.wiki/a/b.gif"), gson));
		assertNotNull("a JPEG, the long way", WikiPictures.thumbnailUrl(page("https://oldschool.runescape.wiki/b.jpeg"), gson));
	}

	/** The wiki's page answer whose thumbnail is at {@code source}. */
	private static String page(String source)
	{
		return "{\"query\":{\"pages\":[{\"title\":\"X\",\"thumbnail\":{\"source\":\"" + source + "\",\"width\":128,"
			+ "\"height\":40}}]}}";
	}

	@Test
	public void aPictureIsDecodedAndFittedToTheBox() throws Exception
	{
		final BufferedImage decoded = WikiPictures.decode(fixtureBytes());
		assertNotNull("the fixture decodes", decoded);
		assertEquals(128, decoded.getWidth());
		assertEquals(40, decoded.getHeight());
		assertEquals("a transparent pixel keeps its alpha", 0, decoded.getRGB(0, 0) >>> 24);
		assertEquals("an opaque red one is red", 0xFFFF0000, decoded.getRGB(64, 20));

		final BufferedImage fitted = WikiPictures.fit(decoded);
		assertEquals(64, fitted.getWidth());
		assertTrue("19 to 21 tall: " + fitted.getHeight(), fitted.getHeight() >= 19 && fitted.getHeight() <= 21);

		final BufferedImage tall = WikiPictures.fit(new BufferedImage(128, 381, BufferedImage.TYPE_INT_ARGB));
		assertEquals(64, tall.getHeight());
		assertTrue("20 to 22 wide: " + tall.getWidth(), tall.getWidth() >= 20 && tall.getWidth() <= 22);

		final BufferedImage small = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
		assertSame("a small picture is not scaled up", small, WikiPictures.fit(small));
		final BufferedImage exact = WikiPictures.fit(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
		assertEquals(64, exact.getWidth());
		assertEquals(64, exact.getHeight());

		assertNull("bytes that are no picture", WikiPictures.decode(new byte[] {1, 2, 3}));
		assertNull("a WebP file", WikiPictures.decode("RIFF....WEBPVP8 ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
		assertNull("no bytes", WikiPictures.decode(new byte[0]));
		assertNull("nothing", WikiPictures.decode(null));
	}

	@Test
	public void aFetchAsksTwiceKeepsThePictureAndNeverRepeats() throws Exception
	{
		final AtomicInteger answers = new AtomicInteger();
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiPictures pictures = new WikiPictures(factory, gson, answers::incrementAndGet);

		pictures.fetch("Duke Sucellus");
		final ArgumentCaptor<Request> requests = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(1)).newCall(requests.capture());
		assertEquals(WikiPictures.requestFor("Duke Sucellus").url(), requests.getValue().url());
		assertNull("not answered yet", pictures.pictureOf("Duke Sucellus"));
		assertNull("never asked for", pictures.pictureOf("Zulrah"));
		assertFalse("not answered yet", pictures.isAnswered("Duke Sucellus"));
		assertFalse("never asked for", pictures.isAnswered("Zulrah"));
		assertEquals(0, answers.get());
		pictures.fetch("Duke Sucellus");
		verify(factory, times(1)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> page = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(page.capture());
		page.getValue().onResponse(call, ok(DUKE));
		final ArgumentCaptor<Request> second = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(2)).newCall(second.capture());
		assertEquals("the second call is the thumbnail's own address", WikiPictures.thumbnailUrl(DUKE, gson),
			second.getValue().url());
		assertNull("not kept while the chain runs", pictures.pictureOf("Duke Sucellus"));
		assertFalse("still on its way", pictures.isAnswered("Duke Sucellus"));
		pictures.fetch("Duke Sucellus");
		verify(factory, times(2)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> thumb = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(thumb.capture());
		thumb.getValue().onResponse(call, picture());
		final BufferedImage kept = pictures.pictureOf("Duke Sucellus");
		assertNotNull("the picture is kept", kept);
		assertTrue("answered", pictures.isAnswered("Duke Sucellus"));
		assertEquals("onAnswer ran exactly once", 1, answers.get());
		assertEquals(64, kept.getWidth());
		assertTrue("19 to 21 tall: " + kept.getHeight(), kept.getHeight() >= 19 && kept.getHeight() <= 21);
		pictures.fetch("Duke Sucellus");
		verify(factory, times(2)).newCall(any(Request.class));

		// A page with no thumbnail ends the chain at one call, with none.
		pictures.fetch("Zulrah");
		verify(factory, times(3)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> bare = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(3)).enqueue(bare.capture());
		bare.getValue().onResponse(call, ok(NO_THUMBNAIL));
		verify(factory, times(3)).newCall(any(Request.class));
		assertNull(pictures.pictureOf("Zulrah"));
		pictures.fetch("Zulrah");
		verify(factory, times(3)).newCall(any(Request.class));

		// A picture call that answers 404 gives none, and is not asked again.
		pictures.fetch("Vorkath");
		final ArgumentCaptor<Callback> vorkathPage = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(4)).enqueue(vorkathPage.capture());
		vorkathPage.getValue().onResponse(call, ok(DUKE));
		verify(factory, times(5)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> vorkathPicture = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(5)).enqueue(vorkathPicture.capture());
		vorkathPicture.getValue().onResponse(call, answer(404, "text/html", "gone".getBytes()));
		assertNull(pictures.pictureOf("Vorkath"));
		pictures.fetch("Vorkath");
		verify(factory, times(5)).newCall(any(Request.class));

		// A picture body that is no picture gives none, and is not asked again.
		pictures.fetch("Nex");
		final ArgumentCaptor<Callback> nexPage = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(6)).enqueue(nexPage.capture());
		nexPage.getValue().onResponse(call, ok(DUKE));
		verify(factory, times(7)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> nexPicture = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(7)).enqueue(nexPicture.capture());
		nexPicture.getValue().onResponse(call, answer(200, "image/png", new byte[] {1, 2, 3}));
		assertNull(pictures.pictureOf("Nex"));
		pictures.fetch("Nex");
		verify(factory, times(7)).newCall(any(Request.class));

		// A thumbnail that is a WebP file is never fetched: one call only.
		pictures.fetch("Mad Angel");
		final ArgumentCaptor<Callback> angel = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(8)).enqueue(angel.capture());
		angel.getValue().onResponse(call, ok(
			page("https://oldschool.runescape.wiki/images/thumb/Mad_Angel.png/128px-Mad_Angel.png.webp")));
		verify(factory, times(8)).newCall(any(Request.class));
		assertNull(pictures.pictureOf("Mad Angel"));
		pictures.fetch("Mad Angel");
		verify(factory, times(8)).newCall(any(Request.class));
	}

	@Test
	public void theInFlightCapHolds() throws Exception
	{
		final AtomicInteger answers = new AtomicInteger();
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiPictures pictures = new WikiPictures(factory, gson, answers::incrementAndGet);

		for (int title = 0; title < 10; title++)
		{
			pictures.fetch("Boss " + title);
		}
		verify(factory, times(10)).newCall(any(Request.class));
		pictures.fetch("Boss 10");
		verify(factory, times(10)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> callbacks = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(10)).enqueue(callbacks.capture());
		callbacks.getAllValues().get(0).onResponse(call, ok(NO_THUMBNAIL));
		pictures.fetch("Boss 10");
		verify(factory, times(11)).newCall(any(Request.class));
	}

	/** The wiki's page answer for a title that has no page. */
	private static String missingPage(String title)
	{
		return "{\"query\":{\"pages\":[{\"title\":\"" + title + "\",\"missing\":true}]}}";
	}

	/** The wiki's search answer with one hit of the given title. */
	private static String searchHit(String title)
	{
		return "{\"batchcomplete\":true,\"query\":{\"searchinfo\":{\"totalhits\":3},\"search\":[{\"ns\":0,"
			+ "\"title\":\"" + title + "\",\"pageid\":9}]}}";
	}

	/** The wiki's page answer for the title, with a PNG thumbnail on the wiki's own host. */
	private static String pageWithPicture(String title)
	{
		return "{\"query\":{\"pages\":[{\"pageid\":9,\"ns\":0,\"title\":\"" + title + "\",\"thumbnail\":{\"source\":"
			+ "\"https://oldschool.runescape.wiki/images/thumb/Tempor_Tantrum.png/128px-Tempor_Tantrum.png?fghij\","
			+ "\"width\":128,\"height\":40}}]}}";
	}

	@Test
	public void aMissingPageTakesTheNearestPagesPicture() throws Exception
	{
		final AtomicInteger answers = new AtomicInteger();
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiPictures pictures = new WikiPictures(factory, gson, answers::incrementAndGet);

		pictures.fetch("Tempor Tantrum Marlin");
		final ArgumentCaptor<Request> first = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(1)).newCall(first.capture());
		assertEquals(WikiPictures.requestFor("Tempor Tantrum Marlin").url(), first.getValue().url());
		final ArgumentCaptor<Callback> page = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(page.capture());
		page.getValue().onResponse(call, ok(missingPage("Tempor Tantrum Marlin")));

		final ArgumentCaptor<Request> second = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(2)).newCall(second.capture());
		final String search = second.getValue().url().toString();
		assertTrue("the second call is the search: " + search, search.contains("list=search"));
		assertEquals("Tempor Tantrum Marlin", second.getValue().url().queryParameter("srsearch"));
		assertNull("not kept while the chain runs", pictures.pictureOf("Tempor Tantrum Marlin"));
		final ArgumentCaptor<Callback> hit = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(hit.capture());
		hit.getValue().onResponse(call, ok(searchHit("The Tempor Tantrum")));

		final ArgumentCaptor<Request> third = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(3)).newCall(third.capture());
		assertEquals(WikiPictures.requestFor("The Tempor Tantrum").url(), third.getValue().url());
		final ArgumentCaptor<Callback> nearest = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(3)).enqueue(nearest.capture());
		final String nearestAnswer = pageWithPicture("The Tempor Tantrum");
		nearest.getValue().onResponse(call, ok(nearestAnswer));

		final ArgumentCaptor<Request> fourth = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(4)).newCall(fourth.capture());
		assertEquals("the fourth call is the thumbnail's own address", WikiPictures.thumbnailUrl(nearestAnswer, gson),
			fourth.getValue().url());
		assertEquals("oldschool.runescape.wiki", fourth.getValue().url().host());
		assertNull("not kept while the chain runs", pictures.pictureOf("Tempor Tantrum Marlin"));
		final ArgumentCaptor<Callback> thumb = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(4)).enqueue(thumb.capture());
		thumb.getValue().onResponse(call, picture());

		final BufferedImage kept = pictures.pictureOf("Tempor Tantrum Marlin");
		assertNotNull("the nearest page's picture is kept under the name asked", kept);
		assertEquals(64, kept.getWidth());
		assertTrue("19 to 21 tall: " + kept.getHeight(), kept.getHeight() >= 19 && kept.getHeight() <= 21);
		assertNull("kept under the asked name only", pictures.pictureOf("The Tempor Tantrum"));

		pictures.fetch("Tempor Tantrum Marlin");
		verify(factory, times(4)).newCall(any(Request.class));
	}

	@Test
	public void aSearchHitThatSharesNoWordGivesNoPicture() throws Exception
	{
		final AtomicInteger answers = new AtomicInteger();
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiPictures pictures = new WikiPictures(factory, gson, answers::incrementAndGet);

		pictures.fetch("Pandemonium");
		final ArgumentCaptor<Callback> page = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(page.capture());
		page.getValue().onResponse(call, ok(missingPage("Pandemonium")));
		verify(factory, times(2)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> search = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(search.capture());
		search.getValue().onResponse(call, ok(searchHit("Chambers of Xeric")));
		verify(factory, times(2)).newCall(any(Request.class));
		assertNull("no word in common, no picture", pictures.pictureOf("Pandemonium"));
		assertTrue("answered with none", pictures.isAnswered("Pandemonium"));
		assertEquals("onAnswer ran once", 1, answers.get());
		pictures.fetch("Pandemonium");
		verify(factory, times(2)).newCall(any(Request.class));

		pictures.fetch("Nonsense title");
		final ArgumentCaptor<Callback> again = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(3)).enqueue(again.capture());
		again.getValue().onResponse(call, ok(missingPage("Nonsense title")));
		verify(factory, times(4)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> none = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(4)).enqueue(none.capture());
		none.getValue().onResponse(call, ok("{\"batchcomplete\":true,\"query\":{\"search\":[]}}"));
		verify(factory, times(4)).newCall(any(Request.class));
		assertNull("a search with no hit", pictures.pictureOf("Nonsense title"));
		pictures.fetch("Nonsense title");
		verify(factory, times(4)).newCall(any(Request.class));
	}

	@Test
	public void theNearestPageMustShareAWord()
	{
		assertTrue(WikiPictures.nearEnough("Tempor Tantrum Marlin", "The Tempor Tantrum"));
		assertTrue(WikiPictures.nearEnough("Jubbly Jive Shark", "Jubbly Jive Barracuda Trial"));
		assertTrue(WikiPictures.nearEnough("Brutus", "Brutus (Ides of Milk)"));
		assertFalse(WikiPictures.nearEnough("Pandemonium", "Chambers of Xeric"));
		assertFalse(WikiPictures.nearEnough("The", "Of"));
	}
}
