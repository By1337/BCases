package dev.by1337.bc.util;

import dev.by1337.virtualentity.core.mappings.Mappings;
import dev.by1337.virtualentity.core.mappings.VirtualEntityRegistrar;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.BLib;
import org.by1337.blib.net.RepositoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public class LibLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("BCases");

    public static void load(Plugin plugin) {

        Path libraries = new File(plugin.getDataFolder(), "libraries").toPath();

        if (!hasClass("com.zaxxer.hikari.HikariConfig")) {
            Path cp = RepositoryUtil.downloadIfNotExist("https://repo1.maven.org/maven2", "com.zaxxer", "HikariCP", "5.1.0", libraries);
            BLib.getApi().getUnsafe().getPluginClasspathUtil().addUrl(plugin, cp.toFile());
        }
        if (!hasClass("dev.by1337.virtualentity.api.VirtualEntityFactory")) {
            Path cp = downloadFromGitHub(
                    "https://github.com/By1337/VirtualEntityApi/releases/download/1.2.3/VirtualEntityApi-1.2.3.jar",
                    "dev.by1337.virtualentity.core",
                    "VirtualEntityApi",
                    "1.2.3",
                    libraries
            );
            BLib.getApi().getUnsafe().getPluginClasspathUtil().addUrl(plugin, cp.toFile());

            Mappings.load();
            VirtualEntityRegistrar.register();
        }
        if (!hasClass("org.by1337.bmenu.Menu")) {
            Path cp = downloadFromGitHub("https://github.com/By1337/BMenu/releases/download/1.6/BMenu-1.6.jar", "org.by1337.bmenu", "BMenu", "1.6", libraries);
            BLib.getApi().getUnsafe().getPluginClasspathUtil().addUrl(plugin, cp.toFile());
        }
    }


    private static boolean hasClass(String s) {
        try {
            Class.forName(s);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Path downloadFromGitHub(String url, String groupId, String artifactId, String version, Path folder) {
        Path resultFolder = folder.resolve(groupId.replace(".", "/")).resolve(artifactId).resolve(version);
        resultFolder.toFile().mkdirs();
        Path out = resultFolder.resolve(artifactId + "-" + version + ".jar");
        if (out.toFile().exists()) {
            return out;
        }
        LOGGER.info("Download {}-{}.jar...", artifactId, version);

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(out));
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }
            return out;
        } catch (Exception t) {
            throw new RuntimeException(t);
        }
    }
}
