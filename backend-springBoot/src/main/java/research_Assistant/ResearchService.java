package research_Assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest request) {
        String prompt = buildPrompt(request);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                       Map.of("parts",new Object[]{
                               Map.of("text",prompt)
                       })
                }
        );

        try {
            String response = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)  // Proper API key handling
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractTextFromResponse(response);
        } catch (WebClientResponseException e) {
            return "Error calling Gemini API: " + e.getResponseBodyAsString();
        }
    }
    private String extractTextFromResponse(String response){

        try{
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()){
                GeminiResponse.Candidate fistCandidate = geminiResponse.getCandidates().get(0);

                if(fistCandidate.getContent() != null && fistCandidate.getContent().getParts() != null
                   && !fistCandidate.getContent().getParts().isEmpty()){
                  return   fistCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No content found in response";
        }catch (Exception e){
             return "Error parse"+ e.getMessage();

        }
    }

    private String buildPrompt(ResearchRequest request)  {
        StringBuilder prompt = new StringBuilder();
        switch (request.getOperation()){

            case "summarize":
                prompt.append("Provide a clear and concise summary of the following text in a few sentences:\n\n ");
                break;
            case "suggest":
                prompt.append("Based on the following content: suggest the related topics and further reading.Format the response with clear headings and bullet points.\n\n");
                break;
            default:
                throw new IllegalArgumentException("Unknown Operation " + request.getOperation());
        }
          prompt.append(request.getContent());
          return prompt.toString();
    }
}
