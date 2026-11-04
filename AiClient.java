package com.seuprojeto.nexus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Conversa com a API da Anthropic usando tool-use: a própria IA decide
 * quando controlar um dispositivo, tocar/pausar música ou abrir conteúdo,
 * em vez de um sistema de palavras-chave fixas.
 */
public class AiClient {

    public interface Callback {
        void onSuccess(String reply);
        void onToolCall(String toolName, JSONObject input, ToolResultCallback resultCallback);
        void onError(String message);
    }

    public interface ToolResultCallback {
        void onResult(String resultText);
    }

    private static JSONArray buildTools() {
        JSONArray tools = new JSONArray();
        try {
            tools.put(new JSONObject()
                    .put("name", "control_device")
                    .put("description", "Liga ou desliga um dispositivo da casa cadastrado pelo nome (ex: tomada da sala, luz do quarto).")
                    .put("input_schema", new JSONObject()
                            .put("type", "object")
                            .put("properties", new JSONObject()
                                    .put("device_name", new JSONObject().put("type", "string"))
                                    .put("action", new JSONObject().put("type", "string")
                                            .put("enum", new JSONArray().put("ligar").put("desligar"))))
                            .put("required", new JSONArray().put("device_name").put("action"))));

            tools.put(new JSONObject()
                    .put("name", "control_music")
                    .put("description", "Controla a reprodução de mídia/música no celular: tocar, pausar, parar, próxima ou anterior.")
                    .put("input_schema", new JSONObject()
                            .put("type", "object")
                            .put("properties", new JSONObject()
                                    .put("action", new JSONObject().put("type", "string")
                                            .put("enum", new JSONArray().put("play").put("pause").put("stop").put("next").put("previous"))))
                            .put("required", new JSONArray().put("action"))));

            tools.put(new JSONObject()
                    .put("name", "open_content")
                    .put("description", "Abre uma página/site na tela para o usuário ver, com um botão para voltar à Nexus.")
                    .put("input_schema", new JSONObject()
                            .put("type", "object")
                            .put("properties", new JSONObject()
                                    .put("url", new JSONObject().put("type", "string"))
                                    .put("title", new JSONObject().put("type", "string")))
                            .put("required", new JSONArray().put("url"))));
        } catch (Exception ignored) {
        }
        return tools;
    }

    public static void sendMessage(String apiKey, String model, List<String[]> history, String userMessage, Callback callback) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                for (String[] turn : history) {
                    messages.put(new JSONObject().put("role", turn[0]).put("content", turn[1]));
                }
                messages.put(new JSONObject().put("role", "user").put("content", userMessage));

                JSONObject firstResponse = callApi(apiKey, model, messages);
                handleResponse(apiKey, model, messages, firstResponse, callback);
            } catch (Exception e) {
                callback.onError("Falha de conexão: " + e.getMessage());
            }
        }).start();
    }

    private static JSONObject callApi(String apiKey, String model, JSONArray messages) throws Exception {
        URL url = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setRequestProperty("content-type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("system", "Você é a Nexus, assistente pessoal de IA. Responda em português do Brasil, direto e natural. " +
                "Use as ferramentas disponíveis quando o pedido do usuário exigir uma ação real (controlar dispositivo, música, abrir conteúdo); " +
                "para conversa normal, apenas responda com texto.");
        body.put("messages", messages);
        body.put("tools", buildTools());

        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        Scanner sc = new Scanner(is, "UTF-8").useDelimiter("\\A");
        String responseText = sc.hasNext() ? sc.next() : "";

        if (code < 200 || code >= 300) {
            throw new RuntimeException("Erro " + code + ": " + responseText);
        }
        return new JSONObject(responseText);
    }

    private static void handleResponse(String apiKey, String model, JSONArray messages, JSONObject json, Callback callback) throws Exception {
        JSONArray content = json.getJSONArray("content");
        String stopReason = json.optString("stop_reason", "");

        if ("tool_use".equals(stopReason)) {
            JSONObject toolUseBlock = null;
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.getJSONObject(i);
                if ("tool_use".equals(block.optString("type"))) {
                    toolUseBlock = block;
                    break;
                }
            }
            if (toolUseBlock == null) {
                callback.onSuccess(extractText(content));
                return;
            }

            String toolName = toolUseBlock.getString("name");
            String toolUseId = toolUseBlock.getString("id");
            JSONObject input = toolUseBlock.optJSONObject("input");
            if (input == null) input = new JSONObject();

            callback.onToolCall(toolName, input, resultText -> {
                try {
                    messages.put(new JSONObject().put("role", "assistant").put("content", content));

                    JSONArray toolResultContent = new JSONArray();
                    toolResultContent.put(new JSONObject()
                            .put("type", "tool_result")
                            .put("tool_use_id", toolUseId)
                            .put("content", resultText));
                    messages.put(new JSONObject().put("role", "user").put("content", toolResultContent));

                    JSONObject second = callApi(apiKey, model, messages);
                    callback.onSuccess(extractText(second.getJSONArray("content")));
                } catch (Exception e) {
                    callback.onError("Erro ao concluir ação: " + e.getMessage());
                }
            });
        } else {
            callback.onSuccess(extractText(content));
        }
    }

    private static String extractText(JSONArray content) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.getJSONObject(i);
            if ("text".equals(block.optString("type"))) sb.append(block.optString("text"));
        }
        return sb.toString().trim();
    }
}
