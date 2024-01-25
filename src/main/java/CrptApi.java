import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Semaphore semaphore;
    private int counterOfFinishedQuery;
    private static final Document docExample = new Document(
            new Description("string"),
            "string",
            "string",
            "LP_INTRODUCE_GOODS",
            true,
            "string",
            "string",
            "string",
            "string",
            "string",
            new ArrayList<>(List.of(
                    new Product(
                            "string",
                            "2020-01-23",
                            "string",
                            "string",
                            "string",
                            "2020-01-23",
                            "string",
                            "string",
                            "string"
                    )
            )),
            "2020-01-23",
            "string"
    );

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        var scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(this::releasePermit, 1, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();
            try {
                var docJson = new Gson().toJson(document);
                sendPost(docJson);
            } finally {
                incrementReadyToRelease();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод, который отправляет сообщение методом POST в Честный знак
     */
    private void sendPost(String body) {
        var post = new HttpPost(URL);
        var entity = new StringEntity(body);
        post.addHeader("content-type", "application/json");
        post.setEntity(entity);
        try (var httpClient = HttpClients.createDefault();
             var response = httpClient.execute(post)) {

            var result = EntityUtils.toString(response.getEntity());
            System.out.println(result);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void releasePermit() {
        semaphore.release(nullifyAndReturnReadyToRelease());
    }

    private synchronized void incrementReadyToRelease() {
        counterOfFinishedQuery++;
    }

    private synchronized int nullifyAndReturnReadyToRelease() {
        var temp = counterOfFinishedQuery;
        counterOfFinishedQuery = 0;
        return temp;
    }

    /**
     * Метод для проверки работоспособности функционала
     */
    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
        for (int i = 0; i < 30; i++) {
            new Thread(() ->
                    api.createDocument(docExample, "not used")
            ).start();
        }
    }
}

record Document(
        Description description,
        String docId,
        String docStatus,
        String docType,
        Boolean importRequest,
        String ownerInn,
        String participantInn,
        String producerInn,
        String productionDate,
        String productionType,
        List<Product> products,
        String regDate,
        String regNumber
) {
}

record Description(
        String participantInn
) {
}

record Product(
        String certificateDocument,
        String certificateDocumentDate,
        String certificateDocumentNumber,
        String ownerInn,
        String producerInn,
        String productionDate,
        String tnvedCode,
        String uitCode,
        String uituCode
) {
}
