package com.cabosschecklist;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wiki's own picture of a page, for the boss card to show in its picture box: the Old School RuneScape Wiki is asked
 * for the page's lead image when a card is drawn, the picture is kept, and the card reads it from here. It uses RuneLite's
 * HTTP client asked in the background ({@code enqueue}, never {@code execute}), with a User-Agent that names this
 * plugin. What is sent is the page's title and nothing else; the answer names the picture's own address, which is
 * followed only when it is on {@code oldschool.runescape.wiki} and a PNG, GIF or JPEG (the wiki's WebP files cannot be
 * shown by the client, so they are never fetched). The picture is decoded with the AWT toolkit's own decoder and
 * scaled down to fit a {@value #BOX} px square, its longer side {@value #BOX} and nothing cropped, on the HTTP client's
 * thread, never the client thread; a picture already that small is kept as it is. A title is asked for once: a page that
 * failed, or has no picture, is remembered as having none and not asked again. When the wiki says there is no page of
 * that title, its own search is asked once for the name and the first hit's picture is used instead, but only when the
 * hit's title shares a word with the name ({@link #nearEnough}); otherwise the card shows none. The asking is capped
 * (10 titles at a time, 60 calls a minute, each page, search and picture call counting, 1 MB read of a picture), and the
 * pictures kept are the 200 asked for last.
 *
 * <p>Choice: a failed call is never retried in the same run; one warning is logged the first time a call fails, and the
 * later ones go to the debug log, so a wiki that is down does not fill the log. The caller's {@code onAnswer} runs after
 * each title's answer (a picture or none) is stored, on the HTTP client's thread (or the caller's, when a call fails at
 * once), outside the lock.
 */
final class WikiPictures
{
	private static final Logger log = LoggerFactory.getLogger(WikiPictures.class);

	/** The wiki's host; the only one this class talks to and reads pictures from. */
	static final String HOST = "oldschool.runescape.wiki";
	/** The User-Agent every call carries, naming the plugin and where its source is. */
	static final String USER_AGENT = "2h CA Boss Checklist (RuneLite plugin; github.com/2hBuilds/ca-boss-checklist)";
	/** The width the wiki is asked to scale its picture to, in px. */
	private static final int THUMB = 128;
	/** The side of the square box a kept picture is fitted to, in px. */
	private static final int BOX = 64;
	/** How many pictures are kept; the oldest go first. */
	private static final int MAX_CACHED = 200;
	/** How many titles may be waiting for an answer at once. */
	private static final int MAX_IN_FLIGHT = 10;
	/** How many calls may be started in one minute. */
	private static final int MAX_PER_MINUTE = 60;
	/** A minute, in {@link System#nanoTime} terms. */
	private static final long MINUTE_NANOS = 60_000_000_000L;
	/** The most bytes of a picture that are read; a longer one is dropped. */
	private static final long MAX_BYTES = 1_000_000L;
	/** The words {@link #nearEnough} does not count: they would match nearly any two titles. */
	private static final Set<String> FILLER = new HashSet<>(Arrays.asList("the", "and", "of", "for", "with"));

	private final Call.Factory calls;
	private final Gson gson;
	private final Runnable onAnswer;
	/** Guards everything below: the answers arrive on the HTTP client's thread and are read on the EDT. */
	private final Object lock = new Object();
	/**
	 * The fitted pictures by title, null for a page that failed or has none; at most {@link #MAX_CACHED}, the oldest out.
	 * A title that is a key was asked for, whatever its value.
	 */
	private final Map<String, BufferedImage> cache = new LinkedHashMap<String, BufferedImage>()
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest)
		{
			return size() > MAX_CACHED;
		}
	};
	/** The titles whose chain of calls (page, search, nearest page, picture) has not ended yet. */
	private final Set<String> inFlight = new HashSet<>();
	/** When the current minute began, in {@link System#nanoTime} terms, and how many calls it has started. */
	private long windowStart = System.nanoTime();
	private int startedInWindow;
	/** Whether a failed call has been logged as a warning yet. */
	private boolean warned;

	/**
	 * @param calls where calls are made: RuneLite's own HTTP client (an {@code okhttp3.Call.Factory}), or a stand-in in a test
	 * @param gson RuneLite's Gson, which reads the answer
	 * @param onAnswer runs after a title's answer (a picture or none) is stored; it may run on any thread
	 */
	WikiPictures(Call.Factory calls, Gson gson, Runnable onAnswer)
	{
		this.calls = calls;
		this.gson = gson;
		this.onAnswer = onAnswer;
	}

	/**
	 * The request for a page's lead image: the wiki's {@code api.php} asked for the page's thumbnail, 128 px wide, redirects
	 * followed, as JSON; the title is percent-encoded (a space as %20). It carries this plugin's User-Agent and nothing
	 * about the player.
	 */
	static Request requestFor(String title)
	{
		final HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host(HOST)
			.addPathSegments("api.php")
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", String.valueOf(THUMB))
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("titles", title)
			.build();
		return new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
	}

	/** The request for the picture at {@code url}, which {@link #thumbnailUrl} has checked: a GET with the User-Agent. */
	static Request requestForPicture(HttpUrl url)
	{
		return new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
	}

	/**
	 * The request for the wiki's own search of a name: {@code api.php} asked for the single best hit, as JSON; the name is
	 * percent-encoded (a space as %20). It carries this plugin's User-Agent and nothing about the player.
	 */
	static Request requestForSearch(String title)
	{
		final HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host(HOST)
			.addPathSegments("api.php")
			.addQueryParameter("action", "query")
			.addQueryParameter("list", "search")
			.addQueryParameter("srsearch", title)
			.addQueryParameter("srlimit", "1")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.build();
		return new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
	}

	/**
	 * The title of the first hit the wiki's search answer holds, or null when there is none. The answer is
	 * {@code {"query": {"search": [{"title": "...", ...}]}}}; no hit is {@code "search": []}. An answer with no search
	 * list, a hit with no title or a blank one, an empty answer, or text that is not JSON gives null.
	 */
	static String firstSearchTitle(String json, Gson gson)
	{
		final JsonObject query = objectOf(parse(json, gson), "query");
		final JsonElement hits = query == null ? null : query.get("search");
		if (hits == null || !hits.isJsonArray() || hits.getAsJsonArray().size() == 0
			|| !hits.getAsJsonArray().get(0).isJsonObject())
		{
			return null;
		}
		final JsonElement title = hits.getAsJsonArray().get(0).getAsJsonObject().get("title");
		if (title == null || !title.isJsonPrimitive() || title.getAsString().trim().isEmpty())
		{
			return null;
		}
		return title.getAsString().trim();
	}

	/**
	 * Whether the wiki's search hit is near enough to the name asked for to be trusted: the two titles share at least one
	 * word. The words are the lower-cased runs of letters and digits in each title; a word shorter than 3 letters and the
	 * words "the", "and", "of", "for" and "with" are not counted; a title with no words left shares none. The examples:
	 * ("Tempor Tantrum Marlin", "The Tempor Tantrum") shares two, so true; ("Jubbly Jive Shark", "Jubbly Jive Barracuda
	 * Trial") true; ("Brutus", "Brutus (Ides of Milk)") true; ("Pandemonium", "Chambers of Xeric") false; ("The", "Of")
	 * false. A hit that is not near enough gives the card nothing, so it never shows another page's picture.
	 */
	static boolean nearEnough(String asked, String hit)
	{
		final Set<String> askedWords = wordsOf(asked);
		if (askedWords.isEmpty())
		{
			return false;
		}
		for (final String word : wordsOf(hit))
		{
			if (askedWords.contains(word))
			{
				return true;
			}
		}
		return false;
	}

	/** The counted words of a title, lower-cased; see {@link #nearEnough}. */
	private static Set<String> wordsOf(String title)
	{
		final Set<String> words = new HashSet<>();
		if (title == null)
		{
			return words;
		}
		for (final String word : title.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
		{
			if (word.length() >= 3 && !FILLER.contains(word))
			{
				words.add(word);
			}
		}
		return words;
	}

	/**
	 * Whether the wiki's page answer says the page does not exist: its first page carries {@code "missing": true}. A page
	 * that exists but has no picture, an empty answer, or text that is not JSON is not missing.
	 */
	static boolean isMissing(String json, Gson gson)
	{
		final JsonObject page = firstPage(json, gson);
		final JsonElement missing = page == null ? null : page.get("missing");
		return missing != null && missing.isJsonPrimitive() && missing.getAsJsonPrimitive().isBoolean()
			&& missing.getAsBoolean();
	}

	/** The first page of a page answer, or null when the answer has none or is not JSON. */
	static JsonObject firstPage(String json, Gson gson)
	{
		final JsonObject query = objectOf(parse(json, gson), "query");
		final JsonElement pages = query == null ? null : query.get("pages");
		if (pages == null || !pages.isJsonArray())
		{
			return null;
		}
		final JsonArray list = pages.getAsJsonArray();
		if (list.size() == 0 || !list.get(0).isJsonObject())
		{
			return null;
		}
		return list.get(0).getAsJsonObject();
	}

	/** The answer as an object, or null when it is empty, is not JSON, or is not an object. */
	private static JsonObject parse(String json, Gson gson)
	{
		try
		{
			return gson.fromJson(json, JsonObject.class);
		}
		catch (JsonParseException e)
		{
			return null;
		}
	}

	/** A member of an object that is itself an object, or null when it is missing or is something else. */
	private static JsonObject objectOf(JsonObject parent, String member)
	{
		if (parent == null)
		{
			return null;
		}
		final JsonElement child = parent.get(member);
		return child != null && child.isJsonObject() ? child.getAsJsonObject() : null;
	}

	/**
	 * The address of the thumbnail the wiki's page answer holds: the first page's {@code thumbnail.source}. The answer is
	 * {@code {"query": {"pages": [{"title": "...", "thumbnail": {"source": "https://...", ...}}]}}}. Null unless the address
	 * is on {@code https}, on the wiki's own host, and its last path segment, in lower case, ends in {@code .png},
	 * {@code .gif}, {@code .jpg} or {@code .jpeg}; null as well for a page that is missing or has no thumbnail, an answer
	 * with no pages, an empty answer, or text that is not JSON.
	 */
	static HttpUrl thumbnailUrl(String json, Gson gson)
	{
		final JsonObject page = firstPage(json, gson);
		final JsonElement thumbnail = page == null ? null : page.get("thumbnail");
		if (thumbnail == null || !thumbnail.isJsonObject())
		{
			return null;
		}
		final JsonElement source = thumbnail.getAsJsonObject().get("source");
		if (source == null || !source.isJsonPrimitive())
		{
			return null;
		}
		final HttpUrl url = HttpUrl.parse(source.getAsString());
		if (url == null || !"https".equals(url.scheme()) || !HOST.equals(url.host()))
		{
			return null;
		}
		final String file = url.pathSegments().get(url.pathSegments().size() - 1).toLowerCase(Locale.ROOT);
		return file.endsWith(".png") || file.endsWith(".gif") || file.endsWith(".jpg") || file.endsWith(".jpeg") ? url : null;
	}

	/**
	 * The picture the bytes hold, as an ARGB image with its transparency kept, decoded by the AWT toolkit and loaded
	 * through an icon so that it is complete when this returns; null for null or empty bytes, for bytes the toolkit cannot
	 * read (a WebP file, a page of text), and for a picture with no size.
	 */
	static BufferedImage decode(byte[] bytes)
	{
		if (bytes == null || bytes.length == 0)
		{
			return null;
		}
		try
		{
			final Image image = Toolkit.getDefaultToolkit().createImage(bytes);
			final ImageIcon icon = new ImageIcon(image);
			if (icon.getImageLoadStatus() != MediaTracker.COMPLETE || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0)
			{
				return null;
			}
			return ImageUtil.bufferedImageFromImage(icon.getImage());
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/**
	 * The picture as the cards keep it: the picture itself when both its sides are {@value #BOX} px or less, else scaled
	 * down so that its longer side is {@value #BOX} and the other follows the ratio. Never scaled up, never cropped.
	 */
	static BufferedImage fit(BufferedImage picture)
	{
		if (picture.getWidth() <= BOX && picture.getHeight() <= BOX)
		{
			return picture;
		}
		return ImageUtil.resizeImage(picture, BOX, BOX, true);
	}

	/**
	 * The picture kept for a title, fitted to the box, or null when the title was never asked for, its answer has not come
	 * yet, or the page has none (or none that could be read).
	 */
	BufferedImage pictureOf(String title)
	{
		synchronized (lock)
		{
			return cache.get(title);
		}
	}

	/** Whether the title's answer (a picture, or none) is kept: false while it was never asked for or is still on its way. */
	boolean isAnswered(String title)
	{
		synchronized (lock)
		{
			return cache.containsKey(title);
		}
	}

	/**
	 * Asks the wiki for a title's picture in the background. Nothing happens when the title is blank, already asked for,
	 * already waiting for its answer, when 10 titles are already waiting, or when 60 calls have been started in the last
	 * minute (the title is then asked for again by the next draw). The page is asked first; a page with no picture the
	 * client can show ends the chain with none, and a page with one leads to a second call for the picture's own address,
	 * whose body is read up to 1 MB, decoded, fitted and kept. A picture that is too long, cannot be read, or whose call
	 * failed is kept as none. Each of the two calls counts in the minute; when the minute is full before the second, the
	 * title is let go unkept and the next draw asks again. A page the wiki says is missing leads to one search call and,
	 * on a hit whose title shares a word with the title asked for, to the hit's page and its picture (a hit's page that is
	 * itself missing ends the chain with none, with no second search); the picture is kept under the title asked for.
	 */
	void fetch(String title)
	{
		if (title == null || title.trim().isEmpty())
		{
			return;
		}
		synchronized (lock)
		{
			if (cache.containsKey(title) || inFlight.contains(title) || inFlight.size() >= MAX_IN_FLIGHT
				|| !startCall())
			{
				return;
			}
			inFlight.add(title);
		}
		ask(title, requestFor(title), Step.PAGE);
	}

	/**
	 * Counts one call started in the minute, or says there is no room: false when 60 have started in the last minute.
	 * The caller holds {@link #lock}.
	 */
	private boolean startCall()
	{
		final long now = System.nanoTime();
		if (now - windowStart >= MINUTE_NANOS)
		{
			windowStart = now;
			startedInWindow = 0;
		}
		if (startedInWindow >= MAX_PER_MINUTE)
		{
			return false;
		}
		startedInWindow++;
		return true;
	}

	/** Makes one call of a title's chain in the background; its answer goes to {@link #answered}. */
	private void ask(String title, Request request, Step step)
	{
		try
		{
			calls.newCall(request).enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e)
				{
					failed(title, e.toString());
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					answered(title, step, response);
				}
			});
		}
		catch (RuntimeException e)
		{
			failed(title, e.toString());
		}
	}

	/**
	 * Reads a 200 answer for the step it was asked in and keeps none for any other answer; the answer is always closed. A
	 * page answer that says the page is missing goes on to the search; a search answer with a hit that is another title and
	 * shares a word with the name goes on to that page; a page answer (the title's own or the hit's) whose thumbnail can be
	 * shown goes on to the picture, and one without ends the chain with none; a picture answer is read, decoded and fitted
	 * here, on the HTTP client's thread, and kept.
	 */
	private void answered(String title, Step step, Response response)
	{
		try (Response answer = response)
		{
			final ResponseBody body = answer.body();
			if (answer.code() != 200 || body == null)
			{
				failed(title, "HTTP " + answer.code());
				return;
			}
			if (step == Step.SEARCH)
			{
				final String nearest = firstSearchTitle(body.string(), gson);
				if (nearest == null || nearest.equals(title) || !nearEnough(title, nearest))
				{
					finish(title, null);
				}
				else
				{
					next(title, requestFor(nearest), Step.NEAREST);
				}
				return;
			}
			if (step == Step.PAGE || step == Step.NEAREST)
			{
				final String json = body.string();
				if (step == Step.PAGE && isMissing(json, gson))
				{
					next(title, requestForSearch(title), Step.SEARCH);
					return;
				}
				final HttpUrl address = thumbnailUrl(json, gson);
				if (address == null)
				{
					finish(title, null);
				}
				else
				{
					next(title, requestForPicture(address), Step.PICTURE);
				}
				return;
			}
			// Choice: one byte more than the cap is read, which says the picture is too long without reading the rest of it.
			final byte[] bytes = body.byteStream().readNBytes((int) MAX_BYTES + 1);
			if (bytes.length > MAX_BYTES)
			{
				failed(title, "the picture is over " + MAX_BYTES + " bytes");
				return;
			}
			final BufferedImage picture = decode(bytes);
			if (picture == null)
			{
				failed(title, "the picture could not be decoded");
				return;
			}
			finish(title, fit(picture));
		}
		catch (IOException | RuntimeException e)
		{
			failed(title, e.toString());
		}
	}

	/** Goes on to the next call of a title's chain, or lets the title go unkept when the minute has no room for it. */
	private void next(String title, Request request, Step step)
	{
		synchronized (lock)
		{
			if (!startCall())
			{
				inFlight.remove(title);
				return;
			}
		}
		ask(title, request, step);
	}

	/** Keeps none for the title and says why: one warning the first time, debug lines after. */
	private void failed(String title, String why)
	{
		final boolean first;
		synchronized (lock)
		{
			first = !warned;
			warned = true;
		}
		if (first)
		{
			log.warn("A wiki picture could not be read ({}); the boss card shows none", why);
		}
		else
		{
			log.debug("No wiki picture for '{}': {}", title, why);
		}
		finish(title, null);
	}

	/** Keeps the picture, or none for null, under the title, takes it off the waiting list, then tells the caller. */
	private void finish(String title, BufferedImage picture)
	{
		synchronized (lock)
		{
			inFlight.remove(title);
			cache.put(title, picture);
		}
		onAnswer.run();
	}

	/** Which call of a title's chain an answer belongs to. */
	private enum Step
	{
		/** The title's own page, asked for its thumbnail. */
		PAGE,
		/** The wiki's search for the title, after its page was missing. */
		SEARCH,
		/** The page the search ranked first, asked for its thumbnail. */
		NEAREST,
		/** The thumbnail's own address. */
		PICTURE
	}
}
