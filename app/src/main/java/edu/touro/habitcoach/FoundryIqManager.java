package edu.touro.habitcoach;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestSystemMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;


public class FoundryIqManager {

    private final SearchClient searchClient;
    private final ChatCompletionsClient aiClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FoundryIqManager(ChatCompletionsClient aiClient) {
        this.aiClient = aiClient;

        // Secrets are now pulled from secrets.properties via BuildConfig
        this.searchClient = new SearchClientBuilder()
                .endpoint(BuildConfig.SEARCH_ENDPOINT)
                .indexName(BuildConfig.INDEX_NAME)
                .credential(new AzureKeyCredential(BuildConfig.SEARCH_KEY))
                .buildClient();
    }

    /**
     * Executes an agentic query: 
     * 1. Retrieves relevant context from Azure AI Search (IQ Layer)
     * 2. Combines it with user habits and question
     * 3. Calls the LLM for a final response
     */
    public void askCoachWithIq(String userQuestion, String habitContext, IqCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Retrieve data from the Foundry IQ Layer (Search)
                StringBuilder iqContext = new StringBuilder();
                
                SearchOptions options = new SearchOptions()
                        .setTop(3);

                for (SearchResult result : searchClient.search(userQuestion, options, Context.NONE)) {
                    SearchDocument doc = result.getDocument(SearchDocument.class);
                    Object contentObj = doc.get("content");
                    if (contentObj != null) {
                        iqContext.append(contentObj.toString()).append("\n\n");
                    }
                }

                // 2. Synthesize with LLM
                List<ChatRequestMessage> messages = new ArrayList<>();
                
                String systemPrompt = "You are an AI Habit Coach powered by Foundry IQ. " +
                        "Use the following expert knowledge context and the user's habit data to provide advice.\n\n" +
                        "Expert Context:\n" + (iqContext.length() > 0 ? iqContext.toString() : "No specific external context found.") + "\n" +
                        "User Progress:\n" + habitContext;

                messages.add(new ChatRequestSystemMessage(systemPrompt));
                messages.add(new ChatRequestUserMessage(userQuestion));

                ChatCompletionsOptions chatOptions = new ChatCompletionsOptions(messages);
                chatOptions.setModel("gpt-4o-mini");

                ChatCompletions completions = aiClient.complete(chatOptions);

                if (completions.getChoices() != null && !completions.getChoices().isEmpty()) {
                    String finalAnswer = completions.getChoices().get(0).getMessage().getContent();
                    mainHandler.post(() -> callback.onSuccess(finalAnswer));
                } else {
                    mainHandler.post(() -> callback.onFailure("AI returned no results."));
                }

            } catch (Exception e) {
                Log.e("FoundryIq", "Error in agentic query", e);
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public interface IqCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }
}
