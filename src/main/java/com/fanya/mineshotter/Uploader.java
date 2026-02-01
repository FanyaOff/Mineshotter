package com.fanya.mineshotter;

import com.fanya.mineshotter.config.MineshotterConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class Uploader {

    public static String uploadFile(File file) {
        MineshotterConfig.UploaderProfile profile = MineshotterConfig.get().getActiveProfile();
        if (profile == null) return null;

        try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build()) {

            String boundary = "---MineshotterBoundary" + UUID.randomUUID();
            byte[] body = buildMultipartBody(file, profile, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(profile.requestUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseResponse(response.body(), profile.urlResultPath);
            } else {
                System.err.println("Upload failed: " + response.statusCode() + " " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] buildMultipartBody(File file, MineshotterConfig.UploaderProfile profile, String boundary) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        if (profile.arguments != null) {
            for (Map.Entry<String, String> entry : profile.arguments.entrySet()) {
                output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write((entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        String fileName = file.getName();
        String formName = profile.fileFormName != null && !profile.fileFormName.isEmpty() ? profile.fileFormName : "file";

        output.write(("Content-Disposition: form-data; name=\"" + formName + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));

        output.write(Files.readAllBytes(file.toPath()));
        output.write(("\r\n").getBytes(StandardCharsets.UTF_8));

        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return output.toByteArray();
    }

    private static String parseResponse(String jsonBody, String pathPattern) {
        try {
            if (pathPattern == null || pathPattern.isEmpty()) return null;

            if (pathPattern.startsWith("{json:") && pathPattern.endsWith("}")) {
                String cleanPath = pathPattern.substring(6, pathPattern.length() - 1);
                String[] keys = cleanPath.split("\\.");

                JsonElement current = JsonParser.parseString(jsonBody);

                for (String key : keys) {
                    if (current.isJsonObject()) {
                        current = current.getAsJsonObject().get(key);
                    } else {
                        return null; // Path invalid
                    }
                    if (current == null) return null; // Key not found
                }

                if (current.isJsonPrimitive()) {
                    return current.getAsString();
                }
            } else {
                return jsonBody.trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
