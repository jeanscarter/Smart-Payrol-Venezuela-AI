package com.nomina.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para consultar la tasa oficial del BCV desde DolarApi.
 */
public final class BcvApiService {

    private static final String API_URL = "https://ve.dolarapi.com/v1/dolares/";

    private BcvApiService() {
    }

    /**
     * Realiza una petición GET a la API y extrae la tasa oficial del BCV.
     *
     * @return el valor de la tasa oficial en bolívares (VES)
     * @throws Exception si ocurre algún error de conexión o parseo
     */
    public static double fetchTasaBcv() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Error al consultar API BCV: Código de estado " + response.statusCode());
        }

        return parseTasaFromJson(response.body());
    }

    private static double parseTasaFromJson(String json) throws Exception {
        Pattern objectPattern = Pattern.compile("\\{[^\\}]*\\}");
        Matcher matcher = objectPattern.matcher(json);

        while (matcher.find()) {
            String objectStr = matcher.group();
            if (objectStr.contains("\"fuente\"") && 
                (objectStr.contains("\"oficial\"") || objectStr.contains("oficial"))) {
                
                Pattern promedioPattern = Pattern.compile("\"promedio\"\\s*:\\s*([0-9.]+)");
                Matcher promMatcher = promedioPattern.matcher(objectStr);
                if (promMatcher.find()) {
                    return Double.parseDouble(promMatcher.group(1));
                }
            }
        }
        throw new Exception("No se encontró la tasa oficial (BCV) en la respuesta de la API.");
    }
}
