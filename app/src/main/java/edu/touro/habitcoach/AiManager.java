package edu.touro.habitcoach;

import android.os.Handler;
import android.os.Looper;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.ChatCompletionsClientBuilder;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestSystemMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import edu.touro.habitcoach.BuildConfig;

public class AiManager {

    private final ChatCompletionsClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiManager() {
        String endpoint = "https://models.inference.ai.azure.com";

        // GITHUB_TOKEN is pulled from secrets.properties via BuildConfig
        this.client = new ChatCompletionsClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(BuildConfig.GITHUB_TOKEN))
                .buildClient();
    }

    public void askAiWithContext(String userQuestion, String embeddedContext, AiCallback callback) {
        // Run in background thread to keep Android smooth
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ChatRequestMessage> chatMessages = new ArrayList<>();
                
                // system instruction for Habit Coach
                String systemInstruction = "You are an expert Habit Coach. Provide motivating, concise advice based on the user's progress. " +
                        "Here is the context: " + embeddedContext;
                
                chatMessages.add(new ChatRequestSystemMessage(systemInstruction));
                chatMessages.add(new ChatRequestUserMessage(userQuestion));

                ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
                options.setModel("gpt-4o-mini"); 

                ChatCompletions completions = client.complete(options);
                
                if (completions.getChoices() != null && !completions.getChoices().isEmpty()) {
                    String answer = completions.getChoices().get(0).getMessage().getContent();
                    // Return result to main thread
                    mainHandler.post(() -> callback.onSuccess(answer));
                } else {
                    mainHandler.post(() -> callback.onFailure("No response from AI."));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }
}
