package co.id.jalin.camunda.training;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CalculateWorker {

    @JobWorker(type = "calculate-value", autoComplete = true)
    public Map<String,Object> calculate(
            @Variable Integer value1,
            @Variable Integer value2
    ){
        Map<String,Object> out = new HashMap<>();
        var result = value1 + value2;
        out.put("result",result);
        out.put("message","Addition result: " + result);
        return out;
    }

}
