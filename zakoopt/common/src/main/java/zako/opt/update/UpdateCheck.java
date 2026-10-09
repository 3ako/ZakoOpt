package zako.opt.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import zako.opt.ZakoOptConfig;
import zako.opt.ZakoOptions;

// one request to the GitHub releases API per game launch, when the title screen first opens; nothing at all when
// switched off. A newer release is offered once, as a dialog over the title screen
@UtilityClass
@Slf4j(topic = "zakoopt")
public class UpdateCheck {
	private final String REPO = "3ako/ZakoOpt";
	private boolean started;
	private volatile String latest;
	private volatile String url;
	private boolean shown;

	public void start() {
		if (ZakoOptConfig.values.updateCheck && !started) {
			started = true;
			fetch();
		}
	}

	// from TitleScreen.tick, so the title screen is the one on display
	public void offer(Screen title) {
		if (latest == null || shown) {
			return;
		}
		shown = true;
		UpdateUi.setScreen(new ConfirmLinkScreen(open -> {
			if (open) {
				UpdateUi.open(url);
			}
			UpdateUi.setScreen(title);
		}, Component.translatable("zakoopt.update.title", latest), Component.translatable("zakoopt.update.message", ZakoOptions.VERSION),
				url, Component.translatable("zakoopt.update.later"), true));
	}

	private void fetch() {
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPO + "/releases/latest"))
				.timeout(Duration.ofSeconds(5))
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", "ZakoOpt/" + ZakoOptions.VERSION)
				.build();
		HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
				.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenAccept(response -> {
					if (response.statusCode() != 200) {
						log.debug("Update check: HTTP {}", response.statusCode());
						return;
					}
					JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
					String tag = release.get("tag_name").getAsString();
					if (Versions.newer(tag, ZakoOptions.VERSION)) {
						url = release.get("html_url").getAsString();
						latest = tag;
					}
				})
				.exceptionally(e -> {
					log.debug("Update check failed", e);
					return null;
				});
	}
}
