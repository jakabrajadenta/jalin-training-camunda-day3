package co.id.jalin.camunda.training;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
@Component
public class CheckStatusWorker {

    private final WebClient webClient = WebClient.create("http://localhost/api");

    @JobWorker(type = "check-status")
    public Map<String,Object> checkStatus(@Variable String nik){
        log.info("Start checking status for {}",nik);
        Map<String,Object> result = new HashMap<>();
        try {
            var response = webClient.get()
                    .uri("/pelanggan.json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            var status = "NOT FOUND";
            if (Objects.nonNull(response) && response.containsKey("data")) {
                var list = (Iterable<Map<String,Object>>) response.get("data");
                for (Map<String, Object> item : list) {
                    if (Objects.equals(nik,item.get("nik"))) {
                        status = (String) item.get("status");
                        break;
                    }
                }
            }
            result.put("status",status);
            result.put("message","Success call API");
        } catch (Exception e) {
            log.error("e: ", e);
            result.put("status","ERROR");
            result.put("message","Error cause " + e.getMessage());
        }
        return result;
    }
}
